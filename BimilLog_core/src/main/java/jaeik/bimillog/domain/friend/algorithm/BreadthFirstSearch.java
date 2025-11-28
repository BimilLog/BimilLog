package jaeik.bimillog.domain.friend.algorithm;

import jaeik.bimillog.domain.friend.entity.FriendRelation;
import jaeik.bimillog.infrastructure.redis.friend.RedisFriendshipRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * <h2>너비 우선 탐색 (BFS) 알고리즘</h2>
 * <p>Redis Pipeline을 활용하여 2촌/3촌 친구 관계를 탐색합니다.</p>
 *
 * <h3>알고리즘 흐름</h3>
 * <ol>
 *   <li>1촌 친구 목록을 시작점으로 설정</li>
 *   <li>Redis Pipeline으로 1촌의 친구 목록 일괄 조회 (네트워크 최적화)</li>
 *   <li>2촌 후보자 필터링 (본인, 1촌 제외)</li>
 *   <li>필요시 3촌까지 확장 탐색</li>
 * </ol>
 *
 * <h3>성능 특성</h3>
 * <ul>
 *   <li><b>시간 복잡도</b>: O(V + E) - V: 노드 수, E: 간선 수</li>
 *   <li><b>공간 복잡도</b>: O(V) - 방문 노드 저장</li>
 *   <li><b>Redis Pipeline</b>: N번의 조회를 1번의 네트워크 왕복으로 처리</li>
 * </ul>
 *
 * @author Jaeik
 * @version 2.3.0
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
        FriendRelation relation = new FriendRelation(memberId, firstDegree);

        // 1. 2촌 Map 생성
        Map<Long, Set<Long>> secondDegreeMap = findSecondDegreeMap(memberId, firstDegree);

        // 2. Map을 CandidateInfo로 변환하여 FriendRelation에 추가
        for (Map.Entry<Long, Set<Long>> entry : secondDegreeMap.entrySet()) {
            FriendRelation.CandidateInfo candidate = FriendRelation.CandidateInfo.of(
                    entry.getKey(),
                    entry.getValue()
            );
            relation.addSecondDegreeCandidate(candidate);
        }

        log.debug("2촌 탐색 완료: memberId={}, 2촌 수={}", memberId, relation.getSecondDegreeCandidates().size());

        return relation;
    }

    /**
     * <h3>3촌 관계 추가 탐색</h3>
     * <p>FriendRelation에 3촌 정보를 추가합니다.</p>
     *
     * @param relation 기존 FriendRelation (2촌까지 포함)
     * @return 3촌 정보가 추가된 FriendRelation
     */
    public FriendRelation addThirdDegreeRelation(FriendRelation relation) {
        // 1. 2촌 후보자 ID들 추출
        Set<Long> secondDegreeIds = relation.getSecondDegreeIds();

        // 2. 3촌 Map 생성
        Map<Long, Set<Long>> thirdDegreeMap = findThirdDegreeMap(
                relation.getMemberId(),
                relation.getFirstDegreeIds(),
                secondDegreeIds
        );

        // 3. Map을 CandidateInfo로 변환하여 FriendRelation에 추가
        for (Map.Entry<Long, Set<Long>> entry : thirdDegreeMap.entrySet()) {
            FriendRelation.CandidateInfo candidate = FriendRelation.CandidateInfo.of(
                    entry.getKey(),
                    entry.getValue()
            );
            relation.addThirdDegreeCandidate(candidate);
        }

        log.debug("3촌 탐색 완료: 3촌 수={}", relation.getThirdDegreeCandidates().size());

        return relation;
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

                // 2촌 Map 구성: Candidate -> [중개 역할을 하는 1촌 친구들]
                secondDegreeMap.computeIfAbsent(candidateId, k -> new HashSet<>()).add(friendId);
            }
        }

        return secondDegreeMap;
    }

    /**
     * <h3>3촌 Map 생성</h3>
     */
    private Map<Long, Set<Long>> findThirdDegreeMap(Long memberId, Set<Long> firstDegree, Set<Long> secondDegreeIds) {
        // 1. 2촌들의 친구 목록을 한 번에 조회 (Pipeline)
        Map<Long, Set<Long>> friendsOfSecondDegreeMap = redisFriendshipRepository.getFriendsBatch(secondDegreeIds);
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
                if (secondDegreeIds.contains(candidateId)) continue;

                // 3촌 Map 구성: Candidate -> [중개 역할을 하는 2촌 친구들]
                thirdDegreeMap.computeIfAbsent(candidateId, k -> new HashSet<>()).add(secondDegreeId);
            }
        }

        return thirdDegreeMap;
    }
}
