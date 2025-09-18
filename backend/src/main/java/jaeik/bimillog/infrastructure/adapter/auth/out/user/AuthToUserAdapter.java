package jaeik.bimillog.infrastructure.adapter.auth.out.user;

import jaeik.bimillog.domain.auth.application.port.out.AuthToUserPort;
import jaeik.bimillog.domain.auth.application.service.SocialService;
import jaeik.bimillog.domain.user.application.port.in.UserQueryUseCase;
import jaeik.bimillog.domain.user.entity.SocialProvider;
import jaeik.bimillog.domain.user.entity.User;
import jaeik.bimillog.infrastructure.adapter.user.out.jpa.BlackListRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

/**
 * <h2>소셜 사용자 관리 어댑터</h2>
 * <p>소셜 로그인 사용자의 정보 조회 및 관리를 담당하는 어댑터입니다.</p>
 * <p>기존 사용자 확인, 프로필 정보 동기화 등 사용자 관리 로직 처리</p>
 *
 * @author Jaeik
 * @version 2.0.0
 */
@Component
@RequiredArgsConstructor
public class AuthToUserAdapter implements AuthToUserPort {

    private final UserQueryUseCase userQueryUseCase;
    private final BlackListRepository blackListRepository;

    /**
     * <h3>기존 소셜 사용자 조회</h3>
     * <p>소셜 제공자와 소셜 ID를 기반으로 기존 사용자를 조회합니다.</p>
     * <p>사용자가 존재하지 않는 경우 빈 Optional을 반환합니다.</p>
     * <p>{@link SocialService}에서 기존 회원 확인 시 호출됩니다.</p>
     *
     * @param provider 소셜 로그인 제공자 (예: KAKAO 등)
     * @param socialId 소셜 플랫폼에서의 사용자 고유 ID
     * @return Optional로 감싼 기존 사용자 정보
     * @author Jaeik
     * @since 2.0.0
     */
    @Override
    @Transactional(readOnly = true)
    public Optional<User> findExistingUser(SocialProvider provider, String socialId) {
        return userQueryUseCase.findByProviderAndSocialId(provider, socialId);
    }


    /**
     * <h3>소셜 계정 영구 차단 여부 확인</h3>
     * <p>소셜 로그인 시 해당 소셜 계정이 영구 차단된 사용자인지 JPA로 확인합니다.</p>
     * <p>소셜 로그인 인증 단계에서 차단된 사용자의 로그인 시도를 방지하기 위해 소셜 로그인 검증 플로우에서 호출합니다.</p>
     * <p>회원 탈퇴나 계정 정지로 인해 BlackList 테이블에 등록된 소셜 계정의 재가입 방지를 위해 사용됩니다.</p>
     *
     * @param provider 소셜 로그인 제공자 (KAKAO, NAVER 등)
     * @param socialId 소셜 로그인 사용자 식별자
     * @return 블랙리스트에 등록된 경우 true, 아니면 false
     * @author Jaeik
     * @since 2.0.0
     */
    @Override
    public boolean existsByProviderAndSocialId(SocialProvider provider, String socialId) {
        return blackListRepository.existsByProviderAndSocialId(provider, socialId);
    }

}