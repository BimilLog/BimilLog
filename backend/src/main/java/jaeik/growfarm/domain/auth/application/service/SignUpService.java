package jaeik.growfarm.domain.auth.application.service;

import jaeik.growfarm.domain.auth.application.port.in.SignUpUseCase;
import jaeik.growfarm.domain.auth.application.port.out.SaveUserPort;
import jaeik.growfarm.domain.auth.application.port.out.TempDataPort;
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

    private final TempDataPort tempDataPort;
    private final SaveUserPort saveUserPort;

    /**
     * <h3>회원 가입 처리</h3>
     * <p>임시 UUID를 사용하여 새로운 사용자를 등록하고, FCM 토큰이 존재하면 이벤트를 발행합니다.</p>
     *
     * @param userName 사용자의 이름
     * @param uuid     임시 UUID
     * @return ResponseCookie 리스트
     * @throws CustomException userName이 null이거나 빈 문자열인 경우 (INVALID_INPUT_VALUE)
     * @throws CustomException uuid가 null이거나 빈 문자열인 경우 (INVALID_TEMP_UUID)  
     * @throws CustomException 임시 데이터가 존재하지 않는 경우 (INVALID_TEMP_DATA)
     * @since 2.0.0
     * @author Jaeik
     */
    @Override
    public List<ResponseCookie> signUp(String userName, String uuid) {
        // TODO: 테스트 실패 - 메인 로직 문제 의심
        // 기존 테스트에서 null/empty userName이 성공하는 비논리적 테스트 발견
        // 수정 필요: 입력 검증 로직 추가로 데이터 무결성 보장
        
        // 입력 검증: userName null/empty 체크
        if (userName == null || userName.trim().isEmpty()) {
            throw new CustomException(ErrorCode.INVALID_INPUT_VALUE);
        }
        
        // 입력 검증: uuid null/empty 체크
        if (uuid == null || uuid.trim().isEmpty()) {
            throw new CustomException(ErrorCode.INVALID_TEMP_UUID);
        }

        Optional<TemporaryUserDataDTO> tempUserData = tempDataPort.getTempData(uuid);

        if (tempUserData.isEmpty()) {
            throw new CustomException(ErrorCode.INVALID_TEMP_DATA);
        } else {
            return saveUserPort.saveNewUser(userName.trim(), uuid, tempUserData.get().toDomainProfile(), tempUserData.get().tokenVO, tempUserData.get().getFcmToken());
        }
    }
}
