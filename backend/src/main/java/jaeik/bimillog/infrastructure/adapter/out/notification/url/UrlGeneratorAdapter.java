
package jaeik.bimillog.infrastructure.adapter.out.notification.url;

import jaeik.bimillog.domain.notification.application.port.out.UrlGeneratorPort;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * <h2>알림 URL 생성 어댑터</h2>
 * <p>알림 URL 생성을 담당하는 어댑터입니다.</p>
 * <p>게시글 URL 생성, 롤링페이퍼 URL 생성</p>
 *
 * @author Jaeik
 * @version 2.0.0
 */
@Component
public class UrlGeneratorAdapter implements UrlGeneratorPort {

    private final String baseUrl;

    /**
     * <h3>생성자</h3>
     * <p>기본 URL을 설정합니다.</p>
     *
     * @param baseUrl 애플리케이션의 기본 URL
     * @author Jaeik
     * @since 2.0.0
     */
    public UrlGeneratorAdapter(@Value("${url}") String baseUrl) {
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
    @Override
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
    @Override
    public String generateRollingPaperUrl(String userName) {
        return baseUrl + "/rolling-paper/" + userName;
    }
}
