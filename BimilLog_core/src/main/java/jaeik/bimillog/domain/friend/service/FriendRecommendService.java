package jaeik.bimillog.domain.friend.service;

import jaeik.bimillog.domain.friend.algorithm.*;
import jaeik.bimillog.domain.friend.entity.FriendRelation;
import jaeik.bimillog.domain.friend.entity.RecommendedFriend;
import jaeik.bimillog.domain.member.out.MemberQueryRepository;
import jaeik.bimillog.infrastructure.redis.friend.RedisFriendshipRepository;
import jaeik.bimillog.infrastructure.redis.friend.RedisInteractionScoreRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

/**
 * <h2>친구 추천 서비스 (최적화 버전)</h2>
 * <p>Redis Pipelining 및 Lazy Loading을 적용하여 성능을 개선했습니다.</p>
 * <p>FriendRelation.CandidateInfo 내부 클래스를 활용하여 타입 안전성을 향상시켰습니다.</p>
 *
 * @author Jaeik
 * @version 2.2.0
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class FriendRecommendService {
    private final RedisFriendshipRepository redisFriendshipRepository;
    private final RedisInteractionScoreRepository redisInteractionScoreRepository;
    private final MemberQueryRepository memberQueryRepository;
    private final BreadthFirstSearch breadthFirstSearch;
    private final FriendRecommendScorer scorer;

    public static final int RECOMMEND_LIMIT = 10;
    public static final int DEPTH_SECOND = 2;
    public static final int DEPTH_THIRD = 3;

    @Transactional(readOnly = true)
    public Page<RecommendedFriend> getRecommendFriendList(Long memberId, Pageable pageable) {
        log.info("친구 추천 시작: memberId={}", memberId);

        // 1. Redis에서 1촌 목록 및 블랙리스트 조회
        Set<Long> firstDegree = redisFriendshipRepository.getFriends(memberId);
        Set<Long> blacklist = getBlacklistIds(memberId);

        // 2. BFS로 2촌 관계 탐색 (Redis Pipeline 활용)
        FriendRelation relation = breadthFirstSearch.findFriendRelation(memberId, firstDegree);

        // 3. 2촌이 부족하면 3촌까지 확장 탐색 (목표: 10명 이상)
        boolean shouldFindThirdDegree = relation.getSecondDegreeIds().size() < RECOMMEND_LIMIT;

        if (shouldFindThirdDegree) {
            relation = breadthFirstSearch.addThirdDegreeRelation(relation);
        }

        // 4. 후보자 ID 수집 및 블랙리스트 제거
        Set<Long> allCandidateIds = relation.getAllCandidateIds();
        allCandidateIds.removeAll(blacklist);

        // 5. Redis Pipeline으로 상호작용 점수 일괄 조회 (좋아요, 댓글 등)
        Map<Long, Double> interactionScores = redisInteractionScoreRepository
                .getInteractionScoresBatch(memberId, allCandidateIds);

        // 6. 각 후보자에게 상호작용 점수 설정
        for (Map.Entry<Long, Double> entry : interactionScores.entrySet()) {
            relation.setInteractionScore(entry.getKey(), entry.getValue());
        }

        log.debug("후보자 선정 완료: 후보 {}명, 점수 조회 완료", allCandidateIds.size());

        // 7. 공통 친구 정보 초기화
        relation.initializeCommonFriends();

        List<RecommendCandidate> candidates = new ArrayList<>();

        // 8. 2촌 후보 점수 계산 (기본 50점 + 공통친구 2점 + 상호작용 최대 10점)
        buildCandidatesFromSecondDegree(relation, blacklist, candidates);

        // 9. 3촌 후보 점수 계산 (기본 20점 + 공통친구 0.5점 + 상호작용 최대 10점)
        if (shouldFindThirdDegree) {
            buildCandidatesFromThirdDegree(relation, blacklist, candidates);
        }

        // 10. 총점 기준 정렬 후 상위 10명 선택
        List<RecommendCandidate> topCandidates = candidates.stream()
                .sorted(Comparator.comparing(RecommendCandidate::getTotalScore).reversed())
                .limit(RECOMMEND_LIMIT)
                .collect(Collectors.toList());

        // 11. 10명 미만일 경우 최근 가입자로 채우기
        if (topCandidates.size() < RECOMMEND_LIMIT) {
            fillWithRecentMembers(memberId, firstDegree, blacklist, topCandidates);
        }

        // 12. DTO 변환 (회원 정보 조회 포함) 및 반환
        List<RecommendedFriend> recommendedFriends = convertToRecommendedFriends(topCandidates);
        log.info("친구 추천 완료: memberId={}, 추천 수={}", memberId, recommendedFriends.size());

        return new PageImpl<>(recommendedFriends, pageable, recommendedFriends.size());
    }

    private void fillWithRecentMembers(Long memberId, Set<Long> firstDegree, Set<Long> blacklist,
                                       List<RecommendCandidate> topCandidates) {
        int remaining = RECOMMEND_LIMIT - topCandidates.size();
        Set<Long> excludeIds = new HashSet<>();
        excludeIds.add(memberId);
        excludeIds.addAll(firstDegree);
        excludeIds.addAll(blacklist);
        topCandidates.forEach(c -> excludeIds.add(c.getMemberId()));

        List<Long> recentMembers = memberQueryRepository.findRecentMembers(excludeIds, remaining);

        // 최근 가입자들에 대해 상호작용 점수 조회
        Set<Long> recentIds = new HashSet<>(recentMembers);
        Map<Long, Double> recentScores = redisInteractionScoreRepository.getInteractionScoresBatch(memberId, recentIds);

        for (Long recentMemberId : recentMembers) {
            Double score = recentScores.getOrDefault(recentMemberId, 0.0);
            RecommendCandidate candidate = RecommendCandidate.builder()
                    .memberId(recentMemberId)
                    .depth(null)  // 최근 가입자는 2촌/3촌이 아니므로 null
                    .interactionScore(score)
                    .build();
            scorer.calculateAndSetScore(candidate);
            topCandidates.add(candidate);
        }
    }

    /**
     * <h3>2촌 후보 생성</h3>
     * <p>FriendRelation의 2촌 후보자 정보를 사용하여 추천 후보를 생성합니다.</p>
     */
    private void buildCandidatesFromSecondDegree(
            FriendRelation relation,
            Set<Long> blacklist,
            List<RecommendCandidate> candidates) {

        for (FriendRelation.CandidateInfo candidateInfo : relation.getSecondDegreeCandidates()) {
            Long candidateId = candidateInfo.getCandidateId();
            if (blacklist.contains(candidateId)) continue;

            Set<Long> commonFriends = candidateInfo.getCommonFriendIds();
            Double score = candidateInfo.getInteractionScore();

            RecommendCandidate candidate = RecommendCandidate.builder()
                    .memberId(candidateId)
                    .depth(DEPTH_SECOND)
                    .commonFriends(commonFriends)
                    .interactionScore(score)
                    .build();

            commonFriends.forEach(candidate::addCommonFriend);
            scorer.calculateAndSetScore(candidate);
            candidates.add(candidate);
        }
    }

    /**
     * <h3>3촌 후보 생성</h3>
     * <p>FriendRelation의 3촌 후보자 정보를 사용하여 추천 후보를 생성합니다.</p>
     */
    private void buildCandidatesFromThirdDegree(
            FriendRelation relation,
            Set<Long> blacklist,
            List<RecommendCandidate> candidates) {

        // 2촌의 공통 친구 정보를 Map으로 변환 (3촌 점수 계산용)
        Map<Long, Set<Long>> secondDegreeCommonFriendsMap = new HashMap<>();
        for (FriendRelation.CandidateInfo secondDegreeCandidate : relation.getSecondDegreeCandidates()) {
            secondDegreeCommonFriendsMap.put(
                    secondDegreeCandidate.getCandidateId(),
                    secondDegreeCandidate.getCommonFriendIds()
            );
        }

        for (FriendRelation.CandidateInfo candidateInfo : relation.getThirdDegreeCandidates()) {
            Long candidateId = candidateInfo.getCandidateId();
            if (blacklist.contains(candidateId)) continue;

            Set<Long> connectedSecondDegree = candidateInfo.getBridgeFriendIds();

            int totalCommonFriendCount = 0;
            for (Long secondDegreeId : connectedSecondDegree) {
                Set<Long> secondsCommons = secondDegreeCommonFriendsMap.getOrDefault(secondDegreeId, Collections.emptySet());
                totalCommonFriendCount += secondsCommons.size();
            }

            Set<Long> virtualCommonFriends = new HashSet<>();
            for (int i = 0; i < totalCommonFriendCount; i++) virtualCommonFriends.add((long) i);

            Double score = candidateInfo.getInteractionScore();

            RecommendCandidate candidate = RecommendCandidate.builder()
                    .memberId(candidateId)
                    .depth(DEPTH_THIRD)
                    .commonFriends(virtualCommonFriends)
                    .interactionScore(score)
                    .build();

            scorer.calculateAndSetScore(candidate);
            candidates.add(candidate);
        }
    }

    /**
     * <h3>블랙리스트 ID 조회</h3>
     *
     * @param memberId 회원 ID
     * @return 블랙리스트 회원 ID 집합
     */
    private Set<Long> getBlacklistIds(Long memberId) {
        return memberQueryRepository.findBlacklistIdsByRequestMemberId(memberId);
    }

    private List<RecommendedFriend> convertToRecommendedFriends(List<RecommendCandidate> candidates) {
        List<Long> candidateIds = candidates.stream()
                .map(RecommendCandidate::getMemberId)
                .collect(Collectors.toList());

        Set<Long> acquaintanceIds = new HashSet<>();
        for (RecommendCandidate candidate : candidates) {
            Integer depth = candidate.getDepth();
            if (depth != null && depth == DEPTH_SECOND && candidate.getAcquaintanceId() != null) {
                acquaintanceIds.add(candidate.getAcquaintanceId());
            }
        }

        List<RecommendedFriend.RecommendedFriendInfo> friendInfos =
                memberQueryRepository.addRecommendedFriendInfo(candidateIds);
        List<RecommendedFriend.AcquaintanceInfo> acquaintanceInfos =
                memberQueryRepository.addAcquaintanceInfo(new ArrayList<>(acquaintanceIds));

        Map<Long, RecommendedFriend.RecommendedFriendInfo> friendInfoMap = friendInfos.stream()
                .collect(Collectors.toMap(RecommendedFriend.RecommendedFriendInfo::friendMemberId, info -> info));
        Map<Long, RecommendedFriend.AcquaintanceInfo> acquaintanceInfoMap = acquaintanceInfos.stream()
                .collect(Collectors.toMap(RecommendedFriend.AcquaintanceInfo::acquaintanceId, info -> info));

        List<RecommendedFriend> recommendedFriends = new ArrayList<>();
        for (RecommendCandidate candidate : candidates) {
            RecommendedFriend.RecommendedFriendInfo friendInfo = friendInfoMap.get(candidate.getMemberId());
            if (friendInfo == null) continue;

            Integer depth = candidate.getDepth();
            Long acquaintanceId = null;
            boolean manyAcquaintance = false;

            if (depth != null && depth == DEPTH_SECOND && candidate.getAcquaintanceId() != null) {
                acquaintanceId = candidate.getAcquaintanceId();
                manyAcquaintance = candidate.isManyAcquaintance();
            }

            RecommendedFriend recommendedFriend = new RecommendedFriend(
                    candidate.getMemberId(), acquaintanceId, manyAcquaintance, depth
            );
            recommendedFriend.setRecommendedFriendName(friendInfo);

            if (depth != null && depth == DEPTH_SECOND && acquaintanceId != null) {
                RecommendedFriend.AcquaintanceInfo acquaintanceInfo = acquaintanceInfoMap.get(acquaintanceId);
                if (acquaintanceInfo != null) {
                    recommendedFriend.setAcquaintanceFriendName(acquaintanceInfo);
                }
            }
            recommendedFriends.add(recommendedFriend);
        }
        return recommendedFriends;
    }
}