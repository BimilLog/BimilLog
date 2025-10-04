package jaeik.bimillog.domain.auth.application.port.out;

import jaeik.bimillog.domain.auth.application.service.SocialLoginService;
import jaeik.bimillog.domain.auth.entity.BlackList;
import jaeik.bimillog.domain.member.entity.SocialProvider;

public interface BlacklistPort {

    /**
     * <h3>소셜 계정 차단 여부 확인</h3>
     * <p>특정 소셜 제공자와 소셜 ID에 해당하는 사용자가 블랙리스트에 등록되어 있는지 확인합니다.</p>
     * <p>회원 탈퇴하거나 계정 차단된 사용자의 소셜 계정 재가입을 방지하기 위해 사용됩니다.</p>
     * <p>{@link SocialLoginService}에서 로그인 시 차단된 사용자의 접근을 막기 위해 호출합니다.</p>
     *
     * @param provider 확인할 소셜 제공자 (KAKAO, GOOGLE 등)
     * @param socialId 확인할 소셜 플랫폼에서의 사용자 고유 ID
     * @return 해당 소셜 계정이 블랙리스트에 등록되어 있으면 true, 아니면 false
     * @author Jaeik
     * @since 2.0.0
     */
    boolean existsByProviderAndSocialId(SocialProvider provider, String socialId);

    /**
     * <h3>블랙리스트 저장</h3>
     * <p>블랙리스트에 사용자 정보를 저장합니다.</p>
     * <p>{@link }에서 회원 탈퇴 시 블랙리스트 등록을 위해 호출됩니다.</p>
     *
     * @param blackList 저장할 블랙리스트 엔티티
     * @author Jaeik
     * @since 2.0.0
     */
    void saveBlackList(BlackList blackList);
}
