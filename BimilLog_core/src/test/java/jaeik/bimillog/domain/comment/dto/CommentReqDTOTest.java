package jaeik.bimillog.domain.comment.dto;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * <h2>CommentReqDTO 단위 테스트</h2>
 * <p>댓글 요청 DTO의 validation 로직을 검증하는 단위 테스트</p>
 * <p>Bean Validation과 @AssertTrue 어노테이션 기반 검증 메서드들을 테스트</p>
 *
 * @author Jaeik
 * @version 2.0.0
 */
@DisplayName("CommentReqDTO 단위 테스트")
@Tag("unit")
class CommentReqDTOTest {

    @Test
    @DisplayName("댓글 작성 검증 - 유효한 데이터")
    void isWriteValid_ValidData_ReturnsTrue() {
        // Given
        CommentReqDTO dto = new CommentReqDTO();
        dto.setPostId(1L);
        dto.setContent("테스트 댓글 내용");
        
        // When & Then
        assertThat(dto.isWriteValid()).isTrue();
    }

    @Test
    @DisplayName("댓글 작성 검증 - 내용이 없는 경우")
    void isWriteValid_EmptyContent_ReturnsFalse() {
        // Given
        CommentReqDTO dto = new CommentReqDTO();
        dto.setPostId(1L);
        dto.setContent("");
        
        // When & Then
        assertThat(dto.isWriteValid()).isFalse();
    }

    @Test
    @DisplayName("댓글 작성 검증 - 내용이 공백인 경우")
    void isWriteValid_BlankContent_ReturnsFalse() {
        // Given
        CommentReqDTO dto = new CommentReqDTO();
        dto.setPostId(1L);
        dto.setContent("   ");
        
        // When & Then
        assertThat(dto.isWriteValid()).isFalse();
    }

    @Test
    @DisplayName("댓글 작성 검증 - postId가 없는 경우 (수정/삭제 요청)")
    void isWriteValid_NoPostId_ReturnsTrue() {
        // Given - 수정 요청 형태
        CommentReqDTO dto = new CommentReqDTO();
        dto.setId(1L);
        dto.setContent("수정된 내용");
        
        // When & Then
        assertThat(dto.isWriteValid()).isTrue(); // 작성 검증 대상이 아니므로 통과
    }

    @Test
    @DisplayName("댓글 수정 검증 - 유효한 데이터")
    void isUpdateValid_ValidData_ReturnsTrue() {
        // Given
        CommentReqDTO dto = new CommentReqDTO();
        dto.setId(1L);
        dto.setContent("수정된 내용");
        
        // When & Then
        assertThat(dto.isUpdateValid()).isTrue();
    }

    @Test
    @DisplayName("댓글 수정 검증 - 내용이 없는 경우")
    void isUpdateValid_EmptyContent_ReturnsFalse() {
        // Given
        CommentReqDTO dto = new CommentReqDTO();
        dto.setId(1L);
        dto.setContent("");
        
        // When & Then
        assertThat(dto.isUpdateValid()).isFalse();
    }

    @Test
    @DisplayName("댓글 수정 검증 - 작성 요청인 경우 (postId 있음)")
    void isUpdateValid_WriteRequest_ReturnsTrue() {
        // Given - 작성 요청
        CommentReqDTO dto = new CommentReqDTO();
        dto.setPostId(1L);
        dto.setContent("작성 내용");
        
        // When & Then
        assertThat(dto.isUpdateValid()).isTrue(); // 수정 검증 대상이 아니므로 통과
    }

    @Test
    @DisplayName("댓글 삭제 검증 - 유효한 데이터")
    void isDeleteValid_ValidData_ReturnsTrue() {
        // Given
        CommentReqDTO dto = new CommentReqDTO();
        dto.setId(1L);
        
        // When & Then
        assertThat(dto.isDeleteValid()).isTrue();
    }

    @Test
    @DisplayName("댓글 삭제 검증 - ID가 없는 경우")
    void isDeleteValid_NoId_ReturnsFalse() {
        // Given
        CommentReqDTO dto = new CommentReqDTO();
        // ID가 설정되지 않음
        
        // When & Then
        assertThat(dto.isDeleteValid()).isFalse();
    }

    @Test
    @DisplayName("댓글 삭제 검증 - 작성 요청인 경우 (content 있음)")
    void isDeleteValid_WriteRequest_ReturnsTrue() {
        // Given - 작성 요청
        CommentReqDTO dto = new CommentReqDTO();
        dto.setPostId(1L);
        dto.setContent("작성 내용");
        
        // When & Then
        assertThat(dto.isDeleteValid()).isTrue(); // 삭제 검증 대상이 아니므로 통과
    }

    @Test
    @DisplayName("대댓글 작성 검증 - 유효한 데이터")
    void isReplyValid_ValidData_ReturnsTrue() {
        // Given
        CommentReqDTO dto = new CommentReqDTO();
        dto.setPostId(1L);
        dto.setParentId(2L);
        dto.setContent("대댓글 내용");
        
        // When & Then
        assertThat(dto.isReplyValid()).isTrue();
    }

    @Test
    @DisplayName("대댓글 작성 검증 - postId가 없는 경우")
    void isReplyValid_NoPostId_ReturnsFalse() {
        // Given
        CommentReqDTO dto = new CommentReqDTO();
        dto.setParentId(2L);
        dto.setContent("대댓글 내용");
        
        // When & Then
        assertThat(dto.isReplyValid()).isFalse();
    }

    @Test
    @DisplayName("대댓글 작성 검증 - 내용이 없는 경우")
    void isReplyValid_EmptyContent_ReturnsFalse() {
        // Given
        CommentReqDTO dto = new CommentReqDTO();
        dto.setPostId(1L);
        dto.setParentId(2L);
        dto.setContent("");
        
        // When & Then
        assertThat(dto.isReplyValid()).isFalse();
    }

    @Test
    @DisplayName("대댓글 작성 검증 - parentId가 없는 경우 (일반 댓글)")
    void isReplyValid_NoParentId_ReturnsTrue() {
        // Given - 일반 댓글
        CommentReqDTO dto = new CommentReqDTO();
        dto.setPostId(1L);
        dto.setContent("일반 댓글");
        
        // When & Then
        assertThat(dto.isReplyValid()).isTrue(); // 대댓글 검증 대상이 아니므로 통과
    }
}