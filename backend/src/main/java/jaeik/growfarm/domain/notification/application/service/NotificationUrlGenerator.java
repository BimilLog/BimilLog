
package jaeik.growfarm.domain.notification.application.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * <h2>알림 URL 생성기</h2>
 * <p>다양한 알림 유형에 대한 URL을 생성하는 컴포넌트</p>
 *
 * @author Jaeik
 * @version 2.0.0
 */
@Component
public class NotificationUrlGenerator {

    private final String baseUrl;

    public NotificationUrlGenerator(@Value("${url}") String baseUrl) {
        this.baseUrl = baseUrl;
    }

    /**
     * <h3>게시물 URL 생성</h3>
     * <p>주어진 게시물 ID에 해당하는 게시물 상세 페이지 URL을 생성합니다.</p>
     *
     * @param postId 게시물 ID
     * @return 게시물 URL
     * @author Jaeik
     * @since 2.0.0
     */
    public String generatePostUrl(Long postId) {
        return baseUrl + "/board/post/" + postId;
    }

    /**
     * <h3>롤링페이퍼 URL 생성</h3>
     * <p>주어진 사용자 이름에 해당하는 롤링페이퍼 페이지 URL을 생성합니다.</p>
     *
     * @param userName 롤링페이퍼 주인의 사용자 이름
     * @return 롤링페이퍼 URL
     * @author Jaeik
     * @since 2.0.0
     */
    public String generateRollingPaperUrl(String userName) {
        return baseUrl + "/rolling-paper/" + userName;
    }
}
