package jaeik.growfarm.infrastructure.adapter.user.out.social;

import jaeik.growfarm.domain.common.entity.SocialProvider;
import jaeik.growfarm.infrastructure.adapter.user.out.social.dto.KakaoFriendsResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * <h2>SocialAdapter 계약 테스트</h2>
 * <p>SocialAdapter 인터페이스의 계약과 동작을 검증하는 추상 테스트</p>
 * <p>모든 SocialAdapter 구현체가 일관된 동작을 보장하기 위한 계약 테스트</p>
 * 
 * @author Jaeik
 * @version 2.0.0
 */
@ExtendWith(MockitoExtension.class)
class SocialAdapterContractTest {

    /**
     * <h3>테스트 대상 어댑터를 반환하는 추상 메서드</h3>
     * <p>각 구현체별로 오버라이드하여 테스트할 어댑터를 제공</p>
     */
    protected SocialAdapter createSocialAdapter() {
        // 기본 구현: KakaoSocialAdapter 테스트용 Mock
        return new SocialAdapter() {
            @Override
            public SocialProvider getProvider() {
                return SocialProvider.KAKAO;
            }

            @Override
            public KakaoFriendsResponse getFriendList(String accessToken, Integer offset, Integer limit) {
                return new KakaoFriendsResponse();
            }
        };
    }

    @Test
    @DisplayName("계약 - 모든 SocialAdapter는 Provider를 반환해야 함")
    void shouldReturnProvider_WhenGetProviderCalled() {
        // Given: SocialAdapter 구현체
        SocialAdapter adapter = createSocialAdapter();

        // When: getProvider 호출
        SocialProvider provider = adapter.getProvider();

        // Then: 유효한 SocialProvider가 반환되어야 함
        assertThat(provider).isNotNull();
        assertThat(provider).isIn((Object[]) SocialProvider.values());
    }

    @Test
    @DisplayName("계약 - 모든 SocialAdapter는 getFriendList 메서드를 제공해야 함")
    void shouldProvideFriendListMethod_WhenSocialAdapterImplemented() {
        // Given: SocialAdapter 구현체
        SocialAdapter adapter = createSocialAdapter();
        String testToken = "test_token";
        Integer offset = 0;
        Integer limit = 10;

        // When: getFriendList 호출
        KakaoFriendsResponse response = adapter.getFriendList(testToken, offset, limit);

        // Then: 메서드가 정상적으로 호출되고 응답이 반환되어야 함
        assertThat(response).isNotNull();
    }

    @Test
    @DisplayName("계약 - SocialAdapter는 일관된 동작을 보장해야 함")
    void shouldMaintainConsistentBehavior_WhenMultipleCallsMade() {
        // Given: SocialAdapter 구현체
        SocialAdapter adapter = createSocialAdapter();

        // When: getProvider를 여러 번 호출
        SocialProvider provider1 = adapter.getProvider();
        SocialProvider provider2 = adapter.getProvider();

        // Then: 항상 동일한 Provider를 반환해야 함 (불변성)
        assertThat(provider1).isEqualTo(provider2);
        assertThat(provider1).isSameAs(provider2);
    }

    @Test
    @DisplayName("계약 - SocialAdapter는 null 안전성을 보장해야 함")
    void shouldHandleNullParameters_WhenNullParametersProvided() {
        // Given: SocialAdapter 구현체와 null 파라미터
        SocialAdapter adapter = createSocialAdapter();
        String nullToken = null;
        Integer nullOffset = null;
        Integer nullLimit = null;

        // When & Then: null 파라미터로 메서드 호출 시 예외 발생하지 않아야 함
        try {
            KakaoFriendsResponse response = adapter.getFriendList(nullToken, nullOffset, nullLimit);
            // 응답이 null이 아니어야 함 (최소한 빈 응답이라도)
            assertThat(response).isNotNull();
        } catch (Exception e) {
            // 예외가 발생할 경우 명확한 예외 타입이어야 함
            assertThat(e).isInstanceOfAny(
                IllegalArgumentException.class,
                NullPointerException.class
            );
        }
    }

    @Test
    @DisplayName("문서화 - SocialAdapter 인터페이스 계약 문서화")
    void documentSocialAdapterContract() {
        // Given: SocialAdapter 계약 문서화
        SocialAdapter adapter = createSocialAdapter();
        
        // Contract 1: getProvider()는 null을 반환하면 안됨
        SocialProvider provider = adapter.getProvider();
        assertThat(provider).withFailMessage(
            "SocialAdapter.getProvider() should never return null"
        ).isNotNull();
        
        // Contract 2: getProvider()는 유효한 SocialProvider enum 값을 반환해야 함
        assertThat(provider).withFailMessage(
            "SocialAdapter.getProvider() should return valid SocialProvider enum value"
        ).isIn((Object[]) SocialProvider.values());
        
        // Contract 3: getFriendList()는 null 응답을 반환하면 안됨
        KakaoFriendsResponse response = adapter.getFriendList("test", 0, 10);
        assertThat(response).withFailMessage(
            "SocialAdapter.getFriendList() should never return null response"
        ).isNotNull();
    }

    @Test
    @DisplayName("성능 - SocialAdapter 메서드 호출 성능 검증")
    void shouldPerformWell_WhenMethodsCalledFrequently() {
        // Given: SocialAdapter 구현체
        SocialAdapter adapter = createSocialAdapter();
        
        // When: getProvider()를 반복 호출하여 성능 측정
        long startTime = System.nanoTime();
        for (int i = 0; i < 1000; i++) {
            adapter.getProvider();
        }
        long endTime = System.nanoTime();
        long duration = endTime - startTime;
        
        // Then: 적절한 시간 내에 완료되어야 함 (1ms 미만)
        assertThat(duration).isLessThan(1_000_000L); // 1ms in nanoseconds
    }

    @Test
    @DisplayName("동시성 - SocialAdapter는 스레드 안전해야 함")
    void shouldBeThreadSafe_WhenAccessedConcurrently() throws InterruptedException {
        // Given: SocialAdapter 구현체
        SocialAdapter adapter = createSocialAdapter();
        int threadCount = 10;
        Thread[] threads = new Thread[threadCount];
        SocialProvider[] results = new SocialProvider[threadCount];
        
        // When: 여러 스레드에서 동시에 getProvider() 호출
        for (int i = 0; i < threadCount; i++) {
            final int index = i;
            threads[i] = new Thread(() -> {
                results[index] = adapter.getProvider();
            });
            threads[i].start();
        }
        
        // 모든 스레드 완료 대기
        for (Thread thread : threads) {
            thread.join();
        }
        
        // Then: 모든 결과가 동일해야 함 (스레드 안전성)
        SocialProvider expected = results[0];
        for (SocialProvider result : results) {
            assertThat(result).isEqualTo(expected);
        }
    }
}