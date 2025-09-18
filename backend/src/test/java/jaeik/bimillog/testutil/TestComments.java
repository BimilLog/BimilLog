package jaeik.bimillog.testutil;

import jaeik.bimillog.domain.comment.entity.Comment;
import jaeik.bimillog.domain.post.entity.Post;
import jaeik.bimillog.domain.user.entity.User;

/**
 * <h2>미리 정의된 테스트 댓글 인스턴스</h2>
 * <p>테스트에서 바로 사용할 수 있는 사전 정의된 댓글 객체들</p>
 * <p>성능 향상 및 코드 간소화를 위해 객체 재사용</p>
 *
 * @author Jaeik
 * @version 2.0.0
 */
public class TestComments {

    // 미리 정의된 댓글 인스턴스들
    public static final Comment USER_COMMENT;
    public static final Comment ANONYMOUS_COMMENT;
    public static final Comment DELETED_COMMENT;
    public static final Comment ADMIN_COMMENT;
    public static final Comment LONG_COMMENT;
    public static final Comment REPLY_COMMENT;
    public static final Comment ANONYMIZED_COMMENT;

    static {
        // 일반 회원 댓글
        USER_COMMENT = Comment.builder()
                .id(1L)
                .post(TestPosts.NORMAL_POST)
                .user(TestUsers.USER1)
                .content("좋은 글이네요!")
                .deleted(false)
                .password(null)
                .build();

        // 익명 댓글 (비밀번호 포함)
        ANONYMOUS_COMMENT = Comment.builder()
                .id(2L)
                .post(TestPosts.NORMAL_POST)
                .user(null)
                .content("익명으로 댓글을 남깁니다.")
                .deleted(false)
                .password(1234)
                .build();

        // 삭제된 댓글
        DELETED_COMMENT = Comment.builder()
                .id(3L)
                .post(TestPosts.NORMAL_POST)
                .user(TestUsers.USER2)
                .content("삭제된 댓글입니다")
                .deleted(true)
                .password(null)
                .build();

        // 관리자 댓글
        ADMIN_COMMENT = Comment.builder()
                .id(4L)
                .post(TestPosts.NOTICE_POST)
                .user(TestUsers.ADMIN)
                .content("관리자가 남기는 댓글입니다.")
                .deleted(false)
                .password(null)
                .build();

        // 긴 댓글
        LONG_COMMENT = Comment.builder()
                .id(5L)
                .post(TestPosts.POPULAR_POST)
                .user(TestUsers.USER3)
                .content("이것은 매우 긴 댓글입니다. 이것은 매우 긴 댓글입니다. 이것은 매우 긴 댓글입니다. 이것은 매우 긴 댓글입니다. 이것은 매우 긴 댓글입니다. 이것은 매우 긴 댓글입니다. 테스트용으로 작성된 긴 댓글의 예시입니다.")
                .deleted(false)
                .password(null)
                .build();

        // 대댓글
        REPLY_COMMENT = Comment.builder()
                .id(6L)
                .post(TestPosts.NORMAL_POST)
                .user(TestUsers.USER2)
                .content("@testUser1 좋은 의견이네요!")
                .deleted(false)
                .password(null)
                .build();

        // 익명화된 댓글 (탈퇴 사용자)
        ANONYMIZED_COMMENT = Comment.builder()
                .id(7L)
                .post(TestPosts.NORMAL_POST)
                .user(null)
                .content("탈퇴한 사용자의 댓글입니다")
                .deleted(true)
                .password(null)
                .build();
    }

    /**
     * 특정 ID를 가진 댓글 생성
     */
    public static Comment withId(Long id) {
        return Comment.builder()
                .id(id)
                .post(USER_COMMENT.getPost())
                .user(USER_COMMENT.getUser())
                .content(USER_COMMENT.getContent())
                .deleted(USER_COMMENT.isDeleted())
                .password(USER_COMMENT.getPassword())
                .build();
    }

    /**
     * 기존 댓글을 복사하며 특정 ID 설정
     */
    public static Comment copyWithId(Comment comment, Long id) {
        return Comment.builder()
                .id(id)
                .post(comment.getPost())
                .user(comment.getUser())
                .content(comment.getContent())
                .deleted(comment.isDeleted())
                .password(comment.getPassword())
                .build();
    }

    /**
     * 특정 게시글에 대한 댓글 생성
     */
    public static Comment withPost(Post post) {
        return Comment.builder()
                .post(post)
                .user(USER_COMMENT.getUser())
                .content(USER_COMMENT.getContent())
                .deleted(false)
                .password(null)
                .build();
    }

    /**
     * 특정 사용자의 댓글 생성
     */
    public static Comment withUser(User user) {
        return Comment.builder()
                .post(USER_COMMENT.getPost())
                .user(user)
                .content(USER_COMMENT.getContent())
                .deleted(false)
                .password(null)
                .build();
    }

    /**
     * 특정 내용의 댓글 생성
     */
    public static Comment withContent(String content) {
        return Comment.builder()
                .post(USER_COMMENT.getPost())
                .user(USER_COMMENT.getUser())
                .content(content)
                .deleted(false)
                .password(null)
                .build();
    }

    /**
     * 익명 댓글 생성 (비밀번호 포함)
     */
    public static Comment anonymous(String content, Integer password) {
        return Comment.builder()
                .post(USER_COMMENT.getPost())
                .user(null)
                .content(content)
                .deleted(false)
                .password(password)
                .build();
    }

    /**
     * 특정 비밀번호를 가진 익명 댓글 생성
     */
    public static Comment withPassword(Integer password) {
        return Comment.builder()
                .post(USER_COMMENT.getPost())
                .user(null)
                .content("익명 댓글입니다")
                .deleted(false)
                .password(password)
                .build();
    }

    /**
     * 삭제된 댓글 생성
     */
    public static Comment deleted() {
        return Comment.builder()
                .post(USER_COMMENT.getPost())
                .user(USER_COMMENT.getUser())
                .content("삭제된 댓글입니다")
                .deleted(true)
                .password(null)
                .build();
    }

    /**
     * 익명화된 댓글 생성 (탈퇴 사용자)
     */
    public static Comment anonymized() {
        return Comment.builder()
                .post(USER_COMMENT.getPost())
                .user(null)
                .content("탈퇴한 사용자의 댓글입니다")
                .deleted(true)
                .password(null)
                .build();
    }

    /**
     * 특정 게시글과 사용자의 댓글 생성
     */
    public static Comment withPostAndUser(Post post, User user) {
        return Comment.builder()
                .post(post)
                .user(user)
                .content("테스트 댓글입니다")
                .deleted(false)
                .password(null)
                .build();
    }

    /**
     * 특정 게시글의 익명 댓글 생성
     */
    public static Comment anonymousForPost(Post post, String content, Integer password) {
        return Comment.builder()
                .post(post)
                .user(null)
                .content(content)
                .deleted(false)
                .password(password)
                .build();
    }

    /**
     * 커스텀 댓글 생성 (모든 속성 지정)
     */
    public static Comment custom(Post post, User user, String content,
                               boolean deleted, Integer password) {
        return Comment.builder()
                .post(post)
                .user(user)
                .content(content)
                .deleted(deleted)
                .password(password)
                .build();
    }

    // Private constructor to prevent instantiation
    private TestComments() {}
}