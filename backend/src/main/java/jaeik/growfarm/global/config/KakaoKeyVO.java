package jaeik.growfarm.global.config;

import lombok.Getter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
@Getter
public class KakaoKeyVO {

    @Value("${spring.kakao.client-id}")
    private String CLIENT_ID; // 앱 REST API 키

    @Value("${spring.kakao.client-secret}")
    private String CLIENT_SECRET; // 앱 시크릿 키

    @Value("${spring.kakao.admin-key}")
    private String ADMIN_KEY; // 앱 어드민 키

    @Value("${spring.kakao.redirect-uri}")
    private String REDIRECT_URI; // 리다이렉트 URL

    private final String AUTHORIZE_URL = "https://kauth.kakao.com/oauth/authorize"; // 인가코드 받기 URL
    private final String TOKEN_URL = "https://kauth.kakao.com/oauth/token"; // 토큰 받기 URL

    private final String LOGOUT_URL = "https://kapi.kakao.com/v1/user/logout"; // 로그아웃 URL
    private final String LOGOUT_WITH_KAKAO_URL = "https://kauth.kakao.com/oauth/logout"; // 카카오계정과 함께 로그아웃 URL
    private final String LOGOUT_WITH_REDIRECT_URL = "https://grow-farm.com/auth/kakao/logout"; // 로그아웃 리다이렉트 URL
    private final String UNLINK_URL = "https://kapi.kakao.com/v1/user/unlink"; // 연결 끊기 URL

    private final String TOKEN_INFO_URL = "https://kapi.kakao.com/v1/user/access_token_info"; // 토큰 정보 보기 URL
    private final String REFRESH_TOKEN_URL = "https://kauth.kakao.com/oauth/token"; // 토큰 갱신하기 URL

    private final String USER_INFO_URL = "https://kapi.kakao.com/v2/user/me"; // 사용자 정보 가져오기 URL
    private final String MULTIPLE_USER_INFO_URL = "https://kapi.kakao.com/v2/app/users"; // 여러 사용자 정보 가져오기 URL
    private final String USER_LIST_URL = "https://kapi.kakao.com/v1/user/ids"; // 사용자 목록 가져오기 URL
    private final String SAVE_USER_INFO_URL = "https://kapi.kakao.com/v1/user/update_profile"; // 사용자 정보 저장하기 URL

    private final String CHECK_CONSENT_URL = "https://kapi.kakao.com/v2/user/scopes	"; // 동의 내역확인하기 URL
    private final String REVOKE_CONSENT_URL = "https://kapi.kakao.com/v2/user/revoke/scopes	"; // 동의 철회하기 URL

    private final String GET_FRIEND_LIST_URL = "https://kapi.kakao.com/v1/api/talk/friends"; // 친구 목록 가져오기 URL

}