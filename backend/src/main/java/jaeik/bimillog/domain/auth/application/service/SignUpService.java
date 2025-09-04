package jaeik.bimillog.domain.auth.application.service;

import jaeik.bimillog.domain.auth.application.port.in.SignUpUseCase;
import jaeik.bimillog.domain.auth.application.port.out.SaveUserPort;
import jaeik.bimillog.domain.auth.application.port.out.RedisUserDataPort;
import jaeik.bimillog.domain.auth.entity.TempUserData;
import jaeik.bimillog.domain.auth.exception.AuthCustomException;
import jaeik.bimillog.domain.auth.exception.AuthErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

/**
 * <h2>회원 가입 서비스</h2>
 * <p>회원 가입 관련 기능을 처리하는 전용 서비스 클래스</p>
 *
 * @author Jaeik
 * @version 2.0.0
 * @since 2.0.0
 */
@Service
@RequiredArgsConstructor
public class SignUpService implements SignUpUseCase {

    private final RedisUserDataPort redisUserDataPort;
    private final SaveUserPort saveUserPort;

    /**
     * <h3>회원 가입 처리</h3>
     * <p>임시 UUID를 사용하여 새로운 사용자를 등록하고, FCM 토큰이 존재하면 이벤트를 발행합니다.</p>
     *
     * @param userName 사용자의 이름
     * @param uuid     임시 UUID
     * @return ResponseCookie 리스트
     * @throws AuthCustomException userName이 null이거나 빈 문자열인 경우 (INVALID_INPUT_VALUE)
     * @throws AuthCustomException uuid가 null이거나 빈 문자열인 경우 (INVALID_TEMP_UUID)  
     * @throws AuthCustomException 임시 데이터가 존재하지 않는 경우 (INVALID_TEMP_DATA)
     * @since 2.0.0
     * @author Jaeik
     */
    @Override
    public List<ResponseCookie> signUp(String userName, String uuid) {
        validateSignUpInput(userName, uuid);

        Optional<TempUserData> tempUserData = redisUserDataPort.getTempData(uuid);

        if (tempUserData.isEmpty()) {
            throw new AuthCustomException(AuthErrorCode.INVALID_TEMP_DATA);
        }
        
        TempUserData userData = tempUserData.get();
        return saveUserPort.saveNewUser(userName.trim(), uuid, userData.userProfile(), userData.tokenVO(), userData.fcmToken());
    }

    /**
     * <h3>회원가입 입력값 검증</h3>
     * <p>userName과 uuid의 유효성을 검증합니다.</p>
     *
     * @param userName 사용자 이름
     * @param uuid     임시 UUID
     * @throws AuthCustomException 입력값이 유효하지 않은 경우
     * @since 2.0.0
     * @author Jaeik
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
     * <h3>문자열 null/empty 검증</h3>
     * <p>문자열이 null이거나 공백인지 검증합니다.</p>
     *
     * @param value 검증할 문자열
     * @return null이거나 공백이면 true, 그렇지 않으면 false
     * @since 2.0.0
     * @author Jaeik
     */
    private boolean isNullOrEmpty(String value) {
        return value == null || value.trim().isEmpty();
    }
}
