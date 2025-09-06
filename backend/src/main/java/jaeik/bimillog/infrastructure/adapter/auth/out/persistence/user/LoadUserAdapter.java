package jaeik.bimillog.infrastructure.adapter.auth.out.persistence.user;

import jaeik.bimillog.domain.auth.application.port.out.LoadUserPort;
import jaeik.bimillog.domain.user.application.port.in.UserQueryUseCase;
import jaeik.bimillog.domain.user.entity.User;
import jaeik.bimillog.domain.user.exception.UserCustomException;
import jaeik.bimillog.domain.user.exception.UserErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * <h2>사용자 조회 어댑터</h2>
 * <p>Auth 도메인에서 User 도메인 정보 조회를 위한 아웃바운드 어댑터</p>
 * <p>헥사고날 아키텍처: CommentToUserPort(아웃바운드 포트) -> CommentToUserAdapter(아웃바운드 어댑터) -> UserQueryUseCase(User의 인바운드 포트)</p>
 *
 * @author Jaeik
 * @version 2.0.0
 * @since 2.0.0
 */
@Component
@RequiredArgsConstructor
public class LoadUserAdapter implements LoadUserPort {

    private final UserQueryUseCase userQueryUseCase;

    /**
     * {@inheritDoc}
     * 
     * <p>User 도메인의 예외(UserErrorCode.USER_NOT_FOUND)를 위임하여 Auth 서비스는 순수한 User 엔티티만 받음</p>
     */
    @Override
    public User findById(Long id) {
        return userQueryUseCase.findById(id)
                .orElseThrow(() -> new UserCustomException(UserErrorCode.USER_NOT_FOUND));
    }
}