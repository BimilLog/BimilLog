package jaeik.bimillog.domain.friend.dto;

import jaeik.bimillog.domain.friend.entity.RecommendCandidate;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * <h2>추천친구 응답 DTO</h2>
 *
 * @version 2.7.0
 * @author Jaeik
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class RecommendedFriendDTO {
    private Long friendMemberId;
    private Integer depth;
    private Long acquaintanceId;
    private boolean manyAcquaintance;
    private String memberName;
    private String acquaintanceName;
    private String introduce;

    private RecommendedFriendDTO(Long friendMemberId, String memberName, Integer depth, Long acquaintanceId,
                                 String acquaintanceName, boolean manyAcquaintance, String introduce) {
        this.friendMemberId = friendMemberId;
        this.memberName = memberName;
        this.depth = depth;
        this.acquaintanceId = acquaintanceId;
        this.acquaintanceName = acquaintanceName;
        this.manyAcquaintance = manyAcquaintance;
        this.introduce = introduce;
    }

    public static RecommendedFriendDTO from(RecommendCandidate candidate,
                                            String memberName,
                                            String acquaintanceName) {
        String introduce = createIntroduce(candidate.getDepth(), acquaintanceName, candidate.isManyAcquaintance());
        return new RecommendedFriendDTO(
                candidate.getMemberId(),
                candidate.getDepth(),
                candidate.getAcquaintanceId(),
                candidate.isManyAcquaintance(),
                memberInfo.memberName,
                memberInfo.acquaintanceName,
                introduce
        );
    }

    private static String createIntroduce(int depth, String acquaintance, boolean manyAcquaintance) {
        if (acquaintance == null || depth != 2) {
            return null;
        }
        if (manyAcquaintance) {
            return acquaintance + " 외 다수의 공통 친구";
        }
        return acquaintance + "의 친구";
    }

    @Getter
    public static class MemberInfo {
        private String friendMemberId;
        private String memberName;
    }
}
