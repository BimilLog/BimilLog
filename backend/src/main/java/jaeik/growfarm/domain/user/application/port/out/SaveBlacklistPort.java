package jaeik.growfarm.domain.user.application.port.out;

import jaeik.growfarm.domain.user.entity.BlackList;

public interface SaveBlacklistPort {
    void save(BlackList blackList);
}
