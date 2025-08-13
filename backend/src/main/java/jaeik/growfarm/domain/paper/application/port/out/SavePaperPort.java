package jaeik.growfarm.domain.paper.application.port.out;

import jaeik.growfarm.domain.paper.entity.Message;

/**
 * <h2>롤링페이퍼 저장 Port</h2>
 * <p>롤링페이퍼 메시지 저장 기능을 위한 아웃바운드 포트</p>
 *
 * @author Jaeik
 * @version 2.0.0
 */
public interface SavePaperPort {
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