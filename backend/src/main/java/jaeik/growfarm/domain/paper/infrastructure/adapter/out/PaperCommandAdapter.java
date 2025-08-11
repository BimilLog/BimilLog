package jaeik.growfarm.domain.paper.infrastructure.adapter.out;

import jaeik.growfarm.domain.paper.domain.Message;
import jaeik.growfarm.domain.paper.application.port.out.DeletePaperPort;
import jaeik.growfarm.domain.paper.application.port.out.SavePaperPort;
import jaeik.growfarm.repository.paper.MessageRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

/**
 * <h2>롤링페이퍼 명령 어댑터</h2>
 * <p>
 * Secondary Adapter: 롤링페이퍼 데이터 저장/삭제를 위한 JPA 구현
 * MessageRepository의 기능을 위임하여 사용
 * </p>
 *
 * @author Jaeik
 * @version 2.1.0
 */
@Repository
@RequiredArgsConstructor
public class PaperCommandAdapter implements SavePaperPort, DeletePaperPort {

    private final MessageRepository messageRepository;

    @Override
    public Message save(Message message) {
        return messageRepository.save(message);
    }

    @Override
    public void deleteById(Long messageId) {
        messageRepository.deleteById(messageId);
    }
}