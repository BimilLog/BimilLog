package jaeik.growfarm.domain.paper.application.service;

import jaeik.growfarm.domain.paper.application.port.in.PaperQueryUseCase;
import jaeik.growfarm.domain.paper.application.port.out.LoadPaperPort;
import jaeik.growfarm.domain.paper.domain.Message;
import jaeik.growfarm.dto.paper.MessageDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@Transactional
@RequiredArgsConstructor
public class PaperService implements PaperQueryUseCase {

    private final LoadPaperPort loadPaperPort;

    @Override
    @Transactional(readOnly = true)
    public List<MessageDTO> findMessagesByUserId(Long userId) {
        return loadPaperPort.findMessageDTOsByUserId(userId);
    }

    @Override
    public Optional<Message> findMessageById(Long messageId) {
        return loadPaperPort.findMessageById(messageId);
    }
}
