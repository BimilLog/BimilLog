package jaeik.bimillog.infrastructure.adapter.comment.out.persistence.post;

import jaeik.bimillog.domain.comment.application.port.out.LoadPostPort;
import jaeik.bimillog.domain.post.application.port.in.PostQueryUseCase;
import jaeik.bimillog.domain.post.entity.Post;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Optional;

/**
 * <h2>게시글 어댑터</h2>
 * <p>
 * Comment 도메인에서 Post 도메인에 접근하기 위한 아웃바운드 어댑터
 * </p>
 *
 * @author Jaeik
 * @version 2.0.0
 */
@Component
@RequiredArgsConstructor
public class CommentLoadPostAdapter implements LoadPostPort {

    private final PostQueryUseCase postQueryUseCase;

    /**
     * <h3>게시글 ID로 게시글 조회</h3>
     * <p>주어진 ID로 게시글을 조회합니다.</p>
     * <p>Post 도메인의 PostQuery 요구사항을 위임하여 사용합니다.</p>
     *
     * @param postId 게시글 ID
     * @return Optional<Post> 조회된 게시글 엔티티. 존재하지 않으면 Optional.empty()
     * @author Jaeik
     * @since 2.0.0
     */
    @Override
    public Optional<Post> findById(Long postId) {
        return postQueryUseCase.findById(postId);
    }
}
