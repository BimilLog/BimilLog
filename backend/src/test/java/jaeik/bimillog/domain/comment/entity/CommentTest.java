package jaeik.bimillog.domain.comment.entity;

import jaeik.bimillog.domain.post.entity.Post;
import jaeik.bimillog.domain.member.entity.member.Member;
import jaeik.bimillog.testutil.TestMembers;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * <h2>Comment 엔티티 단위 테스트</h2>
 * <p>댓글 엔티티의 비즈니스 로직을 검증하는 단위 테스트</p>
 * <p>특히 수정/삭제 권한 검증 로직을 중점적으로 테스트</p>
 *
 * @author Jaeik
 * @version 2.0.0
 */
@DisplayName("Comment 엔티티 단위 테스트")
@Tag("test")
class CommentTest {

    private Member createTestUser(Long id) {
        return TestMembers.copyWithId(TestMembers.MEMBER_1, id);
    }

    private Post createTestPost(Member member) {
        return Post.builder()
                .id(1L)
                .title("테스트 게시글")
                .content("테스트 게시글 내용입니다.")
                .member(member)
                .build();
    }

    @Test
    @DisplayName("회원 댓글 수정/삭제 권한 검증 - 댓글 작성자가 맞는 경우")
    void canModify_MemberComment_Owner_ReturnsTrue() {
        // Given
        Member member = createTestUser(1L);
        Post post = createTestPost(member);
        
        Comment comment = Comment.builder()
                .id(1L)
                .post(post)
                .member(member) // 회원 댓글
                .content("회원 댓글입니다.")
                .deleted(false)
                .password(null) // 회원 댓글은 비밀번호 없음
                .build();
        
        // When & Then
        assertThat(comment.canModify(1L, null)).isTrue();
    }

    @Test
    @DisplayName("회원 댓글 수정/삭제 권한 검증 - 다른 사용자가 시도하는 경우")
    void canModify_MemberComment_NotOwner_ReturnsFalse() {
        // Given
        Member member = createTestUser(1L);
        Post post = createTestPost(member);
        
        Comment comment = Comment.builder()
                .id(1L)
                .post(post)
                .member(member) // 회원 댓글 (member ID = 1L)
                .content("회원 댓글입니다.")
                .deleted(false)
                .password(null)
                .build();
        
        // When & Then - 다른 사용자 (ID = 2L)가 시도
        assertThat(comment.canModify(2L, null)).isFalse();
    }

    @Test
    @DisplayName("회원 댓글 수정/삭제 권한 검증 - userId가 null인 경우")
    void canModify_MemberComment_NullUserId_ReturnsFalse() {
        // Given
        Member member = createTestUser(1L);
        Post post = createTestPost(member);
        
        Comment comment = Comment.builder()
                .id(1L)
                .post(post)
                .member(member) // 회원 댓글
                .content("회원 댓글입니다.")
                .deleted(false)
                .password(null)
                .build();
        
        // When & Then - 익명 사용자가 회원 댓글 수정 시도
        assertThat(comment.canModify(null, 1234)).isFalse();
    }

    @Test
    @DisplayName("익명 댓글 수정/삭제 권한 검증 - 올바른 비밀번호")
    void canModify_AnonymousComment_CorrectPassword_ReturnsTrue() {
        // Given
        Member member = createTestUser(1L);
        Post post = createTestPost(member);
        
        Comment comment = Comment.builder()
                .id(1L)
                .post(post)
                .member(null) // 익명 댓글
                .content("익명 댓글입니다.")
                .deleted(false)
                .password(1234) // 익명 댓글 비밀번호
                .build();
        
        // When & Then
        assertThat(comment.canModify(null, 1234)).isTrue();
    }

    @Test
    @DisplayName("익명 댓글 수정/삭제 권한 검증 - 잘못된 비밀번호")
    void canModify_AnonymousComment_WrongPassword_ReturnsFalse() {
        // Given
        Member member = createTestUser(1L);
        Post post = createTestPost(member);
        
        Comment comment = Comment.builder()
                .id(1L)
                .post(post)
                .member(null) // 익명 댓글
                .content("익명 댓글입니다.")
                .deleted(false)
                .password(1234)
                .build();
        
        // When & Then - 잘못된 비밀번호
        assertThat(comment.canModify(null, 9999)).isFalse();
    }

    @Test
    @DisplayName("익명 댓글 수정/삭제 권한 검증 - 비밀번호가 null인 경우")
    void canModify_AnonymousComment_NullPassword_ReturnsFalse() {
        // Given
        Member member = createTestUser(1L);
        Post post = createTestPost(member);
        
        Comment comment = Comment.builder()
                .id(1L)
                .post(post)
                .member(null) // 익명 댓글
                .content("익명 댓글입니다.")
                .deleted(false)
                .password(1234)
                .build();
        
        // When & Then - 비밀번호 없이 시도
        assertThat(comment.canModify(null, null)).isFalse();
    }

    @Test
    @DisplayName("익명 댓글 수정/삭제 권한 검증 - 회원이 익명 댓글에 접근하는 경우")
    void canModify_AnonymousComment_MemberTryingAccess_ReturnsFalse() {
        // Given
        Member member = createTestUser(1L);
        Post post = createTestPost(member);
        
        Comment comment = Comment.builder()
                .id(1L)
                .post(post)
                .member(null) // 익명 댓글
                .content("익명 댓글입니다.")
                .deleted(false)
                .password(1234)
                .build();
        
        // When & Then - 회원이 익명 댓글에 userId로 접근 시도
        assertThat(comment.canModify(1L, null)).isFalse();
    }

    @Test
    @DisplayName("댓글 수정 기능 테스트")
    void updateComment_UpdatesContent() {
        // Given
        Member member = createTestUser(1L);
        Post post = createTestPost(member);
        
        Comment comment = Comment.builder()
                .id(1L)
                .post(post)
                .member(member)
                .content("원본 댓글 내용")
                .deleted(false)
                .build();
        
        // When
        comment.updateComment("수정된 댓글 내용");
        
        // Then
        assertThat(comment.getContent()).isEqualTo("수정된 댓글 내용");
    }

    @Test
    @DisplayName("댓글 생성 팩토리 메서드 테스트 - 회원 댓글")
    void createComment_MemberComment() {
        // Given
        Member member = createTestUser(1L);
        Post post = createTestPost(member);
        
        // When
        Comment comment = Comment.createComment(post, member, "회원 댓글 내용", null);
        
        // Then
        assertThat(comment.getPost()).isEqualTo(post);
        assertThat(comment.getMember()).isEqualTo(member);
        assertThat(comment.getContent()).isEqualTo("회원 댓글 내용");
        assertThat(comment.isDeleted()).isFalse();
        assertThat(comment.getPassword()).isNull();
    }

    @Test
    @DisplayName("댓글 생성 팩토리 메서드 테스트 - 익명 댓글")
    void createComment_AnonymousComment() {
        // Given
        Member member = createTestUser(1L);
        Post post = createTestPost(member);
        
        // When
        Comment comment = Comment.createComment(post, null, "익명 댓글 내용", 1234);
        
        // Then
        assertThat(comment.getPost()).isEqualTo(post);
        assertThat(comment.getMember()).isNull();
        assertThat(comment.getContent()).isEqualTo("익명 댓글 내용");
        assertThat(comment.isDeleted()).isFalse();
        assertThat(comment.getPassword()).isEqualTo(1234);
    }
}