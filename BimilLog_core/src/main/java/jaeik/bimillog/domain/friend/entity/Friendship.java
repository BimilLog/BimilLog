package jaeik.bimillog.domain.friend.entity;

import jaeik.bimillog.domain.global.entity.BaseEntity;
import jaeik.bimillog.domain.member.entity.Member;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.*;


// 친구 요청은 양방향이다 1, 100이면 100, 1이 존재하면 저장되지말아야함

/**
 * 친구관계 설정테이블
 * 멤버가 삭제될시 친구관계도 삭제되게 한다.
 */
@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "friendship",
        uniqueConstraints = {@UniqueConstraint(name = "unique_friend_pair", columnNames = {"member_id", "friend_id"})})
public class Friendship extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "friendship_id")
    private Long id;

    // DB레벨 CasCade설정 member 삭제시 Friendship삭제
    @NotNull
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id")
    private Member member;

    // DB레벨 CasCade설정 member 삭제시 Friendship삭제
    @NotNull
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "friend_id")
    private Member friend;
}
