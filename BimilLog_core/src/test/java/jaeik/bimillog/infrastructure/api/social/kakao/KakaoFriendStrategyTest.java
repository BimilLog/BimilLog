package jaeik.bimillog.infrastructure.api.social.kakao;

import jaeik.bimillog.domain.member.entity.KakaoFriends;
import jaeik.bimillog.infrastructure.api.dto.KakaoFriendsDTO;
import jaeik.bimillog.testutil.BaseUnitTest;
import jaeik.bimillog.testutil.builder.KakaoTestDataBuilder;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;

import java.util.Arrays;
import java.util.Collections;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * <h2>KakaoFriendStrategy 테스트</h2>
 * <p>카카오 친구 전략이 API 호출 결과를 올바르게 매핑하는지 검증합니다.</p>
 */
@Tag("unit")
class KakaoFriendStrategyTest extends BaseUnitTest {

    private static final String AUTHORIZATION_PREFIX = "Bearer ";

    @Mock
    private KakaoApiClient kakaoApiClient;

    @InjectMocks
    private KakaoFriendStrategy kakaoFriendStrategy;

    @Test
    @DisplayName("정상 케이스 - 카카오 친구 목록 조회 성공")
    void shouldGetFriendList_WhenValidParametersProvided() {
        String accessToken = "valid_access_token";
        Integer offset = 0;
        Integer limit = 10;

        KakaoFriendsDTO expectedResponse = KakaoTestDataBuilder.createKakaoFriendsResponse(
            Arrays.asList(
                KakaoTestDataBuilder.createKakaoFriend(1L, "uuid1", "친구1", "image1.jpg", false),
                KakaoTestDataBuilder.createKakaoFriend(2L, "uuid2", "친구2", "image2.jpg", true)
            ),
            2, null, null, 1
        );

        when(kakaoApiClient.getFriends(eq(AUTHORIZATION_PREFIX + accessToken), eq(offset), eq(limit)))
            .thenReturn(expectedResponse);

        KakaoFriends result = kakaoFriendStrategy.getFriendList(accessToken, offset, limit);

        assertThat(result).isNotNull();
        assertThat(result.elements()).hasSize(2);
        assertThat(result.totalCount()).isEqualTo(2);
        assertThat(result.favoriteCount()).isEqualTo(1);

        KakaoFriends.Friend friend1 = result.elements().getFirst();
        assertThat(friend1.id()).isEqualTo(1L);
        assertThat(friend1.profileNickname()).isEqualTo("친구1");
        assertThat(friend1.favorite()).isFalse();

        KakaoFriends.Friend friend2 = result.elements().get(1);
        assertThat(friend2.id()).isEqualTo(2L);
        assertThat(friend2.profileNickname()).isEqualTo("친구2");
        assertThat(friend2.favorite()).isTrue();

        verify(kakaoApiClient).getFriends(eq(AUTHORIZATION_PREFIX + accessToken), eq(offset), eq(limit));
    }

    @Test
    @DisplayName("경계값 - 빈 친구 목록 조회")
    void shouldHandleEmptyFriendList_WhenNoFriendsFound() {
        String accessToken = "valid_access_token";
        Integer offset = 0;
        Integer limit = 10;

        KakaoFriendsDTO emptyResponse = KakaoTestDataBuilder.createKakaoFriendsResponse(
            Collections.emptyList(), 0, null, null, 0
        );

        when(kakaoApiClient.getFriends(eq(AUTHORIZATION_PREFIX + accessToken), eq(offset), eq(limit)))
            .thenReturn(emptyResponse);

        KakaoFriends result = kakaoFriendStrategy.getFriendList(accessToken, offset, limit);

        assertThat(result).isNotNull();
        assertThat(result.elements()).isEmpty();
        assertThat(result.totalCount()).isEqualTo(0);
        assertThat(result.favoriteCount()).isEqualTo(0);

        verify(kakaoApiClient).getFriends(eq(AUTHORIZATION_PREFIX + accessToken), eq(offset), eq(limit));
    }

    @Test
    @DisplayName("경계값 - 페이지네이션 파라미터로 친구 목록 조회")
    void shouldGetFriendListWithPagination_WhenPaginationParametersProvided() {
        String accessToken = "valid_access_token";
        Integer offset = 10;
        Integer limit = 5;

        KakaoFriendsDTO paginatedResponse = KakaoTestDataBuilder.createKakaoFriendsResponse(
            Arrays.asList(
                KakaoTestDataBuilder.createKakaoFriend(11L, "uuid11", "친구11", "image11.jpg", true),
                KakaoTestDataBuilder.createKakaoFriend(12L, "uuid12", "친구12", "image12.jpg", false)
            ),
            2, "before_url", "after_url", 1
        );

        when(kakaoApiClient.getFriends(eq(AUTHORIZATION_PREFIX + accessToken), eq(offset), eq(limit)))
            .thenReturn(paginatedResponse);

        KakaoFriends result = kakaoFriendStrategy.getFriendList(accessToken, offset, limit);

        assertThat(result).isNotNull();
        assertThat(result.elements()).hasSize(2);
        assertThat(result.beforeUrl()).isEqualTo("before_url");
        assertThat(result.afterUrl()).isEqualTo("after_url");
        assertThat(result.favoriteCount()).isEqualTo(1);

        verify(kakaoApiClient).getFriends(eq(AUTHORIZATION_PREFIX + accessToken), eq(offset), eq(limit));
    }
}
