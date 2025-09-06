package jaeik.bimillog.infrastructure.adapter.auth.out.cache;

import jaeik.bimillog.domain.auth.application.port.out.RedisUserDataPort;
import jaeik.bimillog.domain.user.entity.SocialProvider;
import jaeik.bimillog.domain.auth.entity.LoginResult;
import jaeik.bimillog.domain.auth.exception.AuthCustomException;
import jaeik.bimillog.domain.auth.exception.AuthErrorCode;
import jaeik.bimillog.domain.user.entity.Token;
import jaeik.bimillog.infrastructure.adapter.auth.out.social.dto.SocialLoginUserData;
import jaeik.bimillog.infrastructure.adapter.auth.out.social.dto.TemporaryUserDataDTO;
import jaeik.bimillog.infrastructure.auth.AuthCookieManager;
import jaeik.bimillog.infrastructure.exception.CustomException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.Map;
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

    private static final String TEMP_KEY_PREFIX = "temp:user:";
    private static final Duration TTL = Duration.ofMinutes(5);
    
    private static final String NULL_UUID_MESSAGE = "유효하지 않은 임시 UUID 제공됨: {}";
    private static final String NULL_PROFILE_MESSAGE = "UUID {}에 대해 유효하지 않은 사용자 프로필 제공됨";
    private static final String NULL_TOKEN_MESSAGE = "UUID {}에 대해 유효하지 않은 토큰 데이터 제공됨";

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
     * @param token 토큰 정보
     * @param fcmToken FCM 토큰 (선택적)
     * @throws CustomException UUID, userProfile, token이 유효하지 않은 경우
     * @since 2.0.0
     * @author Jaeik
     */
    @Override
    public void saveTempData(String uuid, LoginResult.SocialUserProfile userProfile, Token token, String fcmToken) {
        validateTempDataInputs(uuid, userProfile, token);

        executeRedisOperation(() -> {
            String key = buildTempKey(uuid);
            TemporaryUserDataDTO tempData = TemporaryUserDataDTO.fromDomainProfile(userProfile, token, fcmToken);
            redisTemplate.opsForValue().set(key, tempData, TTL);
            log.debug("UUID {}에 대한 임시 데이터가 Redis에 성공적으로 저장됨", uuid);
        }, uuid, "저장", AuthErrorCode.INVALID_USER_DATA);
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
    public Optional<LoginResult.TempUserData> getTempData(String uuid) {
        if (uuid == null) {
            log.debug("임시 데이터 조회에 null UUID 제공됨, 빈 결과 반환");
            return Optional.empty();
        }

        try {
            String key = buildTempKey(uuid);
            Object data = redisTemplate.opsForValue().get(key);
            return convertRedisDataToDomain(uuid, data);
        } catch (Exception e) {
            log.error("UUID {}에 대한 임시 데이터 Redis 조회 실패: {}", uuid, e.getMessage(), e);
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
            log.debug("임시 데이터 제거에 null UUID 제공됨, 무시");
            return;
        }

        executeRedisOperationSafely(() -> {
            Boolean deleted = redisTemplate.delete(buildTempKey(uuid));
            log.debug(deleted
                    ? "UUID {}에 대한 임시 데이터가 Redis에서 성공적으로 제거됨"
                    : "UUID {}에 대해 Redis에서 제거할 임시 데이터가 발견되지 않음", uuid);
        }, uuid, "제거");
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

    /* ===================== Private Helpers ===================== */

    private String buildTempKey(String uuid) {
        return TEMP_KEY_PREFIX + uuid;
    }

    private void executeRedisOperation(Runnable operation, String uuid, String operationType, AuthErrorCode errorCode) {
        try {
            operation.run();
        } catch (Exception e) {
            log.error("UUID {}에 대한 임시 데이터 Redis {} 실패: {}", uuid, operationType, e.getMessage(), e);
            throw new AuthCustomException(errorCode);
        }
    }

    private void executeRedisOperationSafely(Runnable operation, String uuid, String operationType) {
        try {
            operation.run();
        } catch (Exception e) {
            log.error("UUID {}에 대한 임시 데이터 Redis {} 실패: {}", uuid, operationType, e.getMessage(), e);
        }
    }

    private void validateTempDataInputs(String uuid, LoginResult.SocialUserProfile userProfile, Token token) {
        if (isInvalidUuid(uuid)) {
            log.warn(NULL_UUID_MESSAGE, uuid);
            throw new AuthCustomException(AuthErrorCode.INVALID_TEMP_UUID);
        }
        if (userProfile == null) {
            log.warn(NULL_PROFILE_MESSAGE, uuid);
            throw new AuthCustomException(AuthErrorCode.INVALID_USER_DATA);
        }
        if (token == null) {
            log.warn(NULL_TOKEN_MESSAGE, uuid);
            throw new AuthCustomException(AuthErrorCode.INVALID_TOKEN_DATA);
        }
    }

    private boolean isInvalidUuid(String uuid) {
        return uuid == null || uuid.trim().isEmpty();
    }

    private Optional<LoginResult.TempUserData> convertRedisDataToDomain(String uuid, Object data) {
        if (data == null) {
            log.debug("UUID {}에 대한 임시 데이터가 Redis에서 발견되지 않음", uuid);
            return Optional.empty();
        }

        try {
            TemporaryUserDataDTO dto = convertToDTO(uuid, data);
            return Optional.of(LoginResult.TempUserData.of(dto.toDomainProfile(), dto.getToken(), dto.getFcmToken()));
        } catch (Exception e) {
            log.error("UUID {}에 대한 임시 데이터 변환 실패: {}", uuid, e.getMessage(), e);
            throw new AuthCustomException(AuthErrorCode.INVALID_TEMP_DATA);
        }
    }

    private TemporaryUserDataDTO convertToDTO(String uuid, Object data) {
        return switch (data) {
            case TemporaryUserDataDTO dto -> dto;
            case Map<?, ?> map -> {
                log.debug("UUID {}에 대해 LinkedHashMap 타입 데이터를 DTO로 변환 시도", uuid);
                yield convertMapToDTO(map);
            }
            default -> {
                log.warn("UUID {}에 대해 조회된 데이터가 예상 타입이 아님, 실제: {}", uuid, data.getClass().getSimpleName());
                throw new AuthCustomException(AuthErrorCode.INVALID_TEMP_DATA);
            }
        };
    }

    @SuppressWarnings("unchecked")
    private TemporaryUserDataDTO convertMapToDTO(Map<?, ?> map) {
        try {
            Token token = extractTokenFromMap((Map<String, Object>) map.get("token"));
            SocialLoginUserData socialData = extractSocialDataFromMap((Map<String, Object>) map.get("socialLoginUserData"));
            String fcmToken = (String) map.get("fcmToken");

            return new TemporaryUserDataDTO(socialData, token, fcmToken);
        } catch (Exception e) {
            log.error("LinkedHashMap -> TemporaryUserDataDTO 변환 실패: {}", e.getMessage(), e);
            throw new AuthCustomException(AuthErrorCode.INVALID_TEMP_DATA);
        }
    }

    private Token extractTokenFromMap(Map<String, Object> tokenMap) {
        return Token.createTemporaryToken(
                (String) tokenMap.get("accessToken"),
                (String) tokenMap.get("refreshToken")
        );
    }

    private SocialLoginUserData extractSocialDataFromMap(Map<String, Object> socialMap) {
        return new SocialLoginUserData(
                (String) socialMap.get("socialId"),
                (String) socialMap.get("email"),
                SocialProvider.valueOf((String) socialMap.get("provider")),
                (String) socialMap.get("nickname"),
                (String) socialMap.get("profileImageUrl"),
                (String) socialMap.get("fcmToken")
        );
    }
}
