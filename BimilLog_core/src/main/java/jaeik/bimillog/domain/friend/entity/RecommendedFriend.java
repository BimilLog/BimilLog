package jaeik.bimillog.domain.friend.entity;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;

@Getter
public class RecommendedFriend {
    @NotNull
    private final Long friendMemberId; // 추천 친구 Id

    @NotNull
    private String memberName; // 추천 친구 닉네임

    @NotNull
    private final Integer depth; // 촌수 2촌 또는 3촌

    private final Long acquaintanceId; // 추천친구와 연결된 친구의 Id, 3촌은 null
    private String acquaintance; // 추천친구와 연결된 친구의 닉네임, 3촌은 null
    private final boolean manyAcquaintance; // 아는 사람이 여러명인지 구분하는 플래그, 3촌은 null
    private String introduce; // 2촌일 경우 ex) 홍길동의 친구, 공통친구가 2명이상이면 홍길동외 다수의 친구, 3촌은 null

    public RecommendedFriend(Long friendMemberId, Long acquaintanceId, boolean manyAcquaintance, Integer depth) {
        this.friendMemberId = friendMemberId;
        this.acquaintanceId = acquaintanceId;
        this.manyAcquaintance = manyAcquaintance;
        this.depth = depth;
    }

    public void setRecommendedFriendName (RecommendedFriend.RecommendedFriendInfo recommendedFriendInfo) {
        this.memberName = recommendedFriendInfo.memberName;
    }

    public void setAcquaintanceFriendName(RecommendedFriend.RecommendedFriendInfo recommendedFriendInfo) {
        this.acquaintance = recommendedFriendInfo.acquaintance;
        this.introduce = createIntroduce(depth, acquaintance, manyAcquaintance);
    }

    public record RecommendedFriendInfo(Long friendMemberId, String memberName, Long acquaintanceId, String acquaintance) {}

    private String createIntroduce(Integer depth, String acquaintance, boolean manyAcquaintance) {
        if (acquaintance == null || depth != 2) {
            return null; // 3촌은 acquaintance가 Null이다.
        }

        if (manyAcquaintance) {
            return acquaintance + " 외 다수의 친구";
        }

        return acquaintance + "의 친구";
    }
}
