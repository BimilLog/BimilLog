package jaeik.bimillog.testutil.builder;

import jaeik.bimillog.domain.comment.dto.CommentReqDTO;
import jaeik.bimillog.domain.comment.entity.Comment;
import jaeik.bimillog.domain.member.entity.Member;
import jaeik.bimillog.domain.post.entity.Post;
import jaeik.bimillog.testutil.fixtures.TestFixtures;

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
     * <h3>테스트용 댓글 생성 (기본)</h3>
     * <p>지정된 회원과 게시글로 테스트용 댓글을 생성합니다.</p>
     *
     * @param post 댓글이 달린 게시글
     * @param member 댓글 작성자
     * @param content 댓글 내용
     * @return Comment 테스트용 댓글 엔티티
     */
    public static Comment createComment(Post post, Member member, String content) {
        return Comment.createComment(post, member, content, null);
    }
    
    /**
     * ID가 설정된 Comment 엔티티 생성
     */
    public static Comment withId(Long id, Comment comment) {
        TestFixtures.setFieldValue(comment, "id", id);
        return comment;
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
}