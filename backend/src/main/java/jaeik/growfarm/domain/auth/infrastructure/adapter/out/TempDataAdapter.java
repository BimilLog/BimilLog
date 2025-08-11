package jaeik.growfarm.domain.auth.infrastructure.adapter.out;

import jaeik.growfarm.domain.auth.application.port.out.ManageTemporaryDataPort;
import jaeik.growfarm.dto.auth.SocialLoginUserData;
import jaeik.growfarm.dto.user.TokenDTO;
import jaeik.growfarm.service.auth.TempUserDataManager;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * <h2>임시 데이터 어댑터</h2>
 * <p>신규 사용자의 임시 데이터 관리를 위한 어댑터</p>
 *
 * @author Jaeik
 * @version 2.0.0
 */
@Component
@RequiredArgsConstructor
public class TempDataAdapter implements ManageTemporaryDataPort {

    private final TempUserDataManager tempUserDataManager;

    @Override
    public String saveTempData(SocialLoginUserData userData, TokenDTO tokenDTO, String fcmToken) {
        return tempUserDataManager.saveTempData(userData, tokenDTO, fcmToken);
    }

    @Override
    public TempUserDataManager.TempUserData getTempData(String uuid) {
        return tempUserDataManager.getTempData(uuid);
    }
}