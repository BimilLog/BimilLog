package jaeik.bimillog.domain.notification.application.port.out;

/**
 * <h2>알림 URL 생성 포트</h2>
 * <p>알림과 관련된 URL 생성 기능을 정의하는 Secondary Port</p>
 * <p>헥사고날 아키텍처 원칙에 따라 도메인에서 인프라 관심사를 분리</p>
 *
 * @author Jaeik
 * @version 2.0.0
 */
public interface NotificationUrlPort {

    /**
     * <h3>게시물 URL 생성</h3>
     * <p>주어진 게시물 ID에 해당하는 게시물 상세 페이지 URL을 생성합니다.</p>
     *
     * @param postId 게시물 ID
     * @return 게시물 URL
     * @author Jaeik
     * @since 2.0.0
     */
    String generatePostUrl(Long postId);

    /**
     * <h3>롤링페이퍼 URL 생성</h3>
     * <p>주어진 사용자 이름에 해당하는 롤링페이퍼 페이지 URL을 생성합니다.</p>
     *
     * @param userName 롤링페이퍼 주인의 사용자 이름
     * @return 롤링페이퍼 URL
     * @author Jaeik
     * @since 2.0.0
     */
    String generateRollingPaperUrl(String userName);
}