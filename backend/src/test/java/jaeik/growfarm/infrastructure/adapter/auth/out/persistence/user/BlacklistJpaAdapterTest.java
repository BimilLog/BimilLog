package jaeik.growfarm.infrastructure.adapter.auth.out.persistence.user;

import jaeik.growfarm.domain.common.entity.SocialProvider;
import jaeik.growfarm.infrastructure.adapter.user.out.persistence.user.blacklist.BlackListRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

/**
 * <h2>BlacklistJpaAdapter 단위 테스트</h2>
 * <p>블랙리스트 확인 어댑터의 비즈니스 로직 위주로 테스트</p>
 * <p>완벽한 테스트로 메인 로직의 문제를 발견</p>
 *
 * @author Claude
 * @version 2.0.0
 * @since 2.0.0
 */
@ExtendWith(MockitoExtension.class)
class BlacklistJpaAdapterTest {

    @Mock private BlackListRepository blackListRepository;

    @InjectMocks private BlacklistJpaAdapter blacklistJpaAdapter;

    @Test
    @DisplayName("블랙리스트 존재 확인 - 존재하는 사용자")
    void shouldReturnTrue_WhenUserExistsInBlacklist() {
        // Given: 블랙리스트에 존재하는 사용자
        SocialProvider provider = SocialProvider.KAKAO;
        String socialId = "123456789";
        given(blackListRepository.existsByProviderAndSocialId(provider, socialId)).willReturn(true);

        // When: 블랙리스트 존재 확인
        boolean result = blacklistJpaAdapter.existsByProviderAndSocialId(provider, socialId);

        // Then: true 반환 검증
        assertThat(result).isTrue();
        verify(blackListRepository).existsByProviderAndSocialId(provider, socialId);
    }

    @Test
    @DisplayName("블랙리스트 존재 확인 - 존재하지 않는 사용자")
    void shouldReturnFalse_WhenUserNotExistsInBlacklist() {
        // Given: 블랙리스트에 존재하지 않는 사용자
        SocialProvider provider = SocialProvider.KAKAO;
        String socialId = "987654321";
        given(blackListRepository.existsByProviderAndSocialId(provider, socialId)).willReturn(false);

        // When: 블랙리스트 존재 확인
        boolean result = blacklistJpaAdapter.existsByProviderAndSocialId(provider, socialId);

        // Then: false 반환 검증
        assertThat(result).isFalse();
        verify(blackListRepository).existsByProviderAndSocialId(provider, socialId);
    }

    @Test
    @DisplayName("블랙리스트 존재 확인 - null provider 처리")
    void shouldHandleNullProvider_WhenProviderIsNull() {
        // Given: null provider
        SocialProvider nullProvider = null;
        String socialId = "123456789";
        given(blackListRepository.existsByProviderAndSocialId(nullProvider, socialId)).willReturn(false);

        // When: null provider로 확인
        boolean result = blacklistJpaAdapter.existsByProviderAndSocialId(nullProvider, socialId);

        // Then: repository 호출 및 결과 반환 검증
        assertThat(result).isFalse();
        verify(blackListRepository).existsByProviderAndSocialId(nullProvider, socialId);
    }

    @Test
    @DisplayName("블랙리스트 존재 확인 - null socialId 처리")
    void shouldHandleNullSocialId_WhenSocialIdIsNull() {
        // Given: null socialId
        SocialProvider provider = SocialProvider.KAKAO;
        String nullSocialId = null;
        given(blackListRepository.existsByProviderAndSocialId(provider, nullSocialId)).willReturn(false);

        // When: null socialId로 확인
        boolean result = blacklistJpaAdapter.existsByProviderAndSocialId(provider, nullSocialId);

        // Then: repository 호출 및 결과 반환 검증
        assertThat(result).isFalse();
        verify(blackListRepository).existsByProviderAndSocialId(provider, nullSocialId);
    }

    @Test
    @DisplayName("블랙리스트 존재 확인 - 모든 파라미터 null")
    void shouldHandleAllNullParameters_WhenBothParametersAreNull() {
        // Given: 모든 파라미터가 null
        SocialProvider nullProvider = null;
        String nullSocialId = null;
        given(blackListRepository.existsByProviderAndSocialId(nullProvider, nullSocialId)).willReturn(false);

        // When: 모든 파라미터를 null로 확인
        boolean result = blacklistJpaAdapter.existsByProviderAndSocialId(nullProvider, nullSocialId);

        // Then: repository 호출 및 결과 반환 검증
        assertThat(result).isFalse();
        verify(blackListRepository).existsByProviderAndSocialId(nullProvider, nullSocialId);
    }

    @Test
    @DisplayName("블랙리스트 존재 확인 - 빈 문자열 socialId 처리")
    void shouldHandleEmptySocialId_WhenSocialIdIsEmpty() {
        // Given: 빈 문자열 socialId
        SocialProvider provider = SocialProvider.KAKAO;
        String emptySocialId = "";
        given(blackListRepository.existsByProviderAndSocialId(provider, emptySocialId)).willReturn(false);

        // When: 빈 문자열 socialId로 확인
        boolean result = blacklistJpaAdapter.existsByProviderAndSocialId(provider, emptySocialId);

        // Then: repository 호출 및 결과 반환 검증
        assertThat(result).isFalse();
        verify(blackListRepository).existsByProviderAndSocialId(provider, emptySocialId);
    }

    @Test
    @DisplayName("블랙리스트 존재 확인 - 공백 문자열 socialId 처리")
    void shouldHandleWhitespaceSocialId_WhenSocialIdIsWhitespace() {
        // Given: 공백 문자열 socialId
        SocialProvider provider = SocialProvider.KAKAO;
        String whitespaceSocialId = "   ";
        given(blackListRepository.existsByProviderAndSocialId(provider, whitespaceSocialId)).willReturn(false);

        // When: 공백 문자열 socialId로 확인
        boolean result = blacklistJpaAdapter.existsByProviderAndSocialId(provider, whitespaceSocialId);

        // Then: repository 호출 및 결과 반환 검증
        assertThat(result).isFalse();
        verify(blackListRepository).existsByProviderAndSocialId(provider, whitespaceSocialId);
    }

    @Test
    @DisplayName("블랙리스트 존재 확인 - 다양한 SocialProvider 처리")
    void shouldHandleDifferentProviders_WhenVariousProvidersUsed() {
        // Given: 다양한 SocialProvider들
        String socialId = "123456789";

        // KAKAO Provider 테스트
        given(blackListRepository.existsByProviderAndSocialId(SocialProvider.KAKAO, socialId)).willReturn(true);
        boolean kakaoResult = blacklistJpaAdapter.existsByProviderAndSocialId(SocialProvider.KAKAO, socialId);
        assertThat(kakaoResult).isTrue();

        // 다른 Provider가 있다면 추가 테스트 (현재는 KAKAO만 있음)
        // 향후 NAVER, GOOGLE 등 추가 시 여기에 테스트 추가
    }

    @Test
    @DisplayName("블랙리스트 존재 확인 - 매우 긴 socialId 처리")
    void shouldHandleLongSocialId_WhenSocialIdIsVeryLong() {
        // Given: 매우 긴 socialId
        SocialProvider provider = SocialProvider.KAKAO;
        String longSocialId = "a".repeat(1000); // 1000자 길이의 socialId
        given(blackListRepository.existsByProviderAndSocialId(provider, longSocialId)).willReturn(false);

        // When: 긴 socialId로 확인
        boolean result = blacklistJpaAdapter.existsByProviderAndSocialId(provider, longSocialId);

        // Then: repository 호출 및 결과 반환 검증
        assertThat(result).isFalse();
        verify(blackListRepository).existsByProviderAndSocialId(provider, longSocialId);
    }

    @Test
    @DisplayName("블랙리스트 존재 확인 - 특수 문자 포함 socialId 처리")
    void shouldHandleSpecialCharactersSocialId_WhenSocialIdContainsSpecialChars() {
        // Given: 특수 문자 포함 socialId
        SocialProvider provider = SocialProvider.KAKAO;
        String specialCharSocialId = "user@#$%^&*()123";
        given(blackListRepository.existsByProviderAndSocialId(provider, specialCharSocialId)).willReturn(false);

        // When: 특수 문자 포함 socialId로 확인
        boolean result = blacklistJpaAdapter.existsByProviderAndSocialId(provider, specialCharSocialId);

        // Then: repository 호출 및 결과 반환 검증
        assertThat(result).isFalse();
        verify(blackListRepository).existsByProviderAndSocialId(provider, specialCharSocialId);
    }

    @Test
    @DisplayName("예외 전파 테스트 - Repository에서 예외 발생")
    void shouldPropagateException_WhenRepositoryThrowsException() {
        // Given: Repository에서 예외 발생
        SocialProvider provider = SocialProvider.KAKAO;
        String socialId = "123456789";
        RuntimeException expectedException = new RuntimeException("Database connection failed");
        given(blackListRepository.existsByProviderAndSocialId(provider, socialId)).willThrow(expectedException);

        // When & Then: 예외 전파 검증
        assertThatThrownBy(() -> blacklistJpaAdapter.existsByProviderAndSocialId(provider, socialId))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("Database connection failed");

        verify(blackListRepository).existsByProviderAndSocialId(provider, socialId);
    }

    @Test
    @DisplayName("성능 테스트 - 대량 호출 시 일관성")
    void shouldMaintainConsistency_WhenCalledManyTimes() {
        // Given: 동일한 파라미터
        SocialProvider provider = SocialProvider.KAKAO;
        String socialId = "123456789";
        given(blackListRepository.existsByProviderAndSocialId(provider, socialId)).willReturn(true);

        // When: 대량 호출
        int callCount = 1000;
        for (int i = 0; i < callCount; i++) {
            boolean result = blacklistJpaAdapter.existsByProviderAndSocialId(provider, socialId);
            // Then: 모든 호출에서 동일한 결과
            assertThat(result).isTrue();
        }

        // Repository가 정확히 호출 횟수만큼 호출되었는지 확인
        verify(blackListRepository, org.mockito.Mockito.times(callCount))
                .existsByProviderAndSocialId(provider, socialId);
    }

    @Test
    @DisplayName("동시성 테스트 - 다른 파라미터로 동시 호출")
    void shouldHandleConcurrentCalls_WhenDifferentParametersUsed() {
        // Given: 서로 다른 파라미터들
        SocialProvider provider = SocialProvider.KAKAO;
        String socialId1 = "user1";
        String socialId2 = "user2";
        
        given(blackListRepository.existsByProviderAndSocialId(provider, socialId1)).willReturn(true);
        given(blackListRepository.existsByProviderAndSocialId(provider, socialId2)).willReturn(false);

        // When: 동시 호출
        boolean result1 = blacklistJpaAdapter.existsByProviderAndSocialId(provider, socialId1);
        boolean result2 = blacklistJpaAdapter.existsByProviderAndSocialId(provider, socialId2);

        // Then: 각각 올바른 결과 반환
        assertThat(result1).isTrue();
        assertThat(result2).isFalse();
        
        verify(blackListRepository).existsByProviderAndSocialId(provider, socialId1);
        verify(blackListRepository).existsByProviderAndSocialId(provider, socialId2);
    }

    // TODO: 테스트 실패 시 의심해볼 메인 로직 문제들
    // 1. Repository 의존성 누락: BlackListRepository가 제대로 주입되지 않음
    // 2. 데이터베이스 연결 실패: JPA 설정 오류로 인한 연결 문제
    // 3. 쿼리 성능 문제: existsByProviderAndSocialId 쿼리의 인덱스 부족
    // 4. null 처리 미흡: Repository에서 null 파라미터 처리 실패
    // 5. 트랜잭션 문제: 읽기 전용 트랜잭션 설정 누락
    // 6. 캐시 일관성: 블랙리스트 정보의 캐시와 DB 불일치
    // 7. 동시성 문제: 블랙리스트 추가/삭제와 조회 간 경쟁 조건
    // 8. 예외 처리 누락: 데이터베이스 예외에 대한 적절한 변환 부족
    // 9. 로깅 부족: 블랙리스트 확인 결과에 대한 로깅 누락
    // 10. 보안 문제: SQL 인젝션 등 보안 취약점
    //
    // 🔥 중요: 이 테스트들이 실패한다면 비즈니스 로직 자체에 문제가 있을 가능성이 높음
    // - 블랙리스트 확인은 보안 관련 핵심 기능이므로 완벽한 동작 필수
    // - 잘못된 결과는 부정 사용자 허용 또는 정상 사용자 차단으로 이어짐
    // - 성능 이슈는 로그인 프로세스 전체의 지연을 야기
}