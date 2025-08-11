package jaeik.growfarm.domain.admin.application.port.out;

import jaeik.growfarm.domain.user.domain.BlackList;

public interface SaveBlacklistPort {
    void save(BlackList blackList);
}
