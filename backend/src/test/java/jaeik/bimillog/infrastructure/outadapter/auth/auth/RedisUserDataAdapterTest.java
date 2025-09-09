package jaeik.bimillog.infrastructure.outadapter.auth.auth;

import jaeik.bimillog.BimilLogApplication;
import jaeik.bimillog.domain.auth.entity.LoginResult;
import jaeik.bimillog.domain.user.entity.SocialProvider;
import jaeik.bimillog.domain.user.entity.Token;
import jaeik.bimillog.infrastructure.adapter.auth.out.auth.RedisUserDataAdapter;
import jaeik.bimillog.infrastructure.auth.AuthCookieManager;
import jaeik.bimillog.domain.auth.exception.AuthCustomException;
import jaeik.bimillog.testutil.TestContainersConfiguration;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.context.annotation.Import;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.ResponseCookie;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.Optional;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

/**
 * <h2>RedisUserDataAdapter 통합 테스트</h2>
 * <p>Redis TestContainers를 사용한 실제 Redis 환경에서의 테스트</p>
 * <p>PostCacheSyncAdapterTest 패턴을 참고하여 작성</p>
 *
 * @author Jaeik
 * @version 2.0.0
 */
@DataJpaTest(
        excludeFilters = @ComponentScan.Filter(
                type = FilterType.ASSIGNABLE_TYPE,
                classes = BimilLogApplication.class
        )
)
@Testcontainers
@Import({RedisUserDataAdapter.class, TestContainersConfiguration.class})
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@TestPropertySource(properties = {
        "spring.jpa.hibernate.ddl-auto=create",
        "logging.level.jaeik.bimillog.infrastructure.adapter.auth.out.cache=DEBUG"
})
class RedisUserDataAdapterTest {

    @Autowired
    private RedisUserDataAdapter redisTempDataAdapter;
    
    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    @MockitoBean
    private AuthCookieManager authCookieManager;

    private LoginResult.SocialUserProfile testUserProfile;
    private Token testToken;
    private String testUuid;

    @BeforeEach
    void setUp() {
        // Redis 초기화
        try (RedisConnection connection = redisTemplate.getConnectionFactory().getConnection()) {
            if (connection != null) {
                connection.serverCommands().flushAll();
            }
        } catch (Exception e) {
            System.err.println("Redis flush warning: " + e.getMessage());
        }
        
        // 테스트 데이터 준비
        testUuid = "test-uuid-12345";
        testUserProfile = new LoginResult.SocialUserProfile(
            "123456789", 
            "test@example.com", 
            SocialProvider.KAKAO, 
            "testUser", 
            "https://example.com/profile.jpg"
        );
        testToken = Token.createTemporaryToken("access-token", "refresh-token");
                
    }

    @Test
    @DisplayName("정상 케이스 - 임시 데이터 저장 및 조회")
    void shouldSaveAndRetrieveTempData_WhenValidDataProvided() {
        // When: 임시 데이터 저장
        redisTempDataAdapter.saveTempData(testUuid, testUserProfile, testToken, "test-fcm-token");

        // Then: 저장된 데이터 조회 검증
        Optional<LoginResult.TempUserData> savedData = redisTempDataAdapter.getTempData(testUuid);
        
        assertThat(savedData).isPresent();
        assertThat(savedData.get().userProfile().socialId()).isEqualTo("123456789");
        assertThat(savedData.get().userProfile().email()).isEqualTo("test@example.com");
        assertThat(savedData.get().token().getAccessToken()).isEqualTo("access-token");
        assertThat(savedData.get().fcmToken()).isEqualTo("test-fcm-token");
        
        // Redis에서 직접 확인
        String key = "temp:user:" + testUuid;
        assertThat(redisTemplate.hasKey(key)).isTrue();
    }

    @Test
    @DisplayName("정상 케이스 - TTL 설정 확인")
    void shouldSetCorrectTTL_WhenDataSaved() {
        // When: 임시 데이터 저장
        redisTempDataAdapter.saveTempData(testUuid, testUserProfile, testToken, "test-fcm-token");
        
        String key = "temp:user:" + testUuid;
        
        // Then: TTL이 설정되어 있음 (약 5분)
        Long ttl = redisTemplate.getExpire(key, TimeUnit.SECONDS);
        assertThat(ttl).isBetween(290L, 300L); // 5분 = 300초, 약간의 오차 허용
        
        // 즉시 조회 시에는 데이터 존재
        Optional<LoginResult.TempUserData> immediateResult = redisTempDataAdapter.getTempData(testUuid);
        assertThat(immediateResult).isPresent();
    }

    @Test
    @DisplayName("예외 케이스 - null 값들로 저장 시 예외 발생")
    void shouldThrowException_WhenInvalidDataProvided() {
        // When & Then: null UUID로 저장 시도 시 예외 발생
        assertThatThrownBy(() -> redisTempDataAdapter.saveTempData(null, testUserProfile, testToken, null))
                .isInstanceOf(AuthCustomException.class);
                
        // null userProfile로 저장 시도 시 예외 발생
        assertThatThrownBy(() -> redisTempDataAdapter.saveTempData(testUuid, null, testToken, null))
                .isInstanceOf(AuthCustomException.class);
                
        // null tokenVO로 저장 시도 시 예외 발생
        assertThatThrownBy(() -> redisTempDataAdapter.saveTempData(testUuid, testUserProfile, null, null))
                .isInstanceOf(AuthCustomException.class);
    }

    @Test
    @DisplayName("경계값 - 존재하지 않는 UUID로 조회 시 빈 결과")
    void shouldReturnEmpty_WhenUuidNotExists() {
        // Given: 존재하지 않는 UUID
        String nonExistentUuid = "non-existent-uuid";

        // When: 존재하지 않는 UUID로 조회
        Optional<LoginResult.TempUserData> result = redisTempDataAdapter.getTempData(nonExistentUuid);

        // Then: 빈 Optional 반환
        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("경계값 - null UUID로 조회 시 빈 결과")
    void shouldReturnEmpty_WhenUuidIsNull() {
        // Given: null UUID
        String nullUuid = null;

        // When: null UUID로 조회
        Optional<LoginResult.TempUserData> result = redisTempDataAdapter.getTempData(nullUuid);

        // Then: 빈 Optional 반환
        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("정상 케이스 - 임시 데이터 삭제")
    void shouldRemoveTempData_WhenDataExists() {
        // Given: 저장된 데이터
        redisTempDataAdapter.saveTempData(testUuid, testUserProfile, testToken, "test-fcm-token");
        assertThat(redisTempDataAdapter.getTempData(testUuid)).isPresent();

        // When: 데이터 삭제
        redisTempDataAdapter.removeTempData(testUuid);

        // Then: 데이터가 삭제되어 조회되지 않음
        Optional<LoginResult.TempUserData> result = redisTempDataAdapter.getTempData(testUuid);
        assertThat(result).isEmpty();
        
        // Redis에서도 삭제됨 확인
        String key = "temp:user:" + testUuid;
        assertThat(redisTemplate.hasKey(key)).isFalse();
    }

    @Test
    @DisplayName("경계값 - 존재하지 않는 데이터 삭제")
    void shouldHandleRemoveNonExistentData_WhenDataNotExists() {
        // Given: 존재하지 않는 UUID
        String nonExistentUuid = "non-existent-uuid";

        // When: 존재하지 않는 데이터 삭제 시도 (예외 발생하지 않아야 함)
        redisTempDataAdapter.removeTempData(nonExistentUuid);

        // Then: 예외 없이 정상 처리됨
        assertThat(redisTempDataAdapter.getTempData(nonExistentUuid)).isEmpty();
    }

    @Test
    @DisplayName("경계값 - null UUID로 삭제")
    void shouldHandleRemoveWithNullUuid_WhenUuidIsNull() {
        // Given: null UUID
        String nullUuid = null;

        // When: null UUID로 삭제 시도 (예외 발생하지 않아야 함)
        redisTempDataAdapter.removeTempData(nullUuid);

        // Then: 예외 없이 처리됨
        assertThat(redisTempDataAdapter.getTempData(nullUuid)).isEmpty();
    }

    @Test
    @DisplayName("정상 케이스 - 임시 쿠키 생성")
    void shouldCreateTempCookie_WhenValidUuidProvided() {
        // Given: AuthCookieManager에서 반환할 쿠키
        ResponseCookie expectedCookie = ResponseCookie.from("tempUserId", testUuid)
                .maxAge(300) // 5분
                .httpOnly(true)
                .build();
        given(authCookieManager.createTempCookie(testUuid)).willReturn(expectedCookie);

        // When: 임시 쿠키 생성
        ResponseCookie actualCookie = redisTempDataAdapter.createTempCookie(testUuid);

        // Then: 올바른 쿠키 생성 검증
        assertThat(actualCookie).isEqualTo(expectedCookie);
        assertThat(actualCookie.getName()).isEqualTo("tempUserId");
        assertThat(actualCookie.getValue()).isEqualTo(testUuid);
        verify(authCookieManager).createTempCookie(testUuid);
    }

    @Test
    @DisplayName("정상 케이스 - 데이터 덮어쓰기")
    void shouldOverwriteData_WhenSameUuidSavedMultipleTimes() {
        // Given: 첫 번째 데이터 저장
        redisTempDataAdapter.saveTempData(testUuid, testUserProfile, testToken, "test-fcm-token");
        
        // 새로운 데이터
        LoginResult.SocialUserProfile newUserProfile = new LoginResult.SocialUserProfile(
            "987654321", 
            "new@example.com", 
            SocialProvider.KAKAO, 
            "newUser", 
            "https://example.com/new-profile.jpg"
        );
        Token newToken = Token.createTemporaryToken("access-token", "refresh-token");
                

        // When: 동일한 UUID로 새 데이터 저장
        redisTempDataAdapter.saveTempData(testUuid, newUserProfile, newToken, "new-fcm-token");

        // Then: 새 데이터로 덮어써짐
        Optional<LoginResult.TempUserData> result = redisTempDataAdapter.getTempData(testUuid);
        assertThat(result).isPresent();
        assertThat(result.get().userProfile().socialId()).isEqualTo("987654321");
        assertThat(result.get().userProfile().nickname()).isEqualTo("newUser");
        assertThat(result.get().token().getAccessToken()).isEqualTo("access-token");
        assertThat(result.get().fcmToken()).isEqualTo("new-fcm-token");
    }

    @Test
    @DisplayName("멀티 인스턴스 - 여러 어댑터 인스턴스에서 동일 데이터 접근")
    void shouldShareDataBetweenInstances_WhenMultipleAdaptersAccess() {
        // Given: 첫 번째 어댑터에서 저장
        redisTempDataAdapter.saveTempData(testUuid, testUserProfile, testToken, "test-fcm-token");

        // When: 새로운 어댑터 인스턴스 생성 (멀티 인스턴스 환경 시뮬레이션)
        RedisUserDataAdapter secondAdapter = new RedisUserDataAdapter(redisTemplate, authCookieManager);
        
        // Then: 두 번째 어댑터에서도 동일한 데이터 조회 가능
        Optional<LoginResult.TempUserData> result = secondAdapter.getTempData(testUuid);
        assertThat(result).isPresent();
        assertThat(result.get().userProfile().nickname()).isEqualTo("testUser");
        
        // 첫 번째 어댑터에서 삭제
        redisTempDataAdapter.removeTempData(testUuid);
        
        // 두 번째 어댑터에서도 삭제됨 확인
        assertThat(secondAdapter.getTempData(testUuid)).isEmpty();
    }

    @Test
    @DisplayName("성능 테스트 - 대량 데이터 처리")
    void shouldHandleLargeAmountOfData_WhenManyEntriesStored() {
        // Given: 대량의 UUID와 데이터
        int dataCount = 50; // Redis 테스트이므로 적당한 수량

        // When: 대량 데이터 저장
        for (int i = 0; i < dataCount; i++) {
            String uuid = "load-test-uuid-" + i;
            LoginResult.SocialUserProfile userProfile = new LoginResult.SocialUserProfile(
                "id-" + i, 
                "user" + i + "@example.com", 
                SocialProvider.KAKAO, 
                "user-" + i, 
                "https://example.com/profile" + i + ".jpg"
            );
            redisTempDataAdapter.saveTempData(uuid, userProfile, testToken, "fcm-" + i);
        }

        // Then: 모든 데이터가 정상 저장되고 조회됨
        for (int i = 0; i < dataCount; i++) {
            String uuid = "load-test-uuid-" + i;
            Optional<LoginResult.TempUserData> result = redisTempDataAdapter.getTempData(uuid);
            assertThat(result).isPresent();
            assertThat(result.get().userProfile().nickname()).isEqualTo("user-" + i);
        }

        // 정리: 저장된 데이터 삭제
        for (int i = 0; i < dataCount; i++) {
            redisTempDataAdapter.removeTempData("load-test-uuid-" + i);
        }
    }

    @Test
    @DisplayName("데이터 일관성 - Redis 키 패턴 검증")
    void shouldUseCorrectKeyPattern_WhenDataSaved() {
        // When: 데이터 저장
        redisTempDataAdapter.saveTempData(testUuid, testUserProfile, testToken, "test-fcm-token");
        
        // Then: 올바른 키 패턴 사용 확인
        String expectedKey = "temp:user:" + testUuid;
        assertThat(redisTemplate.hasKey(expectedKey)).isTrue();
        
        // 다른 패턴의 키는 존재하지 않음
        assertThat(redisTemplate.hasKey("cache:user:" + testUuid)).isFalse();
        assertThat(redisTemplate.hasKey("token:user:" + testUuid)).isFalse();
    }

    @Test
    @DisplayName("FCM 토큰 - null FCM 토큰으로 저장 및 조회")
    void shouldHandleNullFcmToken_WhenFcmTokenIsNull() {
        // When: FCM 토큰 없이 저장
        redisTempDataAdapter.saveTempData(testUuid, testUserProfile, testToken, null);

        // Then: FCM 토큰이 null로 저장됨
        Optional<LoginResult.TempUserData> result = redisTempDataAdapter.getTempData(testUuid);
        assertThat(result).isPresent();
        assertThat(result.get().fcmToken()).isNull();
        assertThat(result.get().userProfile().nickname()).isEqualTo("testUser");
    }
}