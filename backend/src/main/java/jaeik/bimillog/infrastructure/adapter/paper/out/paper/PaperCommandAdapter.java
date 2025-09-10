package jaeik.bimillog.infrastructure.adapter.paper.out.paper;

import jaeik.bimillog.domain.paper.application.port.out.PaperCommandPort;
import jaeik.bimillog.domain.paper.application.service.PaperCommandService;
import jaeik.bimillog.domain.paper.entity.Message;
import jaeik.bimillog.infrastructure.adapter.paper.out.jpa.MessageRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * <h2>롤링페이퍼 명령 어댑터</h2>
 * <p>롤링페이퍼 도메인의 명령 작업을 담당하는 어댑터입니다.</p>
 * <p>메시지 저장, 메시지 삭제</p>
 *
 * @author Jaeik
 * @version 2.0.0
 */
@Component
@RequiredArgsConstructor
public class PaperCommandAdapter implements PaperCommandPort {

    private final MessageRepository messageRepository;

    /**
     * <h3>롤링페이퍼 메시지 저장</h3>
     * <p>새로운 메시지 엔티티를 데이터베이스에 저장합니다.</p>
     * <p>메시지 내용과 그리드 레이아웃 정보를 포함하여 저장하고 ID를 반환합니다.</p>
     * <p>{@link PaperCommandService#writeMessage}에서 호출됩니다.</p>
     *
     * @param message 저장할 메시지 엔티티 (내용, 위치, 크기, 장식 정보 포함)
     * @return Message 저장된 메시지 엔티티 (생성된 ID 포함)
     * @author Jaeik
     * @since 2.0.0
     */
    @Override
    public Message save(Message message) {
        return messageRepository.save(message);
    }

    /**
     * <h3>롤링페이퍼 메시지 삭제</h3>
     * <p>지정된 ID의 메시지를 데이터베이스에서 삭제합니다.</p>
     * <p>메시지 소유권 검증이 완료된 후 호출됩니다.</p>
     * <p>{@link PaperCommandService#deleteMessageInMyPaper}에서 호출됩니다.</p>
     *
     * @param messageId 삭제할 메시지의 고유 식별자
     * @author Jaeik
     * @since 2.0.0
     */
    @Override
    public void deleteById(Long messageId) {
        messageRepository.deleteById(messageId);
    }
}