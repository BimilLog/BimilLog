package jaeik.bimillog.domain.friend.entity;

import jaeik.bimillog.domain.global.entity.BaseEntity;
import jaeik.bimillog.domain.member.entity.Member;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 친구 요청테이블
 * 멤버가 삭제될시 친구요청도 삭제되게 한다.
 */
// 친구 요청은 양방향이다 1, 100이면 100, 1이 존재하면 저장되지말아야함
@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "friend_request",
        uniqueConstraints = {@UniqueConstraint(name = "unique_friend_request", columnNames = {"sender", "receiver"})})
public class FriendRequest extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "friend_request_id")
    private Long id;

    // DB레벨 CasCade설정 member 삭제시 FriendRequest삭제
    @NotNull
    @ManyToOne
    @JoinColumn(name = "sender_id")
    private Member sender;

    // DB레벨 CasCade설정 member 삭제시 FriendRequest삭제
    @NotNull
    @ManyToOne
    @JoinColumn(name = "receiver_id")
    private Member receiver;
}
