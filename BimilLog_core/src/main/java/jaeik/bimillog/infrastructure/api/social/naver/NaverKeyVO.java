package jaeik.bimillog.infrastructure.api.social.naver;

import lombok.Getter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * <h2>네이버 API 키 정보 클래스</h2>
 * <p>네이버 API와 관련된 키 정보를 관리하는 클래스</p>
 *
 * @author Jaeik
 * @version 2.0.0
 */
@Component
@Getter
public class NaverKeyVO {

    @Value("${spring.naver.client-id}")
    private String CLIENT_ID; // 애플리케이션 클라이언트 아이디

    @Value("${spring.naver.client-secret}")
    private String CLIENT_SECRET; // 애플리케이션 클라이언트 시크릿

    @Value("${spring.naver.redirect-uri}")
    private String REDIRECT_URI; // 서비스 URL (Callback URL)

    // 인증 관련 URL
    private final String AUTHORIZE_URL = "https://nid.naver.com/oauth2.0/authorize"; // 인증 요청 URL
    private final String TOKEN_URL = "https://nid.naver.com/oauth2.0/token"; // 토큰 발급/갱신/삭제 URL

    // 사용자 정보 조회 URL
    private final String USER_INFO_URL = "https://openapi.naver.com/v1/nid/me"; // 회원 프로필 조회 URL
}
