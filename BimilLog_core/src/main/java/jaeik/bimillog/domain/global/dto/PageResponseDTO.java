package jaeik.bimillog.domain.global.dto;

import org.springframework.data.domain.Page;

import java.util.List;

/**
 * <h2>Spring Data Page 직렬화 래퍼 DTO</h2>
 * <p>
 * {@link org.springframework.data.domain.PageImpl}을 Jackson으로 직렬화할 때
 * sort 파라미터가 포함된 {@code Pageable}의 {@code Sort.Direction} 직렬화 오류를 방지하기 위한 래퍼.
 * </p>
 * <p>프론트엔드 {@code PageResponse<T>} 인터페이스와 호환됩니다.</p>
 *
 * @param content          데이터 목록
 * @param totalElements    전체 요소 수
 * @param totalPages       전체 페이지 수
 * @param first            첫 번째 페이지 여부
 * @param last             마지막 페이지 여부
 * @param number           현재 페이지 번호 (0부터 시작)
 * @param size             페이지 크기
 * @param numberOfElements 현재 페이지의 실제 요소 수
 * @param empty            빈 페이지 여부
 * @param <T>              데이터 타입
 * @author Jaeik
 * @version 2.8.0
 */
public record PageResponseDTO<T>(
        List<T> content,
        long totalElements,
        int totalPages,
        boolean first,
        boolean last,
        int number,
        int size,
        int numberOfElements,
        boolean empty
) {
    /**
     * <h3>Page 인스턴스로부터 PageResponseDTO 생성</h3>
     *
     * @param page Spring Data Page 인스턴스
     * @param <T>  데이터 타입
     * @return PageResponseDTO 인스턴스
     */
    public static <T> PageResponseDTO<T> from(Page<T> page) {
        return new PageResponseDTO<>(
                page.getContent(),
                page.getTotalElements(),
                page.getTotalPages(),
                page.isFirst(),
                page.isLast(),
                page.getNumber(),
                page.getSize(),
                page.getNumberOfElements(),
                page.isEmpty()
        );
    }
}
