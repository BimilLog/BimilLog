package jaeik.bimillog.infrastructure.adapter.out.user;

import jaeik.bimillog.domain.user.application.port.out.DeleteUserPort;
import jaeik.bimillog.infrastructure.adapter.out.auth.jpa.TokenRepository;
import jaeik.bimillog.infrastructure.adapter.out.user.jpa.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * <h2>사용자 삭제 어댑터</h2>
 * <p>사용자 삭제 및 로그아웃 처리를 위한 영속성 어댑터</p>
 * <p>로그아웃 처리, 회원 탈퇴 처리</p>
 * <p>로그아웃 쿠키 생성, 다중 로그인 지원</p>
 *
 * @author Jaeik
 * @version 2.0.0
 */
@Component
@RequiredArgsConstructor
public class DeleteUserAdapter implements DeleteUserPort {

    private final TokenRepository tokenRepository;
    private final UserRepository userRepository;



    /**
     * <h3>회원 탈퇴 처리</h3>
     * <p>사용자를 탈퇴시키고, 소셜 로그아웃을 수행합니다.</p>
     * <p>{@link WithdrawService}에서 회원 탈퇴 처리 시 호출됩니다.</p>
     *
     * @param userId 사용자 ID
     * @since 2.0.0
     * @author Jaeik
     */
    @Override
    @Transactional
    public void performWithdrawProcess(Long userId) {
        tokenRepository.deleteAllByUserId(userId);
        userRepository.deleteById(userId);
    }
}
