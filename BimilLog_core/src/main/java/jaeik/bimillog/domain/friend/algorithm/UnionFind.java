package jaeik.bimillog.domain.friend.algorithm;

import jaeik.bimillog.domain.friend.entity.FriendRelation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.*;

/**
 * <h2>Union-Find 자료구조</h2>
 * <p>공통 친구를 찾기 위한 Union-Find (Disjoint Set) 알고리즘 구현</p>
 * <p>경로 압축(Path Compression)과 랭크 기반 합치기(Union by Rank)를 사용합니다.</p>
 *
 * @author Jaeik
 * @version 2.2.0
 */
@Component
@Slf4j
public class UnionFind {

    /**
     * 부모 노드 저장 배열
     */
    private Map<Long, Long> parent;

    /**
     * 랭크 저장 배열 (트리의 높이)
     */
    private Map<Long, Integer> rank;

    /**
     * <h3>Union-Find 초기화</h3>
     * <p>각 노드를 자기 자신을 부모로 하는 독립적인 집합으로 초기화합니다.</p>
     *
     * @param nodes 초기화할 노드 집합
     */
    public void init(Set<Long> nodes) {
        parent = new HashMap<>();
        rank = new HashMap<>();

        for (Long node : nodes) {
            parent.put(node, node);
            rank.put(node, 0);
        }
    }

    /**
     * <h3>루트 노드 찾기</h3>
     * <p>경로 압축을 사용하여 루트 노드를 찾습니다.</p>
     *
     * @param x 노드 ID
     * @return 루트 노드 ID
     */
    public Long find(Long x) {
        if (parent.get(x).equals(x)) {
            return x;
        }

        // 경로 압축: 루트 노드를 직접 부모로 설정
        Long root = find(parent.get(x));
        parent.put(x, root);
        return root;
    }

    /**
     * <h3>두 노드 합치기</h3>
     * <p>랭크 기반 합치기를 사용하여 두 집합을 합칩니다.</p>
     *
     * @param x 노드 1
     * @param y 노드 2
     */
    public void union(Long x, Long y) {
        Long rootX = find(x);
        Long rootY = find(y);

        if (rootX.equals(rootY)) {
            return;
        }

        // 랭크가 작은 트리를 큰 트리 아래에 연결
        int rankX = rank.get(rootX);
        int rankY = rank.get(rootY);

        if (rankX < rankY) {
            parent.put(rootX, rootY);
        } else if (rankX > rankY) {
            parent.put(rootY, rootX);
        } else {
            parent.put(rootY, rootX);
            rank.put(rootX, rankX + 1);
        }
    }

    /**
     * <h3>공통 친구 그룹 구축 (FriendRelation 직접 수정)</h3>
     * <p>FriendRelation의 2촌 후보자들을 분석하여 각 후보자의 공통 친구를 설정합니다.</p>
     * <p>예: 2촌 A가 1촌 B, C와 연결 → A의 공통 친구는 {B, C}</p>
     *
     * @param relation FriendRelation 객체
     */
    public void buildCommonFriendGroups(FriendRelation relation) {
        List<FriendRelation.CandidateInfo> secondDegreeCandidates = relation.getSecondDegreeCandidates();

        // 모든 노드 수집 (2촌 + 1촌)
        Set<Long> allNodes = new HashSet<>();
        for (FriendRelation.CandidateInfo candidate : secondDegreeCandidates) {
            allNodes.add(candidate.getCandidateId());
            allNodes.addAll(candidate.getConnectedFriendIds());
        }

        // Union-Find 초기화
        init(allNodes);

        // 2촌과 연결된 1촌들을 Union
        for (FriendRelation.CandidateInfo candidate : secondDegreeCandidates) {
            Long candidateId = candidate.getCandidateId();
            Set<Long> connectedFirstDegree = candidate.getConnectedFriendIds();

            for (Long firstDegreeFriendId : connectedFirstDegree) {
                union(candidateId, firstDegreeFriendId);
            }
        }

        // 공통 친구 그룹 설정
        for (FriendRelation.CandidateInfo candidate : secondDegreeCandidates) {
            Set<Long> commonFriends = new HashSet<>(candidate.getConnectedFriendIds());
            candidate.setCommonFriendIds(commonFriends);
        }

        log.debug("공통 친구 그룹 구축 완료: 2촌 수={}", secondDegreeCandidates.size());
    }
}
