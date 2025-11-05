package jaeik.bimillog.infrastructure.adapter.in.member.dto;

import jaeik.bimillog.domain.member.entity.Member;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * <h3>간단한 사용자 정보 DTO</h3>
 * <p>사용자 목록 조회 시 필요한 기본 정보만 담는 DTO</p>
 *
 * @author Jaeik
 * @since 2.0.0
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SimpleMemberDTO {

    private String memberName;

    /**
     * <h3>Member 엔티티로부터 SimpleMemberDTO 생성</h3>
     * <p>도메인 엔티티를 DTO로 변환합니다.</p>
     *
     * @param member 사용자 엔티티
     * @return SimpleMemberDTO 객체
     * @author Jaeik
     * @since 2.0.0
     */
    public static SimpleMemberDTO fromMember(Member member) {
        return SimpleMemberDTO.builder()
                .memberName(member.getMemberName())
                .build();
    }
}
