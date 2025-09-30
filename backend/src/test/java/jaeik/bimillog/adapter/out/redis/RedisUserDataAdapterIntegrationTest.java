package jaeik.bimillog.adapter.out.redis;

import jaeik.bimillog.infrastructure.adapter.out.api.dto.SocialUserProfileDTO;
import jaeik.bimillog.domain.auth.exception.AuthCustomException;
import jaeik.bimillog.domain.user.entity.TempUserData;
import jaeik.bimillog.infrastructure.adapter.out.redis.RedisUserDataAdapter;
import jaeik.bimillog.testutil.RedisTestHelper;
import jaeik.bimillog.testutil.TestContainersConfiguration;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.Optional;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * <h2>RedisUserDataAdapter 통합 테스트</h2>
 * <p>Redis TestContainers를 사용한 실제 Redis 환경에서의 테스트</p>
 * <p>RedisPostSyncAdapterTest 패턴을 참고하여 작성</p>
 *
 * @author Jaeik
 * @version 2.0.0
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
@ActiveProfiles("tc")
@Testcontainers
@Import(TestContainersConfiguration.class)
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Tag("tc")
class RedisUserDataAdapterIntegrationTest {

    @Autowired
    private RedisUserDataAdapter redisTempDataAdapter;
    
    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    private SocialUserProfileDTO testUserProfile;
    private String testUuid;

    @BeforeEach
    void setUp() {
        // Redis 초기화
        RedisTestHelper.flushRedis(redisTemplate);
        
        // 테스트 데이터 준비
        testUuid = "test-uuid-12345";
        testUserProfile = RedisTestHelper.defaultSocialUserProfile();
    }

    @Test
    @DisplayName("정상 케이스 - 임시 데이터 저장 및 조회")
    void shouldSaveAndRetrieveTempData_WhenValidDataProvided() {
        // When: 임시 데이터 저장
        redisTempDataAdapter.saveTempData(testUuid, testUserProfile, "test-fcm-TemporaryToken");

        // Then: 저장된 데이터 조회 검증
        Optional<TempUserData> savedData = redisTempDataAdapter.getTempData(testUuid);
        
        assertThat(savedData).isPresent();
        assertThat(savedData.get().getSocialUserProfileDTO().socialId()).isEqualTo("123456789");
        assertThat(savedData.get().getSocialUserProfileDTO().email()).isEqualTo("test@example.com");
        assertThat(savedData.get().getSocialUserProfileDTO().kakaoAccessToken()).isEqualTo("access-token");
        assertThat(savedData.get().getFcmToken()).isEqualTo("test-fcm-TemporaryToken");
        
        // Redis에서 직접 확인
        String key = RedisTestHelper.RedisKeys.tempUserData(testUuid);
        assertThat(redisTemplate.hasKey(key)).isTrue();
    }

    @Test
    @DisplayName("정상 케이스 - TTL 설정 확인")
    void shouldSetCorrectTTL_WhenDataSaved() {
        // When: 임시 데이터 저장
        redisTempDataAdapter.saveTempData(testUuid, testUserProfile, "test-fcm-TemporaryToken");
        
        String key = RedisTestHelper.RedisKeys.tempUserData(testUuid);
        
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
        assertThatThrownBy(() -> redisTempDataAdapter.saveTempData(null, testUserProfile, null))
                .isInstanceOf(AuthCustomException.class);

        // null userProfile로 저장 시도 시 예외 발생
        assertThatThrownBy(() -> redisTempDataAdapter.saveTempData(testUuid, null, null))
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
        redisTempDataAdapter.saveTempData(testUuid, testUserProfile, "test-fcm-TemporaryToken");
        assertThat(redisTempDataAdapter.getTempData(testUuid)).isPresent();

        // When: 데이터 삭제
        redisTempDataAdapter.removeTempData(testUuid);

        // Then: 데이터가 삭제되어 조회되지 않음
        Optional<TempUserData> result = redisTempDataAdapter.getTempData(testUuid);
        assertThat(result).isEmpty();
        
        // Redis에서도 삭제됨 확인
        String key = RedisTestHelper.RedisKeys.tempUserData(testUuid);
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
    @DisplayName("FCM 토큰 - null FCM 토큰으로 저장 및 조회")
    void shouldHandleNullFcmToken_WhenFcmTokenIsNull() {
        // When: FCM 토큰 없이 저장
        redisTempDataAdapter.saveTempData(testUuid, testUserProfile, null);

        // Then: FCM 토큰이 null로 저장됨
        Optional<TempUserData> result = redisTempDataAdapter.getTempData(testUuid);
        assertThat(result).isPresent();
        assertThat(result.get().getFcmToken()).isNull();
        assertThat(result.get().getSocialUserProfileDTO().nickname()).isEqualTo("testUser");
    }
}
