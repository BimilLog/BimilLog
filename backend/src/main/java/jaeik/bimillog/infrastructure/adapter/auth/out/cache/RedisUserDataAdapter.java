package jaeik.bimillog.infrastructure.adapter.auth.out.cache;

import jaeik.bimillog.domain.auth.application.port.out.RedisUserDataPort;
import jaeik.bimillog.domain.auth.entity.TempUserData;
import jaeik.bimillog.domain.user.entity.TokenVO;
import jaeik.bimillog.domain.auth.application.port.out.SocialLoginPort;
import jaeik.bimillog.infrastructure.adapter.auth.out.social.dto.TemporaryUserDataDTO;
import jaeik.bimillog.infrastructure.auth.AuthCookieManager;
import jaeik.bimillog.infrastructure.exception.CustomException;
import jaeik.bimillog.infrastructure.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.Optional;

/**
 * <h2>Redis 임시 데이터 어댑터</h2>
 * <p>Redis를 사용하여 신규 사용자의 임시 데이터 관리를 위한 어댑터</p>
 * <p>소셜 로그인 프로세스에서 임시 데이터를 안전하게 저장/조회/삭제</p>
 * <p>기존 인메모리 방식 대비 멀티 인스턴스 환경 지원 및 메모리 안정성 향상</p>
 *
 * @author Jaeik
 * @version 2.0.0
 * @since 2.0.0
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class RedisUserDataAdapter implements RedisUserDataPort {

    private final RedisTemplate<String, Object> redisTemplate;
    private final AuthCookieManager authCookieManager;

    // Redis 키 접두사 - 다른 도메인과 충돌 방지
    private static final String TEMP_KEY_PREFIX = "temp:user:";
    // TTL - 회원가입 프로세스 완료를 위한 충분한 시간
    private static final Duration TTL = Duration.ofMinutes(5);

    /**
     * <h3>임시 사용자 데이터 저장</h3>
     * <p>소셜 사용자 프로필 정보를 Redis에 임시 저장합니다.</p>
     * <p>비즈니스 규칙:</p>
     * <ul>
     *   <li>UUID는 필수 (null, 빈 문자열 불허)</li>
     *   <li>userProfile는 필수 (null 불허)</li> 
     *   <li>tokenVO는 필수 (null 불허)</li>
     *   <li>fcmToken은 선택적 (null 허용)</li>
     *   <li>동일 UUID 재저장 시 덮어쓰기</li>
     *   <li>Redis TTL로 자동 만료 (5분)</li>
     * </ul>
     *
     * @param uuid UUID 키
     * @param userProfile 소셜 사용자 프로필 (순수 도메인 모델)
     * @param tokenVO 토큰 정보
     * @param fcmToken FCM 토큰 (선택적)
     * @throws CustomException UUID, userProfile, tokenVO가 유효하지 않은 경우
     * @since 2.0.0
     * @author Jaeik
     */
    @Override
    public void saveTempData(String uuid, SocialLoginPort.SocialUserProfile userProfile, TokenVO tokenVO, String fcmToken) {
        // 1. UUID 검증
        if (uuid == null || uuid.trim().isEmpty()) {
            log.warn("Invalid temp UUID provided: {}", uuid);
            throw new CustomException(ErrorCode.INVALID_TEMP_UUID);
        }
        
        // 2. userProfile 검증
        if (userProfile == null) {
            log.warn("Invalid user profile provided for UUID: {}", uuid);
            throw new CustomException(ErrorCode.INVALID_USER_DATA);
        }
        
        // 3. tokenVO 검증
        if (tokenVO == null) {
            log.warn("Invalid token data provided for UUID: {}", uuid);
            throw new CustomException(ErrorCode.INVALID_TOKEN_DATA);
        }
        
        try {
            String key = TEMP_KEY_PREFIX + uuid;
            
            // 도메인 모델을 인프라 DTO로 변환하여 저장
            TemporaryUserDataDTO tempData = TemporaryUserDataDTO.fromDomainProfile(userProfile, tokenVO, fcmToken);
            
            // Redis에 TTL과 함께 저장
            redisTemplate.opsForValue().set(key, tempData, TTL);
            
            log.debug("Temporary data saved successfully to Redis for UUID: {}", uuid);
            
        } catch (Exception e) {
            log.error("Failed to save temporary data to Redis for UUID: {}, error: {}", uuid, e.getMessage(), e);
            throw new CustomException(ErrorCode.INVALID_USER_DATA);
        }
    }

    /**
     * <h3>임시 사용자 데이터 조회</h3>
     * <p>UUID를 사용하여 Redis에서 임시 사용자 데이터를 조회합니다.</p>
     * <p>비즈니스 규칙:</p>
     * <ul>
     *   <li>null UUID는 빈 결과 반환 (예외 아님)</li>
     *   <li>존재하지 않는 UUID는 빈 결과 반환</li>
     *   <li>만료된 데이터는 Redis에서 자동 정리됨</li>
     * </ul>
     *
     * @param uuid UUID 키
     * @return Optional로 감싼 임시 사용자 데이터
     * @since 2.0.0
     * @author Jaeik
     */
    @Override
    public Optional<TempUserData> getTempData(String uuid) {
        if (uuid == null) {
            log.debug("Null UUID provided for temp data lookup, returning empty");
            return Optional.empty();
        }
        
        try {
            String key = TEMP_KEY_PREFIX + uuid;
            Object data = redisTemplate.opsForValue().get(key);
            
            if (data != null) {
                if (data instanceof TemporaryUserDataDTO dto) {
                    log.debug("Temporary data found in Redis for UUID: {}", uuid);
                    return Optional.of(convertToDomain(dto));
                } else {
                    log.warn("Retrieved data is not of expected type for UUID: {}, got: {}", uuid, data.getClass().getSimpleName());
                    return Optional.empty();
                }
            } else {
                log.debug("No temporary data found in Redis for UUID: {}", uuid);
                return Optional.empty();
            }
            
        } catch (Exception e) {
            log.error("Failed to retrieve temporary data from Redis for UUID: {}, error: {}", uuid, e.getMessage(), e);
            // Redis 장애 시 빈 결과 반환으로 우아한 degradation
            return Optional.empty();
        }
    }

    /**
     * <h3>임시 사용자 데이터 삭제</h3>
     * <p>UUID를 사용하여 Redis에서 임시 사용자 데이터를 삭제합니다.</p>
     * <p>비즈니스 규칙:</p>
     * <ul>
     *   <li>null UUID는 무시 (정상 동작)</li>
     *   <li>존재하지 않는 UUID 삭제는 무시</li>
     *   <li>삭제 실패 시 로그 기록</li>
     * </ul>
     *
     * @param uuid UUID 키
     * @since 2.0.0
     * @author Jaeik
     */
    public void removeTempData(String uuid) {
        if (uuid == null) {
            log.debug("Null UUID provided for temp data removal, ignoring");
            return;
        }
        
        try {
            String key = TEMP_KEY_PREFIX + uuid;
            Boolean deleted = redisTemplate.delete(key);
            
            if (Boolean.TRUE.equals(deleted)) {
                log.debug("Temporary data removed successfully from Redis for UUID: {}", uuid);
            } else {
                log.debug("No temporary data found to remove in Redis for UUID: {}", uuid);
            }
            
        } catch (Exception e) {
            log.error("Failed to remove temporary data from Redis for UUID: {}, error: {}", uuid, e.getMessage(), e);
        }
    }

    /**
     * <h3>임시 사용자 ID 쿠키 생성</h3>
     * <p>신규 회원가입 시 사용자의 임시 UUID를 담는 쿠키를 생성</p>
     *
     * @param uuid 임시 사용자 ID
     * @return 임시 사용자 ID 쿠키
     * @since 2.0.0
     * @author Jaeik
     */
    @Override
    public ResponseCookie createTempCookie(String uuid) {
        return authCookieManager.createTempCookie(uuid);
    }

    /**
     * <h3>인프라 DTO를 도메인 모델로 변환</h3>
     * <p>TemporaryUserDataDTO를 순수 도메인 모델인 TempUserData로 변환합니다.</p>
     * <p>헥사고날 아키텍처 준수: 어댑터에서만 변환 처리</p>
     *
     * @param dto 인프라 계층의 임시 사용자 데이터 DTO
     * @return 순수 도메인 모델
     * @since 2.0.0
     * @author Jaeik
     */
    private TempUserData convertToDomain(TemporaryUserDataDTO dto) {
        return TempUserData.of(
            dto.toDomainProfile(),
            dto.getTokenVO(),
            dto.getFcmToken()
        );
    }
}