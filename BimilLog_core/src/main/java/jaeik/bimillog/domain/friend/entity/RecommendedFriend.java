package jaeik.bimillog.domain.friend.entity;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import org.springframework.lang.Nullable;

import java.util.List;

@Getter
public class RecommendedFriend {
    @NotNull
    private final Long friendMemberId; // 추천 친구 Id

    @NotNull
    private final Integer depth; // 촌수 2촌 또는 3촌

    @NotNull
    @Size(min = 1)
    private final List<String> acquaintance; // 추천친구와 연결된 친구의 닉네임

    @Nullable
    private final String introduce; // 2촌일 경우 ex) 홍길동의 친구, 공통친구가 2명이상이면 홍길동외 다수의 친구, 3촌은 표시 x

    @NotNull
    private String memberName; // 추천 친구 닉네임


    public RecommendedFriend(Long friendMemberId, Integer depth, List<String> acquaintance) {
        this.friendMemberId = friendMemberId;
        this.acquaintance = acquaintance;
        this.introduce = createIntroduce(depth, acquaintance);
        this.depth = depth;
    }

    public void updateInfo(RecommendedFriend.RecommendedFriendInfo recommendedFriendInfo) {
        this.memberName = recommendedFriendInfo.memberName;
    }

    public record RecommendedFriendInfo(Long friendMemberId, String memberName) {}

    private String createIntroduce(Integer depth, List<String> acquaintance) {
        if (depth == null || depth != 2) {
            return null; // 3촌은 표시 X
        }

        if (acquaintance.size() >= 2) {
            String first = acquaintance.getFirst();
            return first + " 외 다수의 친구";
        }

        return acquaintance.getFirst() + "의 친구";
    }
}
