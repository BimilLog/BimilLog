package jaeik.bimillog.infrastructure.outadapter.user.social;

import jaeik.bimillog.domain.user.entity.KakaoFriendsResponseVO;
import jaeik.bimillog.infrastructure.adapter.user.dto.KakaoFriendsDTO;
import jaeik.bimillog.infrastructure.adapter.user.out.social.KakaoFriendAdapter;
import jaeik.bimillog.infrastructure.adapter.user.out.social.KakaoSocialAdapter;
import jaeik.bimillog.infrastructure.adapter.user.dto.KakaoFriendDTO;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;

import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

/**
 * <h2>KakaoFriendAdapter 테스트</h2>
 * <p>카카오 친구 목록 어댑터의 동작 검증</p>
 * <p>카카오 소셜 어댑터를 통한 친구 목록 조회 기능 테스트</p>
 * 
 * @author Jaeik
 * @version 2.0.0
 */
@ExtendWith(MockitoExtension.class)
class KakaoFriendAdapterTest {

    @Mock
    private KakaoSocialAdapter kakaoSocialAdapter;

    @InjectMocks
    private KakaoFriendAdapter kakaoFriendAdapter;

    @Test
    @DisplayName("정상 케이스 - 카카오 친구 목록 조회 성공")
    void shouldGetFriendList_WhenValidParametersProvided() throws Exception {
        // Given: 유효한 액세스 토큰과 파라미터, 예상 친구 목록 응답
        String accessToken = "valid_access_token";
        Integer offset = 0;
        Integer limit = 10;
        
        KakaoFriendsDTO expectedResponse = createKakaoFriendsResponse(
            Arrays.asList(
                createKakaoFriendDTO(1L, "uuid1", "친구1", "image1.jpg", false),
                createKakaoFriendDTO(2L, "uuid2", "친구2", "image2.jpg", true)
            ),
            2, null, null, 1
        );
        
        given(kakaoSocialAdapter.getFriendList(eq(accessToken), eq(offset), eq(limit)))
            .willReturn(Mono.just(expectedResponse));

        // When: 친구 목록 조회 실행
        KakaoFriendsResponseVO result = kakaoFriendAdapter.getFriendList(accessToken, offset, limit).block();

        // Then: 올바른 친구 목록이 반환되고 KakaoSocialAdapter가 호출되었는지 검증
        assertThat(result).isNotNull();
        assertThat(result.elements()).hasSize(2);
        assertThat(result.totalCount()).isEqualTo(2);
        assertThat(result.favoriteCount()).isEqualTo(1);
        
        // 첫 번째 친구 검증
        KakaoFriendsResponseVO.Friend friend1 = result.elements().get(0);
        assertThat(friend1.id()).isEqualTo(1L);
        assertThat(friend1.profileNickname()).isEqualTo("친구1");
        assertThat(friend1.favorite()).isFalse();
        
        // 두 번째 친구 검증
        KakaoFriendsResponseVO.Friend friend2 = result.elements().get(1);
        assertThat(friend2.id()).isEqualTo(2L);
        assertThat(friend2.profileNickname()).isEqualTo("친구2");
        assertThat(friend2.favorite()).isTrue();
        
        verify(kakaoSocialAdapter).getFriendList(eq(accessToken), eq(offset), eq(limit));
    }

    @Test
    @DisplayName("경계값 - 빈 친구 목록 조회")
    void shouldHandleEmptyFriendList_WhenNoFriendsFound() throws Exception {
        // Given: 빈 친구 목록 응답
        String accessToken = "valid_access_token";
        Integer offset = 0;
        Integer limit = 10;
        
        KakaoFriendsDTO emptyResponse = createKakaoFriendsResponse(
            Collections.emptyList(), 0, null, null, 0
        );
        
        given(kakaoSocialAdapter.getFriendList(eq(accessToken), eq(offset), eq(limit)))
            .willReturn(Mono.just(emptyResponse));

        // When: 빈 친구 목록 조회 실행
        KakaoFriendsResponseVO result = kakaoFriendAdapter.getFriendList(accessToken, offset, limit).block();

        // Then: 빈 목록이 올바르게 반환되는지 검증
        assertThat(result).isNotNull();
        assertThat(result.elements()).isEmpty();
        assertThat(result.totalCount()).isEqualTo(0);
        assertThat(result.favoriteCount()).isEqualTo(0);
        
        verify(kakaoSocialAdapter).getFriendList(eq(accessToken), eq(offset), eq(limit));
    }

    @Test
    @DisplayName("경계값 - 페이지네이션 파라미터로 친구 목록 조회")
    void shouldGetFriendListWithPagination_WhenPaginationParametersProvided() throws Exception {
        // Given: 페이지네이션 파라미터와 친구 목록 응답
        String accessToken = "valid_access_token";
        Integer offset = 10;
        Integer limit = 5;
        
        KakaoFriendsDTO paginatedResponse = createKakaoFriendsResponse(
            Arrays.asList(
                createKakaoFriendDTO(11L, "uuid11", "친구11", "image11.jpg", true),
                createKakaoFriendDTO(12L, "uuid12", "친구12", "image12.jpg", false)
            ),
            2, "before_url", "after_url", 1
        );
        
        given(kakaoSocialAdapter.getFriendList(eq(accessToken), eq(offset), eq(limit)))
            .willReturn(Mono.just(paginatedResponse));

        // When: 페이지네이션으로 친구 목록 조회 실행
        KakaoFriendsResponseVO result = kakaoFriendAdapter.getFriendList(accessToken, offset, limit).block();

        // Then: 페이지네이션 정보가 올바르게 반환되는지 검증
        assertThat(result).isNotNull();
        assertThat(result.elements()).hasSize(2);
        assertThat(result.beforeUrl()).isEqualTo("before_url");
        assertThat(result.afterUrl()).isEqualTo("after_url");
        assertThat(result.favoriteCount()).isEqualTo(1);
        
        verify(kakaoSocialAdapter).getFriendList(eq(accessToken), eq(offset), eq(limit));
    }

    @Test
    @DisplayName("예외 케이스 - null 액세스 토큰으로 친구 목록 조회")
    void shouldHandleNullAccessToken_WhenNullAccessTokenProvided() throws Exception {
        // Given: null 액세스 토큰과 빈 응답
        String nullAccessToken = null;
        Integer offset = 0;
        Integer limit = 10;
        
        KakaoFriendsDTO emptyResponse = createKakaoFriendsResponse(
            Collections.emptyList(), 0, null, null, 0
        );
        
        given(kakaoSocialAdapter.getFriendList(eq(nullAccessToken), eq(offset), eq(limit)))
            .willReturn(Mono.just(emptyResponse));

        // When: null 액세스 토큰으로 친구 목록 조회 실행
        KakaoFriendsResponseVO result = kakaoFriendAdapter.getFriendList(nullAccessToken, offset, limit).block();

        // Then: KakaoSocialAdapter에 null이 전달되고 결과가 반환되는지 검증
        assertThat(result).isNotNull();
        verify(kakaoSocialAdapter).getFriendList(eq(nullAccessToken), eq(offset), eq(limit));
    }

    @Test
    @DisplayName("예외 케이스 - null offset으로 친구 목록 조회")
    void shouldHandleNullOffset_WhenNullOffsetProvided() throws Exception {
        // Given: null offset과 빈 응답
        String accessToken = "valid_access_token";
        Integer nullOffset = null;
        Integer limit = 10;
        
        KakaoFriendsDTO emptyResponse = createKakaoFriendsResponse(
            Collections.emptyList(), 0, null, null, 0
        );
        
        given(kakaoSocialAdapter.getFriendList(eq(accessToken), eq(nullOffset), eq(limit)))
            .willReturn(Mono.just(emptyResponse));

        // When: null offset으로 친구 목록 조회 실행
        KakaoFriendsResponseVO result = kakaoFriendAdapter.getFriendList(accessToken, nullOffset, limit).block();

        // Then: KakaoSocialAdapter에 null이 전달되고 결과가 반환되는지 검증
        assertThat(result).isNotNull();
        verify(kakaoSocialAdapter).getFriendList(eq(accessToken), eq(nullOffset), eq(limit));
    }

    @Test
    @DisplayName("예외 케이스 - null limit으로 친구 목록 조회")
    void shouldHandleNullLimit_WhenNullLimitProvided() throws Exception {
        // Given: null limit과 빈 응답
        String accessToken = "valid_access_token";
        Integer offset = 0;
        Integer nullLimit = null;
        
        KakaoFriendsDTO emptyResponse = createKakaoFriendsResponse(
            Collections.emptyList(), 0, null, null, 0
        );
        
        given(kakaoSocialAdapter.getFriendList(eq(accessToken), eq(offset), eq(nullLimit)))
            .willReturn(Mono.just(emptyResponse));

        // When: null limit으로 친구 목록 조회 실행
        KakaoFriendsResponseVO result = kakaoFriendAdapter.getFriendList(accessToken, offset, nullLimit).block();

        // Then: KakaoSocialAdapter에 null이 전달되고 결과가 반환되는지 검증
        assertThat(result).isNotNull();
        verify(kakaoSocialAdapter).getFriendList(eq(accessToken), eq(offset), eq(nullLimit));
    }

    @Test
    @DisplayName("경계값 - 음수 offset과 limit으로 친구 목록 조회")
    void shouldHandleNegativeValues_WhenNegativeOffsetAndLimitProvided() throws Exception {
        // Given: 음수 offset과 limit
        String accessToken = "valid_access_token";
        Integer negativeOffset = -1;
        Integer negativeLimit = -5;
        
        KakaoFriendsDTO response = createKakaoFriendsResponse(
            Collections.emptyList(), 0, null, null, 0
        );
        
        given(kakaoSocialAdapter.getFriendList(eq(accessToken), eq(negativeOffset), eq(negativeLimit)))
            .willReturn(Mono.just(response));

        // When: 음수 파라미터로 친구 목록 조회 실행
        KakaoFriendsResponseVO result = kakaoFriendAdapter.getFriendList(accessToken, negativeOffset, negativeLimit).block();

        // Then: 어댑터가 파라미터를 그대로 전달하는지 검증
        assertThat(result).isNotNull();
        verify(kakaoSocialAdapter).getFriendList(eq(accessToken), eq(negativeOffset), eq(negativeLimit));
    }

    @Test
    @DisplayName("성능 - 대용량 친구 목록 조회")
    void shouldHandleLargeFriendList_WhenLargeLimitProvided() throws Exception {
        // Given: 대용량 친구 목록 요청
        String accessToken = "valid_access_token";
        Integer offset = 0;
        Integer largeLimit = 1000;
        
        // 1000명의 친구 목록 시뮬레이션 (실제로는 적은 수로 테스트)
        List<KakaoFriendDTO> largeFriendList = Arrays.asList(
            createKakaoFriendDTO(1L, "uuid1", "친구1", "image1.jpg", true),
            createKakaoFriendDTO(2L, "uuid2", "친구2", "image2.jpg", false)
        );
        
        KakaoFriendsDTO largeResponse = createKakaoFriendsResponse(
            largeFriendList, 1000, null, null, 500
        );
        
        given(kakaoSocialAdapter.getFriendList(eq(accessToken), eq(offset), eq(largeLimit)))
            .willReturn(Mono.just(largeResponse));

        // When: 대용량 친구 목록 조회 실행
        KakaoFriendsResponseVO result = kakaoFriendAdapter.getFriendList(accessToken, offset, largeLimit).block();

        // Then: 대용량 요청도 올바르게 처리되는지 검증
        assertThat(result).isNotNull();
        assertThat(result.totalCount()).isEqualTo(1000);
        assertThat(result.favoriteCount()).isEqualTo(500);
        
        verify(kakaoSocialAdapter).getFriendList(eq(accessToken), eq(offset), eq(largeLimit));
    }

    /**
     * <h3>KakaoFriendDTO 생성 헬퍼 메서드</h3>
     */
    private KakaoFriendDTO createKakaoFriendDTO(Long id, String uuid, String nickname, String image, Boolean favorite) throws Exception {
        KakaoFriendDTO friend = new KakaoFriendDTO();
        
        // Reflection을 사용하여 private 필드 설정
        setField(friend, "id", id);
        setField(friend, "uuid", uuid);
        setField(friend, "profileNickname", nickname);
        setField(friend, "profileThumbnailImage", image);
        setField(friend, "favorite", favorite);
        
        return friend;
    }

    /**
     * <h3>KakaoFriendsDTO 생성 헬퍼 메서드</h3>
     */
    private KakaoFriendsDTO createKakaoFriendsResponse(List<KakaoFriendDTO> elements, Integer totalCount,
                                                       String beforeUrl, String afterUrl, Integer favoriteCount) throws Exception {
        KakaoFriendsDTO response = new KakaoFriendsDTO();
        
        // Reflection을 사용하여 private 필드 설정
        setField(response, "elements", elements);
        setField(response, "totalCount", totalCount);
        setField(response, "beforeUrl", beforeUrl);
        setField(response, "afterUrl", afterUrl);
        setField(response, "favoriteCount", favoriteCount);
        
        return response;
    }

    /**
     * <h3>Reflection을 사용한 필드 값 설정 헬퍼 메서드</h3>
     */
    private void setField(Object target, String fieldName, Object value) throws Exception {
        Field field = target.getClass().getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(target, value);
    }
}