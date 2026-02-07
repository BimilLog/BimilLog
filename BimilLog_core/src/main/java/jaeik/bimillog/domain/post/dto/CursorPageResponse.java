package jaeik.bimillog.domain.post.dto;

import java.util.List;

/**
 * <h2>커서 기반 페이지네이션 응답 DTO</h2>
 *
 * @param content    조회된 데이터 목록
 * @param nextCursor 다음 페이지 요청 시 사용할 커서 (마지막 게시글 ID)
 * @param hasNext    다음 페이지 존재 여부
 * @param size       요청된 페이지 크기
 * @param <T>        데이터 타입
 * @author Jaeik
 * @version 2.7.0
 */
public record CursorPageResponse<T>(
        List<T> content,
        Long nextCursor,
        boolean hasNext,
        int size
) {
    /**
     * <h3>CursorPageResponse 생성 팩토리 메서드</h3>
     *
     */
    public static <T> CursorPageResponse<T> of(List<T> content, Long nextCursor, boolean hasNext, int size) {
        return new CursorPageResponse<>(content, nextCursor, hasNext, size);
    }
}
