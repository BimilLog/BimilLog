package jaeik.bimillog.domain.notification.application.port.out;

import jaeik.bimillog.domain.notification.application.service.NotificationCommandService;

/**
 * <h2>알림 URL 생성 포트</h2>
 * <p>알림 클릭 시 이동할 페이지 URL을 동적으로 생성하는 포트입니다.</p>
 * <p>게시글 상세 페이지, 롤링페이퍼 페이지 URL 생성</p>
 *
 * @author Jaeik
 * @version 2.0.0
 */
public interface UrlGeneratorPort {

    /**
     * <h3>게시물 URL 생성</h3>
     * <p>댓글 작성 또는 인기글 등극 알림 클릭 시 이동할 게시물 상세 페이지 URL을 생성합니다.</p>
     * <p>프론트엔드의 라우팅 규칙에 따라 게시글 ID를 포함한 URL을 반환합니다.</p>
     * <p>{@link NotificationCommandService}에서 댓글 또는 인기글 등극 알림 생성 시 호출됩니다.</p>
     *
     * @param postId 대상 게시물 ID
     * @return 게시물 상세 페이지 URL (예: /posts/{postId})
     * @author Jaeik
     * @since 2.0.0
     */
    String generatePostUrl(Long postId);

    /**
     * <h3>롤링페이퍼 URL 생성</h3>
     * <p>롤링페이퍼 메시지 작성 알림 클릭 시 이동할 롤링페이퍼 페이지 URL을 생성합니다.</p>
     * <p>프론트엔드의 라우팅 규칙에 따라 사용자명을 기반으로 한 롤링페이퍼 경로를 생성합니다.</p>
     * <p>{@link NotificationCommandService}에서 롤링페이퍼 메시지 작성 알림 생성 시 호출됩니다.</p>
     *
     * @param userName 롤링페이퍼 소유자의 사용자명
     * @return 롤링페이퍼 페이지 URL (예: /papers/{userName})
     * @author Jaeik
     * @since 2.0.0
     */
    String generateRollingPaperUrl(String userName);
}