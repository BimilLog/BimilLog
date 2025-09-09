package jaeik.bimillog.infrastructure.adapter.comment.out.post;

import jaeik.bimillog.domain.comment.application.port.out.CommentToPostPort;
import jaeik.bimillog.domain.post.application.port.in.PostQueryUseCase;
import jaeik.bimillog.domain.post.entity.Post;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * <h2>Comment-Post 도메인 간 통신 어댑터</h2>
 * <p>
 * Comment 도메인에서 Post 도메인에 접근하기 위한 아웃바운드 어댑터입니다.
 * </p>
 * <p>
 * 헥사고날 아키텍처에서 도메인 간 분리를 유지하면서 Comment 도메인이 
 * Post 도메인의 데이터에 접근할 수 있도록 중개 역할을 수행합니다.
 * </p>
 * <p>
 * CommentService에서 댓글이 속한 게시글의 정보를 조회할 때 사용됩니다.
 * </p>
 *
 * @author Jaeik
 * @version 2.0.0
 */
@Component
@RequiredArgsConstructor
public class CommentToPostAdapter implements CommentToPostPort {

    private final PostQueryUseCase postQueryUseCase;

    /**
     * <h3>게시글 ID로 게시글 조회</h3>
     * <p>Post 도메인의 데이터를 조회합니다.</p>
     * <p>Comment 도메인에서 댓글이 속한 게시글의 정보를 확인할 때 사용됩니다.</p>
     * <p>Post 도메인의 예외 처리를 위임하여 Comment 서비스는 순수한 Post 엔티티를 받습니다.</p>
     *
     * @param postId 조회할 게시글 ID
     * @return Post 조회된 게시글 엔티티
     * @author Jaeik
     * @since 2.0.0
     */
    @Override
    public Post findById(Long postId) {
        return postQueryUseCase.findById(postId);
    }
}
