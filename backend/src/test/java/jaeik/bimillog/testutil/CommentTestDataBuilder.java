package jaeik.bimillog.testutil;

import jaeik.bimillog.domain.comment.entity.Comment;
import jaeik.bimillog.domain.post.entity.Post;
import jaeik.bimillog.domain.user.entity.ExistingUserDetail;
import jaeik.bimillog.domain.user.entity.User;
import jaeik.bimillog.infrastructure.adapter.in.comment.dto.CommentReqDTO;
import jaeik.bimillog.infrastructure.adapter.out.auth.CustomUserDetails;

/**
 * <h2>댓글 테스트 데이터 빌더</h2>
 * <p>댓글 통합 테스트에서 사용할 테스트 데이터를 생성하는 빌더 클래스</p>
 * <p>중복 코드를 제거하고 테스트 가독성을 향상시킵니다.</p>
 *
 * @author Jaeik  
 * @version 2.0.0
 */
public class CommentTestDataBuilder {

    // TestUsers 클래스의 메서드를 사용하세요:
    // - TestUsers.createUnique() : 고유한 사용자 생성
    // - TestUsers.createUniqueWithPrefix(prefix) : 접두사로 고유한 사용자 생성
    // TestFixtures 클래스의 메서드를 사용하세요:
    // - TestFixtures.createPostWithUser(user) : 기본 게시글 생성
    // - TestFixtures.createPostWithId(id, user, title, content) : ID 포함 게시글 생성

    /**
     * <h3>테스트용 댓글 생성</h3>
     * <p>지정된 사용자와 게시글로 테스트용 댓글을 생성합니다.</p>
     *
     * @param user 댓글 작성자
     * @param post 댓글이 달린 게시글
     * @param content 댓글 내용
     * @return Comment 테스트용 댓글 엔티티
     */
    public static Comment createTestComment(User user, Post post, String content) {
        return Comment.builder()
                .content(content)
                .user(user)
                .post(post)
                .deleted(false)
                .build();
    }

    /**
     * <h3>테스트용 댓글 요청 DTO 생성</h3>
     * <p>API 호출에 사용할 댓글 요청 DTO를 생성합니다.</p>
     *
     * @param postId 게시글 ID
     * @param content 댓글 내용
     * @return CommentReqDTO 테스트용 댓글 요청 DTO
     */
    public static CommentReqDTO createCommentReqDTO(Long postId, String content) {
        CommentReqDTO requestDto = new CommentReqDTO();
        requestDto.setPostId(postId);
        requestDto.setContent(content);
        return requestDto;
    }

    /**
     * <h3>테스트용 대댓글 요청 DTO 생성</h3>
     * <p>부모 댓글이 있는 대댓글 요청 DTO를 생성합니다.</p>
     *
     * @param postId 게시글 ID
     * @param parentId 부모 댓글 ID
     * @param content 댓글 내용
     * @return CommentReqDTO 테스트용 대댓글 요청 DTO
     */
    public static CommentReqDTO createReplyCommentReqDTO(Long postId, Long parentId, String content) {
        CommentReqDTO requestDto = createCommentReqDTO(postId, content);
        requestDto.setParentId(parentId);
        return requestDto;
    }

    /**
     * <h3>테스트용 댓글 수정 요청 DTO 생성</h3>
     * <p>댓글 수정에 사용할 요청 DTO를 생성합니다.</p>
     *
     * @param commentId 수정할 댓글 ID
     * @param content 새 댓글 내용
     * @return CommentReqDTO 테스트용 댓글 수정 요청 DTO
     */
    public static CommentReqDTO createUpdateCommentReqDTO(Long commentId, String content) {
        CommentReqDTO requestDto = new CommentReqDTO();
        requestDto.setId(commentId);
        requestDto.setContent(content);
        return requestDto;
    }

    /**
     * <h3>테스트용 댓글 삭제 요청 DTO 생성</h3>
     * <p>댓글 삭제에 사용할 요청 DTO를 생성합니다.</p>
     *
     * @param commentId 삭제할 댓글 ID
     * @return CommentReqDTO 테스트용 댓글 삭제 요청 DTO
     */
    public static CommentReqDTO createDeleteCommentReqDTO(Long commentId) {
        CommentReqDTO requestDto = new CommentReqDTO();
        requestDto.setId(commentId);
        return requestDto;
    }

    /**
     * <h3>테스트용 익명 댓글 삭제 요청 DTO 생성</h3>
     * <p>비밀번호가 필요한 익명 댓글 삭제 요청 DTO를 생성합니다.</p>
     *
     * @param commentId 삭제할 댓글 ID
     * @param password 댓글 비밀번호
     * @return CommentReqDTO 테스트용 익명 댓글 삭제 요청 DTO
     */
    public static CommentReqDTO createAnonymousDeleteCommentReqDTO(Long commentId, Integer password) {
        CommentReqDTO requestDto = createDeleteCommentReqDTO(commentId);
        requestDto.setPassword(password);
        return requestDto;
    }


    /**
     * <h3>테스트용 CustomUserDetails 생성</h3>
     * <p>Spring Security 테스트에 사용할 CustomUserDetails를 생성합니다.</p>
     *
     * @param user 사용자 엔티티
     * @return CustomUserDetails 테스트용 사용자 인증 정보
     */
    public static CustomUserDetails createUserDetails(User user) {
        ExistingUserDetail userDetail = ExistingUserDetail.builder()
                .userId(user.getId())
                .socialId(user.getSocialId())
                .socialNickname(user.getSocialNickname())
                .thumbnailImage(user.getThumbnailImage())
                .userName(user.getUserName())
                .provider(user.getProvider())
                .role(user.getRole())
                .build();

        return new CustomUserDetails(userDetail);
    }

    /**
     * <h3>ID가 포함된 테스트용 댓글 생성</h3>
     * <p>지정된 ID를 가진 댓글을 생성합니다.</p>
     *
     * @param id 댓글 ID
     * @param user 댓글 작성자
     * @param post 댓글이 달린 게시글
     * @param content 댓글 내용
     * @return Comment 테스트용 댓글 엔티티
     */
    public static Comment createTestCommentWithId(Long id, User user, Post post, String content) {
        Comment comment = createTestComment(user, post, content);
        TestFixtures.setFieldValue(comment, "id", id);
        return comment;
    }

    /**
     * <h3>테스트용 익명 댓글 생성</h3>
     * <p>비밀번호가 있는 익명 댓글을 생성합니다.</p>
     *
     * @param post 댓글이 달린 게시글
     * @param content 댓글 내용
     * @param password 댓글 비밀번호
     * @return Comment 테스트용 익명 댓글 엔티티
     */
    public static Comment createAnonymousComment(Post post, String content, Integer password) {
        return Comment.createComment(post, null, content, password);
    }

    /**
     * <h3>ID가 포함된 익명 댓글 생성</h3>
     * <p>지정된 ID를 가진 익명 댓글을 생성합니다.</p>
     *
     * @param id 댓글 ID
     * @param post 댓글이 달린 게시글
     * @param content 댓글 내용
     * @param password 댓글 비밀번호
     * @return Comment 테스트용 익명 댓글 엔티티
     */
    public static Comment createAnonymousCommentWithId(Long id, Post post, String content, Integer password) {
        Comment comment = createAnonymousComment(post, content, password);
        TestFixtures.setFieldValue(comment, "id", id);
        return comment;
    }

    /**
     * <h3>테스트용 대댓글 생성</h3>
     * <p>부모 댓글이 있는 대댓글을 생성합니다.</p>
     *
     * @param user 댓글 작성자
     * @param post 댓글이 달린 게시글
     * @param parent 부모 댓글
     * @param content 댓글 내용
     * @return Comment 테스트용 대댓글 엔티티
     */
    public static Comment createReplyComment(User user, Post post, Comment parent, String content) {
        Comment reply = createTestComment(user, post, content);
        TestFixtures.setFieldValue(reply, "parent", parent);
        return reply;
    }

    /**
     * <h3>ID가 포함된 대댓글 생성</h3>
     * <p>지정된 ID를 가진 대댓글을 생성합니다.</p>
     *
     * @param id 댓글 ID
     * @param user 댓글 작성자
     * @param post 댓글이 달린 게시글
     * @param parent 부모 댓글
     * @param content 댓글 내용
     * @return Comment 테스트용 대댓글 엔티티
     */
    public static Comment createReplyCommentWithId(Long id, User user, Post post, Comment parent, String content) {
        Comment reply = createReplyComment(user, post, parent, content);
        TestFixtures.setFieldValue(reply, "id", id);
        return reply;
    }

    /**
     * <h3>삭제된 댓글 생성</h3>
     * <p>소프트 삭제된 상태의 댓글을 생성합니다.</p>
     *
     * @param user 댓글 작성자
     * @param post 댓글이 달린 게시글
     * @param content 댓글 내용
     * @return Comment 삭제된 댓글 엔티티
     */
    public static Comment createDeletedComment(User user, Post post, String content) {
        Comment comment = createTestComment(user, post, content);
        TestFixtures.setFieldValue(comment, "deleted", true);
        return comment;
    }

    /**
     * <h3>ID가 포함된 삭제된 댓글 생성</h3>
     * <p>지정된 ID를 가진 삭제된 댓글을 생성합니다.</p>
     *
     * @param id 댓글 ID
     * @param user 댓글 작성자
     * @param post 댓글이 달린 게시글
     * @param content 댓글 내용
     * @return Comment 삭제된 댓글 엔티티
     */
    public static Comment createDeletedCommentWithId(Long id, User user, Post post, String content) {
        Comment comment = createDeletedComment(user, post, content);
        TestFixtures.setFieldValue(comment, "id", id);
        return comment;
    }

    /**
     * <h3>테스트용 CommentLike 생성</h3>
     * <p>댓글 좋아요를 생성합니다.</p>
     *
     * @param comment 댓글
     * @param user 좋아요한 사용자
     * @return CommentLike 댓글 좋아요 엔티티
     */
    public static jaeik.bimillog.domain.comment.entity.CommentLike createCommentLike(Comment comment, User user) {
        return jaeik.bimillog.domain.comment.entity.CommentLike.builder()
                .comment(comment)
                .user(user)
                .build();
    }

    /**
     * <h3>테스트용 CommentClosure 생성</h3>
     * <p>댓글 계층 구조를 위한 클로저 테이블 엔트리를 생성합니다.</p>
     *
     * @param ancestor 조상 댓글
     * @param descendant 자손 댓글
     * @param depth 계층 깊이
     * @return CommentClosure 댓글 클로저 엔티티
     */
    public static jaeik.bimillog.domain.comment.entity.CommentClosure createCommentClosure(
            Comment ancestor, Comment descendant, int depth) {
        return jaeik.bimillog.domain.comment.entity.CommentClosure.createCommentClosure(
                ancestor, descendant, depth);
    }

    /**
     * <h3>계층형 댓글 구조 생성</h3>
     * <p>부모-자식 관계가 설정된 댓글 구조를 생성합니다.</p>
     *
     * @param post 게시글
     * @param parentUser 부모 댓글 작성자
     * @param childUser 자식 댓글 작성자
     * @return 배열 [부모 댓글, 자식 댓글]
     */
    public static Comment[] createCommentHierarchy(Post post, User parentUser, User childUser) {
        Comment parent = createTestCommentWithId(100L, parentUser, post, "부모 댓글");
        Comment child = createReplyCommentWithId(200L, childUser, post, parent, "자식 댓글");
        return new Comment[] {parent, child};
    }

    /**
     * <h3>댓글 좋아요 관계 설정</h3>
     * <p>여러 사용자가 댓글에 좋아요한 상태를 설정합니다.</p>
     *
     * @param comment 대상 댓글
     * @param users 좋아요한 사용자들
     * @return CommentLike 리스트
     */
    public static java.util.List<jaeik.bimillog.domain.comment.entity.CommentLike> createCommentLikes(
            Comment comment, User... users) {
        java.util.List<jaeik.bimillog.domain.comment.entity.CommentLike> likes = new java.util.ArrayList<>();
        for (User user : users) {
            likes.add(createCommentLike(comment, user));
        }
        return likes;
    }
}