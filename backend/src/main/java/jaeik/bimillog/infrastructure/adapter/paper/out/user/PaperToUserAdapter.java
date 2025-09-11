package jaeik.bimillog.infrastructure.adapter.paper.out.user;

import jaeik.bimillog.domain.paper.application.port.out.PaperToUserPort;
import jaeik.bimillog.domain.paper.application.service.PaperCommandService;
import jaeik.bimillog.domain.paper.application.service.PaperQueryService;
import jaeik.bimillog.domain.user.application.port.in.UserQueryUseCase;
import jaeik.bimillog.domain.user.entity.User;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Optional;

/**
 * <h2>Paper에서 User 도메인 접근 어댑터</h2>
 * <p>Paper 도메인에서 User 도메인의 데이터에 접근하기 위한 어댑터입니다.</p>
 * <p>사용자 조회, 사용자 존재성 검증</p>
 *
 * @author Jaeik
 * @version 2.0.0
 */
@Component
@RequiredArgsConstructor
public class PaperToUserAdapter implements PaperToUserPort {

    private final UserQueryUseCase userQueryUseCase;

    /**
     * <h3>사용자명으로 User 조회</h3>
     * <p>특정 사용자명에 해당하는 User 엔티티를 조회합니다.</p>
     * <p>User 도메인의 UserQueryUseCase를 통해 사용자 정보를 조회합니다.</p>
     * <p>{@link PaperCommandService#writeMessage}에서 사용자 검증 시 호출됩니다.</p>
     *
     * @param userName 조회할 사용자의 사용자명
     * @return Optional<User> 조회된 User 엔티티
     * @author Jaeik
     * @since 2.0.0
     */
    @Override
    public Optional<User> findByUserName(String userName) {
        return userQueryUseCase.findByUserName(userName);
    }

    /**
     * <h3>사용자명 존재 여부 확인</h3>
     * <p>특정 사용자명을 가진 사용자가 시스템에 존재하는지 확인합니다.</p>
     * <p>User 도메인의 UserQueryUseCase를 통해 존재 여부만 확인합니다.</p>
     * <p>{@link PaperQueryService#visitPaper}에서 사용자 검증 시 호출됩니다.</p>
     *
     * @param userName 존재 여부를 확인할 사용자명
     * @return boolean 사용자 존재 여부
     * @author Jaeik
     * @since 2.0.0
     */
    @Override
    public boolean existsByUserName(String userName) {
        return userQueryUseCase.existsByUserName(userName);
    }
}