package jaeik.bimillog.infrastructure.adapter.out.auth;

import jaeik.bimillog.domain.auth.application.port.out.BlacklistPort;
import jaeik.bimillog.domain.auth.application.service.SocialLoginService;
import jaeik.bimillog.domain.auth.entity.BlackList;
import jaeik.bimillog.domain.member.entity.member.SocialProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class BlacklistAdapter implements BlacklistPort {

    private final BlackListRepository blackListRepository;

    /**
     * <h3>소셜 계정 블랙리스트 확인</h3>
     * <p>소셜 로그인 시 해당 소셜 계정이 블랙리스트에 등록되어 있는지 확인합니다.</p>
     * <p>회원 탈퇴나 계정 차단으로 인해 BlackList 테이블에 등록된 소셜 계정의 재가입을 방지합니다.</p>
     * <p>{@link SocialLoginService}에서 로그인 인증 단곀4에서 차단된 사용자의 접근을 막기 위해 호출됩니다.</p>
     *
     * @param provider 소셜 로그인 제공자 (KAKAO, NAVER 등)
     * @param socialId 소셜 로그인 사용자 식별자
     * @return 블랙리스트에 등록되어 있으면 true, 아니면 false
     * @author Jaeik
     * @since 2.0.0
     */
    @Override
    public boolean existsByProviderAndSocialId(SocialProvider provider, String socialId) {
        return blackListRepository.existsByProviderAndSocialId(provider, socialId);
    }

    /**
     * <h3>블랙리스트 저장</h3>
     * <p>블랙리스트에 사용자 정보를 저장합니다.</p>
     * <p>{@link WithdrawService}에서 회원 탈퇴 시 블랙리스트 등록을 위해 호출됩니다.</p>
     *
     * @param blackList 저장할 블랙리스트 엔티티
     * @author Jaeik
     * @since 2.0.0
     */
    @Override
    public void saveBlackList(BlackList blackList) {
        blackListRepository.save(blackList);
    }
}
