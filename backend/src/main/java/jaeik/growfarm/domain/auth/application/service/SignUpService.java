package jaeik.growfarm.domain.auth.application.service;

import jaeik.growfarm.domain.auth.application.port.in.SignUpUseCase;
import jaeik.growfarm.domain.auth.application.port.out.ManageAuthDataPort;
import jaeik.growfarm.domain.auth.application.port.out.ManageTemporaryDataPort;
import jaeik.growfarm.infrastructure.adapter.auth.out.social.dto.TemporaryUserDataDTO;
import jaeik.growfarm.infrastructure.exception.CustomException;
import jaeik.growfarm.infrastructure.exception.ErrorCode;
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

    private final ManageTemporaryDataPort manageTemporaryDataPort;
    private final ManageAuthDataPort manageAuthDataPort;

    /**
     * <h3>회원 가입 처리</h3>
     * <p>임시 UUID를 사용하여 새로운 사용자를 등록하고, FCM 토큰이 존재하면 이벤트를 발행합니다.</p>
     *
     * @param userName 사용자의 이름
     * @param uuid     임시 UUID
     * @return ResponseCookie 리스트
     * @since 2.0.0
     * @author Jaeik
     */
    @Override
    public List<ResponseCookie> signUp(String userName, String uuid) {
        Optional<TemporaryUserDataDTO> tempUserData = manageTemporaryDataPort.getTempData(uuid);

        if (tempUserData.isEmpty()) {
            throw new CustomException(ErrorCode.INVALID_TEMP_DATA);
        } else {
            return manageAuthDataPort.saveNewUser(userName, uuid, tempUserData.get().socialLoginUserData, tempUserData.get().tokenDTO, tempUserData.get().getFcmToken());
        }
    }
}
