package jaeik.bimillog.domain.auth.service;

import jaeik.bimillog.domain.auth.adapter.AuthToJwtAdapter;
import jaeik.bimillog.domain.auth.adapter.AuthToMemberAdapter;
import jaeik.bimillog.domain.auth.entity.AuthToken;
import jaeik.bimillog.domain.auth.dto.LoginResultDTO;
import jaeik.bimillog.domain.auth.entity.SocialMemberProfile;
import jaeik.bimillog.domain.auth.entity.SocialToken;
import jaeik.bimillog.domain.auth.repository.*;
import jaeik.bimillog.domain.global.entity.CustomUserDetails;
import jaeik.bimillog.domain.member.entity.Member;
import jaeik.bimillog.domain.member.entity.SocialProvider;
import jaeik.bimillog.infrastructure.exception.CustomException;
import jaeik.bimillog.infrastructure.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

/**
 * <h2>소셜 로그인 트랜잭션 서비스</h2>
 * <p>소셜 로그인 흐름에서 데이터베이스 트랜잭션이 필요한 작업을 처리합니다.</p>
 * <p>기존 회원은 프로필·토큰을 갱신하고 JWT 토큰을 생성하며, 신규 회원은 임시 이름으로 즉시 가입 처리 후 JWT 토큰을 생성합니다.</p>
 * <p>{@link SocialLoginService}로부터 소셜 인증 결과를 받아 트랜잭션 내에서 최종 로그인을 완료합니다.</p>
 *
 * @author Jaeik
 * @version 2.0.0
 */
@Service
@RequiredArgsConstructor
public class SocialLoginTransactionalService {
    private final AuthToMemberAdapter authToMemberAdapter;
    private final BlackListRepository blackListRepository;
    private final AuthToJwtAdapter authToJwtAdapter;
    private final AuthTokenRepository authTokenRepository;
    private final SocialTokenRepository socialTokenRepository;

    /**
     * <h3>소셜 로그인 최종 처리</h3>
     * <p>블랙리스트 검증 후 기존 회원/신규 회원 분기 처리를 수행합니다.</p>
     * <p>기존 회원은 {@link #handleExistingMember}로, 신규 회원은 {@link #handleNewMember}로 위임합니다.</p>
     *
     * @param provider             소셜 플랫폼 제공자 (KAKAO 등)
     * @param socialMemberProfile  소셜 플랫폼에서 받은 사용자 프로필
     */
    @Transactional
    public LoginResultDTO finishLogin(SocialProvider provider, SocialMemberProfile socialMemberProfile) {

        // 블랙리스트 사용자 확인
        if (blackListRepository.existsByProviderAndSocialId(provider, socialMemberProfile.getSocialId())) {
            throw new CustomException(ErrorCode.AUTH_BLACKLIST_USER);
        }

        // 기존 유저 유무 조회
        Optional<Member> member = authToMemberAdapter.findByProviderAndSocialId(provider, socialMemberProfile.getSocialId());
        return member.map(value ->
                handleExistingMember(value, socialMemberProfile))
                .orElseGet(() -> handleNewMember(socialMemberProfile));
    }

    /**
     * <h3>기존 회원 로그인 처리</h3>
     * <p>기존 회원의 로그인 요청을 처리하며, 다음 작업을 순차적으로 수행합니다:</p>
     * <ol>
     *   <li>소셜 토큰 업데이트 또는 생성</li>
     *   <li>회원 프로필 업데이트 (닉네임, 프로필 이미지)</li>
     *   <li>AuthToken 생성 및 저장</li>
     *   <li>FCM 토큰 등록</li>
     *   <li>MemberDetail 생성</li>
     *   <li>JWT Access/Refresh 토큰 생성</li>
     *   <li>JWT 토큰 값 반환</li>
     * </ol>
     *
     * @param existingMember       DB에 존재하는 기존 회원 엔티티
     * @param socialMemberProfile  소셜 플랫폼에서 받은 사용자 프로필
     */
    private LoginResultDTO handleExistingMember(Member existingMember, SocialMemberProfile socialMemberProfile) {
        String accessToken = socialMemberProfile.getAccessToken();
        String refreshToken = socialMemberProfile.getRefreshToken();
        String nickname = socialMemberProfile.getNickname();
        String profileImageUrl = socialMemberProfile.getProfileImageUrl();

        // 1. 소셜 토큰 업데이트 또는 생성
        SocialToken socialToken = existingMember.getSocialToken();
        if (socialToken == null) {
            // 소셜 토큰이 없으면 새로 생성 (이전 버전에서 로그아웃으로 삭제된 경우)
            socialToken = SocialToken.createSocialToken(accessToken, refreshToken);
            socialToken = socialTokenRepository.save(socialToken);
        } else {
            // 소셜 토큰이 있으면 업데이트
            socialToken.updateTokens(accessToken, refreshToken);
        }

        // 2. 멤버 정보 업데이트
        Member updateMember = authToMemberAdapter.handleExistingMember(existingMember, nickname, profileImageUrl, socialToken);

        List<String> tokens = loginProcess(updateMember);
        return LoginResultDTO.createLoginResult(tokens.getFirst(), tokens.get(1));
    }

    /**
     * <h3>신규 회원 가입 처리</h3>
     * <p>임시 이름(냥_XXXXXX)을 자동 생성하여 즉시 회원가입 후 JWT 토큰을 발급합니다.</p>
     *
     * @param socialMemberProfile  소셜 플랫폼에서 받은 사용자 프로필
     */
    private LoginResultDTO handleNewMember(SocialMemberProfile socialMemberProfile) {
        SocialToken initialSocialToken = SocialToken.createSocialToken(
                socialMemberProfile.getAccessToken(),
                socialMemberProfile.getRefreshToken()
        );

        SocialToken persistedSocialToken = socialTokenRepository.save(initialSocialToken);
        Member persistedMember = authToMemberAdapter.handleNewMember(socialMemberProfile, persistedSocialToken);

        List<String> tokens = loginProcess(persistedMember);

        return LoginResultDTO.createLoginResult(tokens.getFirst(), tokens.get(1));
    }

    private List<String> loginProcess(Member persistedMember) {
        // AuthToken 생성
        AuthToken initialAuthToken = AuthToken.createToken("", persistedMember);
        AuthToken persistedAuthToken = authTokenRepository.save(initialAuthToken);

        // CustomUserDetails 생성
        CustomUserDetails userDetails = CustomUserDetails.ofExisting(persistedMember, persistedAuthToken.getId());

        // 액세스 토큰 및 리프레시 토큰 생성 및 업데이트
        String jwtAccessToken = authToJwtAdapter.generateAccessToken(userDetails);
        String jwtRefreshToken = authToJwtAdapter.generateRefreshToken(userDetails);
        persistedAuthToken.updateJwtRefreshToken(jwtRefreshToken);
        return List.of(jwtAccessToken, jwtRefreshToken);
    }
}
