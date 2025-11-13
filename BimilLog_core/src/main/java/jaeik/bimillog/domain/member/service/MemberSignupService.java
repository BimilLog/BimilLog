package jaeik.bimillog.domain.member.service;

import jaeik.bimillog.domain.auth.entity.AuthToken;
import jaeik.bimillog.domain.auth.entity.SocialToken;
import jaeik.bimillog.domain.auth.entity.SocialMemberProfile;
import jaeik.bimillog.domain.auth.exception.AuthCustomException;
import jaeik.bimillog.domain.auth.exception.AuthErrorCode;
import jaeik.bimillog.domain.global.entity.CustomUserDetails;
import jaeik.bimillog.domain.global.out.GlobalAuthTokenSaveAdapter;
import jaeik.bimillog.domain.global.out.GlobalCookieAdapter;
import jaeik.bimillog.domain.global.out.GlobalJwtAdapter;
import jaeik.bimillog.domain.global.out.GlobalSocialTokenCommandAdapter;
import jaeik.bimillog.domain.member.entity.Member;
import jaeik.bimillog.domain.member.entity.Setting;
import jaeik.bimillog.domain.member.exception.MemberCustomException;
import jaeik.bimillog.domain.member.exception.MemberErrorCode;
import jaeik.bimillog.domain.member.out.SaveMemberAdapter;
import jaeik.bimillog.infrastructure.redis.RedisMemberDataAdapter;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

/**
 * <h2>회원가입 서비스</h2>
 * <p>소셜 로그인 이후 Redis에 저장된 임시 프로필을 정식 회원으로 승격시키는 업무를 담당합니다.</p>
 * <p>임시 데이터 조회 → Member/Setting 생성 → SocialToken · AuthToken · FCM 저장 → JWT 쿠키 발급 흐름을 묶습니다.</p>
 *
 * @author Jaeik
 * @version 2.0.0
 */
@Service
@RequiredArgsConstructor
public class MemberSignupService {

    private final RedisMemberDataAdapter redisMemberDataPort;
    private final SaveMemberAdapter saveMemberPort;
    private final GlobalCookieAdapter globalCookieAdapter;
    private final GlobalJwtAdapter globalJwtAdapter;
    private final GlobalAuthTokenSaveAdapter globalAuthTokenSaveAdapter;
    private final GlobalSocialTokenCommandAdapter globalSocialTokenCommandAdapter;

    /**
     * <h3>신규 회원 가입 처리</h3>
     * <p>Redis에 저장된 소셜 프로필과 사용자가 입력한 표시 이름으로 정식 회원을 생성합니다.</p>
     * <p>소셜 토큰/회원/인증 토큰/FCM 토큰을 순차적으로 저장하고 최종 JWT 쿠키를 발급합니다.</p>
     * <p>Race Condition 방지를 위해 데이터베이스 UNIQUE 제약조건 위반 예외를 처리합니다.</p>
     *
     * @param memberName 사용자가 입력한 표시 이름
     * @param uuid Redis에 저장된 임시 프로필 키
     * @return JWT 액세스/리프레시 쿠키 목록
     * @throws AuthCustomException 임시 데이터가 만료되었거나 존재하지 않을 때
     * @throws MemberCustomException EXISTED_NICKNAME - 닉네임이 중복된 경우 (Race Condition 시)
     * @author Jaeik
     * @since 2.0.0
     */
    @Transactional
    public List<ResponseCookie> signup(String memberName, String uuid) {
        try {
            Optional<SocialMemberProfile> socialMemberProfile = redisMemberDataPort.getTempData(uuid);

            if (socialMemberProfile.isEmpty()) {
                throw new AuthCustomException(AuthErrorCode.INVALID_TEMP_DATA);
            }

            SocialMemberProfile memberProfile = socialMemberProfile.get();

            // 소셜 토큰 생성 및 영속화
            SocialToken initialSocialToken = SocialToken.createSocialToken(memberProfile.getAccessToken(), memberProfile.getRefreshToken());
            SocialToken persistedSocialToken = globalSocialTokenCommandAdapter.save(initialSocialToken);

            // 멤버 생성 및 저장 (Setting은 생성 Cascade로 영속화 필요 없음)
            Setting setting = Setting.createSetting();
            Member member = Member.createMember(
                    memberProfile.getSocialId(),
                    memberProfile.getProvider(),
                    memberProfile.getNickname(),
                    memberProfile.getProfileImageUrl(),
                    memberName,
                    setting,
                    persistedSocialToken);

            Member persistedMember = saveMemberPort.saveNewMember(member);

            // AuthToken 생성 및 저장
            AuthToken initialAuthToken = AuthToken.createToken("", persistedMember);
            AuthToken persistedAuthToken = globalAuthTokenSaveAdapter.save(initialAuthToken);

            // CustomUserDetails 생성
            CustomUserDetails userDetails = CustomUserDetails.ofExisting(persistedMember, persistedAuthToken.getId());

            // 액세스 토큰 및 리프레시 토큰 생성 및 업데이트
            String accessToken = globalJwtAdapter.generateAccessToken(userDetails);
            String refreshToken = globalJwtAdapter.generateRefreshToken(userDetails);
            globalAuthTokenSaveAdapter.updateJwtRefreshToken(persistedAuthToken.getId(), refreshToken);

            // 레디스 정보 삭제
            redisMemberDataPort.removeTempData(uuid);

            // JWT 쿠키 생성 및 반환
            return globalCookieAdapter.generateJwtCookie(accessToken, refreshToken);
        } catch (DataIntegrityViolationException e) {
            if (e.getMessage() != null && e.getMessage().contains("member_name")) {
                throw new MemberCustomException(MemberErrorCode.EXISTED_NICKNAME);
            }
            throw e;
        }
    }
}
