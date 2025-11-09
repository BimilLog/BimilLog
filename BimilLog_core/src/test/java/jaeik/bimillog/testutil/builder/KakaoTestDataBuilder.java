package jaeik.bimillog.testutil.builder;

import jaeik.bimillog.infrastructure.api.dto.KakaoFriendsDTO;
import jaeik.bimillog.testutil.fixtures.TestFixtures;

import java.util.List;

/**
 * <h2>카카오 API 테스트 데이터 빌더</h2>
 * <p>카카오 API 관련 테스트에서 사용되는 DTO와 응답 객체 생성 유틸리티</p>
 * <p>반복적인 Mock 데이터 생성 코드를 중앙화하여 테스트 가독성 향상</p>
 * 
 * <h3>제공되는 기능:</h3>
 * <ul>
 *   <li>KakaoFriendsDTO 및 Friend 객체 생성</li>
 *   <li>카카오 토큰 응답 생성</li>
 *   <li>카카오 회원 정보 응답 생성</li>
 * </ul>
 *
 * @author Jaeik
 * @version 1.0.0
 */
public class KakaoTestDataBuilder {

    /**
     * KakaoFriendsDTO.Friend 객체 생성
     * @param id 친구 ID
     * @param uuid 친구 UUID
     * @param nickname 친구 닉네임
     * @param image 프로필 이미지 URL
     * @param favorite 즐겨찾기 여부
     * @return KakaoFriendsDTO.Friend 객체
     */
    public static KakaoFriendsDTO.Friend createKakaoFriend(Long id, String uuid, 
                                                          String nickname, String image, 
                                                          Boolean favorite) {
        try {
            KakaoFriendsDTO.Friend friend = new KakaoFriendsDTO.Friend();
            TestFixtures.setFieldValue(friend, "id", id);
            TestFixtures.setFieldValue(friend, "uuid", uuid);
            TestFixtures.setFieldValue(friend, "profileNickname", nickname);
            TestFixtures.setFieldValue(friend, "profileThumbnailImage", image);
            TestFixtures.setFieldValue(friend, "favorite", favorite);
            return friend;
        } catch (Exception e) {
            throw new RuntimeException("Failed to create KakaoFriend", e);
        }
    }

    /**
     * KakaoFriendsDTO 응답 객체 생성
     * @param elements 친구 목록
     * @param totalCount 전체 친구 수
     * @param beforeUrl 이전 페이지 URL
     * @param afterUrl 다음 페이지 URL
     * @param favoriteCount 즐겨찾기 친구 수
     * @return KakaoFriendsDTO 객체
     */
    public static KakaoFriendsDTO createKakaoFriendsResponse(List<KakaoFriendsDTO.Friend> elements,
                                                            Integer totalCount,
                                                            String beforeUrl, 
                                                            String afterUrl,
                                                            Integer favoriteCount) {
        try {
            KakaoFriendsDTO response = new KakaoFriendsDTO();
            TestFixtures.setFieldValue(response, "elements", elements);
            TestFixtures.setFieldValue(response, "totalCount", totalCount);
            TestFixtures.setFieldValue(response, "beforeUrl", beforeUrl);
            TestFixtures.setFieldValue(response, "afterUrl", afterUrl);
            TestFixtures.setFieldValue(response, "favoriteCount", favoriteCount);
            return response;
        } catch (Exception e) {
            throw new RuntimeException("Failed to create KakaoFriendsResponse", e);
        }
    }
}