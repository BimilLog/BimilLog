package jaeik.growfarm.service.paper;

import jaeik.growfarm.dto.paper.MessageDTO;
import jaeik.growfarm.entity.message.Message;
import jaeik.growfarm.repository.message.MessageRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * <h2> 페이퍼 업데이트 서비스</h2>
 * <p>
 * 메시지의 DB작업을 처리하는 서비스
 * </p>
 *
 *
 * @author Jaeik
 * @since 1.0.0
 */
@Service
@RequiredArgsConstructor
public class PaperUpdateService {

    private final MessageRepository messageRepository;

    /**
     * <h3>메시지 저장</h3>
     * <p>
     * 메시지를 데이터베이스에 저장합니다.
     * </p>
     *
     * @param message 저장할 메시지 객체
     * @author Jaeik
     * @since 1.0.0
     */
    @Transactional
    public void saveMessage(Message message) {
        messageRepository.save(message);
    }

    /**
     * <h3>메시지 삭제</h3>
     * <p>
     * 메시지를 데이터베이스에서 삭제합니다.
     * </p>
     *
     * @param messageDTO 삭제할 메시지 DTO
     * @author Jaeik
     * @since 1.0.0
     */
    @Transactional
    public void deleteMessage(MessageDTO messageDTO) {
        messageRepository.deleteById(messageDTO.getId());
    }
}
