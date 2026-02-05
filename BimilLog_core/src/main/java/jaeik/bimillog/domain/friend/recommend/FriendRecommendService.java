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
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
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
 * @version 2.6.0
 * @author Jaeik
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


    // 최종 추천 인원 수
    private static final int RECOMMEND_LIMIT = 10;
    private static final int FIRST_FRIEND_SCAN_LIMIT = 50;
    private static final int SECOND_DEGREE_SAMPLE_SIZE = 30;
    private static final int THIRD_DEGREE_SAMPLE_SIZE = 100;


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
        try {
            // 1. 내 친구(1촌) 조회
            Set<Long> myFriends = redisFriendshipRepository.getFriends(memberId, FIRST_FRIEND_SCAN_LIMIT);

            // 2. 후보자 탐색 (2촌 -> 3촌 순차 확장) 및 점수 계산
            List<RecommendCandidate> candidates = findAndScoreCandidates(memberId, myFriends);

            // 3. 상호작용 점수 주입
            injectInteractionScores(memberId, candidates);

            // 4. 점수순 정렬 및 상위 N명 추출
            candidates.sort(Comparator.comparingDouble(RecommendCandidate::calculateTotalScore).reversed());
            List<RecommendCandidate> topCandidates = new ArrayList<>(candidates.subList(0, Math.min(candidates.size(), RECOMMEND_LIMIT)));

            // 5. 부족한 인원 보충 (최근 가입자) 및 블랙리스트 필터링
            fillAndFilter(memberId, myFriends, topCandidates);

            // 6. 회원 정보 조회 및 응답 DTO 변환
            return toResponsePage(topCandidates, pageable);
        } catch (CustomException e) {
            ErrorCode code = e.getErrorCode();
            if (code == ErrorCode.FRIEND_REDIS_SHIP_QUERY_ERROR
                    || code == ErrorCode.FRIEND_REDIS_INTERACTION_QUERY_ERROR) {
                log.warn("Redis 실패로 DB 폴백 실행: memberId={}, errorCode={}", memberId, code, e);
                return getRecommendFromDb(memberId, pageable);
            }
            throw e;
        }
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

        // A. 2촌 탐색 (친구당 랜덤 30명씩)
        List<Long> myFriendList = new ArrayList<>(myFriends);

        // 레디스에서 2촌 결과 가져옴
        List<Object> secondResults = redisFriendshipRepository.getFriendsBatch(myFriendList, SECOND_DEGREE_SAMPLE_SIZE);

        for (int i = 0; i < myFriendList.size(); i++) {
            Long friendId = myFriendList.get(i); // 1촌 친구
            List<Long> secondResultList = List.of(); // 1촌의 친구 (2촌)
            if (secondResults.get(i) instanceof List<?>) {
                secondResultList = Collections.singletonList((Long) secondResults.get(i));
            }

            for (Long secondFriendId : secondResultList) {
                // 중복제거 2촌 친구에 나자신이 있거나 내 친구가 2촌 친구에 있는 경우
                if (secondFriendId.equals(memberId) || myFriends.contains(secondFriendId)) {
                    continue;
                }

                // 2촌ID로 2촌의 상세정보 찾기
                RecommendCandidate candidate = candidateMap.get(secondFriendId);
                // 등록되지 않았으면 ID와 2촌깊이 삽입
                if (candidate == null) {
                    candidate = RecommendCandidate.of(secondFriendId, 2);
                    candidateMap.put(secondFriendId, candidate);
                }
                // 이미 등록 되었으면 1촌끼리 같은 2촌을 안다는 뜻 공통친구에 추가
                candidate.addCommonFriend(friendId);
            }
        }

        // B. 3촌 탐색 2촌이 10명 이하일 때
        if (candidateMap.size() < RECOMMEND_LIMIT) {
            // 2촌의 ID들
            List<Long> secondDegreeList = new ArrayList<>(candidateMap.keySet());

            // 2촌의 ID로 3촌을 친구들을 불러옴
            List<Object> thirdResults = redisFriendshipRepository.getFriendsBatch(secondDegreeList, THIRD_DEGREE_SAMPLE_SIZE);

            for (int i = 0; i < secondDegreeList.size(); i++) {
                Long secondDegreeId = secondDegreeList.get(i); // 2촌 ID
                List<Long> thirdResultList = List.of(); // 2촌의 친구 (3촌)
                if (thirdResults.get(i) instanceof List<?>) {
                    thirdResultList = Collections.singletonList((Long) thirdResults.get(i));
                }

                for (Long thirdFriendId : thirdResultList) {
                    // 중복제거 3촌친구가 나이거나 1촌친구에 3촌이 있거나 2촌친구에 3촌이 있거나
                    if (thirdFriendId.equals(memberId) || myFriends.contains(thirdFriendId) || candidateMap.containsKey(thirdFriendId)) {
                        continue;
                    }

                    // 2촌친구의 공통친구 크기
                    // 여러 1촌들이 2촌을 알고 거기서 2촌의 친구로 나오는 3촌은 2촌의 공통친구 만큼의 점수를 이어받음
                    int bridgeScore = candidateMap.get(secondDegreeId).getCommonFriends().size();

                    // 3촌친구들로 3촌의 공통친구 찾기
                    RecommendCandidate candidate = candidateMap.get(thirdFriendId);

                    // 등록되지 않았으면 ID와 3촌깊이 삽입
                    if (candidate == null) {
                        candidate = RecommendCandidate.of(thirdFriendId, 3);
                        candidateMap.put(thirdFriendId, candidate);
                    }

                    // 이미 등록되었으면 2촌이 이미 3촌을 안다는 뜻
                    candidate.addThreeDegreeScore(bridgeScore);
                }
            }
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
        Set<Long> candidateIds = new HashSet<>();
        for (RecommendCandidate c : candidates) {
            candidateIds.add(c.getMemberId());
        }
        Map<Long, Double> scores = redisInteractionScoreRepository.getInteractionScoresBatch(memberId, candidateIds);
        for (RecommendCandidate c : candidates) {
            c.setInteractionScore(scores.getOrDefault(c.getMemberId(), 0.0));
        }
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
            for (RecommendCandidate c : candidates) {
                excludeIds.add(c.getMemberId());
            }

            // 상호작용 점수 전체 조회 (Fallback)
            Map<Long, Double> allInteractions = redisInteractionScoreRepository.getAllInteractionScores(memberId);

            // 1. 상호작용 점수 기반 추가
            for (Map.Entry<Long, Double> entry : allInteractions.entrySet()) {
                if (candidates.size() >= RECOMMEND_LIMIT) break;
                Long id = entry.getKey();
                Double score = entry.getValue();
                if (!excludeIds.contains(id)) {
                    candidates.add(RecommendCandidate.builder().memberId(id).interactionScore(score).depth(0).build());
                    excludeIds.add(id);
                }
            }

            // 2. 그래도 부족하면 최근 가입자 추가
            if (candidates.size() < RECOMMEND_LIMIT) {
                List<Long> newMembers = memberRepository.findIdByIdNotInOrderByCreatedAtDesc(
                        excludeIds,
                        PageRequest.of(0, RECOMMEND_LIMIT - candidates.size())
                );
                for (Long id : newMembers) {
                    candidates.add(RecommendCandidate.of(id, 0));
                }
            }
        }

        // 블랙리스트 제거 (마지막에 처리)
        Set<Long> blacklist = memberBlacklistRepository.findBlacklistIdsByRequestMemberId(memberId);
        candidates.removeIf(recommendCandidate -> blacklist.contains(recommendCandidate.getMemberId()));
    }

    /**
     * <h3>Redis 실패 시 DB 기반으로 친구 추천 목록을 조회합니다.</h3>
     * <p>
     * Redis 경로와 동일한 BFS 알고리즘을 DB에서 수행하되,
     * 상호작용 점수 계산은 제외합니다.
     * </p>
     *
     * @param memberId 추천을 요청한 회원 ID
     * @param pageable 페이지네이션 정보
     * @return 추천 친구 목록 (페이지)
     */
    private Page<RecommendedFriend> getRecommendFromDb(Long memberId, Pageable pageable) {
        // 1. DB로 1촌 조회
        Set<Long> myFriends = friendshipQueryRepository.getMyFriendIdsSet(memberId, FIRST_FRIEND_SCAN_LIMIT);

        // 2. DB로 2촌/3촌 탐색
        List<RecommendCandidate> candidates = findAndScoreCandidatesFromDb(memberId, myFriends);

        // 3. 상호작용 점수 주입 생략

        // 4. 점수순 정렬 + 상위 N명 추출
        candidates.sort(Comparator.comparingDouble(RecommendCandidate::calculateTotalScore).reversed());
        List<RecommendCandidate> topCandidates = new ArrayList<>(candidates.subList(0, Math.min(candidates.size(), RECOMMEND_LIMIT)));

        // 5. 상호작용 제외 보충 + 블랙리스트 필터링
        fillAndFilterWithoutInteraction(memberId, myFriends, topCandidates);

        // 6. 응답 변환
        return toResponsePage(topCandidates, pageable);
    }

    /**
     * <h3>DB 기반으로 2촌, 3촌 후보자를 탐색하고 점수를 계산합니다.</h3>
     * <p>
     * {@link #findAndScoreCandidates}와 동일한 BFS 로직이지만
     * Redis 대신 {@link FriendshipQueryRepository#getFriendIdsBatch}를 사용합니다.
     * </p>
     *
     * @param memberId  현재 회원 ID
     * @param myFriends 내 친구(1촌) ID 집합
     * @return 추천 후보자 목록 (점수 계산 완료)
     */
    private List<RecommendCandidate> findAndScoreCandidatesFromDb(Long memberId, Set<Long> myFriends) {
        Map<Long, RecommendCandidate> candidateMap = new HashMap<>();

        if (myFriends.isEmpty()) return new ArrayList<>();

        // A. 2촌 탐색
        Map<Long, Set<Long>> friendsOfFriends = friendshipQueryRepository.getFriendIdsBatch(myFriends);
        for (Map.Entry<Long, Set<Long>> entry : friendsOfFriends.entrySet()) {
            Long friendId = entry.getKey();
            Set<Long> friendsSet = entry.getValue();
            if (friendsSet == null) continue;
            for (Long targetId : friendsSet) {
                if (targetId.equals(memberId) || myFriends.contains(targetId)) continue;

                RecommendCandidate candidate = candidateMap.get(targetId);
                if (candidate == null) {
                    candidate = RecommendCandidate.of(targetId, 2);
                    candidateMap.put(targetId, candidate);
                }
                candidate.addCommonFriend(friendId);
            }
        }

        // B. 3촌 탐색 (2촌이 부족할 경우에만 수행)
        if (candidateMap.size() < RECOMMEND_LIMIT) {
            Set<Long> secondDegreeIds = candidateMap.keySet();
            Map<Long, Set<Long>> friendsOfSeconds = friendshipQueryRepository.getFriendIdsBatch(secondDegreeIds);

            for (Map.Entry<Long, Set<Long>> entry : friendsOfSeconds.entrySet()) {
                Long secondDegreeId = entry.getKey();
                Set<Long> friendsSet = entry.getValue();
                if (friendsSet == null) continue;
                for (Long targetId : friendsSet) {
                    if (targetId.equals(memberId) || myFriends.contains(targetId) || candidateMap.containsKey(targetId)) continue;

                    int bridgeScore = candidateMap.get(secondDegreeId).getCommonFriends().size();
                    RecommendCandidate candidate = candidateMap.get(targetId);
                    if (candidate == null) {
                        candidate = RecommendCandidate.of(targetId, 3);
                        candidateMap.put(targetId, candidate);
                    }
                    candidate.addThreeDegreeScore(bridgeScore);
                }
            }
        }

        return new ArrayList<>(candidateMap.values());
    }

    /**
     * <h3>상호작용 점수 없이 부족한 추천 인원을 보충하고 블랙리스트를 필터링합니다.</h3>
     * <p>
     * Redis 폴백 경로에서 사용되며, 상호작용 점수 조회 없이 최근 가입자로만 보충합니다.
     * </p>
     *
     * @param memberId   현재 회원 ID
     * @param myFriends  내 친구(1촌) ID 집합
     * @param candidates 추천 후보자 목록 (수정됨)
     */
    private void fillAndFilterWithoutInteraction(Long memberId, Set<Long> myFriends, List<RecommendCandidate> candidates) {
        if (candidates.size() < RECOMMEND_LIMIT) {
            Set<Long> excludeIds = new HashSet<>(myFriends);
            excludeIds.add(memberId);
            for (RecommendCandidate c : candidates) {
                excludeIds.add(c.getMemberId());
            }

            // 최근 가입자로 보충
            List<Long> newMembers = memberRepository.findIdByIdNotInOrderByCreatedAtDesc(
                    excludeIds,
                    PageRequest.of(0, RECOMMEND_LIMIT - candidates.size())
            );
            for (Long id : newMembers) {
                candidates.add(RecommendCandidate.of(id, 0));
            }
        }

        // 블랙리스트 제거
        Set<Long> blacklist = memberBlacklistRepository.findBlacklistIdsByRequestMemberId(memberId);
        candidates.removeIf(recommendCandidate -> blacklist.contains(recommendCandidate.getMemberId()));
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