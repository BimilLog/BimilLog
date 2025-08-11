package jaeik.growfarm.domain.paper.application.port.in;

import jaeik.growfarm.domain.paper.domain.Message;
import jaeik.growfarm.dto.paper.MessageDTO;

import java.util.List;
import java.util.Optional;

public interface PaperQueryUseCase {
    List<MessageDTO> findMessagesByUserId(Long userId);
    Optional<Message> findMessageById(Long messageId);
}
