package jaeik.bimillog.domain.post.application.service;

import jaeik.bimillog.domain.post.application.port.in.PostInteractionUseCase;
import jaeik.bimillog.domain.post.application.port.out.*;
import jaeik.bimillog.domain.post.application.port.out.LoadUserInfoPort;
import jaeik.bimillog.domain.post.entity.Post;
import jaeik.bimillog.domain.post.entity.PostLike;
import jaeik.bimillog.domain.user.entity.User;
import jaeik.bimillog.domain.post.exception.PostCustomException;
import jaeik.bimillog.domain.post.exception.PostErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

/**
 * <h2>게시글 상호작용 서비스</h2>
 * <p>PostInteractionUseCase의 구현체로, 게시글의 추천 및 조회수 관련 비즈니스 로직을 처리합니다.</p>
 * <p>헥사고널 아키텍처 Use Case Implementation</p>
 *
 * @author Jaeik
 * @version 2.0.0
 */
@Service
@Transactional
@RequiredArgsConstructor
@Slf4j
public class PostInteractionService implements PostInteractionUseCase {

    private final PostCommandPort postCommandPort;
    private final PostQueryPort postQueryPort;
    private final PostLikeCommandPort postLikeCommandPort;
    private final PostLikeQueryPort postLikeQueryPort;
    private final LoadUserInfoPort loadUserInfoPort;

    /**
     * <h3>게시글 추천</h3>
     * <p>게시글을 추천하거나 추천 취소합니다.</p>
     * <p>이미 추천한 게시글인 경우 추천을 취소하고, 추천하지 않은 게시글인 경우 추천을 추가합니다.</p>
     *
     * @param userId 현재 로그인한 사용자 ID
     * @param postId 추천할 게시글 ID
     * @throws PostCustomException 게시글을 찾을 수 없는 경우
     * @since 2.0.0
     * @author Jaeik
     */
    @Override
    public void likePost(Long userId, Long postId) {
        User user = loadUserInfoPort.getReferenceById(userId);
        Post post = postQueryPort.findById(postId)
                .orElseThrow(() -> new PostCustomException(PostErrorCode.POST_NOT_FOUND));

        if (postLikeQueryPort.existsByUserAndPost(user, post)) {
            postLikeCommandPort.deleteByUserAndPost(user, post);
            log.debug("게시글 추천 취소됨: userId={}, postId={}", userId, postId);
        } else {
            PostLike postLike = PostLike.builder().user(user).post(post).build();
            postLikeCommandPort.save(postLike);
            log.debug("게시글 추천됨: userId={}, postId={}", userId, postId);
        }
    }

    /**
     * <h3>조회수 증가</h3>
     * <p>게시글의 조회수를 1 증가시킵니다.</p>
     * <p>Controller에서 이미 게시글 존재를 검증했으므로 직접 UPDATE 쿼리만 실행하여 성능을 최적화합니다.</p>
     *
     * @param postId 조회수를 증가시킬 게시글 ID
     * @since 2.0.0
     * @author Jaeik
     */
    @Override
    public void incrementViewCount(Long postId) {
        postCommandPort.incrementViewByPostId(postId);
        log.debug("게시글 조회수 증가됨: postId={}", postId);
    }
}