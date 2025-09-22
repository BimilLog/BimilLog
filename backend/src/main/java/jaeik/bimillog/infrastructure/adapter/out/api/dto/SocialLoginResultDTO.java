package jaeik.bimillog.infrastructure.adapter.out.api.dto;

import jaeik.bimillog.domain.auth.application.port.out.SocialStrategyPort;
import jaeik.bimillog.domain.user.entity.SocialProvider;
import jaeik.bimillog.domain.auth.entity.Token;

/**
 * <h3>인증 결과 데이터</h3>
 * <p>소셜 플랫폼 OAuth 인증 완료 후 반환되는 결과 데이터입니다.</p>
 * <p>소셜 사용자 프로필과 토큰 정보를 포함하여 인증 플로우에서 사용됩니다.</p>
 * <p>{@link SocialStrategyPort}의 authenticate 메서드 반환값으로 사용됩니다.</p>
 *
 * @param socialId 소셜 플랫폼에서의 사용자 고유 ID
 * @param email 사용자 이메일 주소
 * @param provider 소셜 플랫폼 제공자 (KAKAO, GOOGLE 등)
 * @param nickname 소셜 플랫폼에서의 사용자 닉네임
 * @param profileImageUrl 프로필 이미지 URL (선택사항)
 * @param token 소셜 로그인으로 발급받은 토큰 정보
 * @author Jaeik
 * @since 2.0.0
 */
public record SocialLoginResultDTO(
        String socialId,
        String email,
        SocialProvider provider,
        String nickname,
        String profileImageUrl,
        Token token
) {}