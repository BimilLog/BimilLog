package jaeik.bimillog.domain.paper.application.port.out;

import jaeik.bimillog.domain.paper.entity.Message;

import java.util.List;
import java.util.Optional;

/**
 * <h2>롤링페이퍼 조회 포트</h2>
 * <p>
 * 아웃바운드 포트: 롤링페이퍼 데이터 조회를 위한 포트
 * 기존 PaperReadRepository의 모든 기능을 포함
 * </p>
 *
 * @author Jaeik
 * @version 2.0.0
 */
public interface PaperQueryPort {

    /**
     * <h3>메시지 ID로 메시지 조회</h3>
     * <p>주어진 메시지 ID에 해당하는 메시지를 조회합니다.</p>
     *
     * @param messageId 조회할 메시지의 ID
     * @return 메시지 엔티티 (Optional)
     * @author Jaeik
     * @since 2.0.0
     */
    Optional<Message> findMessageById(Long messageId);

    /**
     * <h3>사용자 ID로 메시지 조회</h3>
     * <p>
     * 기존 PaperReadRepository.findMessageDTOsByUserId() 메서드와 동일한 기능
     * 도메인 엔티티를 반환하여 Service에서 VO로 변환
     * </p>
     *
     * @param userId 조회할 사용자 ID
     * @return 해당 사용자의 롤링페이퍼 메시지 목록 (최신순 정렬)
     * @author Jaeik
     * @since 2.0.0
     */
    List<Message> findMessagesByUserId(Long userId);

    /**
     * <h3>사용자명으로 방문용 메시지 조회</h3>
     * <p>
     * 기존 PaperReadRepository.findVisitMessageDTOsByUserName() 메서드와 동일한 기능
     * 도메인 엔티티를 반환하여 Service에서 VisitMessageDetail VO로 변환
     * </p>
     *
     * @param userName 조회할 사용자명
     * @return 방문용 메시지 목록
     * @author Jaeik
     * @since 2.0.0
     */
    List<Message> findMessagesByUserName(String userName);

    /**
     * <h3>메시지 소유자 ID 조회</h3>
     * <p>메시지 ID로 해당 메시지가 속한 롤링페이퍼 소유자의 ID를 조회합니다.</p>
     * <p>성능 최적화를 위해 전체 엔티티가 아닌 userId만 조회합니다.</p>
     *
     * @param messageId 조회할 메시지의 ID
     * @return 롤링페이퍼 소유자의 사용자 ID (Optional)
     * @author Jaeik
     * @since 2.0.0
     */
    Optional<Long> findOwnerIdByMessageId(Long messageId);
}