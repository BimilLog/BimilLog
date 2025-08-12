package jaeik.growfarm.domain.paper.application.port.out;

import jaeik.growfarm.domain.paper.entity.Message;

public interface SavePaperPort {
    Message save(Message message);
}