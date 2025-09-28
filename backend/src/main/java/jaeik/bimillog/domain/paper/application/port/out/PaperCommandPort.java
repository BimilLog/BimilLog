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
     * <h3>롤링페이퍼 메시지 삭제</h3>
     * <p>사용자의 롤링페이퍼 메시지를 삭제합니다.</p>
     * <p>messageId가 null인 경우: 해당 사용자의 모든 메시지를 삭제 (회원탈퇴 시)</p>
     * <p>messageId가 있는 경우: 특정 메시지를 삭제 (단건 삭제)</p>
     * <p>{@link PaperCommandService#deleteMessageInMyPaper}에서 호출됩니다.</p>
     * <p>- 단건 삭제 시: 소유권 검증 완료 후 호출</p>
     * <p>- 전체 삭제 시: 회원탈퇴 처리 중 호출</p>
     *
     * @param userId 사용자 ID
     * @param messageId 삭제할 메시지 ID (null인 경우 모든 메시지 삭제)
     * @author Jaeik
     * @since 2.0.0
     */
    void deleteMessage(Long userId, Long messageId);
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
