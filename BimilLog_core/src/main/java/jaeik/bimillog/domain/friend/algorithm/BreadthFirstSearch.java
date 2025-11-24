package jaeik.bimillog.domain.friend.algorithm;

import jaeik.bimillog.infrastructure.redis.friend.RedisFriendshipRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.*;

/**
 * <h2>BFS 친구 탐색 알고리즘 (최적화)</h2>
 * <p>Redis Pipeline을 사용하여 네트워크 지연을 최소화합니다.</p>
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class BreadthFirstSearch {

    private final RedisFriendshipRepository redisFriendshipRepository;

    /**
     * <h3>2촌 탐색 (Batch)</h3>
     * <p>1촌 목록을 한 번에 전송하여 파이프라인으로 2촌을 탐색합니다.</p>
     */
    public Map<Long, Set<Long>> findSecondDegree(Long memberId, Set<Long> firstDegree) {
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

        log.debug("2촌 탐색 완료: memberId={}, 2촌 수={}", memberId, secondDegreeMap.size());
        return secondDegreeMap;
    }

    /**
     * <h3>3촌 탐색 (Batch)</h3>
     * <p>2촌 목록을 한 번에 전송하여 파이프라인으로 3촌을 탐색합니다.</p>
     */
    public Map<Long, Set<Long>> findThirdDegree(Long memberId, Set<Long> firstDegree, Map<Long, Set<Long>> secondDegree) {
        // 1. 2촌들의 친구 목록을 한 번에 조회 (Pipeline)
        Map<Long, Set<Long>> friendsOfSecondDegreeMap = redisFriendshipRepository.getFriendsBatch(secondDegree.keySet());
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
                if (secondDegree.containsKey(candidateId)) continue;

                // 3촌 Map 구성: Candidate -> [연결된 2촌 친구들]
                thirdDegreeMap.computeIfAbsent(candidateId, k -> new HashSet<>()).add(secondDegreeId);
            }
        }

        log.debug("3촌 탐색 완료: 3촌 수={}", thirdDegreeMap.size());
        return thirdDegreeMap;
    }
}