package jaeik.bimillog.infrastructure.adapter.post.dto;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.*;

/**
 * <h2>게시글 검색 요청 DTO</h2>
 * <p>게시글 검색 API의 요청 데이터를 검증하는 DTO입니다.</p>
 * <p>검색 타입별 최소 길이 검증, FULLTEXT 검색 조건 검증</p>
 * <p>PostQueryController의 검색 API에서 사용됩니다.</p>
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

    public enum SearchType {
        TITLE("title"),
        CONTENT("content"),
        WRITER("writer"),
        TITLE_CONTENT("title_content");
        
        private final String value;
        
        SearchType(String value) {
            this.value = value;
        }
        
        public String getValue() {
            return value;
        }
        
        public static SearchType fromValue(String value) {
            for (SearchType type : SearchType.values()) {
                if (type.value.equals(value)) {
                    return type;
                }
            }
            return null;
        }
    }

    @NotNull(message = "검색 타입은 필수입니다")
    @Pattern(regexp = "^(title|content|writer|title_content)$", message = "검색 타입은 title, content, writer, title_content 중 하나여야 합니다")
    private String type;

    @NotBlank(message = "검색어는 필수입니다")
    private String query;

    @AssertTrue(message = "작성자 검색은 최소 1자 이상, 다른 검색은 최소 2자 이상이어야 합니다")
    public boolean isQueryLengthValid() {
        if (query == null) return false;
        
        String trimmedQuery = query.trim();
        if (trimmedQuery.isEmpty()) return false;
        
        if ("writer".equals(type)) {
            return trimmedQuery.length() >= 1;
        }
        return trimmedQuery.length() >= 2;
    }

    @AssertTrue(message = "제목+내용 검색은 최소 3자 이상이어야 합니다")
    public boolean isFullTextSearchValid() {
        if (!"title_content".equals(type)) return true;
        if (query == null) return false;
        
        return query.trim().length() >= 3;
    }

    public String getTrimmedQuery() {
        return query != null ? query.trim() : "";
    }

    public SearchType getSearchType() {
        return SearchType.fromValue(type);
    }
}