package jaeik.growfarm.domain.paper.application.port.out;

/**
 * <h2>롤링페이퍼 삭제 Port</h2>
 * <p>롤링페이퍼 메시지 삭제 기능을 위한 아웃바운드 포트</p>
 *
 * @author Jaeik
 * @version 2.0.0
 */
public interface DeletePaperPort {
    /**
     * <h3>메시지 ID로 메시지 삭제</h3>
     * <p>주어진 메시지 ID에 해당하는 롤링페이퍼 메시지를 삭제합니다.</p>
     *
     * @param messageId 삭제할 메시지의 ID
     * @author Jaeik
     * @since 2.0.0
     */
    void deleteById(Long messageId);
}
