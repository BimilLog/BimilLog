package jaeik.bimillog.domain.member.application.service;

import jaeik.bimillog.domain.auth.entity.AuthToken;
import jaeik.bimillog.domain.auth.entity.KakaoToken;
import jaeik.bimillog.domain.auth.entity.SocialMemberProfile;
import jaeik.bimillog.domain.auth.exception.AuthCustomException;
import jaeik.bimillog.domain.auth.exception.AuthErrorCode;
import jaeik.bimillog.domain.global.application.port.in.GlobalFcmSaveUseCase;
import jaeik.bimillog.domain.global.application.port.out.GlobalAuthTokenSavePort;
import jaeik.bimillog.domain.global.application.port.out.GlobalCookiePort;
import jaeik.bimillog.domain.global.application.port.out.GlobalJwtPort;
import jaeik.bimillog.domain.global.application.port.out.GlobalKakaoTokenCommandPort;
import jaeik.bimillog.domain.global.entity.MemberDetail;
import jaeik.bimillog.domain.member.application.port.in.SignUpUseCase;
import jaeik.bimillog.domain.member.application.port.out.RedisMemberDataPort;
import jaeik.bimillog.domain.member.application.port.out.SaveMemberPort;
import jaeik.bimillog.domain.member.entity.Setting;
import jaeik.bimillog.domain.member.entity.member.Member;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
    private final GlobalAuthTokenSavePort globalAuthTokenSavePort;
    private final GlobalKakaoTokenCommandPort globalKakaoTokenCommandPort;
    private final GlobalFcmSaveUseCase globalFcmSaveUseCase;


    @Override
    @Transactional
    public List<ResponseCookie> signUp(String memberName, String uuid) {
        Optional<SocialMemberProfile> socialMemberProfile = redisMemberDataPort.getTempData(uuid);

        if (socialMemberProfile.isEmpty()) {
            throw new AuthCustomException(AuthErrorCode.INVALID_TEMP_DATA);
        }

        SocialMemberProfile memberProfile = socialMemberProfile.get();

        // 카카오 토큰 생성 및 영속화
        KakaoToken initialKakaoToken = KakaoToken.createKakaoToken(memberProfile.getKakaoAccessToken(), memberProfile.getKakaoRefreshToken());
        KakaoToken persistedKakaoToken = globalKakaoTokenCommandPort.save(initialKakaoToken);

        // 멤버 생성 및 저장 (Setting은 생성 Cascade로 영속화 필요 없음)
        Setting setting = Setting.createSetting();
        Member member = Member.createMember(
                memberProfile.getSocialId(),
                memberProfile.getProvider(),
                memberProfile.getNickname(),
                memberProfile.getProfileImageUrl(),
                memberName,
                setting,
                persistedKakaoToken);

        Member persistedMember = saveMemberPort.saveNewMember(member);

        // AuthToken 생성 및 저장
        AuthToken initialAuthToken = AuthToken.createToken("", persistedMember);
        AuthToken persistedAuthToken = globalAuthTokenSavePort.save(initialAuthToken);

        // FCM 토큰 생성 및 저장
        Long fcmTokenId = globalFcmSaveUseCase.registerFcmToken(persistedMember, memberProfile.getFcmToken());

        // MemberDetail 생성
        MemberDetail memberDetail = MemberDetail.ofExisting(persistedMember, persistedAuthToken.getId(), fcmTokenId);

        // 액세스 토큰 및 리프레시 토큰 생성 및 업데이트
        String accessToken = globalJwtPort.generateAccessToken(memberDetail);
        String refreshToken = globalJwtPort.generateRefreshToken(memberDetail);
        globalAuthTokenSavePort.updateJwtRefreshToken(persistedAuthToken.getId(), refreshToken);

        // 레디스 정보 삭제
        redisMemberDataPort.removeTempData(uuid);

        // JWT 쿠키 생성 및 반환
        return globalCookiePort.generateJwtCookie(accessToken, refreshToken);
    }
}
