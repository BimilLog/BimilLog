package jaeik.bimillog.infrastructure.adapter.out.redis;

import jaeik.bimillog.BimilLogApplication;
import jaeik.bimillog.domain.auth.entity.SocialUserProfile;
import jaeik.bimillog.domain.auth.entity.TempUserData;
import jaeik.bimillog.domain.auth.exception.AuthCustomException;
import jaeik.bimillog.domain.user.entity.SocialProvider;
import jaeik.bimillog.domain.user.entity.Token;
import jaeik.bimillog.infrastructure.adapter.out.auth.AuthCookieManager;
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
 * <p>RedisPostSyncAdapterTest 패턴을 참고하여 작성</p>
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

    private SocialUserProfile testUserProfile;
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
        testUserProfile = new SocialUserProfile(
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
        Optional<TempUserData> savedData = redisTempDataAdapter.getTempData(testUuid);
        
        assertThat(savedData).isPresent();
        assertThat(savedData.get().socialId()).isEqualTo("123456789");
        assertThat(savedData.get().email()).isEqualTo("test@example.com");
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
        Optional<TempUserData> immediateResult = redisTempDataAdapter.getTempData(testUuid);
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
        Optional<TempUserData> result = redisTempDataAdapter.getTempData(nonExistentUuid);

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
        Optional<TempUserData> result = redisTempDataAdapter.getTempData(testUuid);
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
    @DisplayName("FCM 토큰 - null FCM 토큰으로 저장 및 조회")
    void shouldHandleNullFcmToken_WhenFcmTokenIsNull() {
        // When: FCM 토큰 없이 저장
        redisTempDataAdapter.saveTempData(testUuid, testUserProfile, testToken, null);

        // Then: FCM 토큰이 null로 저장됨
        Optional<TempUserData> result = redisTempDataAdapter.getTempData(testUuid);
        assertThat(result).isPresent();
        assertThat(result.get().fcmToken()).isNull();
        assertThat(result.get().nickname()).isEqualTo("testUser");
    }
}