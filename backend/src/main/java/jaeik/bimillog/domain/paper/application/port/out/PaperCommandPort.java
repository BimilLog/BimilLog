package jaeik.bimillog.domain.paper.application.port.out;

import jaeik.bimillog.domain.paper.application.service.PaperCommandService;
import jaeik.bimillog.domain.paper.entity.Message;

/**
 * <h2>롤링페이퍼 명령 포트</h2>
 * <p>롤링페이퍼 도메인의 명령 작업을 담당하는 포트입니다.</p>
 * <p>메시지 저장, 메시지 삭제</p>
 *
 * @author Jaeik
 * @version 2.0.0
 */
public interface PaperCommandPort {
    /**
     * <h3>메시지 ID로 메시지 삭제</h3>
     * <p>특정 ID에 해당하는 롤링페이퍼 메시지를 삭제합니다.</p>
     * <p>롤링페이퍼 소유자가 자신의 메시지를 삭제할 때 사용됩니다.</p>
     * <p>{@link PaperCommandService#deleteMessageInMyPaper}에서 소유권 검증 후 호출됩니다.</p>
     *
     * @param messageId 삭제할 메시지의 ID
     * @author Jaeik
     * @since 2.0.0
     */
    void deleteById(Long messageId);

    /**
     * <h3>메시지 저장</h3>
     * <p>새로운 롤링페이퍼 메시지를 저장합니다.</p>
     * <p>익명 메시지 작성 시 사용됩니다.</p>
     * <p>{@link PaperCommandService#writeMessage}에서 메시지 생성 후 호출됩니다.</p>
     *
     * @param message 저장할 메시지 엔티티
     * @return Message 저장된 메시지 엔티티 (ID가 할당됨)
     * @author Jaeik
     * @since 2.0.0
     */
    Message save(Message message);
}
