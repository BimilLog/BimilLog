//package jaeik.bimillog.domain.member.entity;
//
//import jaeik.bimillog.domain.global.entity.BaseEntity;
//import jakarta.persistence.*;
//import jakarta.validation.constraints.NotNull;
//import lombok.Getter;
//
///**
// * 친구관계 설정테이블
// * 멤버가 삭제될시 친구관계도 삭제되게 한다.
// */
//@Entity
//@Getter
//@Table(name = "friendship", uniqueConstraints =
//        {@UniqueConstraint(name = "unique_member_friend", columnNames = {"friendship_id, member_id, friend_id"})})
//public class Friendship extends BaseEntity {
//
//    @Id
//    @GeneratedValue(strategy = GenerationType.IDENTITY)
//    @Column(name = "friendship_id")
//    private Long id;
//
//    // DB레벨 CasCade설정 member 삭제시 Friend삭제
//    @NotNull
//    @ManyToOne(fetch = FetchType.LAZY)
//    @JoinColumn(name = "member_id")
//    private Member member;
//
//    // DB레벨 CasCade설정 member 삭제시 Friend삭제
//    @NotNull
//    @ManyToOne(fetch = FetchType.LAZY)
//    @JoinColumn(name = "friend_id")
//    private Member friend;
//
//    @NotNull
//    @Column(name = "friend_status")
//    private FriendStatus status;
//}
