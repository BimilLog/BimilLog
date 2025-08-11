
package jaeik.growfarm.domain.notification.application.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class NotificationUrlGenerator {

    private final String baseUrl;

    public NotificationUrlGenerator(@Value("${url}") String baseUrl) {
        this.baseUrl = baseUrl;
    }

    public String generatePostUrl(Long postId) {
        return baseUrl + "/board/post/" + postId;
    }

    public String generateRollingPaperUrl(String userName) {
        return baseUrl + "/rolling-paper/" + userName;
    }
}
