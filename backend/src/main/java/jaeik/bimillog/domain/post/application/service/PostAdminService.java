package jaeik.bimillog.domain.post.application.service;

import jaeik.bimillog.domain.post.application.port.in.PostAdminUseCase;
import jaeik.bimillog.domain.post.application.port.out.PostQueryPort;
import jaeik.bimillog.domain.post.application.port.out.PostCommandPort;
import jaeik.bimillog.domain.post.entity.Post;
import jaeik.bimillog.domain.post.event.PostSetAsNoticeEvent;
import jaeik.bimillog.domain.post.event.PostUnsetAsNoticeEvent;
import jaeik.bimillog.infrastructure.exception.CustomException;
import jaeik.bimillog.infrastructure.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * <h2>게시글 관리자 서비스</h2>
 * <p>PostAdminUseCase의 구현체로, 게시글 공지사항 설정/해제 관련 비즈니스 로직을 처리합니다.</p>
 * <p>관리자 권한이 필요한 기능들을 담당합니다.</p>
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
    private final ApplicationEventPublisher eventPublisher;

    /**
     * <h3>게시글 공지 설정</h3>
     * <p>특정 게시글을 공지로 설정합니다.</p>
     * <p>설정 후 PostSetAsNoticeEvent를 발행하여 공지사항 캐시 무효화를 트리거합니다.</p>
     *
     * @param postId 공지로 설정할 게시글 ID
     * @throws CustomException 게시글을 찾을 수 없는 경우
     * @since 2.0.0
     * @author Jaeik
     */
    @Override
    public void setPostAsNotice(Long postId) {
        Post post = postQueryPort.findById(postId)
                .orElseThrow(() -> new CustomException(ErrorCode.POST_NOT_FOUND));
        
        post.setAsNotice();
        postCommandPort.save(post);
        log.info("공지사항 설정: postId={}, title={}", postId, post.getTitle());
        
        eventPublisher.publishEvent(new PostSetAsNoticeEvent(postId));
    }

    /**
     * <h3>게시글 공지 해제</h3>
     * <p>게시글의 공지 설정을 해제합니다.</p>
     * <p>해제 후 PostUnsetAsNoticeEvent를 발행하여 공지사항 캐시 무효화를 트리거합니다.</p>
     *
     * @param postId 공지 설정을 해제할 게시글 ID
     * @throws CustomException 게시글을 찾을 수 없는 경우
     * @since 2.0.0
     * @author Jaeik
     */
    @Override
    public void unsetPostAsNotice(Long postId) {
        Post post = postQueryPort.findById(postId)
                .orElseThrow(() -> new CustomException(ErrorCode.POST_NOT_FOUND));
        
        post.unsetAsNotice();
        postCommandPort.save(post);
        log.info("공지사항 해제: postId={}, title={}", postId, post.getTitle());
        
        eventPublisher.publishEvent(new PostUnsetAsNoticeEvent(postId));
    }
}