package jaeik.bimillog.domain.member.application.service;

import jaeik.bimillog.domain.auth.application.port.out.AuthTokenPort;
import jaeik.bimillog.domain.auth.entity.SocialUserProfile;
import jaeik.bimillog.domain.auth.exception.AuthCustomException;
import jaeik.bimillog.domain.auth.exception.AuthErrorCode;
import jaeik.bimillog.domain.global.application.port.out.GlobalCookiePort;
import jaeik.bimillog.domain.global.application.port.out.GlobalJwtPort;
import jaeik.bimillog.domain.member.application.port.in.SignUpUseCase;
import jaeik.bimillog.domain.member.application.port.out.RedisMemberDataPort;
import jaeik.bimillog.domain.member.application.port.out.SaveMemberPort;
import jaeik.bimillog.domain.member.entity.memberdetail.ExistingMemberDetail;
import jaeik.bimillog.infrastructure.adapter.in.member.web.MemberCommandController;
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

    private final RedisMemberDataPort redisMemberDataPort;
    private final SaveMemberPort saveMemberPort;
    private final GlobalCookiePort globalCookiePort;
    private final GlobalJwtPort globalJwtPort;
    private final AuthTokenPort authTokenPort;

    /**
     * <h3>신규 사용자 회원 가입 처리</h3>
     * <p>소셜 로그인 후 최초 회원 가입하는 사용자를 시스템에 등록합니다.</p>
     * <p>임시 UUID로 저장된 소셜 인증 정보를 조회하여 실제 사용자 계정을 생성합니다.</p>
     * <p>{@link MemberCommandController}에서 POST /api/member/signup 요청 처리 시 호출됩니다.</p>
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
        Optional<SocialUserProfile> socialUserProfile = redisMemberDataPort.getTempData(uuid);

        if (socialUserProfile.isEmpty()) {
            throw new AuthCustomException(AuthErrorCode.INVALID_TEMP_DATA);
        }

        SocialUserProfile userProfile = socialUserProfile.get();
        ExistingMemberDetail userDetail = (ExistingMemberDetail) saveMemberPort.saveNewUser(userName.trim(), userProfile);
        String accessToken = globalJwtPort.generateAccessToken(userDetail);
        String refreshToken = globalJwtPort.generateRefreshToken(userDetail);

        // DB에 JWT 리프레시 토큰 저장 (보안 강화 - SocialLoginService와 동일)
        authTokenPort.updateJwtRefreshToken(userDetail.getTokenId(), refreshToken);

        redisMemberDataPort.removeTempData(uuid);
        return globalCookiePort.generateJwtCookie(accessToken, refreshToken);
    }
}
