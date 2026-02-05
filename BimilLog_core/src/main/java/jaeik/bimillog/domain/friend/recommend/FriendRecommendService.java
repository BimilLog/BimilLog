package jaeik.bimillog.domain.friend.recommend;

import jaeik.bimillog.domain.friend.entity.RecommendedFriend;
import jaeik.bimillog.domain.friend.repository.FriendshipQueryRepository;
import jaeik.bimillog.domain.member.repository.MemberBlacklistRepository;
import jaeik.bimillog.domain.member.repository.MemberQueryRepository;
import jaeik.bimillog.domain.member.repository.MemberRepository;
import jaeik.bimillog.infrastructure.exception.CustomException;
import jaeik.bimillog.infrastructure.exception.ErrorCode;
import jaeik.bimillog.infrastructure.redis.friend.RedisFriendshipRepository;
import jaeik.bimillog.infrastructure.redis.friend.RedisInteractionScoreRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.redis.core.ZSetOperations.TypedTuple;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

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
 * @author Jaeik
 * @version 2.6.0
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class FriendRecommendService {
    private final RedisFriendshipRepository redisFriendshipRepository;
    private final RedisInteractionScoreRepository redisInteractionScoreRepository;
    private final FriendshipQueryRepository friendshipQueryRepository;
    private final MemberQueryRepository memberQueryRepository;
    private final MemberRepository memberRepository;
    private final MemberBlacklistRepository memberBlacklistRepository;

    private static final int RECOMMEND_LIMIT = 10;
    private static final int FIRST_FRIEND_SCAN_LIMIT = 50;
    private static final int SECOND_DEGREE_SAMPLE_SIZE = 30;
    private static final int THIRD_DEGREE_SAMPLE_SIZE = 100;

    /**
     * 친구 추천 목록을 조회합니다.
     *
     * @param memberId 추천을 요청한 회원 ID
     * @param pageable 페이지네이션 정보
     * @return 추천 친구 목록 (페이지)
     */
    @Transactional(readOnly = true)
    public Page<RecommendedFriend> getRecommendFriendList(Long memberId, Pageable pageable) {
        try {
            return getRecommendedFriends(memberId, pageable, true);
        } catch (Exception e) {
            log.error("Redis 실패로 DB 폴백 실행: memberId={}", memberId, e);
            try {
                return getRecommendedFriends(memberId, pageable, false);
            } catch (Exception dbe) {
                throw new CustomException(ErrorCode.FRIEND_RECOMMEND_FAIL);
            }
        }
    }

    private Page<RecommendedFriend> getRecommendedFriends(Long memberId, Pageable pageable, boolean useRedis) {
        List<RecommendCandidate> candidates = new ArrayList<>();

        // 1. 1촌 조회
        Set<Long> myFriends = useRedis
                ? redisFriendshipRepository.getFriends(memberId, FIRST_FRIEND_SCAN_LIMIT)
                : friendshipQueryRepository.getMyFriendIdsSet(memberId, FIRST_FRIEND_SCAN_LIMIT);

        // 2. 후보자 탐색 (2촌 -> 3촌 순차 확장) 및 점수 계산
        Map<Long, RecommendCandidate> candidateMap = findAndScoreCandidates(memberId, myFriends, useRedis);

        // 후보자가 1명이라도 존재하면
        if (!candidateMap.isEmpty()) {
            // 3. 상호작용 점수 주입 (Redis만)
            if (useRedis) {
                injectInteractionScores(memberId, candidateMap);
            }
            candidates = new ArrayList<>(candidateMap.values());
        }

        // 4. 부족한 인원 보충
        if (candidates.size() < RECOMMEND_LIMIT) {
            Set<Long> excludeIds = buildExcludeIds(memberId, myFriends, candidates);

            // 4-1. 상호작용 점수 기반 (Redis만)
            if (useRedis) {
                fillFromInteractionScores(memberId, candidates, excludeIds);
            }

            // 4-2. 최근 가입자
            if (candidates.size() < RECOMMEND_LIMIT) {
                fillFromRecentMembers(candidates, excludeIds);
            }
        }

        // 5. 블랙리스트 필터링
        Set<Long> blacklist = memberBlacklistRepository.findBlacklistIdsByRequestMemberId(memberId);
        candidates.removeIf(c -> blacklist.contains(c.getMemberId()));

        // 6. 최종 정렬 및 상위 N명 추출
        List<RecommendCandidate> topCandidates = candidates.stream()
                .sorted(Comparator.comparingDouble(RecommendCandidate::calculateTotalScore).reversed())
                .limit(RECOMMEND_LIMIT)
                .toList();

        // 7. 회원 정보 조회 및 응답 DTO 변환
        return toResponsePage(topCandidates, pageable);
    }

    /**
     * <h3>2촌 3촌 후보자 탐색</h3>
     * <p>2촌 탐색:내 친구들의 친구 목록을 조회하여, 나와 친구가 아닌 사람을 2촌으로 분류합니다.</p>
     * <p>3촌 탐색  2촌 후보가 부족할 경우에만 수행됩니다.
     * 2촌의 친구 중 나와 친구가 아니고 2촌도 아닌 사람을 3촌으로 분류합니다.
     * 연결 고리가 되는 2촌의 공통 친구 수를 기반으로 가산점을 부여합니다.
     * </p>
     *
     * @param memberId  현재 회원 ID
     * @param myFriends 내 친구(1촌) ID 집합
     * @return 추천 후보자 Map (ID -> 후보자)
     */
    private Map<Long, RecommendCandidate> findAndScoreCandidates(Long memberId, Set<Long> myFriends, boolean useRedis) {
        Map<Long, RecommendCandidate> candidateMap = new HashMap<>();

        // [1촌이 없는 경우] -> 바로 상호작용 기반 추천으로 점프하기 위해 빈 Map 반환
        if (myFriends.isEmpty()) return candidateMap;

        // A. 2촌 탐색 (친구당 랜덤 30명씩)
        List<Long> myFriendList = new ArrayList<>(myFriends);

        // 2촌 결과 가져옴
        List<List<Long>> secondResults = useRedis
                ? toListOfLists(redisFriendshipRepository.getFriendsBatch(myFriendList, SECOND_DEGREE_SAMPLE_SIZE))
                : friendshipQueryRepository.getFriendIdsBatch(myFriendList);
        processDegreeSearch(myFriendList, secondResults, 2, memberId, myFriends, candidateMap);

        // B. 3촌 탐색 (2촌이 10명 이하일 때)
        if (candidateMap.size() < RECOMMEND_LIMIT) {
            // 2촌의 ID들
            List<Long> secondDegreeList = new ArrayList<>(candidateMap.keySet());

            // 2촌의 ID로 3촌 친구들을 불러옴
            List<List<Long>> thirdResults = useRedis
                    ? toListOfLists(redisFriendshipRepository.getFriendsBatch(secondDegreeList, THIRD_DEGREE_SAMPLE_SIZE))
                    : friendshipQueryRepository.getFriendIdsBatch(secondDegreeList);
            processDegreeSearch(secondDegreeList, thirdResults, 3, memberId, myFriends, candidateMap);
        }

        return candidateMap;
    }

    /**
     * 2촌/3촌 탐색 공통 처리
     */
    private void processDegreeSearch(List<Long> friendIdList, List<List<Long>> results, int depth,
                                     Long memberId, Set<Long> myFriends, Map<Long, RecommendCandidate> candidateMap) {
        for (int i = 0; i < friendIdList.size(); i++) {
            Long friendId = friendIdList.get(i); // 1촌 또는 2촌 친구
            List<Long> resultList = results.get(i); // 1촌의 친구 (2촌) 또는 2촌의 친구 (3촌)

            for (Long targetId : resultList) {
                // 중복제거: 나 자신이거나 1촌친구에 있는 경우
                if (targetId.equals(memberId) || myFriends.contains(targetId)) {
                    continue;
                }

                // 친구ID로 상세 정보 가져오기
                RecommendCandidate candidate = candidateMap.get(targetId);

                if (candidate != null) {
                    // 3촌 탐색 시 이미 2촌으로 등록된 경우 건너뛰기
                    // 3촌의 경우 2촌 중복은 따로 계산해야함 왜냐하면 Map에서 2촌과 3촌이 섞여있고 객체내부 depth로만 구분할 수 있기 때문
                    if (depth == 3 && candidate.getDepth() == 2) {
                        continue;
                    }
                } else {
                    double parentScore = 0;
                    if (depth == 3) {
                        // 3촌은 부모 2촌의 점수를 4분의 1 이어받음 3촌은 친구ID를 기록하지 않음
                        parentScore = candidateMap.get(friendId).getCommonScore();
                    }

                    // 등록되지 않았으면 새로운 상세 정보 생성
                    candidate = RecommendCandidate.initialCandidate(targetId, depth, parentScore, 0);
                    candidateMap.put(targetId, candidate);
                }

                // 등록 여부와 상관없이 공통친구 추가 및 점수 증가
                candidate.addCommonFriendAndScore((depth == 2) ? friendId : null);
            }
        }
    }

    /**
     * <h3>후보자들에게 상호작용 점수를 주입합니다.</h3>
     * <p>상호작용점수는 최대 1500명까지 가능하기에 500명씩 배치로 파이프라인</p>
     * <p>DB의 경우에는 계산하지 않는다. 이유 : 상호작용 계산은 시간이 오래걸리지만 친구점수에서 높은 비중이 아님.</p>
     *
     * @param memberId     현재 회원 ID
     * @param candidateMap 추천 후보자 Map (ID -> 후보자)
     */
    private void injectInteractionScores(Long memberId, Map<Long, RecommendCandidate> candidateMap) {
        List<Long> candidateIds = new ArrayList<>(candidateMap.keySet());
        List<Object> results = redisInteractionScoreRepository.getInteractionScoresBatch(memberId, candidateIds);

        for (int i = 0; i < candidateIds.size(); i++) {
            Object scoreObj = results.get(i);
            if (scoreObj != null) {
                candidateMap.get(candidateIds.get(i)).setInteractionScore(Double.parseDouble(scoreObj.toString()));
            }
        }
    }

    /**
     * <h3>제외할 ID 집합을 생성합니다.</h3>
     * <p>자기 자신, 1촌 친구, 이미 등록된 후보자를 제외 대상으로 설정합니다.</p>
     *
     * @param memberId   현재 회원 ID
     * @param myFriends  내 친구(1촌) ID 집합
     * @param candidates 현재까지의 추천 후보자 목록
     * @return 제외할 ID 집합
     */
    private Set<Long> buildExcludeIds(Long memberId, Set<Long> myFriends, List<RecommendCandidate> candidates) {
        Set<Long> excludeIds = new HashSet<>(myFriends);
        excludeIds.add(memberId);
        for (RecommendCandidate c : candidates) {
            excludeIds.add(c.getMemberId());
        }
        return excludeIds;
    }

    /**
     * <h3>상호작용 점수 기반으로 부족한 인원을 보충합니다.</h3>
     * <p>Redis ZSet에서 상호작용 점수 상위 10명을 조회하여 후보자로 추가합니다.</p>
     *
     * @param memberId   현재 회원 ID
     * @param candidates 추천 후보자 목록 (수정됨)
     * @param excludeIds 제외할 ID 집합 (수정됨)
     */
    private void fillFromInteractionScores(Long memberId, List<RecommendCandidate> candidates, Set<Long> excludeIds) {
        Set<TypedTuple<Object>> topInteractions = redisInteractionScoreRepository.getTopInteractionScores(memberId, RECOMMEND_LIMIT);

        for (TypedTuple<Object> tuple : topInteractions) {
            if (candidates.size() >= RECOMMEND_LIMIT) {
                break;
            }
            Long id = Long.valueOf(tuple.getValue().toString());
            if (!excludeIds.contains(id)) {
                candidates.add(RecommendCandidate.initialCandidate(id, 0, 0, tuple.getScore()));
                excludeIds.add(id);
            }
        }
    }

    /**
     * <h3>최근 가입자로 부족한 인원을 보충합니다.</h3>
     * <p>상호작용 점수 기반 보충 후에도 인원이 부족하면 최근 가입자 10명을 추가합니다.</p>
     *
     * @param candidates 추천 후보자 목록 (수정됨)
     * @param excludeIds 제외할 ID 집합
     */
    private void fillFromRecentMembers(List<RecommendCandidate> candidates, Set<Long> excludeIds) {
        List<Long> needMemberIds = memberRepository.getNeedMemberIds(excludeIds);
        for (Long id : needMemberIds) {
            candidates.add(RecommendCandidate.initialCandidate(id, 0, 0, 0));
        }
    }

    /**
     * Redis 파이프라인 결과를 변환합니다.
     */
    @SuppressWarnings("unchecked")
    private List<List<Long>> toListOfLists(List<Object> results) {
        List<List<Long>> converted = new ArrayList<>();
        for (Object result : results) {
            if (result instanceof List<?>) {
                converted.add((List<Long>) result);
            }
        }
        return converted;
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

        List<Long> memberIds = new ArrayList<>();
        Set<Long> acqIds = new HashSet<>();
        for (RecommendCandidate c : candidates) {
            memberIds.add(c.getMemberId());
            if (c.getAcquaintanceId() != null) {
                acqIds.add(c.getAcquaintanceId());
            }
        }

        // Bulk Fetch
        Map<Long, RecommendedFriend.RecommendedFriendInfo> friendInfos = new HashMap<>();
        for (RecommendedFriend.RecommendedFriendInfo info : memberQueryRepository.addRecommendedFriendInfo(memberIds)) {
            friendInfos.put(info.friendMemberId(), info);
        }

        Map<Long, RecommendedFriend.AcquaintanceInfo> acqInfos;
        if (acqIds.isEmpty()) {
            acqInfos = Collections.emptyMap();
        } else {
            acqInfos = new HashMap<>();
            for (RecommendedFriend.AcquaintanceInfo info : memberQueryRepository.addAcquaintanceInfo(new ArrayList<>(acqIds))) {
                acqInfos.put(info.acquaintanceId(), info);
            }
        }

        // Mapping
        List<RecommendedFriend> result = new ArrayList<>();
        for (RecommendCandidate c : candidates) {
            if (!friendInfos.containsKey(c.getMemberId())) continue;

            RecommendedFriend.RecommendedFriendInfo info = friendInfos.get(c.getMemberId());
            RecommendedFriend.AcquaintanceInfo acqInfo = acqInfos.get(c.getAcquaintanceId());

            RecommendedFriend friend = new RecommendedFriend(c.getMemberId(), c.getAcquaintanceId(), c.isManyAcquaintance(), c.getDepth());
            friend.setRecommendedFriendName(info);
            if (acqInfo != null) friend.setAcquaintanceFriendName(acqInfo);
            result.add(friend);
        }

        return new PageImpl<>(result, pageable, result.size());
    }
}