package jaeik.bimillog.domain.friend.entity;

import lombok.Getter;

@Getter
public class FriendUnion {
    private Long id;
    public int parent;
    private int rank;
    private int degree;
    private int score;

    public FriendUnion(Long id, int degree) {
        this.id = id;
        this.parent = id.intValue();
        this.rank = 0;
        this.score = (degree == 2 ? 50 : 20);
        this.degree = degree;
    }

    public FriendUnion() {

    }

    public void addScore(int s) {
        this.score += s;
    }

    public void addRank() {
        this.rank++;
    }
}
