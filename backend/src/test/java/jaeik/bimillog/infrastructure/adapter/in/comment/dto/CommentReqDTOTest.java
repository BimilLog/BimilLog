package jaeik.bimillog.infrastructure.adapter.in.comment.dto;

import org.junit.jupiter.api.DisplayName;
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
    @DisplayName("비밀번호 검증 - 익명 댓글 (유효한 비밀번호)")
    void isPasswordValid_AnonymousComment_ValidPassword_ReturnsTrue() {
        // Given
        CommentReqDTO dto = new CommentReqDTO();
        dto.setPostId(1L);
        dto.setContent("익명 댓글");
        dto.setPassword(1234);
        
        // When & Then
        assertThat(dto.isPasswordValid()).isTrue();
    }

    @Test
    @DisplayName("비밀번호 검증 - 익명 댓글 (유효하지 않은 비밀번호)")
    void isPasswordValid_AnonymousComment_InvalidPassword_ReturnsFalse() {
        // Given
        CommentReqDTO dto = new CommentReqDTO();
        dto.setPostId(1L);
        dto.setContent("익명 댓글");
        dto.setPassword(999); // 1000 미만
        
        // When & Then
        assertThat(dto.isPasswordValid()).isFalse();
    }

    @Test
    @DisplayName("비밀번호 검증 - 회원 댓글 (userId 설정됨)")
    void isPasswordValid_MemberComment_ReturnsTrue() {
        // Given
        CommentReqDTO dto = new CommentReqDTO();
        dto.setPostId(1L);
        dto.setContent("회원 댓글");
        dto.setUserId(1L);
        
        // When & Then
        assertThat(dto.isPasswordValid()).isTrue(); // 회원 댓글은 비밀번호가 null이어야 함
    }

    @Test
    @DisplayName("비밀번호 검증 - 컨트롤러에서 설정 예정인 경우 통과")
    void isPasswordValid_PendingControllerSetup_ReturnsTrue() {
        // Given - 컨트롤러에서 userId를 설정하기 전 상태
        CommentReqDTO dto = new CommentReqDTO();
        dto.setPostId(1L);
        dto.setContent("댓글 내용");
        // userId도 password도 설정되지 않은 상태
        
        // When & Then
        assertThat(dto.isPasswordValid()).isTrue(); // 컨트롤러에서 설정할 예정이므로 통과
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