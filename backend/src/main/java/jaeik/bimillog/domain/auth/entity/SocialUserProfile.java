package jaeik.bimillog.domain.auth.entity;

import jaeik.bimillog.domain.user.entity.SocialProvider;

/**
 * <h3>소셜 사용자 프로필</h3>
 * <p>
 * 소셜 로그인으로부터 받은 사용자 프로필 정보를 담는 순수 도메인 모델
 * 헥사고날 아키텍처에서 도메인 계층의 순수 값 객체
 * </p>
 *
 * @param socialId 소셜 ID
 * @param email 이메일 주소
 * @param provider 소셜 제공자
 * @param nickname 사용자 닉네임
 * @param profileImageUrl 프로필 이미지 URL
 * @author Jaeik
 * @since 2.0.0
 */
public record SocialUserProfile(
        String socialId, 
        String email, 
        SocialProvider provider,
        String nickname, 
        String profileImageUrl
) {}