package jaeik.bimillog.testutil.builder;

import jaeik.bimillog.domain.comment.dto.CommentReqDTO;
import jaeik.bimillog.domain.comment.entity.Comment;
import jaeik.bimillog.domain.member.entity.Member;
import jaeik.bimillog.domain.post.entity.jpa.Post;

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

}