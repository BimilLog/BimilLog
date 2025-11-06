package jaeik.bimillog.out.paper;

import jaeik.bimillog.domain.paper.application.port.out.PaperCommandPort;
import jaeik.bimillog.domain.paper.application.service.PaperCommandService;
import jaeik.bimillog.domain.paper.entity.Message;
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
     * @param message 저장할 메시지 엔티티
     * @return Message 저장된 메시지 엔티티
     * @author Jaeik
     * @since 2.0.0
     */
    @Override
    public Message save(Message message) {
        return messageRepository.save(message);
    }

    /**
     * <h3>롤링페이퍼 메시지 삭제</h3>
     * <p>사용자의 롤링페이퍼 메시지를 데이터베이스에서 삭제합니다.</p>
     * <p>messageId가 null인 경우: 해당 사용자의 모든 메시지를 일괄 삭제 (회원탈퇴 시)</p>
     * <p>messageId가 있는 경우: 특정 메시지를 삭제 (단건 삭제)</p>
     * <p>{@link PaperCommandService#deleteMessageInMyPaper}에서 호출됩니다.</p>
     *
     * @param memberId 사용자 ID (전체 삭제 시 사용)
     * @param messageId 삭제할 메시지 ID (null인 경우 memberId로 전체 삭제)
     * @author Jaeik
     * @since 2.0.0
     */
    @Override
    public void deleteMessage(Long memberId, Long messageId) {
        if (messageId != null) {
            messageRepository.deleteById(messageId);
        } else {
            messageRepository.deleteAllByMember_Id(memberId);
        }
    }
}