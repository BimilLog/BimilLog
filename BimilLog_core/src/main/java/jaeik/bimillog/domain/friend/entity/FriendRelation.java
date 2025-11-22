package jaeik.bimillog.domain.friend.entity;

import lombok.*;

import java.util.Set;

@Getter
@AllArgsConstructor
public class FriendRelation {
    private Long memberId; // 본인
    private Set<Long> firstDegreeIds; // 1촌
    private Set<Long> secondDegreeIds; // 2촌
    private Set<Long> thirdDegreeIds; // 3촌

    public static FriendRelation createSecondRelation(Long memberId, Set<Long> firstDegreeIds, Set<Long> secondDegreeIds) {
        return new FriendRelation(memberId, firstDegreeIds, secondDegreeIds, Set.of());
    }

    public void updateThirdRelation(Set<Long> thirdDegreeIds) {
        this.thirdDegreeIds = thirdDegreeIds;
    }
}
