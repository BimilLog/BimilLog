package jaeik.growfarm.domain.post.application.service;

import jaeik.growfarm.domain.post.application.port.in.PostInteractionUseCase;
import jaeik.growfarm.domain.post.application.port.out.*;
import jaeik.growfarm.domain.post.entity.Post;
import jaeik.growfarm.domain.post.entity.PostLike;
import jaeik.growfarm.domain.user.entity.User;
import jaeik.growfarm.global.exception.CustomException;
import jaeik.growfarm.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * <h2>게시글 상호작용 서비스</h2>
 * <p>PostInteractionUseCase의 구현체로, 게시글의 좋아요 및 조회수 관련 비즈니스 로직을 처리합니다.</p>
 *
 * @author Jaeik
 * @version 2.0.0
 */
@Service
@Transactional
@RequiredArgsConstructor
@Slf4j
public class PostInteractionService implements PostInteractionUseCase {

    private final LoadPostPort loadPostPort;
    private final SavePostLikePort savePostLikePort;
    private final DeletePostLikePort deletePostLikePort;
    private final ExistPostLikePort existPostLikePort;
    private final LoadUserPort loadUserPort;

    /**
     * <h3>게시글 추천</h3>
     * <p>게시글을 추천하거나 추천 취소합니다.</p>
     * <p>이미 추천한 게시글인 경우 추천을 취소하고, 추천하지 않은 게시글인 경우 추천을 추가합니다.</p>
     *
     * @param userId 현재 로그인한 사용자 ID
     * @param postId 추천할 게시글 ID
     * @throws CustomException 게시글을 찾을 수 없는 경우
     * @since 2.0.0
     * @author Jaeik
     */
    @Override
    public void likePost(Long userId, Long postId) {
        User user = loadUserPort.getReferenceById(userId);
        Post post = loadPostPort.findById(postId)
                .orElseThrow(() -> new CustomException(ErrorCode.POST_NOT_FOUND));

        if (existPostLikePort.existsByUserAndPost(user, post)) {
            deletePostLikePort.deleteByUserAndPost(user, post);
            log.debug("Post like removed: userId={}, postId={}", userId, postId);
        } else {
            PostLike postLike = PostLike.builder().user(user).post(post).build();
            savePostLikePort.save(postLike);
            log.debug("Post like added: userId={}, postId={}", userId, postId);
        }
    }

    /**
     * <h3>게시글 조회수 증가</h3>
     * <p>게시글의 조회수를 1 증가시킵니다.</p>
     * <p>엔티티의 더티 체킹에 의해 자동으로 저장됩니다.</p>
     *
     * @param postId 조회수를 증가시킬 게시글 ID
     * @throws CustomException 게시글을 찾을 수 없는 경우
     * @since 2.0.0
     * @author Jaeik
     */
    @Override
    public void incrementViewCount(Long postId) {
        Post post = loadPostPort.findById(postId)
                .orElseThrow(() -> new CustomException(ErrorCode.POST_NOT_FOUND));
        post.incrementView();
        log.debug("Post view count incremented: postId={}, newViewCount={}", postId, post.getViews());
        // @Transactional에 의해 더티 체킹되므로 명시적으로 save할 필요 없음
    }
}