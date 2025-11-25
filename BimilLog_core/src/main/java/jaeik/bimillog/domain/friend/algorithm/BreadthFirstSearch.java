package jaeik.bimillog.domain.friend.algorithm;

import jaeik.bimillog.domain.friend.entity.FriendRelation;
import jaeik.bimillog.infrastructure.redis.friend.RedisFriendshipRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.*;

/**
 * <h2>BFS 친구 탐색 알고리즘 (최적화)</h2>
 * <p>Redis Pipeline을 사용하여 네트워크 지연을 최소화합니다.</p>
 * <p>FriendRelation 도메인 객체를 반환하여 타입 안전성을 보장합니다.</p>
 *
 * @author Jaeik
 * @version 2.1.0
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class BreadthFirstSearch {

    private final RedisFriendshipRepository redisFriendshipRepository;

    /**
     * <h3>친구 관계 탐색 (FriendRelation 반환)</h3>
     * <p>1촌 목록을 기반으로 2촌 관계를 탐색하고 FriendRelation 객체로 반환합니다.</p>
     *
     * @param memberId     본인 회원 ID
     * @param firstDegree  1촌 ID 집합
     * @return FriendRelation 객체 (2촌 정보 포함)
     */
    public FriendRelation findFriendRelation(Long memberId, Set<Long> firstDegree) {
        Map<Long, Set<Long>> secondDegreeConnections = findSecondDegreeMap(memberId, firstDegree);

        log.debug("2촌 탐색 완료: memberId={}, 2촌 수={}", memberId, secondDegreeConnections.size());

        return FriendRelation.createWithConnections(memberId, firstDegree, secondDegreeConnections);
    }

    /**
     * <h3>3촌 관계 추가 탐색</h3>
     * <p>FriendRelation에 3촌 정보를 추가합니다.</p>
     *
     * @param relation 기존 FriendRelation (2촌까지 포함)
     * @return 3촌 정보가 추가된 FriendRelation
     */
    public FriendRelation addThirdDegreeRelation(FriendRelation relation) {
        Map<Long, Set<Long>> thirdDegreeConnections = findThirdDegreeMap(
                relation.getMemberId(),
                relation.getFirstDegreeIds(),
                relation.getSecondDegreeConnections()
        );

        log.debug("3촌 탐색 완료: 3촌 수={}", thirdDegreeConnections.size());

        relation.updateThirdRelation(thirdDegreeConnections);
        return relation;
    }

    /**
     * <h3>2촌 탐색 (내부 로직 - Map 반환)</h3>
     * <p>1촌 목록을 한 번에 전송하여 파이프라인으로 2촌을 탐색합니다.</p>
     *
     * @deprecated 외부에서는 {@link #findFriendRelation} 사용 권장
     */
    @Deprecated
    public Map<Long, Set<Long>> findSecondDegree(Long memberId, Set<Long> firstDegree) {
        return findSecondDegreeMap(memberId, firstDegree);
    }

    /**
     * <h3>2촌 Map 생성</h3>
     */
    private Map<Long, Set<Long>> findSecondDegreeMap(Long memberId, Set<Long> firstDegree) {
        // 1. 1촌들의 친구 목록을 한 번에 조회 (Pipeline)
        Map<Long, Set<Long>> friendsOfFirstDegreeMap = redisFriendshipRepository.getFriendsBatch(firstDegree);
        Map<Long, Set<Long>> secondDegreeMap = new HashMap<>();

        // 2. 결과 처리 (메모리 연산)
        for (Map.Entry<Long, Set<Long>> entry : friendsOfFirstDegreeMap.entrySet()) {
            Long friendId = entry.getKey(); // 1촌 ID
            Set<Long> friendsOfFriend = entry.getValue(); // 1촌의 친구들 (잠재적 2촌)

            if (friendsOfFriend == null || friendsOfFriend.isEmpty()) {
                continue;
            }

            for (Long candidateId : friendsOfFriend) {
                // 본인 제외
                if (candidateId.equals(memberId)) continue;
                // 이미 친구인 사람 제외 (1촌)
                if (firstDegree.contains(candidateId)) continue;

                // 2촌 Map 구성: Candidate -> [연결된 1촌 친구들]
                secondDegreeMap.computeIfAbsent(candidateId, k -> new HashSet<>()).add(friendId);
            }
        }

        return secondDegreeMap;
    }

    /**
     * <h3>3촌 탐색 (하위 호환성)</h3>
     * <p>2촌 목록을 한 번에 전송하여 파이프라인으로 3촌을 탐색합니다.</p>
     *
     * @deprecated 외부에서는 {@link #addThirdDegreeRelation} 사용 권장
     */
    @Deprecated
    public Map<Long, Set<Long>> findThirdDegree(Long memberId, Set<Long> firstDegree, Map<Long, Set<Long>> secondDegree) {
        return findThirdDegreeMap(memberId, firstDegree, secondDegree);
    }

    /**
     * <h3>3촌 Map 생성</h3>
     */
    private Map<Long, Set<Long>> findThirdDegreeMap(Long memberId, Set<Long> firstDegree, Map<Long, Set<Long>> secondDegreeConnections) {
        // 1. 2촌들의 친구 목록을 한 번에 조회 (Pipeline)
        Map<Long, Set<Long>> friendsOfSecondDegreeMap = redisFriendshipRepository.getFriendsBatch(secondDegreeConnections.keySet());
        Map<Long, Set<Long>> thirdDegreeMap = new HashMap<>();

        // 2. 결과 처리
        for (Map.Entry<Long, Set<Long>> entry : friendsOfSecondDegreeMap.entrySet()) {
            Long secondDegreeId = entry.getKey(); // 2촌 ID
            Set<Long> friendsOfSecond = entry.getValue(); // 2촌의 친구들 (잠재적 3촌)

            if (friendsOfSecond == null || friendsOfSecond.isEmpty()) {
                continue;
            }

            for (Long candidateId : friendsOfSecond) {
                // 본인 제외
                if (candidateId.equals(memberId)) continue;
                // 1촌 제외
                if (firstDegree.contains(candidateId)) continue;
                // 2촌 제외
                if (secondDegreeConnections.containsKey(candidateId)) continue;

                // 3촌 Map 구성: Candidate -> [연결된 2촌 친구들]
                thirdDegreeMap.computeIfAbsent(candidateId, k -> new HashSet<>()).add(secondDegreeId);
            }
        }

        return thirdDegreeMap;
    }
}