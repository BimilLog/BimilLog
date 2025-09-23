package jaeik.bimillog.infrastructure.adapter.in.comment;

import jaeik.bimillog.domain.comment.entity.Comment;
import jaeik.bimillog.domain.post.entity.Post;
import jaeik.bimillog.domain.user.entity.*;
import jaeik.bimillog.infrastructure.adapter.in.comment.dto.CommentReqDTO;
import jaeik.bimillog.infrastructure.adapter.out.auth.CustomUserDetails;
import jaeik.bimillog.testutil.TestSettings;
import jaeik.bimillog.testutil.TestUsers;

/**
 * <h2>댓글 테스트 데이터 빌더</h2>
 * <p>댓글 통합 테스트에서 사용할 테스트 데이터를 생성하는 빌더 클래스</p>
 * <p>중복 코드를 제거하고 테스트 가독성을 향상시킵니다.</p>
 *
 * @author Jaeik  
 * @version 2.0.0
 */
public class CommentTestDataBuilder {

    /**
     * <h3>고유한 테스트 사용자 생성</h3>
     * <p>타임스탬프 기반으로 고유한 socialId와 userName을 가진 사용자를 생성합니다.</p>
     *
     * @return User 테스트용 사용자 엔티티
     */
    public static User createTestUser() {
        return TestUsers.createUnique();
    }

    /**
     * <h3>고유한 테스트 사용자 생성 (접두사 지정)</h3>
     * <p>접두사와 타임스탬프 기반으로 고유한 사용자를 생성합니다.</p>
     *
     * @param prefix 사용자 식별 접두사
     * @return User 테스트용 사용자 엔티티
     */
    public static User createTestUser(String prefix) {
        return TestUsers.createUniqueWithPrefix(prefix);
    }

    /**
     * <h3>테스트용 게시글 생성</h3>
     * <p>지정된 사용자로 테스트용 게시글을 생성합니다.</p>
     *
     * @param user 게시글 작성자
     * @return Post 테스트용 게시글 엔티티
     */
    public static Post createTestPost(User user) {
        return createTestPost(user, "테스트 게시글", "테스트 게시글 내용입니다.");
    }

    /**
     * <h3>테스트용 게시글 생성 (제목, 내용 지정)</h3>
     * <p>지정된 제목과 내용으로 테스트용 게시글을 생성합니다.</p>
     *
     * @param user 게시글 작성자
     * @param title 게시글 제목
     * @param content 게시글 내용
     * @return Post 테스트용 게시글 엔티티
     */
    public static Post createTestPost(User user, String title, String content) {
        return Post.builder()
                .title(title)
                .content(content)
                .user(user)
                .build();
    }

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
}