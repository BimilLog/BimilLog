package jaeik.growfarm.domain.paper.application.port.out;

import jaeik.growfarm.entity.message.Message;

/**
 * <h2>롤링페이퍼 저장 포트</h2>
 * <p>
 * Secondary Port: 롤링페이퍼 데이터 저장/삭제를 위한 포트
 * 기존 PaperCommandRepository의 모든 기능을 포함
 * </p>
 *
 * @author Jaeik
 * @version 2.0.0 (헥사고날 아키텍처 적용)
 */
public interface SavePaperPort {

    /**
     * <h3>메시지 저장</h3>
     * <p>
     * 기존 PaperCommandRepository.save() 메서드와 동일한 기능
     * JPA를 통한 메시지 엔티티 저장
     * </p>
     *
     * @param message 저장할 메시지 도메인 객체
     * @return 저장된 메시지 (ID 포함)
     * @author Jaeik
     * @since 2.0.0
     */
    Message save(Message message);

    /**
     * <h3>메시지 삭제</h3>
     * <p>
     * 기존 PaperCommandRepository.deleteById() 메서드와 동일한 기능
     * </p>
     *
     * @param messageId 삭제할 메시지 ID
     * @author Jaeik
     * @since 2.0.0
     */
    void deleteById(Long messageId);
}