package jaeik.bimillog.domain.post.dto;

import jaeik.bimillog.domain.post.repository.PostQueryType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;

/**
 * <h2>게시글 검색 요청 DTO</h2>
 * <p>게시글 검색 API의 요청 데이터를 검증하는 DTO입니다.</p>
 * <p>검색 타입별 검증 처리</p>
 *
 * @author Jaeik
 * @version 2.0.0
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PostSearchDTO {

    @NotNull(message = "검색 타입은 필수입니다")
    private PostQueryType type;

    @NotBlank(message = "검색어는 필수입니다")
    private String query;

    public String getTrimmedQuery() {
        return query != null ? query.trim() : "";
    }
}