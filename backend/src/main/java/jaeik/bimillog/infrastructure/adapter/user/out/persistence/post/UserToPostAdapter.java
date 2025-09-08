package jaeik.bimillog.infrastructure.adapter.user.out.persistence.post;

import jaeik.bimillog.domain.post.application.port.in.PostQueryUseCase;
import jaeik.bimillog.domain.post.entity.PostSearchResult;
import jaeik.bimillog.domain.user.application.port.out.UserToPostPort;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;

/**
 * <h2>게시글 조회 어댑터</h2>
 * <p>사용자 도메인에서 게시글 도메인의 입력 포트를 통해 접근하는 어댑터</p>
 *
 * @author jaeik
 * @version 2.0.0
 */
@Component
@RequiredArgsConstructor
public class UserToPostAdapter implements UserToPostPort {

    private final PostQueryUseCase postQueryUseCase;

    /**
     * <h3>사용자 작성 게시글 목록 조회</h3>
     * <p>특정 사용자가 작성한 게시글 목록을 게시글 도메인을 통해 조회합니다.</p>
     *
     * @param userId   사용자 ID
     * @param pageable 페이지 정보
     * @return Page<PostSearchResult> 작성한 게시글 목록 페이지
     * @author jaeik
     * @since 2.0.0
     */
    @Override
    public Page<PostSearchResult> findPostsByUserId(Long userId, Pageable pageable) {
        return postQueryUseCase.getUserPosts(userId, pageable);
    }

    /**
     * <h3>사용자 추천한 게시글 목록 조회</h3>
     * <p>특정 사용자가 추천한 게시글 목록을 게시글 도메인을 통해 조회합니다.</p>
     *
     * @param userId   사용자 ID
     * @param pageable 페이지 정보
     * @return Page<PostSearchResult> 추천한 게시글 목록 페이지
     * @author jaeik
     * @since 2.0.0
     */
    @Override
    public Page<PostSearchResult> findLikedPostsByUserId(Long userId, Pageable pageable) {
        return postQueryUseCase.getUserLikedPosts(userId, pageable);
    }
}