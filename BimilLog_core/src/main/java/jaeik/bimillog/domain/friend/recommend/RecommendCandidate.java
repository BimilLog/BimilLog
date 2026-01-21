package jaeik.bimillog.domain.friend.recommend;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.util.HashSet;
import java.util.Set;

@Getter
@Setter
@Builder
public class RecommendCandidate {
    private Long memberId;
    private int depth; // 2촌, 3촌, 0(기타)

    @Builder.Default
    private Set<Long> commonFriends = new HashSet<>(); // 2촌용 공통친구 목록

    @Builder.Default
    private double virtualScore = 0.0; // 3촌용 가산점

    @Builder.Default
    private double interactionScore = 0.0; // 상호작용 점수

    // 화면 표시용 필드
    private Long acquaintanceId;
    private boolean manyAcquaintance;

    // --- Factory Method ---
    public static RecommendCandidate of(Long memberId, int depth) {
        return RecommendCandidate.builder()
                .memberId(memberId)
                .depth(depth)
                .commonFriends(new HashSet<>())
                .build();
    }

    // --- Logic ---
    public void addCommonFriend(Long friendId) {
        this.commonFriends.add(friendId);
        if (this.acquaintanceId == null) this.acquaintanceId = friendId; // 첫 친구를 대표로
        if (this.commonFriends.size() >= 2) this.manyAcquaintance = true;
    }

    public void addVirtualScore(int score) {
        this.virtualScore += (score * 0.5); // 3촌 가중치 로직 내부화
    }

    public double calculateTotalScore() {
        double base = (depth == 2) ? 50 : (depth == 3) ? 20 : 0;
        double commonScore = (depth == 2)
                ? Math.min(commonFriends.size(), 10) * 2
                : Math.min(virtualScore, 5); // 3촌은 최대 5점
        double interaction = Math.min(interactionScore, 10.0);

        return base + commonScore + interaction;
    }
}