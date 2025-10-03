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
import jaeik.bimillog.domain.member.application.port.in.MemberSignupUseCase;
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
 * <p>소셜 로그인 이후 Redis에 저장된 임시 프로필을 정식 회원으로 승격시키는 업무를 담당합니다.</p>
 * <p>임시 데이터 조회 → Member/Setting 생성 → KakaoToken · AuthToken · FCM 저장 → JWT 쿠키 발급 흐름을 묶습니다.</p>
 *
 * @author Jaeik
 * @version 2.0.0
 */
@Service
@RequiredArgsConstructor
public class MemberSignupService implements MemberSignupUseCase {

    private final RedisMemberDataPort redisMemberDataPort;
    private final SaveMemberPort saveMemberPort;
    private final GlobalCookiePort globalCookiePort;
    private final GlobalJwtPort globalJwtPort;
    private final GlobalAuthTokenSavePort globalAuthTokenSavePort;
    private final GlobalKakaoTokenCommandPort globalKakaoTokenCommandPort;
    private final GlobalFcmSaveUseCase globalFcmSaveUseCase;

    /**
     * <h3>신규 회원 가입 처리</h3>
     * <p>Redis에 저장된 소셜 프로필과 사용자가 입력한 표시 이름으로 정식 회원을 생성합니다.</p>
     * <p>카카오 토큰/회원/인증 토큰/FCM 토큰을 순차적으로 저장하고 최종 JWT 쿠키를 발급합니다.</p>
     *
     * @param memberName 사용자가 입력한 표시 이름
     * @param uuid Redis에 저장된 임시 프로필 키
     * @return JWT 액세스/리프레시 쿠키 목록
     * @throws AuthCustomException 임시 데이터가 만료되었거나 존재하지 않을 때
     * @author Jaeik
     * @since 2.0.0
     */
    @Override
    @Transactional
    public List<ResponseCookie> signup(String memberName, String uuid) {
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
