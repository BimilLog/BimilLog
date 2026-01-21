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

/**
 * <h2>친구 추천 서비스</h2>
 * <p>
 * BFS(너비 우선 탐색) 기반의 친구 추천 알고리즘을 구현합니다.
 * 2촌(친구의 친구) → 3촌 순서로 탐색하며, 공통 친구 수와 상호작용 점수를 기반으로
 * 추천 우선순위를 계산합니다.
 * </p>
 *
 * <h3>추천 알고리즘 흐름:</h3>
 * <ol>
 *   <li>내 친구(1촌) 목록 조회 (Redis)</li>
 *   <li>2촌 탐색: 친구의 친구 중 나와 친구가 아닌 사람 탐색</li>
 *   <li>3촌 탐색: 2촌이 부족할 경우 추가 탐색</li>
 *   <li>상호작용 점수 주입 (Redis)</li>
 *   <li>총점 기준 정렬 및 상위 N명 추출</li>
 *   <li>부족 시 최근 가입자로 보충</li>
 *   <li>블랙리스트 필터링</li>
 * </ol>
 *
 * @version 2.6.0
 * @author Jaeik
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class FriendRecommendService {
    private final RedisFriendshipRepository redisFriendshipRepository;
    private final RedisInteractionScoreRepository redisInteractionScoreRepository;
    private final MemberQueryRepository memberQueryRepository;
    private final MemberRepository memberRepository;
    private final MemberBlacklistRepository memberBlacklistRepository;

    // 1촌 조회 시 최대 스캔 수
    private static final int FIRST_FRIEND_SCAN_LIMIT = 200;

    // 최종 추천 인원 수
    private static final int RECOMMEND_LIMIT = 10;

    /**
     * 친구 추천 목록을 조회합니다.
     * <p>
     * 2촌, 3촌 순서로 BFS 탐색을 수행하고, 공통 친구 수와 상호작용 점수를 기반으로
     * 추천 우선순위를 계산하여 상위 N명을 반환합니다.
     * </p>
     *
     * @param memberId 추천을 요청한 회원 ID
     * @param pageable 페이지네이션 정보
     * @return 추천 친구 목록 (페이지)
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

    /**
     * <h3>BFS 기반으로 2촌, 3촌 후보자를 탐색하고 점수를 계산합니다.</h3>
     * <p>
     * <b>2촌 탐색:</b> 내 친구들의 친구 목록을 조회하여, 나와 친구가 아닌 사람을 2촌으로 분류합니다.
     * 이때 공통 친구 수를 카운트하여 점수 계산에 사용합니다.
     * </p>
     * <p>
     * <b>3촌 탐색:</b> 2촌 후보가 부족할 경우에만 수행됩니다.
     * 2촌의 친구 중 나와 친구가 아니고 2촌도 아닌 사람을 3촌으로 분류합니다.
     * 연결 고리가 되는 2촌의 공통 친구 수를 기반으로 가산점을 부여합니다.
     * </p>
     *
     * @param memberId  현재 회원 ID
     * @param myFriends 내 친구(1촌) ID 집합
     * @return 추천 후보자 목록 (점수 계산 완료)
     */
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

    /**
     * <h3>후보자들에게 상호작용 점수를 주입합니다.</h3>
     * <p>
     * Redis에서 해당 회원과 후보자들 간의 상호작용 점수를 일괄 조회하여
     * 각 후보자 객체에 설정합니다. 상호작용 점수는 롤링페이퍼 작성, 게시글 좋아요 등
     * 과거 활동 이력을 기반으로 산정됩니다.
     * </p>
     *
     * @param memberId   현재 회원 ID
     * @param candidates 추천 후보자 목록
     */
    private void injectInteractionScores(Long memberId, List<RecommendCandidate> candidates) {
        if (candidates.isEmpty()) return;
        Set<Long> candidateIds = candidates.stream().map(RecommendCandidate::getMemberId).collect(Collectors.toSet());
        Map<Long, Double> scores = redisInteractionScoreRepository.getInteractionScoresBatch(memberId, candidateIds);
        candidates.forEach(c -> c.setInteractionScore(scores.getOrDefault(c.getMemberId(), 0.0)));
    }

    /**
     * <h3>부족한 추천 인원을 보충하고 블랙리스트를 필터링합니다.</h3>
     * <p>
     * 추천 후보가 {@link #RECOMMEND_LIMIT} 미만인 경우 다음 순서로 보충합니다:
     * <ol>
     *   <li>상호작용 점수가 있는 회원 추가</li>
     *   <li>최근 가입자 추가</li>
     * </ol>
     * 마지막으로 블랙리스트에 등록된 회원을 제거합니다.
     * </p>
     *
     * @param memberId   현재 회원 ID
     * @param myFriends  내 친구(1촌) ID 집합 (제외 대상)
     * @param candidates 추천 후보자 목록 (수정됨)
     */
    private void fillAndFilter(Long memberId, Set<Long> myFriends, List<RecommendCandidate> candidates) {
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

        // 블랙리스트 제거 (마지막에 처리)
        Set<Long> blacklist = memberBlacklistRepository.findBlacklistIdsByRequestMemberId(memberId);
        candidates.removeIf(c -> blacklist.contains(c.getMemberId()));
    }

    /**
     * <h3>추천 후보자 목록을 응답 DTO 페이지로 변환합니다.</h3>
     * <p>
     * 후보자들의 회원 정보(닉네임, 프로필 이미지 등)와 함께 아는 친구(acquaintance) 정보를
     * 일괄 조회하여 {@link RecommendedFriend} 객체로 매핑합니다.
     * </p>
     *
     * @param candidates 추천 후보자 목록
     * @param pageable   페이지네이션 정보
     * @return 추천 친구 응답 페이지
     */
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