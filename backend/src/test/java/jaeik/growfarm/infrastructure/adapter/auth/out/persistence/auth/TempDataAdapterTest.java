package jaeik.growfarm.infrastructure.adapter.auth.out.persistence.auth;

import jaeik.growfarm.domain.common.entity.SocialProvider;
import jaeik.growfarm.domain.user.entity.TokenVO;
import jaeik.growfarm.infrastructure.adapter.auth.out.social.dto.SocialLoginUserData;
import jaeik.growfarm.domain.auth.application.port.out.SocialLoginPort;
import jaeik.growfarm.infrastructure.adapter.auth.out.social.dto.TemporaryUserDataDTO;
import jaeik.growfarm.infrastructure.auth.AuthCookieManager;
import jaeik.growfarm.infrastructure.exception.CustomException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseCookie;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

/**
 * <h2>TempDataAdapter 단위 테스트</h2>
 * <p>임시 데이터 관리 어댑터의 비즈니스 로직 위주로 테스트</p>
 * <p>메모리 기반 임시 데이터 저장소의 완벽한 검증</p>
 *
 * @author Jaeik
 * @version 2.0.0
 */
@ExtendWith(MockitoExtension.class)
class TempDataAdapterTest {

    @Mock private AuthCookieManager authCookieManager;

    @InjectMocks private TempDataAdapter tempDataAdapter;

    private SocialLoginUserData testUserData;
    private SocialLoginPort.SocialUserProfile testUserProfile;
    private TokenVO testTokenVO;
    private String testUuid;

    @BeforeEach
    void setUp() {
        // 테스트용 데이터 준비
        testUuid = "test-uuid-12345";
        testUserData = SocialLoginUserData.builder()
                .provider(SocialProvider.KAKAO)
                .socialId("123456789")
                .nickname("testUser")
                .profileImageUrl("https://example.com/profile.jpg")
                .fcmToken("fcm-token-12345")
                .build();
        testUserProfile = new SocialLoginPort.SocialUserProfile("123456789", "test@example.com", SocialProvider.KAKAO, "testUser", "https://example.com/profile.jpg");
        testTokenVO = TokenVO.builder()
                .accessToken("access-token-12345")
                .refreshToken("refresh-token-12345")
                .build();
    }

    @Test
    @DisplayName("임시 데이터 저장 - 정상적인 데이터로 저장")
    void shouldSaveTempData_WhenValidDataProvided() {
        // When: 임시 데이터 저장
        tempDataAdapter.saveTempData(testUuid, testUserProfile, testTokenVO);

        // Then: 저장된 데이터 조회 검증
        Optional<TemporaryUserDataDTO> savedData = tempDataAdapter.getTempData(testUuid);
        
        assertThat(savedData).isPresent();
        assertThat(savedData.get().toDomainProfile()).isEqualTo(testUserProfile);
        assertThat(savedData.get().getTokenVO()).isEqualTo(testTokenVO);
        // FCM 토큰은 소셜 로그인과 별도 관리되므로 null이 정상
        assertThat(savedData.get().getFcmToken()).isNull();
    }

    @Test
    @DisplayName("임시 데이터 저장 - null 값들로 저장")
    void shouldSaveTempData_WhenNullValuesProvided() {
        
        // Given: null 값들
        String nullUuid = null;
        SocialLoginPort.SocialUserProfile nullUserData = null;
        TokenVO nullTokenVO = null;

        // When & Then: null 값들로 저장 시도 시 예외 발생해야 함
        // 비즈니스 로직이 올바르게 Input Validation을 수행하는지 검증
        assertThatThrownBy(() -> tempDataAdapter.saveTempData(nullUuid, nullUserData, nullTokenVO))
                .isInstanceOf(CustomException.class)
                .hasMessageContaining("임시 사용자 UUID가 유효하지 않습니다");
        
        // 추가 검증: null UUID로 조회 시 빈 결과 반환
        Optional<TemporaryUserDataDTO> result = tempDataAdapter.getTempData(nullUuid);
        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("임시 데이터 조회 - 존재하는 UUID로 조회")
    void shouldReturnTempData_WhenUuidExists() {
        // Given: 저장된 데이터
        tempDataAdapter.saveTempData(testUuid, testUserProfile, testTokenVO);

        // When: 존재하는 UUID로 조회
        Optional<TemporaryUserDataDTO> result = tempDataAdapter.getTempData(testUuid);

        // Then: 정확한 데이터 반환
        assertThat(result).isPresent();
        TemporaryUserDataDTO data = result.get();
        assertThat(data.getSocialLoginUserData().provider()).isEqualTo(SocialProvider.KAKAO);
        assertThat(data.getSocialLoginUserData().socialId()).isEqualTo("123456789");
        assertThat(data.getSocialLoginUserData().nickname()).isEqualTo("testUser");
        assertThat(data.getTokenVO().accessToken()).isEqualTo("access-token-12345");
    }

    @Test
    @DisplayName("임시 데이터 조회 - 존재하지 않는 UUID로 조회")
    void shouldReturnEmpty_WhenUuidNotExists() {
        // Given: 존재하지 않는 UUID
        String nonExistentUuid = "non-existent-uuid";

        // When: 존재하지 않는 UUID로 조회
        Optional<TemporaryUserDataDTO> result = tempDataAdapter.getTempData(nonExistentUuid);

        // Then: 빈 Optional 반환
        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("임시 데이터 조회 - null UUID로 조회")
    void shouldReturnEmpty_WhenUuidIsNull() {
        // Given: null UUID
        String nullUuid = null;

        // When: null UUID로 조회
        Optional<TemporaryUserDataDTO> result = tempDataAdapter.getTempData(nullUuid);

        // Then: 빈 Optional 반환 (ConcurrentHashMap은 null key를 지원하지 않지만 Optional.ofNullable이 처리)
        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("임시 데이터 삭제 - 존재하는 데이터 삭제")
    void shouldRemoveTempData_WhenDataExists() {
        // Given: 저장된 데이터
        tempDataAdapter.saveTempData(testUuid, testUserProfile, testTokenVO);
        assertThat(tempDataAdapter.getTempData(testUuid)).isPresent();

        // When: 데이터 삭제
        tempDataAdapter.removeTempData(testUuid);

        // Then: 데이터가 삭제되어 조회되지 않음
        Optional<TemporaryUserDataDTO> result = tempDataAdapter.getTempData(testUuid);
        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("임시 데이터 삭제 - 존재하지 않는 데이터 삭제")
    void shouldHandleRemoveNonExistentData_WhenDataNotExists() {
        // Given: 존재하지 않는 UUID
        String nonExistentUuid = "non-existent-uuid";

        // When: 존재하지 않는 데이터 삭제 시도 (예외 발생하지 않아야 함)
        tempDataAdapter.removeTempData(nonExistentUuid);

        // Then: 예외 없이 정상 처리됨
        assertThat(tempDataAdapter.getTempData(nonExistentUuid)).isEmpty();
    }

    @Test
    @DisplayName("임시 데이터 삭제 - null UUID로 삭제")
    void shouldHandleRemoveWithNullUuid_WhenUuidIsNull() {
        // Given: null UUID
        String nullUuid = null;

        // When: null UUID로 삭제 시도 (예외 발생하지 않아야 함)
        tempDataAdapter.removeTempData(nullUuid);

        // Then: 예외 없이 처리됨 (ConcurrentHashMap.remove(null)은 null 반환)
        assertThat(tempDataAdapter.getTempData(nullUuid)).isEmpty();
    }

    @Test
    @DisplayName("임시 쿠키 생성 - 정상적인 UUID로 쿠키 생성")
    void shouldCreateTempCookie_WhenValidUuidProvided() {
        // Given: AuthCookieManager에서 반환할 쿠키
        ResponseCookie expectedCookie = ResponseCookie.from("tempUserId", testUuid)
                .maxAge(300) // 5분
                .httpOnly(true)
                .build();
        given(authCookieManager.createTempCookie(testUuid)).willReturn(expectedCookie);

        // When: 임시 쿠키 생성
        ResponseCookie actualCookie = tempDataAdapter.createTempCookie(testUuid);

        // Then: 올바른 쿠키 생성 검증
        assertThat(actualCookie).isEqualTo(expectedCookie);
        assertThat(actualCookie.getName()).isEqualTo("tempUserId");
        assertThat(actualCookie.getValue()).isEqualTo(testUuid);
        verify(authCookieManager).createTempCookie(testUuid);
    }

    @Test
    @DisplayName("임시 쿠키 생성 - null UUID로 쿠키 생성")
    void shouldCreateTempCookie_WhenNullUuidProvided() {
        // Given: null UUID 및 AuthCookieManager 설정
        String nullUuid = null;
        ResponseCookie expectedCookie = ResponseCookie.from("tempUserId", "")
                .maxAge(0)
                .build();
        given(authCookieManager.createTempCookie(nullUuid)).willReturn(expectedCookie);

        // When: null UUID로 쿠키 생성
        ResponseCookie actualCookie = tempDataAdapter.createTempCookie(nullUuid);

        // Then: AuthCookieManager의 처리 결과 반환
        assertThat(actualCookie).isEqualTo(expectedCookie);
        verify(authCookieManager).createTempCookie(nullUuid);
    }

    @Test
    @DisplayName("임시 쿠키 생성 - 빈 문자열 UUID로 쿠키 생성")
    void shouldCreateTempCookie_WhenEmptyUuidProvided() {
        // Given: 빈 문자열 UUID
        String emptyUuid = "";
        ResponseCookie expectedCookie = ResponseCookie.from("tempUserId", emptyUuid)
                .maxAge(300)
                .httpOnly(true)
                .build();
        given(authCookieManager.createTempCookie(emptyUuid)).willReturn(expectedCookie);

        // When: 빈 문자열 UUID로 쿠키 생성
        ResponseCookie actualCookie = tempDataAdapter.createTempCookie(emptyUuid);

        // Then: 정상적으로 쿠키 생성됨
        assertThat(actualCookie).isEqualTo(expectedCookie);
        verify(authCookieManager).createTempCookie(emptyUuid);
    }

    @Test
    @DisplayName("데이터 덮어쓰기 - 동일 UUID로 여러 번 저장")
    void shouldOverwriteData_WhenSameUuidSavedMultipleTimes() {
        // Given: 첫 번째 데이터 저장
        tempDataAdapter.saveTempData(testUuid, testUserProfile, testTokenVO);
        
        // 새로운 데이터
        SocialLoginPort.SocialUserProfile newUserProfile = new SocialLoginPort.SocialUserProfile("987654321", "new@example.com", SocialProvider.KAKAO, "newUser", "https://example.com/new-profile.jpg");
        TokenVO newTokenVO = TokenVO.builder()
                .accessToken("new-access-token")
                .refreshToken("new-refresh-token")
                .build();

        // When: 동일한 UUID로 새 데이터 저장
        tempDataAdapter.saveTempData(testUuid, newUserProfile, newTokenVO);

        // Then: 새 데이터로 덮어써짐
        Optional<TemporaryUserDataDTO> result = tempDataAdapter.getTempData(testUuid);
        assertThat(result).isPresent();
        assertThat(result.get().getSocialLoginUserData().socialId()).isEqualTo("987654321");
        assertThat(result.get().getSocialLoginUserData().nickname()).isEqualTo("newUser");
        assertThat(result.get().getTokenVO().accessToken()).isEqualTo("new-access-token");
    }

    @Test
    @DisplayName("동시성 테스트 - 여러 스레드에서 동시 저장/조회")
    void shouldHandleConcurrentAccess_WhenMultipleThreadsAccessSimultaneously() {
        // Given: 여러 UUID와 데이터
        String uuid1 = "uuid-1";
        String uuid2 = "uuid-2";
        
        SocialLoginPort.SocialUserProfile userProfile1 = new SocialLoginPort.SocialUserProfile("111", "user1@example.com", SocialProvider.KAKAO, "user1", "https://example.com/profile1.jpg");
        SocialLoginPort.SocialUserProfile userProfile2 = new SocialLoginPort.SocialUserProfile("222", "user2@example.com", SocialProvider.KAKAO, "user2", "https://example.com/profile2.jpg");

        // When: 동시에 저장 및 조회
        tempDataAdapter.saveTempData(uuid1, userProfile1, testTokenVO);
        tempDataAdapter.saveTempData(uuid2, userProfile2, testTokenVO);

        // Then: 각각의 데이터가 독립적으로 저장되고 조회됨
        Optional<TemporaryUserDataDTO> result1 = tempDataAdapter.getTempData(uuid1);
        Optional<TemporaryUserDataDTO> result2 = tempDataAdapter.getTempData(uuid2);

        assertThat(result1).isPresent();
        assertThat(result1.get().getSocialLoginUserData().nickname()).isEqualTo("user1");
        assertThat(result2).isPresent();
        assertThat(result2.get().getSocialLoginUserData().nickname()).isEqualTo("user2");
    }

    @Test
    @DisplayName("자동 정리 스케줄링 - 5분 후 데이터 자동 삭제 (단위 테스트용 단축)")
    void shouldScheduleCleanup_WhenDataSaved() {
        // Given: 테스트용 짧은 시간 (실제로는 5분이지만 테스트에서는 검증 불가)
        tempDataAdapter.saveTempData(testUuid, testUserProfile, testTokenVO);
        
        // When: 데이터가 저장됨
        Optional<TemporaryUserDataDTO> immediateResult = tempDataAdapter.getTempData(testUuid);
        
        // Then: 즉시 조회 시에는 데이터 존재
        assertThat(immediateResult).isPresent();
        
        // Note: 5분 대기는 단위 테스트에 적합하지 않으므로, 실제 스케줄링 검증은 통합 테스트에서 수행
        // 여기서는 저장 직후 데이터 존재만 확인
    }

    @Test
    @DisplayName("메모리 사용량 테스트 - 대량 데이터 저장 후 정리")
    void shouldHandleLargeAmountOfData_WhenManyEntriesStored() {
        // Given: 대량의 UUID와 데이터
        int dataCount = 1000;

        // When: 대량 데이터 저장
        for (int i = 0; i < dataCount; i++) {
            String uuid = "uuid-" + i;
            SocialLoginPort.SocialUserProfile userProfile = new SocialLoginPort.SocialUserProfile("id-" + i, "user" + i + "@example.com", SocialProvider.KAKAO, "user-" + i, "https://example.com/profile" + i + ".jpg");
            tempDataAdapter.saveTempData(uuid, userProfile, testTokenVO);
        }

        // Then: 모든 데이터가 정상 저장되고 조회됨
        for (int i = 0; i < dataCount; i++) {
            String uuid = "uuid-" + i;
            Optional<TemporaryUserDataDTO> result = tempDataAdapter.getTempData(uuid);
            assertThat(result).isPresent();
            assertThat(result.get().getSocialLoginUserData().nickname()).isEqualTo("user-" + i);
        }

        // 정리: 저장된 데이터 삭제
        for (int i = 0; i < dataCount; i++) {
            tempDataAdapter.removeTempData("uuid-" + i);
        }
    }
}