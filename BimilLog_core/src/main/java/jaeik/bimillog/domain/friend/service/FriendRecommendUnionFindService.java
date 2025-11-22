package jaeik.bimillog.domain.friend.service;

import jaeik.bimillog.domain.friend.entity.FriendRelation;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class FriendRecommendUnionFindService {
    // Map<ID, FriendUnion> 구조는 한 멤버의 추천친구 관계도를 저장하는 용도로 사용
    private final Map<Long, FriendUnion> nodes = new HashMap<>();

    /**
     * Union-Find의 노드를 모두 비웁니다. (새로운 멤버의 추천 계산을 시작하기 전 호출)
     */
    public void clearNodes() {
        nodes.clear();
    }

    /**
     * FriendRelation을 기반으로 2촌(50점) 및 3촌(20점) 노드를 초기화합니다.
     *
     * @param relation 현재 멤버의 친구 관계 데이터
     */
    public void initializeNodes(FriendRelation relation) {
        clearNodes();

        // 2촌 등록 (기본 점수 50)
        for (Long second : relation.getSecondDegreeIds()) {
            addNode(second, 2);
        }

        // 3촌 등록 (기본 점수 20)
        for (Long third : relation.getThirdDegreeIds()) {
            addNode(third, 3);
        }
    }

    public void addNode(Long id, int degree) {
        // 이미 존재하는 노드는 덮어쓰지 않고, 중복 추가를 방지합니다.
        if (!nodes.containsKey(id)) {
            nodes.put(id, new FriendUnion(id, degree));
        }
    }

    /**
     * 특정 ID를 가진 노드에 점수를 추가합니다.
     * @param id 점수를 추가할 추천인 ID
     * @param score 추가 점수
     */
    public void addScore(Long id, int score) {
        FriendUnion n = nodes.get(id);
        if (n != null) {
            n.addScore(score);
        }
    }

    public FriendUnion getNode(Long id) {
        return nodes.get(id);
    }

    public Long find(Long id) {
        FriendUnion n = nodes.get(id);
        if (n == null) return null;

        if (n.getParent() != n.getId().intValue()) {
            // 경로 압축 (Path compression)
            Long rootId = find((long) n.getParent());
            if (rootId != null) {
                n.parent = rootId.intValue();
            } else {
                // 루트를 찾을 수 없는 경우 (이상 상황)
                return (long) n.getParent();
            }
        }
        return (long) n.getParent();
    }

    public void union(Long a, Long b) {
        // null 체크 추가
        if (!nodes.containsKey(a) || !nodes.containsKey(b)) return;

        Long ra = find(a);
        Long rb = find(b);

        if (ra == null || rb == null || ra.equals(rb)) return;

        FriendUnion A = nodes.get(ra);
        FriendUnion B = nodes.get(rb);

        if (A.getRank() < B.getRank()) {
            B.parent = A.getId().intValue(); // 랭크가 낮은 A를 B 밑으로
        } else if (A.getRank() > B.getRank()) {
            A.parent = B.getId().intValue(); // 랭크가 낮은 B를 A 밑으로
        } else {
            B.parent = A.getId().intValue();
            A.addRank();
        }
    }

    public Collection<FriendUnion> allNodes() {
        return nodes.values();
    }

    @Getter
    public static class FriendUnion {
        private Long id;
        public int parent;
        private int rank;
        private int degree;
        private int score;

        public FriendUnion(Long id, int degree) {
            this.id = id;
            this.parent = id.intValue();
            this.rank = 0;
            this.score = (degree == 2 ? 50 : 20); // 2촌 기본 50점, 3촌 기본 20점
            this.degree = degree;
        }

        public FriendUnion() {
            // 기본 생성자
        }

        public void addScore(int s) {
            this.score += s;
        }

        public void addRank() {
            this.rank++;
        }

    }

}