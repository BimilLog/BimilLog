package jaeik.growfarm.domain.paper.application.port.out;

import jaeik.growfarm.domain.paper.entity.Message;

/**
 * <h2>롤링페이퍼 명령 Port</h2>
 * <p>
 * Secondary Port: 롤링페이퍼 메시지 생성/삭제를 위한 아웃바운드 포트
 * 기존 MessageRepository의 기능을 추상화
 * </p>
 *
 * @author Jaeik
 * @version 2.0.0
 */
public interface PaperCommandPort {
    /**
     * <h3>메시지 ID로 메시지 삭제</h3>
     * <p>주어진 메시지 ID에 해당하는 롤링페이퍼 메시지를 삭제합니다.</p>
     *
     * @param messageId 삭제할 메시지의 ID
     * @author Jaeik
     * @since 2.0.0
     */
    void deleteById(Long messageId);

    /**
     * <h3>메시지 저장</h3>
     * <p>새로운 롤링페이퍼 메시지를 저장합니다.</p>
     *
     * @param message 저장할 메시지 엔티티
     * @return 저장된 메시지 엔티티
     * @author Jaeik
     * @since 2.0.0
     */
    Message save(Message message);
}
