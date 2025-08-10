package jaeik.growfarm.service.paper.write;

import jaeik.growfarm.dto.paper.MessageDTO;

public interface PaperWriteService {
    void writeMessage(String userName, MessageDTO messageDTO);
}
