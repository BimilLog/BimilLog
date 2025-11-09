package jaeik.bimillog.domain.member.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.*;

/**
 * <h2>멤버 검색 요청 DTO</h2>
 * <p>멤버 검색 API의 요청 데이터를 검증하는 DTO입니다.</p>
 * <p>MemberQueryController의 검색 API에서 사용됩니다.</p>
 *
 * @author Jaeik
 * @version 2.0.0
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MemberSearchDTO {

    @NotBlank(message = "검색어는 필수입니다")
    private String query;

    public String getTrimmedQuery() {
        return query != null ? query.trim() : "";
    }
}
