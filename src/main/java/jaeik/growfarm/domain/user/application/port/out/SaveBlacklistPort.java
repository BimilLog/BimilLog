package jaeik.growfarm.domain.user.application.port.out;

import jaeik.growfarm.domain.user.domain.BlackList;

public interface SaveBlacklistPort {
    void save(BlackList blackList);
}
