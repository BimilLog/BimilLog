package jaeik.bimillog.domain.auth.application.service;

import jaeik.bimillog.domain.auth.application.port.in.SignUpUseCase;
import jaeik.bimillog.domain.auth.application.port.out.RedisUserDataPort;
import jaeik.bimillog.domain.auth.application.port.out.SaveUserPort;
import jaeik.bimillog.domain.auth.entity.LoginResult;
import jaeik.bimillog.domain.auth.exception.AuthCustomException;
import jaeik.bimillog.domain.auth.exception.AuthErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

/**
 * <h2>회원가입 서비스</h2>
 * <p>소셜 로그인을 통한 신규 사용자의 회원가입을 처리하는 서비스입니다.</p>
 * <p>임시 데이터 검증, 사용자 계정 생성, 인증 쿠키 발급</p>
 *
 * @author Jaeik
 * @version 2.0.0
 */
@Service
@RequiredArgsConstructor
public class SignUpService implements SignUpUseCase {

    private final RedisUserDataPort redisUserDataPort;
    private final SaveUserPort saveUserPort;

    /**
     * <h3>신규 사용자 회원 가입 처리</h3>
     * <p>소셜 로그인 후 최초 회원 가입하는 사용자를 시스템에 등록합니다.</p>
     * <p>임시 UUID로 저장된 소셜 인증 정보를 조회하여 실제 사용자 계정을 생성합니다.</p>
     * <p>소셜 로그인 성공 후 사용자 이름 입력 완료 시 호출됩니다.</p>
     *
     * @param userName 사용자가 입력한 표시 이름
     * @param uuid 임시 소셜 인증 데이터 저장용 UUID 키
     * @return ResponseCookie JWT 토큰이 설정된 쿠키 목록
     * @throws AuthCustomException userName이 null이거나 빈 문자열인 경우 (INVALID_INPUT_VALUE)
     * @throws AuthCustomException uuid가 null이거나 빈 문자열인 경우 (INVALID_TEMP_UUID)  
     * @throws AuthCustomException 임시 데이터가 존재하지 않는 경우 (INVALID_TEMP_DATA)
     * @author Jaeik
     * @since 2.0.0
     */
    @Override
    public List<ResponseCookie> signUp(String userName, String uuid) {
        validateSignUpInput(userName, uuid);

        Optional<LoginResult.TempUserData> tempUserData = redisUserDataPort.getTempData(uuid);

        if (tempUserData.isEmpty()) {
            throw new AuthCustomException(AuthErrorCode.INVALID_TEMP_DATA);
        }
        
        LoginResult.TempUserData userData = tempUserData.get();
        return saveUserPort.saveNewUser(userName.trim(), uuid, userData.userProfile(), userData.token(), userData.fcmToken());
    }

    /**
     * <h3>회원가입 입력값 유효성 검증</h3>
     * <p>사용자 이름과 임시 UUID의 유효성을 검증합니다.</p>
     * <p>null 값이나 공백 문자열을 사전에 차단하여 비즈니스 로직의 안정성을 보장합니다.</p>
     * <p>{@link #signUp} 메서드에서 실제 처리 전 입력값 검증을 위해 호출됩니다.</p>
     *
     * @param userName 검증할 사용자 이름
     * @param uuid 검증할 임시 UUID
     * @throws AuthCustomException 입력값이 유효하지 않은 경우
     * @author Jaeik
     * @since 2.0.0
     */
    private void validateSignUpInput(String userName, String uuid) {
        if (isNullOrEmpty(userName)) {
            throw new AuthCustomException(AuthErrorCode.INVALID_INPUT_VALUE);
        }
        
        if (isNullOrEmpty(uuid)) {
            throw new AuthCustomException(AuthErrorCode.INVALID_TEMP_UUID);
        }
    }

    /**
     * <h3>문자열 null/공백 검증</h3>
     * <p>문자열이 null이거나 공백 문자열인지 검증하는 유틸리티 메서드입니다.</p>
     * <p>trim()을 통해 앞뒤 공백을 제거한 후 빈 문자열 여부를 판단합니다.</p>
     * <p>{@link #validateSignUpInput} 메서드에서 입력값 유효성 검증 시 호출됩니다.</p>
     *
     * @param value 검증할 문자열
     * @return null이거나 공백 문자열이면 true, 유효한 문자열이면 false
     * @author Jaeik
     * @since 2.0.0
     */
    private boolean isNullOrEmpty(String value) {
        return value == null || value.trim().isEmpty();
    }
}
