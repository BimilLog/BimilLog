package jaeik.bimillog.domain.user.application.service;

import jaeik.bimillog.domain.auth.exception.AuthCustomException;
import jaeik.bimillog.domain.auth.exception.AuthErrorCode;
import jaeik.bimillog.domain.global.application.port.out.GlobalCookiePort;
import jaeik.bimillog.domain.global.application.port.out.GlobalJwtPort;
import jaeik.bimillog.domain.user.application.port.in.SignUpUseCase;
import jaeik.bimillog.domain.user.application.port.out.RedisUserDataPort;
import jaeik.bimillog.domain.user.application.port.out.SaveUserPort;
import jaeik.bimillog.domain.user.entity.ExistingUserDetail;
import jaeik.bimillog.domain.user.entity.TempUserData;
import jaeik.bimillog.infrastructure.adapter.in.user.web.UserCommandController;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

/**
 * <h2>회원가입 서비스</h2>
 * <p>소셜 로그인을 통한 신규 사용자의 회원가입을 처리하는 서비스입니다.</p>
 * <p>임시 데이터 조회, 사용자 계정 생성, 인증 쿠키 발급</p>
 *
 * @author Jaeik
 * @version 2.0.0
 */
@Service
@RequiredArgsConstructor
public class SignUpService implements SignUpUseCase {

    private final RedisUserDataPort redisUserDataPort;
    private final SaveUserPort saveUserPort;
    private final GlobalCookiePort globalCookiePort;
    private final GlobalJwtPort globalJwtPort;

    /**
     * <h3>신규 사용자 회원 가입 처리</h3>
     * <p>소셜 로그인 후 최초 회원 가입하는 사용자를 시스템에 등록합니다.</p>
     * <p>임시 UUID로 저장된 소셜 인증 정보를 조회하여 실제 사용자 계정을 생성합니다.</p>
     * <p>{@link UserCommandController}에서 POST /api/user/signup 요청 처리 시 호출됩니다.</p>
     *
     * @param userName 사용자가 입력한 표시 이름 (DTO에서 이미 검증됨)
     * @param uuid 임시 소셜 인증 데이터 저장용 UUID 키 (DTO에서 이미 검증됨)
     * @return ResponseCookie JWT 토큰이 설정된 쿠키 목록
     * @throws AuthCustomException 임시 데이터가 존재하지 않는 경우 (INVALID_TEMP_DATA)
     * @author Jaeik
     * @since 2.0.0
     */
    @Override
    public List<ResponseCookie> signUp(String userName, String uuid) {
        Optional<TempUserData> tempUserData = redisUserDataPort.getTempData(uuid);

        if (tempUserData.isEmpty()) {
            throw new AuthCustomException(AuthErrorCode.INVALID_TEMP_DATA);
        }

        TempUserData userData = tempUserData.get();
        ExistingUserDetail userDetail = (ExistingUserDetail) saveUserPort.saveNewUser(userName.trim(), userData.getSocialUserProfile(), userData.getFcmToken());
        String accessToken = globalJwtPort.generateAccessToken(userDetail);
        String refreshToken = globalJwtPort.generateRefreshToken(userDetail);
        redisUserDataPort.removeTempData(uuid);
        return globalCookiePort.generateJwtCookie(accessToken, refreshToken);
    }
}
