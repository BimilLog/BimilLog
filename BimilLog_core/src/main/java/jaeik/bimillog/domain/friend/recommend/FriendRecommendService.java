package jaeik.bimillog.domain.friend.recommend;

import jaeik.bimillog.domain.friend.entity.RecommendedFriend;
import jaeik.bimillog.domain.member.repository.MemberBlacklistRepository;
import jaeik.bimillog.domain.member.repository.MemberQueryRepository;
import jaeik.bimillog.domain.member.repository.MemberRepository;
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
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class FriendRecommendService {

    private final RedisFriendshipRepository redisFriendshipRepository;
    private final RedisInteractionScoreRepository redisInteractionScoreRepository;
    private final MemberQueryRepository memberQueryRepository;
    private final MemberRepository memberRepository;
    private final MemberBlacklistRepository memberBlacklistRepository;

    private static final int FIRST_FRIEND_SCAN_LIMIT = 200;
    private static final int RECOMMEND_LIMIT = 10;

    /**
     * 친구 추천 메인 흐름
     */
    @Transactional(readOnly = true)
    public Page<RecommendedFriend> getRecommendFriendList(Long memberId, Pageable pageable) {
        // 1. 내 친구(1촌) 조회
        Set<Long> myFriends = redisFriendshipRepository.getFriends(memberId, FIRST_FRIEND_SCAN_LIMIT);

        // 2. 후보자 탐색 (2촌 -> 3촌 순차 확장) 및 점수 계산
        List<RecommendCandidate> candidates = findAndScoreCandidates(memberId, myFriends);

        // 3. 상호작용 점수 주입
        injectInteractionScores(memberId, candidates);

        // 4. 점수순 정렬 및 상위 N명 추출
        candidates.sort(Comparator.comparingDouble(RecommendCandidate::calculateTotalScore).reversed());
        List<RecommendCandidate> topCandidates = candidates.stream().limit(RECOMMEND_LIMIT).collect(Collectors.toList());

        // 5. 부족한 인원 보충 (최근 가입자) 및 블랙리스트 필터링
        fillAndFilter(memberId, myFriends, topCandidates);

        // 6. 회원 정보 조회 및 응답 DTO 변환
        return toResponsePage(topCandidates, pageable);
    }

    // --- [Core Logic] BFS 탐색 & 점수 계산 ---

    private List<RecommendCandidate> findAndScoreCandidates(Long memberId, Set<Long> myFriends) {
        Map<Long, RecommendCandidate> candidateMap = new HashMap<>();

        // [1촌이 없는 경우] -> 바로 상호작용 기반 추천으로 점프하기 위해 빈 리스트 반환 (혹은 로직 분기)
        if (myFriends.isEmpty()) return new ArrayList<>();

        // A. 2촌 탐색
        Map<Long, Set<Long>> friendsOfFriends = redisFriendshipRepository.getFriendsBatch(myFriends);
        friendsOfFriends.forEach((friendId, friendsSet) -> {
            if (friendsSet == null) return;
            for (Long targetId : friendsSet) {
                if (targetId.equals(memberId) || myFriends.contains(targetId)) continue; // 나 자신이거나 이미 친구면 제외

                candidateMap.computeIfAbsent(targetId, id -> RecommendCandidate.of(id, 2))
                        .addCommonFriend(friendId); // 공통친구(friendId) 추가
            }
        });

        // B. 3촌 탐색 (2촌이 부족할 경우에만 수행)
        if (candidateMap.size() < RECOMMEND_LIMIT) {
            Set<Long> secondDegreeIds = candidateMap.keySet();
            Map<Long, Set<Long>> friendsOfSeconds = redisFriendshipRepository.getFriendsBatchForThirdDegree(secondDegreeIds);

            friendsOfSeconds.forEach((secondDegreeId, friendsSet) -> {
                if (friendsSet == null) return;
                for (Long targetId : friendsSet) {
                    if (targetId.equals(memberId) || myFriends.contains(targetId) || candidateMap.containsKey(targetId)) continue;

                    // 3촌은 '연결고리가 되는 2촌'이 가지고 있는 '나와의 공통친구 수'를 더함
                    int bridgeScore = candidateMap.get(secondDegreeId).getCommonFriends().size();
                    candidateMap.computeIfAbsent(targetId, id -> RecommendCandidate.of(id, 3))
                            .addVirtualScore(bridgeScore);
                }
            });
        }

        return new ArrayList<>(candidateMap.values());
    }

    // --- [Helper] 보조 로직들 ---

    private void injectInteractionScores(Long memberId, List<RecommendCandidate> candidates) {
        if (candidates.isEmpty()) return;
        Set<Long> candidateIds = candidates.stream().map(RecommendCandidate::getMemberId).collect(Collectors.toSet());
        Map<Long, Double> scores = redisInteractionScoreRepository.getInteractionScoresBatch(memberId, candidateIds);
        candidates.forEach(c -> c.setInteractionScore(scores.getOrDefault(c.getMemberId(), 0.0)));
    }

    private void fillAndFilter(Long memberId, Set<Long> myFriends, List<RecommendCandidate> candidates) {
        // 블랙리스트 제거
        Set<Long> blacklist = memberBlacklistRepository.findBlacklistIdsByRequestMemberId(memberId);
        candidates.removeIf(c -> blacklist.contains(c.getMemberId()));

        // 부족하면 최근 가입자 or 상호작용 점수 높은 사람으로 채우기
        if (candidates.size() < RECOMMEND_LIMIT) {
            Set<Long> excludeIds = new HashSet<>(myFriends);
            excludeIds.add(memberId);
            candidates.forEach(c -> excludeIds.add(c.getMemberId()));

            // 상호작용 점수 전체 조회 (Fallback)
            Map<Long, Double> allInteractions = redisInteractionScoreRepository.getAllInteractionScores(memberId);

            // 1. 상호작용 점수 기반 추가
            allInteractions.forEach((id, score) -> {
                if (candidates.size() >= RECOMMEND_LIMIT) return;
                if (!excludeIds.contains(id)) {
                    candidates.add(RecommendCandidate.builder().memberId(id).interactionScore(score).depth(0).build());
                    excludeIds.add(id);
                }
            });

            // 2. 그래도 부족하면 최근 가입자 추가
            if (candidates.size() < RECOMMEND_LIMIT) {
                List<Long> newMembers = memberRepository.findIdByIdNotInOrderByCreatedAtDesc(
                        excludeIds,
                        org.springframework.data.domain.PageRequest.of(0, RECOMMEND_LIMIT - candidates.size())
                );
                newMembers.forEach(id -> candidates.add(RecommendCandidate.of(id, 0)));
            }
        }
    }

    private Page<RecommendedFriend> toResponsePage(List<RecommendCandidate> candidates, Pageable pageable) {
        if (candidates.isEmpty()) return Page.empty(pageable);

        List<Long> memberIds = candidates.stream().map(RecommendCandidate::getMemberId).toList();

        // 2촌인 경우 표시할 '함께 아는 친구' ID 수집
        Set<Long> acqIds = candidates.stream()
                .map(RecommendCandidate::getAcquaintanceId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

        // Bulk Fetch
        Map<Long, RecommendedFriend.RecommendedFriendInfo> friendInfos = memberQueryRepository.addRecommendedFriendInfo(memberIds).stream()
                .collect(Collectors.toMap(RecommendedFriend.RecommendedFriendInfo::friendMemberId, Function.identity()));

        Map<Long, RecommendedFriend.AcquaintanceInfo> acqInfos = acqIds.isEmpty() ? Collections.emptyMap() :
                memberQueryRepository.addAcquaintanceInfo(new ArrayList<>(acqIds)).stream()
                        .collect(Collectors.toMap(RecommendedFriend.AcquaintanceInfo::acquaintanceId, Function.identity()));

        // Mapping
        List<RecommendedFriend> result = candidates.stream()
                .filter(c -> friendInfos.containsKey(c.getMemberId()))
                .map(c -> {
                    RecommendedFriend.RecommendedFriendInfo info = friendInfos.get(c.getMemberId());
                    RecommendedFriend.AcquaintanceInfo acqInfo = acqInfos.get(c.getAcquaintanceId());

                    RecommendedFriend friend = new RecommendedFriend(c.getMemberId(), c.getAcquaintanceId(), c.isManyAcquaintance(), c.getDepth());
                    friend.setRecommendedFriendName(info);
                    if (acqInfo != null) friend.setAcquaintanceFriendName(acqInfo);
                    return friend;
                })
                .collect(Collectors.toList());

        return new PageImpl<>(result, pageable, result.size());
    }
}