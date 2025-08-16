package jaeik.growfarm.infrastructure.adapter.auth.out.social.dto;

import jaeik.growfarm.domain.common.SocialProvider;
import lombok.Builder;

/**
 * <h2>소셜 로그인 사용자 데이터</h2>
 * <p>소셜 로그인 시 필요한 사용자 정보를 담는 레코드 클래스</p>
 *
 * @param socialId         소셜 ID
 * @param email            이메일 주소
 * @param provider         소셜 제공자 (예: GOOGLE, KAKAO 등)
 * @param nickname         사용자 닉네임
 * @param profileImageUrl  프로필 이미지 URL
 * @param fcmToken         Firebase Cloud Messaging 토큰
 * @author Jaeik
 * @version 2.0.0
 */
@Builder
public record SocialLoginUserData(String socialId, String email,
                                  SocialProvider provider, String nickname,
                                  String profileImageUrl, String fcmToken) {
}
