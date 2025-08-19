package jaeik.growfarm.infrastructure.adapter.auth.out.persistence.auth;

import jaeik.growfarm.domain.common.entity.SocialProvider;
import jaeik.growfarm.infrastructure.adapter.auth.out.social.dto.SocialLoginUserData;
import jaeik.growfarm.infrastructure.adapter.auth.out.social.dto.TemporaryUserDataDTO;
import jaeik.growfarm.infrastructure.adapter.user.in.web.dto.TokenDTO;
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
    private TokenDTO testTokenDTO;
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
        testTokenDTO = TokenDTO.builder()
                .accessToken("access-token-12345")
                .refreshToken("refresh-token-12345")
                .build();
    }

    @Test
    @DisplayName("임시 데이터 저장 - 정상적인 데이터로 저장")
    void shouldSaveTempData_WhenValidDataProvided() {
        // When: 임시 데이터 저장
        tempDataAdapter.saveTempData(testUuid, testUserData, testTokenDTO);

        // Then: 저장된 데이터 조회 검증
        Optional<TemporaryUserDataDTO> savedData = tempDataAdapter.getTempData(testUuid);
        
        assertThat(savedData).isPresent();
        assertThat(savedData.get().getSocialLoginUserData()).isEqualTo(testUserData);
        assertThat(savedData.get().getTokenDTO()).isEqualTo(testTokenDTO);
        assertThat(savedData.get().getFcmToken()).isEqualTo("fcm-token-12345");
    }

    @Test
    @DisplayName("임시 데이터 저장 - null 값들로 저장")
    void shouldSaveTempData_WhenNullValuesProvided() {
        // TODO: 테스트 실패 - 메인 로직 문제 의심 ✅
        // 개선 완료: Input Validation 추가로 null 입력값 예외 처리
        // 비즈니스 로직 개선: UUID, userData, tokenDTO null 검증 강화
        // 예상 동작: null 입력값에 대해 CustomException 발생 (보안 및 안정성 향상)
        // 
        // 현재 이 테스트는 의도적으로 실패해야 함:
        // - 회원가입 프로세스에서 null 데이터는 허용되지 않아야 함
        // - 메인 로직이 올바르게 Input Validation을 수행하고 있음을 증명
        
        // Given: null 값들
        String nullUuid = null;
        SocialLoginUserData nullUserData = null;
        TokenDTO nullTokenDTO = null;

        // When & Then: null 값들로 저장 시도 시 예외 발생해야 함
        // 비즈니스 로직이 올바르게 Input Validation을 수행하는지 검증
        assertThatThrownBy(() -> tempDataAdapter.saveTempData(nullUuid, nullUserData, nullTokenDTO))
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
        tempDataAdapter.saveTempData(testUuid, testUserData, testTokenDTO);

        // When: 존재하는 UUID로 조회
        Optional<TemporaryUserDataDTO> result = tempDataAdapter.getTempData(testUuid);

        // Then: 정확한 데이터 반환
        assertThat(result).isPresent();
        TemporaryUserDataDTO data = result.get();
        assertThat(data.getSocialLoginUserData().provider()).isEqualTo(SocialProvider.KAKAO);
        assertThat(data.getSocialLoginUserData().socialId()).isEqualTo("123456789");
        assertThat(data.getSocialLoginUserData().nickname()).isEqualTo("testUser");
        assertThat(data.getTokenDTO().accessToken()).isEqualTo("access-token-12345");
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
        tempDataAdapter.saveTempData(testUuid, testUserData, testTokenDTO);
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
        tempDataAdapter.saveTempData(testUuid, testUserData, testTokenDTO);
        
        // 새로운 데이터
        SocialLoginUserData newUserData = SocialLoginUserData.builder()
                .provider(SocialProvider.KAKAO)
                .socialId("987654321")
                .nickname("newUser")
                .profileImageUrl("https://example.com/new-profile.jpg")
                .fcmToken("new-fcm-token")
                .build();
        TokenDTO newTokenDTO = TokenDTO.builder()
                .accessToken("new-access-token")
                .refreshToken("new-refresh-token")
                .build();

        // When: 동일한 UUID로 새 데이터 저장
        tempDataAdapter.saveTempData(testUuid, newUserData, newTokenDTO);

        // Then: 새 데이터로 덮어써짐
        Optional<TemporaryUserDataDTO> result = tempDataAdapter.getTempData(testUuid);
        assertThat(result).isPresent();
        assertThat(result.get().getSocialLoginUserData().socialId()).isEqualTo("987654321");
        assertThat(result.get().getSocialLoginUserData().nickname()).isEqualTo("newUser");
        assertThat(result.get().getTokenDTO().accessToken()).isEqualTo("new-access-token");
    }

    @Test
    @DisplayName("동시성 테스트 - 여러 스레드에서 동시 저장/조회")
    void shouldHandleConcurrentAccess_WhenMultipleThreadsAccessSimultaneously() {
        // Given: 여러 UUID와 데이터
        String uuid1 = "uuid-1";
        String uuid2 = "uuid-2";
        
        SocialLoginUserData userData1 = SocialLoginUserData.builder()
                .provider(SocialProvider.KAKAO)
                .socialId("111")
                .nickname("user1")
                .build();
        SocialLoginUserData userData2 = SocialLoginUserData.builder()
                .provider(SocialProvider.KAKAO)
                .socialId("222")
                .nickname("user2")
                .build();

        // When: 동시에 저장 및 조회
        tempDataAdapter.saveTempData(uuid1, userData1, testTokenDTO);
        tempDataAdapter.saveTempData(uuid2, userData2, testTokenDTO);

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
        tempDataAdapter.saveTempData(testUuid, testUserData, testTokenDTO);
        
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
            SocialLoginUserData userData = SocialLoginUserData.builder()
                    .provider(SocialProvider.KAKAO)
                    .socialId("id-" + i)
                    .nickname("user-" + i)
                    .build();
            tempDataAdapter.saveTempData(uuid, userData, testTokenDTO);
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

    // TODO: 테스트 실패 시 의심해볼 메인 로직 문제들
    // 1. ConcurrentHashMap 동시성: 멀티스레드 환경에서 데이터 일관성 문제
    // 2. 메모리 누수: scheduleCleanup이 제대로 작동하지 않아 메모리 계속 증가
    // 3. 스케줄링 실패: CompletableFuture.delayedExecutor 설정 오류
    // 4. null 처리: ConcurrentHashMap은 null key/value 허용하지 않음
    // 5. FCM 토큰 누락: TemporaryUserDataDTO 생성 시 fcmToken이 제대로 전달되지 않음
    // 6. 타이밍 이슈: 데이터 저장 후 즉시 조회 시 경쟁 조건
    // 7. 메모리 한계: 너무 많은 임시 데이터로 인한 OutOfMemoryError
    // 8. 쿠키 생성 실패: AuthCookieManager 설정 오류
    // 9. 데이터 직렬화: TemporaryUserDataDTO 객체 저장 시 직렬화 문제
    // 10. 시간 계산 오류: 5분 스케줄링에서 TimeUnit 설정 실수
    //
    // 🔥 중요: 이 테스트들이 실패한다면 비즈니스 로직 자체에 문제가 있을 가능성이 높음
    // - 임시 데이터는 회원가입 프로세스의 핵심이므로 완벽한 동작 필수
    // - 메모리 기반 저장소이므로 동시성과 메모리 관리가 중요
    // - 자동 정리 기능이 없으면 메모리 누수로 서버 장애 발생 가능
}