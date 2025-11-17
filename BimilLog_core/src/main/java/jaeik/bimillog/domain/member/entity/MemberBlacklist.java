package jaeik.bimillog.domain.member.entity;

import jaeik.bimillog.domain.global.entity.BaseEntity;
import jaeik.bimillog.domain.member.service.MemberBlacklistService;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.*;

// 블랙리스트는 양방향이 아니다 서로 차단이 가능하다.
@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
@Table(name = "member_blacklist",
        uniqueConstraints = {@UniqueConstraint(name = "member_blacklist", columnNames = {"request_member_id", "black_member_id"})})
public class MemberBlacklist extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "member_black_list_id")
    private Long id;

    // DB레벨 CasCade설정 member 삭제시 MemberBlacklist삭제
    @NotNull
    @ManyToOne
    @JoinColumn(name = "request_member_id")
    private Member requestMember;

    // DB레벨 CasCade설정 member 삭제시 MemberBlacklist삭제
    @NotNull
    @ManyToOne
    @JoinColumn(name = "black_member_id")
    private Member blackMember;

    public static MemberBlacklist createMemberBlacklist(Member requestMember, Member blackMember) {
        return MemberBlacklist.builder()
                .requestMember(requestMember)
                .blackMember(blackMember)
                .build();
    }
}
