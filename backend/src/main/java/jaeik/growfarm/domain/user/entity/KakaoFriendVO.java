package jaeik.growfarm.domain.user.entity;

import lombok.Builder;

/**
 * <h3>카카오 친구 정보 값 객체</h3>
 * <p>
 * 카카오 친구 정보를 담는 도메인 값 객체
 * </p>
 *
 * @param id 카카오 친구 ID
 * @param uuid 친구 UUID
 * @param profileNickname 프로필 닉네임
 * @param profileThumbnailImage 프로필 썸네일 이미지
 * @param favorite 즐겨찾기 여부
 * @param userName 비밀로그 사용자 이름 (가입한 경우)
 * @author Jaeik
 * @since 2.0.0
 */
public record KakaoFriendVO(
        Long id,
        String uuid,
        String profileNickname,
        String profileThumbnailImage,
        Boolean favorite,
        String userName
) {

    @Builder
    public KakaoFriendVO {
    }

    /**
     * <h3>카카오 친구 값 객체 생성</h3>
     * <p>친구 정보로 KakaoFriendVO를 생성합니다.</p>
     *
     * @param id 카카오 친구 ID
     * @param uuid 친구 UUID
     * @param profileNickname 프로필 닉네임
     * @param profileThumbnailImage 프로필 썸네일 이미지
     * @param favorite 즐겨찾기 여부
     * @param userName 비밀로그 사용자 이름
     * @return KakaoFriendVO 객체
     */
    public static KakaoFriendVO of(Long id, String uuid, String profileNickname, String profileThumbnailImage, Boolean favorite, String userName) {
        return new KakaoFriendVO(id, uuid, profileNickname, profileThumbnailImage, favorite, userName);
    }
}