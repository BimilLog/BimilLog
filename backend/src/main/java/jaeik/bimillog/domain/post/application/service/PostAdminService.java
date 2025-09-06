package jaeik.bimillog.domain.post.application.service;

import jaeik.bimillog.domain.post.application.port.in.PostAdminUseCase;
import jaeik.bimillog.domain.post.application.port.out.PostCommandPort;
import jaeik.bimillog.domain.post.application.port.out.PostQueryPort;
import jaeik.bimillog.domain.post.entity.Post;
import jaeik.bimillog.domain.post.exception.PostCustomException;
import jaeik.bimillog.domain.post.exception.PostErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * <h2>게시글 관리자 서비스</h2>
 * <p>PostAdminUseCase의 구현체로, 게시글 공지사항 설정/해제 핵심 비즈니스 로직을 처리합니다.</p>
 * <p>관리자 권한이 필요한 기능들을 담당하며, 캐시 동기화는 Controller에서 별도 처리됩니다.</p>
 *
 * @author Jaeik
 * @version 2.0.0
 */
@Service
@Transactional
@RequiredArgsConstructor
@Slf4j
public class PostAdminService implements PostAdminUseCase {

    private final PostQueryPort postQueryPort;
    private final PostCommandPort postCommandPort;

    /**
     * <h3>게시글 공지 토글</h3>
     * <p>게시글의 공지 설정을 토글합니다. 현재 공지이면 해제하고, 공지가 아니면 설정합니다.</p>
     * <p>순수한 비즈니스 로직만 처리하며, 캐시 동기화는 Controller에서 별도 처리됩니다.</p>
     *
     * @param postId 공지 토글할 게시글 ID
     * @throws PostCustomException 게시글을 찾을 수 없는 경우
     * @since 2.0.0
     * @author Jaeik
     */
    @Override
    public void togglePostNotice(Long postId) {
        Post post = postQueryPort.findById(postId)
                .orElseThrow(() -> new PostCustomException(PostErrorCode.POST_NOT_FOUND));
        
        if (post.isNotice()) {
            post.unsetAsNotice();
            log.info("공지사항 해제: postId={}, title={}", postId, post.getTitle());
        } else {
            post.setAsNotice();
            log.info("공지사항 설정: postId={}, title={}", postId, post.getTitle());
        }
        
        postCommandPort.save(post);
        log.info("게시글 공지 토글 DB 업데이트 완료: postId={}, isNotice={}", postId, post.isNotice());
    }

    /**
     * {@inheritDoc}
     * 
     * <p>게시글을 조회하여 현재 공지 상태를 반환합니다.</p>
     * <p>캐시 동기화를 위한 상태 확인에 사용됩니다.</p>
     */
    @Override
    public boolean isPostNotice(Long postId) {
        Post post = postQueryPort.findById(postId)
                .orElseThrow(() -> new PostCustomException(PostErrorCode.POST_NOT_FOUND));
        
        boolean isNotice = post.isNotice();
        log.debug("게시글 공지 상태 조회: postId={}, isNotice={}", postId, isNotice);
        return isNotice;
    }
}