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
public class RecommendedFriendDTO {
    private final Long friendMemberId;
    private final String memberName;
    private final Integer depth;
    private final Long acquaintanceId;
    private final String acquaintanceName;
    private final boolean manyAcquaintance;
    private final String introduce;

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
                memberName,
                candidate.getDepth(),
                candidate.getAcquaintanceId(),
                acquaintanceName,
                candidate.isManyAcquaintance(),
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
    @AllArgsConstructor
    public static class MemberInfo {
        private Long memberId;
        private String memberName;
    }
}
