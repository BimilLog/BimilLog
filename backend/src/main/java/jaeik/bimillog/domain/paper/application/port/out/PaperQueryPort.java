package jaeik.bimillog.domain.paper.application.port.out;

import jaeik.bimillog.domain.paper.application.service.PaperCommandService;
import jaeik.bimillog.domain.paper.application.service.PaperQueryService;
import jaeik.bimillog.domain.paper.entity.Message;

import java.util.List;
import java.util.Optional;

/**
 * <h2>롤링페이퍼 조회 포트</h2>
 * <p>롤링페이퍼 도메인의 조회 작업을 담당하는 포트입니다.</p>
 * <p>메시지 ID 조회, 사용자별 메시지 조회, 방문용 메시지 조회, 소유자 ID 조회</p>
 *
 * @author Jaeik
 * @version 2.0.0
 */
public interface PaperQueryPort {

    /**
     * <h3>메시지 ID로 메시지 조회</h3>
     * <p>특정 ID에 해당하는 롤링페이퍼 메시지를 조회합니다.</p>
     * <p>메시지 존재성 확인과 상세 정보 제공을 위해 사용됩니다.</p>
     * <p>{@link PaperCommandService}에서 메시지 삭제 전 존재성 검증 시 호출됩니다.</p>
     *
     * @param messageId 조회할 메시지의 ID
     * @return Optional<Message> 조회된 메시지 엔티티 (존재하지 않으면 Optional.empty())
     * @author Jaeik
     * @since 2.0.0
     */
    Optional<Message> findMessageById(Long messageId);

    /**
     * <h3>사용자 ID로 메시지 조회</h3>
     * <p>특정 사용자의 롤링페이퍼에 작성된 모든 메시지를 조회합니다.</p>
     * <p>사용자가 자신의 롤링페이퍼를 확인할 때 받은 메시지들을 볼 수 있도록 제공됩니다.</p>
     * <p>{@link PaperQueryService}에서 내 롤링페이퍼 조회 API 제공 시 호출됩니다.</p>
     *
     * @param userId 조회할 사용자 ID
     * @return List<Message> 해당 사용자의 롤링페이퍼 메시지 목록 (최신순 정렬)
     * @author Jaeik
     * @since 2.0.0
     */
    List<Message> findMessagesByUserId(Long userId);

    /**
     * <h3>사용자명으로 방문용 메시지 조회</h3>
     * <p>특정 사용자명의 롤링페이퍼에 작성된 모든 메시지를 방문자용으로 조회합니다.</p>
     * <p>다른 사용자가 해당 사용자의 롤링페이퍼를 방문하여 메시지를 확인할 때 사용됩니다.</p>
     * <p>{@link PaperQueryService}에서 타인의 롤링페이퍼 방문 조회 API 제공 시 호출됩니다.</p>
     *
     * @param userName 조회할 사용자명
     * @return List<Message> 방문용 메시지 목록
     * @author Jaeik
     * @since 2.0.0
     */
    List<Message> findMessagesByUserName(String userName);

    /**
     * <h3>메시지 소유자 ID 조회</h3>
     * <p>특정 메시지가 속한 롤링페이퍼의 소유자 ID를 조회합니다.</p>
     * <p>메시지 삭제 권한을 검증할 때 현재 사용자가 해당 롤링페이퍼의 소유자인지 확인하는 데 사용됩니다.</p>
     * <p>성능 최적화를 위해 전체 엔티티가 아닌 userId만 조회합니다.</p>
     * <p>{@link PaperCommandService}에서 메시지 삭제 권한 검증 시 호출됩니다.</p>
     *
     * @param messageId 조회할 메시지의 ID
     * @return Optional<Long> 롤링페이퍼 소유자의 사용자 ID (메시지가 존재하지 않으면 Optional.empty())
     * @author Jaeik
     * @since 2.0.0
     */
    Optional<Long> findOwnerIdByMessageId(Long messageId);
}