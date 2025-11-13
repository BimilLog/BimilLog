package jaeik.bimillog.domain.global.out;

import jaeik.bimillog.domain.auth.entity.SocialToken;
import jaeik.bimillog.domain.auth.out.SocialTokenRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * <h2>소셜 토큰 조회 공용 어댑터</h2>
 * <p>여러 도메인에서 공통으로 사용하는 소셜 토큰 조회 기능을 구현하는 어댑터입니다.</p>
 * <p>SocialTokenRepository를 통해 실제 데이터베이스에서 조회합니다.</p>
 * <p>Member와 1:1 관계이므로 Member를 통해 직접 접근하는 것을 권장합니다.</p>
 *
 * @author Jaeik
 * @version 2.0.0
 */
@Component
@RequiredArgsConstructor
public class GlobalSocialTokenQueryAdapter {

    private final SocialTokenRepository socialTokenRepository;

    // Member의 getSocialToken()을 통해 직접 접근하므로 추가 메서드 불필요
}
