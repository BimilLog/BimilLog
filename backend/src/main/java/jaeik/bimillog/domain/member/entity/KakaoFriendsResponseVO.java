package jaeik.bimillog.domain.member.entity;

import java.util.List;

/**
 * <h3>카카오 친구 목록 응답 값 객체</h3>
 * <p>
 * 카카오 친구 목록 API 응답 정보를 담는 도메인 값 객체
 * </p>
 *
 * @param elements 친구 목록
 * @param totalCount 전체 친구 수
 * @param beforeUrl 이전 페이지 URL
 * @param afterUrl 다음 페이지 URL
 * @param favoriteCount 즐겨찾기 친구 수
 * @author Jaeik
 * @since 2.0.0
 */
public record KakaoFriendsResponseVO(
        List<Friend> elements,
        Integer totalCount,
        String beforeUrl,
        String afterUrl,
        Integer favoriteCount
) {
    /**
     * <h3>카카오 친구 목록 응답 값 객체 생성</h3>
     * <p>카카오 친구 목록과 메타데이터로 KakaoFriendsResponseVO를 생성합니다.</p>
     *
     * @param elements 친구 목록
     * @param totalCount 전체 친구 수
     * @param beforeUrl 이전 페이지 URL
     * @param afterUrl 다음 페이지 URL
     * @param favoriteCount 즐겨찾기 친구 수
     * @return KakaoFriendsResponseVO 객체
     */
    public static KakaoFriendsResponseVO of(List<Friend> elements, Integer totalCount, String beforeUrl, String afterUrl, Integer favoriteCount) {
        return new KakaoFriendsResponseVO(elements, totalCount, beforeUrl, afterUrl, favoriteCount);
    }

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
    public record Friend(
            Long id,
            String uuid,
            String profileNickname,
            String profileThumbnailImage,
            Boolean favorite,
            String userName
    ) {
        /**
         * <h3>카카오 친구 값 객체 생성</h3>
         * <p>친구 정보로 Friend를 생성합니다.</p>
         *
         * @param id 카카오 친구 ID
         * @param uuid 친구 UUID
         * @param profileNickname 프로필 닉네임
         * @param profileThumbnailImage 프로필 썸네일 이미지
         * @param favorite 즐겨찾기 여부
         * @param userName 비밀로그 사용자 이름
         * @return Friend 객체
         */
        public static Friend of(Long id, String uuid, String profileNickname, String profileThumbnailImage, Boolean favorite, String userName) {
            return new Friend(id, uuid, profileNickname, profileThumbnailImage, favorite, userName);
        }
    }
}