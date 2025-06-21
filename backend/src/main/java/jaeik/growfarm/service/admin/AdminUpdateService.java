package jaeik.growfarm.service.admin;

import jaeik.growfarm.entity.user.BlackList;
import jaeik.growfarm.repository.notification.EmitterRepository;
import jaeik.growfarm.repository.user.BlackListRepository;
import jaeik.growfarm.service.auth.UserUpdateService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * <h2>관리자 업데이트 서비스 클래스</h2>
 * <p>관리자 관련 업데이트 작업을 처리하는 서비스 클래스</p>
 * @since 1.0.0
 * @author Jaeik
 */
@Service
@RequiredArgsConstructor
public class AdminUpdateService {

    private final BlackListRepository blackListRepository;
    private final EmitterRepository emitterRepository;
    private final UserUpdateService userUpdateService;

    @Transactional
    public void banUserProcess(Long userId, BlackList blackList) {
        blackListRepository.save(blackList);
        emitterRepository.deleteAllEmitterByUserId(userId);
        userUpdateService.performWithdrawProcess(userId);
    }
}
