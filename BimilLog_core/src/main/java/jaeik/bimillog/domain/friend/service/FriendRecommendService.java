package jaeik.bimillog.domain.friend.service;

import jaeik.bimillog.domain.friend.algorithm.*;
import jaeik.bimillog.domain.friend.entity.FriendRelation;
import jaeik.bimillog.domain.friend.entity.RecommendedFriend;
import jaeik.bimillog.domain.friend.repository.FriendshipCacheRepository;
import jaeik.bimillog.domain.friend.repository.InteractionScoreCacheRepository;
import jaeik.bimillog.domain.member.out.MemberQueryAdapter;
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
 *
 * @author Jaeik
 * @version 2.1.0
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class FriendRecommendService {
    private final FriendshipCacheRepository friendshipCacheRepository;
    private final InteractionScoreCacheRepository interactionScoreCacheRepository;
    private final MemberQueryAdapter memberQueryAdapter;
    private final BreadthFirstSearch breadthFirstSearch;
    private final UnionFind unionFind;
    private final FriendRecommendScorer scorer;

    public static final int RECOMMEND_LIMIT = 10;
    public static final int DEPTH_SECOND = 2;
    public static final int DEPTH_THIRD = 3;

    @Transactional(readOnly = true)
    public Page<RecommendedFriend> getRecommendFriendList(Long memberId, Pageable pageable) {
        log.info("친구 추천 시작: memberId={}", memberId);

        // 1. 초기 데이터 수집 (1촌 및 블랙리스트)
        Set<Long> firstDegree = friendshipCacheRepository.getFriends(memberId);
        Set<Long> blacklist = getBlacklistIds(memberId);

        // 2. BFS로 친구 관계 탐색 (FriendRelation 사용)
        FriendRelation relation = breadthFirstSearch.findFriendRelation(memberId, firstDegree);

        // 3. 3촌 탐색 결정
        boolean shouldFindThirdDegree = relation.getSecondDegreeIds().size() < RECOMMEND_LIMIT;

        if (shouldFindThirdDegree) {
            relation = breadthFirstSearch.addThirdDegreeRelation(relation);
        }

        // 4. 후보자 ID 수집 (점수 조회를 위해)
        Set<Long> allCandidateIds = new HashSet<>();
        allCandidateIds.addAll(relation.getSecondDegreeIds());
        allCandidateIds.addAll(relation.getThirdDegreeIds());

        // 블랙리스트 제거
        allCandidateIds.removeAll(blacklist);

        // 5. 필요한 후보자들에 대해서만 상호작용 점수 조회 (Batch)
        Map<Long, Double> interactionScores = interactionScoreCacheRepository
                .getInteractionScoresBatch(memberId, allCandidateIds);

        log.debug("후보자 선정 완료: 후보 {}명, 점수 조회 완료", allCandidateIds.size());

        List<RecommendCandidate> candidates = new ArrayList<>();

        // 6. UnionFind 및 점수 계산
        Map<Long, Set<Long>> commonFriendGroups = unionFind.buildCommonFriendGroups(relation.getSecondDegreeConnections());

        // 6-1. 2촌 점수 계산
        buildCandidatesFromSecondDegree(
                relation.getSecondDegreeConnections(),
                blacklist,
                commonFriendGroups,
                interactionScores,
                candidates
        );

        // 6-2. 3촌 점수 계산 (필요시)
        if (shouldFindThirdDegree) {
            buildCandidatesFromThirdDegree(
                    relation.getThirdDegreeConnections(),
                    blacklist,
                    commonFriendGroups,
                    interactionScores,
                    candidates
            );
        }

        // 7. 정렬 및 Top 10 추출
        List<RecommendCandidate> topCandidates = candidates.stream()
                .sorted(Comparator.comparing(RecommendCandidate::getTotalScore).reversed())
                .limit(RECOMMEND_LIMIT)
                .collect(Collectors.toList());

        // 8. 부족 시 최근 가입자 채우기
        if (topCandidates.size() < RECOMMEND_LIMIT) {
            fillWithRecentMembers(memberId, firstDegree, blacklist, topCandidates, interactionScores);
        }

        // 9. 변환 및 반환
        List<RecommendedFriend> recommendedFriends = convertToRecommendedFriends(topCandidates);
        log.info("친구 추천 완료: memberId={}, 추천 수={}", memberId, recommendedFriends.size());

        return new PageImpl<>(recommendedFriends, pageable, recommendedFriends.size());
    }

    private void fillWithRecentMembers(Long memberId, Set<Long> firstDegree, Set<Long> blacklist,
                                       List<RecommendCandidate> topCandidates, Map<Long, Double> interactionScores) {
        int remaining = RECOMMEND_LIMIT - topCandidates.size();
        Set<Long> excludeIds = new HashSet<>();
        excludeIds.add(memberId);
        excludeIds.addAll(firstDegree);
        excludeIds.addAll(blacklist);
        topCandidates.forEach(c -> excludeIds.add(c.getMemberId()));

        List<Long> recentMembers = memberQueryAdapter.findRecentMembers(excludeIds, remaining);

        // 최근 가입자들에 대해서도 상호작용 점수가 있는지 확인
        Set<Long> newRecentIds = recentMembers.stream()
                .filter(id -> !interactionScores.containsKey(id))
                .collect(Collectors.toSet());

        if (!newRecentIds.isEmpty()) {
            Map<Long, Double> recentScores = interactionScoreCacheRepository.getInteractionScoresBatch(memberId, newRecentIds);
            interactionScores.putAll(recentScores);
        }

        for (Long recentMemberId : recentMembers) {
            Double score = interactionScores.getOrDefault(recentMemberId, 0.0);
            RecommendCandidate candidate = RecommendCandidate.builder()
                    .memberId(recentMemberId)
                    .depth(0)
                    .interactionScore(score)
                    .build();
            scorer.calculateAndSetScore(candidate);
            topCandidates.add(candidate);
        }
    }

    /**
     * <h3>2촌 후보 생성</h3>
     * <p>2촌 관계에서 추천 후보를 생성하고 점수를 계산합니다.</p>
     */
    private void buildCandidatesFromSecondDegree(
            Map<Long, Set<Long>> secondDegreeMap,
            Set<Long> blacklist,
            Map<Long, Set<Long>> commonFriendGroups,
            Map<Long, Double> interactionScores,
            List<RecommendCandidate> candidates) {

        for (Map.Entry<Long, Set<Long>> entry : secondDegreeMap.entrySet()) {
            Long candidateId = entry.getKey();
            if (blacklist.contains(candidateId)) continue;

            Set<Long> commonFriends = commonFriendGroups.getOrDefault(candidateId, new HashSet<>());
            Double score = interactionScores.getOrDefault(candidateId, 0.0);

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
     * <p>3촌 관계에서 추천 후보를 생성하고 점수를 계산합니다.</p>
     */
    private void buildCandidatesFromThirdDegree(
            Map<Long, Set<Long>> thirdDegreeMap,
            Set<Long> blacklist,
            Map<Long, Set<Long>> commonFriendGroups,
            Map<Long, Double> interactionScores,
            List<RecommendCandidate> candidates) {

        for (Map.Entry<Long, Set<Long>> entry : thirdDegreeMap.entrySet()) {
            Long candidateId = entry.getKey();
            if (blacklist.contains(candidateId)) continue;

            Set<Long> connectedSecondDegree = entry.getValue();

            int totalCommonFriendCount = 0;
            for (Long secondDegreeId : connectedSecondDegree) {
                Set<Long> secondsCommons = commonFriendGroups.getOrDefault(secondDegreeId, Collections.emptySet());
                totalCommonFriendCount += secondsCommons.size();
            }

            Set<Long> virtualCommonFriends = new HashSet<>();
            for (int i = 0; i < totalCommonFriendCount; i++) virtualCommonFriends.add((long) i);

            Double score = interactionScores.getOrDefault(candidateId, 0.0);

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
     * <h3>블랙리스트 ID 조회 (최적화)</h3>
     * <p>QueryDSL을 사용하여 ID만 직접 조회하여 N+1 문제를 방지합니다.</p>
     * <p>데이터베이스 접근 실패 시 예외를 전파하여 안전성을 보장합니다.</p>
     *
     * @param memberId 회원 ID
     * @return 블랙리스트 회원 ID 집합
     * @throws org.springframework.dao.DataAccessException 데이터베이스 접근 실패 시
     */
    private Set<Long> getBlacklistIds(Long memberId) {
        return memberQueryAdapter.findBlacklistIdsByRequestMemberId(memberId);
    }

    private List<RecommendedFriend> convertToRecommendedFriends(List<RecommendCandidate> candidates) {
        List<Long> candidateIds = candidates.stream()
                .map(RecommendCandidate::getMemberId)
                .collect(Collectors.toList());

        Set<Long> acquaintanceIds = new HashSet<>();
        for (RecommendCandidate candidate : candidates) {
            if (candidate.getDepth() == DEPTH_SECOND && candidate.getAcquaintanceId() != null) {
                acquaintanceIds.add(candidate.getAcquaintanceId());
            }
        }

        List<RecommendedFriend.RecommendedFriendInfo> friendInfos =
                memberQueryAdapter.addRecommendedFriendInfo(candidateIds);
        List<RecommendedFriend.AcquaintanceInfo> acquaintanceInfos =
                memberQueryAdapter.addAcquaintanceInfo(new ArrayList<>(acquaintanceIds));

        Map<Long, RecommendedFriend.RecommendedFriendInfo> friendInfoMap = friendInfos.stream()
                .collect(Collectors.toMap(RecommendedFriend.RecommendedFriendInfo::friendMemberId, info -> info));
        Map<Long, RecommendedFriend.AcquaintanceInfo> acquaintanceInfoMap = acquaintanceInfos.stream()
                .collect(Collectors.toMap(RecommendedFriend.AcquaintanceInfo::acquaintanceId, info -> info));

        List<RecommendedFriend> recommendedFriends = new ArrayList<>();
        for (RecommendCandidate candidate : candidates) {
            RecommendedFriend.RecommendedFriendInfo friendInfo = friendInfoMap.get(candidate.getMemberId());
            if (friendInfo == null) continue;

            Long acquaintanceId = null;
            boolean manyAcquaintance = false;

            if (candidate.getDepth() == DEPTH_SECOND && candidate.getAcquaintanceId() != null) {
                acquaintanceId = candidate.getAcquaintanceId();
                manyAcquaintance = candidate.isManyAcquaintance();
            }

            RecommendedFriend recommendedFriend = new RecommendedFriend(
                    candidate.getMemberId(), acquaintanceId, manyAcquaintance, candidate.getDepth()
            );
            recommendedFriend.setRecommendedFriendName(friendInfo);

            if (candidate.getDepth() == DEPTH_SECOND && acquaintanceId != null) {
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