package jaeik.growfarm.domain.user.entity;

import lombok.Builder;

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
        List<KakaoFriendVO> elements,
        Integer totalCount,
        String beforeUrl,
        String afterUrl,
        Integer favoriteCount
) {

    @Builder
    public KakaoFriendsResponseVO {
    }

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
    public static KakaoFriendsResponseVO of(List<KakaoFriendVO> elements, Integer totalCount, String beforeUrl, String afterUrl, Integer favoriteCount) {
        return new KakaoFriendsResponseVO(elements, totalCount, beforeUrl, afterUrl, favoriteCount);
    }
}