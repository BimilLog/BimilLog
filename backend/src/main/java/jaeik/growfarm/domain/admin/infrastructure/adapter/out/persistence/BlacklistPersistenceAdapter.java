package jaeik.growfarm.domain.admin.infrastructure.adapter.out.persistence;

import jaeik.growfarm.domain.admin.application.port.out.SaveBlacklistPort;
import jaeik.growfarm.domain.user.domain.BlackList;
import jaeik.growfarm.domain.user.domain.SocialProvider;
import jaeik.growfarm.domain.user.infrastructure.adapter.out.persistence.BlackListRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class BlacklistPersistenceAdapter implements SaveBlacklistPort {

    private final BlackListRepository blackListRepository;

    @Override
    public void save(BlackList blackList) {
        blackListRepository.save(blackList);
    }
}
