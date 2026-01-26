package jaeik.bimillog.domain.comment.entity;

import jaeik.bimillog.domain.member.entity.Member;
import jaeik.bimillog.domain.post.entity.jpa.Post;
import jaeik.bimillog.testutil.TestMembers;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * <h2>Comment 엔티티 단위 테스트</h2>
 * <p>댓글 엔티티의 비즈니스 로직을 검증하는 단위 테스트</p>
 * <p>특히 수정/삭제 권한 검증 로직을 중점적으로 테스트</p>
 */
@DisplayName("Comment 엔티티 단위 테스트")
@Tag("unit")
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

    @ParameterizedTest(name = "userId={0}, password={1} → {2}")
    @CsvSource({
            "1, , true",   // 소유자
            "2, , false",  // 다른 사용자
            ", 1234, false" // 익명 사용자
    })
    @DisplayName("회원 댓글 수정/삭제 권한 검증")
    void shouldValidateMemberCommentPermission(Long userId, Integer password, boolean expected) {
        // Given
        Member member = createTestUser(1L);
        Post post = createTestPost(member);

        Comment comment = Comment.builder()
                .id(1L)
                .post(post)
                .member(member) // 회원 댓글 (member ID = 1L)
                .content("회원 댓글입니다.")
                .deleted(false)
                .password(null) // 회원 댓글은 비밀번호 없음
                .build();

        // When
        boolean result = comment.canModify(userId, password);

        // Then
        assertThat(result).isEqualTo(expected);
    }

    @ParameterizedTest(name = "password={0} → {1}")
    @CsvSource({
            "1234, true",  // 올바른 비밀번호
            "9999, false", // 잘못된 비밀번호
            ", false"      // null 비밀번호
    })
    @DisplayName("익명 댓글 수정/삭제 권한 검증")
    void shouldValidateAnonymousCommentPermission(Integer password, boolean expected) {
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

        // When
        boolean result = comment.canModify(null, password);

        // Then
        assertThat(result).isEqualTo(expected);
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