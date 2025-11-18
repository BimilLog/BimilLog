package jaeik.bimillog.domain.friend.entity.jpa;

import jaeik.bimillog.domain.global.entity.BaseEntity;
import jaeik.bimillog.domain.member.entity.Member;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

// 친구추천은 양방향이 아니다 서로가 친구추천 목록에 뜰 수 있다.
@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class FriendRecommendation extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "friend_recommend_id")
    private Long id;

    // 추천을 보는 사람
    // DB레벨 CasCade설정 member 삭제시 FriendRecommendation삭제
    @NotNull
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id", nullable = false)
    private Member member;

    // 추천된 대상
    // DB레벨 CasCade설정 member 삭제시 FriendRecommendation삭제
    @NotNull
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "recommend_member_id", nullable = false)
    private Member recommendMember;

    // 점수
    @NotNull
    @Column(name = "score", nullable = false)
    private Integer score;

    // 촌수
    @NotNull
    @Column(name = "depth", nullable = false)
    private Integer depth;
}
