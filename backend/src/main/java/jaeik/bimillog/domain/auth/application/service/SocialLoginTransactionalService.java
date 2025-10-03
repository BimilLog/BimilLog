package jaeik.bimillog.domain.auth.application.service;

import jaeik.bimillog.domain.auth.application.port.out.AuthToMemberPort;
import jaeik.bimillog.domain.auth.application.port.out.BlacklistPort;
import jaeik.bimillog.domain.auth.entity.AuthToken;
import jaeik.bimillog.domain.auth.entity.KakaoToken;
import jaeik.bimillog.domain.auth.entity.LoginResult;
import jaeik.bimillog.domain.auth.entity.SocialMemberProfile;
import jaeik.bimillog.domain.auth.exception.AuthCustomException;
import jaeik.bimillog.domain.auth.exception.AuthErrorCode;
import jaeik.bimillog.domain.global.application.port.in.GlobalFcmSaveUseCase;
import jaeik.bimillog.domain.global.application.port.out.GlobalAuthTokenSavePort;
import jaeik.bimillog.domain.global.application.port.out.GlobalCookiePort;
import jaeik.bimillog.domain.global.application.port.out.GlobalJwtPort;
import jaeik.bimillog.domain.global.application.port.out.GlobalKakaoTokenCommandPort;
import jaeik.bimillog.domain.global.entity.MemberDetail;
import jaeik.bimillog.domain.member.entity.member.Member;
import jaeik.bimillog.domain.member.entity.member.SocialProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * <h2>소셜 로그인 트랜잭션 서비스</h2>
 * <p>소셜 로그인 흐름에서 데이터베이스 트랜잭션이 필요한 작업을 처리합니다.</p>
 * <p>기존 회원은 프로필·토큰을 갱신하고 JWT 쿠키를 발급하며, 신규 회원은 Redis에 임시 정보를 저장합니다.</p>
 * <p>{@link SocialLoginService}로부터 소셜 인증 결과를 받아 트랜잭션 내에서 최종 로그인을 완료합니다.</p>
 *
 * @author Jaeik
 * @version 2.0.0
 */
@Service
@RequiredArgsConstructor
public class SocialLoginTransactionalService {

    private final AuthToMemberPort authToMemberPort;
    private final BlacklistPort blacklistPort;
    private final GlobalCookiePort globalCookiePort;
    private final GlobalJwtPort globalJwtPort;
    private final GlobalAuthTokenSavePort globalAuthTokenSavePort;
    private final GlobalKakaoTokenCommandPort globalKakaoTokenCommandPort;
    private final GlobalFcmSaveUseCase globalFcmSaveUseCase;

    /**
     * <h3>소셜 로그인 최종 처리</h3>
     * <p>블랙리스트 검증 후 기존 회원/신규 회원 분기 처리를 수행합니다.</p>
     * <p>기존 회원은 {@link #handleExistingMember}로, 신규 회원은 {@link #handleNewMember}로 위임합니다.</p>
     *
     * @param provider             소셜 플랫폼 제공자 (KAKAO 등)
     * @param socialMemberProfile  소셜 플랫폼에서 받은 사용자 프로필
     * @return 기존 회원은 {@link LoginResult.ExistingUser}, 신규 회원은 {@link LoginResult.NewUser}
     * @author Jaeik
     * @since 2.0.0
     */
    @Transactional
    public LoginResult finishLogin(SocialProvider provider, SocialMemberProfile socialMemberProfile) {

        // 블랙리스트 사용자 확인
        if (blacklistPort.existsByProviderAndSocialId(provider, socialMemberProfile.getSocialId())) {
            throw new AuthCustomException(AuthErrorCode.BLACKLIST_USER);
        }

        // 기존 유저 유무 조회
        Optional<Member> member = authToMemberPort.checkMember(provider, socialMemberProfile.getSocialId());
        return member.map(value ->
                handleExistingMember(value, socialMemberProfile))
                .orElseGet(() -> handleNewMember(socialMemberProfile));
    }

    /**
     * <h3>기존 회원 로그인 처리</h3>
     * <p>기존 회원의 로그인 요청을 처리하며, 다음 작업을 순차적으로 수행합니다:</p>
     * <ol>
     *   <li>카카오 토큰 저장</li>
     *   <li>회원 프로필 업데이트 (닉네임, 프로필 이미지)</li>
     *   <li>AuthToken 생성 및 저장</li>
     *   <li>FCM 토큰 등록</li>
     *   <li>MemberDetail 생성</li>
     *   <li>JWT Access/Refresh 토큰 생성</li>
     *   <li>JWT 쿠키 생성 및 반환</li>
     * </ol>
     *
     * @param existingMember       DB에 존재하는 기존 회원 엔티티
     * @param socialMemberProfile  소셜 플랫폼에서 받은 사용자 프로필
     * @return JWT 쿠키를 포함한 {@link LoginResult.ExistingUser}
     * @author Jaeik
     * @since 2.0.0
     */
    private LoginResult handleExistingMember(Member existingMember, SocialMemberProfile socialMemberProfile) {
        String kakaoAccessToken = socialMemberProfile.getKakaoAccessToken();
        String kakaoRefreshToken = socialMemberProfile.getKakaoRefreshToken();
        String nickname = socialMemberProfile.getNickname();
        String profileImageUrl = socialMemberProfile.getProfileImageUrl();
        String fcmToken = socialMemberProfile.getFcmToken();

        // 1. 카카오 토큰 생성
        KakaoToken initialKakaoToken = KakaoToken.createKakaoToken(kakaoAccessToken, kakaoRefreshToken);
        KakaoToken persistedKakaoToken = globalKakaoTokenCommandPort.save(initialKakaoToken);

        // 2. 멤버 정보 업데이트
        Member updateMember = authToMemberPort.handleExistingMember(existingMember, nickname, profileImageUrl, persistedKakaoToken);

        // 3. AuthToken 생성
        AuthToken initialAuthToken = AuthToken.createToken("", updateMember);
        AuthToken persistedAuthToken = globalAuthTokenSavePort.save(initialAuthToken);

        // 4. FCM 토큰 생성
        Long fcmTokenId = globalFcmSaveUseCase.registerFcmToken(updateMember, fcmToken);

        // 5. MemberDetail 생성
        MemberDetail memberDetail = MemberDetail.ofExisting(updateMember, persistedAuthToken.getId(), fcmTokenId);

        // 6. 액세스 토큰 및 리프레시 토큰 생성 및 업데이트
        String accessToken = globalJwtPort.generateAccessToken(memberDetail);
        String refreshToken = globalJwtPort.generateRefreshToken(memberDetail);
        globalAuthTokenSavePort.updateJwtRefreshToken(persistedAuthToken.getId(), refreshToken);

        // 7. JWT 쿠키 생성 및 반환
        List<ResponseCookie> cookies = globalCookiePort.generateJwtCookie(accessToken, refreshToken);
        return new LoginResult.ExistingUser(cookies);
    }

    /**
     * <h3>신규 회원 로그인 처리</h3>
     * <p>신규 회원의 소셜 프로필을 Redis에 임시 저장하고 회원가입 페이지로 넘길 임시 쿠키를 발급합니다.</p>
     * <p>UUID 기반 임시 쿠키를 통해 회원가입 페이지에서 소셜 프로필 정보를 조회할 수 있습니다.</p>
     *
     * @param socialMemberProfile  소셜 플랫폼에서 받은 사용자 프로필
     * @return 임시 쿠키를 포함한 {@link LoginResult.NewUser}
     * @author Jaeik
     * @since 2.0.0
     */
    private LoginResult handleNewMember(SocialMemberProfile socialMemberProfile) {
        String uuid = UUID.randomUUID().toString();
        authToMemberPort.handleNewUser(socialMemberProfile, uuid);
        ResponseCookie tempCookie = globalCookiePort.createTempCookie(uuid);
        return new LoginResult.NewUser(tempCookie);
    }
}
