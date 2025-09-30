package jaeik.bimillog.infrastructure.adapter.out.member;

import jaeik.bimillog.domain.member.application.port.out.MemberCommandPort;
import jaeik.bimillog.domain.member.application.service.MemberCommandService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * <h2>사용자 명령 어댑터</h2>
 * <p>사용자 도메인의 명령 작업을 담당하는 아웃바운드 어댑터입니다.</p>
 * <p>UserCommandPort의 구현체로 사용자 삭제 등의 명령 작업을 처리합니다.</p>
 * <p>Native Query를 통한 효율적인 데이터 삭제</p>
 *
 * @author Jaeik
 * @version 2.0.0
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class MemberCommandAdapter implements MemberCommandPort {

    private final UserRepository userRepository;

    /**
     * <h3>사용자 계정과 설정 삭제</h3>
     * <p>Native Query를 사용하여 User와 Setting을 동시에 삭제합니다.</p>
     * <p>JOIN을 통한 원자적 삭제로 데이터 일관성을 보장합니다.</p>
     * <p>{@link MemberCommandService#removeUserAccount}에서 회원 탈퇴 처리 시 호출됩니다.</p>
     *
     * @param userId 삭제할 사용자 ID
     * @author Jaeik
     * @since 2.0.0
     */
    @Override
    @Transactional
    public void deleteUserAndSetting(Long userId) {
        log.debug("사용자 계정 삭제 수행 - userId: {}", userId);
        userRepository.deleteUserAndSettingByUserId(userId);
        log.debug("사용자 계정 삭제 완료 - userId: {}", userId);
    }
}