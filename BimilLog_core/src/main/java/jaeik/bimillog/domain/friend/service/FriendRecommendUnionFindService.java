package jaeik.bimillog.domain.friend.service;

import jaeik.bimillog.domain.friend.entity.FriendUnion;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class FriendRecommendUnionFindService {
    private final Map<Long, FriendUnion> nodes = new HashMap<>();

    public void addNode(Long id, int degree) {
        nodes.put(id, new FriendUnion(id, degree));
    }

    public FriendUnion getNode(Long id) {
        return nodes.get(id);
    }

    public Long find(Long id) {
        FriendUnion n = nodes.get(id);
        if (n == null) return null;

        if (n.getParent() != n.getId().intValue()) {
            n.parent = find((long) n.getParent()).intValue();
        }
        return (long) n.getParent();
    }

    public void union(Long a, Long b) {
        Long ra = find(a);
        Long rb = find(b);

        if (ra.equals(rb)) return;

        FriendUnion A = nodes.get(ra);
        FriendUnion B = nodes.get(rb);

        if (A.getRank() < B.getRank()) {
            A.parent = B.getId().intValue();
        } else if (A.getRank() > B.getRank()) {
            B.parent = A.getId().intValue();
        } else {
            B.parent = A.getId().intValue();
            A.addRank();
        }
    }

    public Collection<FriendUnion> allNodes() {
        return nodes.values();
    }
}
