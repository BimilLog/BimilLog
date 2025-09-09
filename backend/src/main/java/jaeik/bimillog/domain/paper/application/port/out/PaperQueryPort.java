package jaeik.bimillog.domain.paper.application.port.out;

import jaeik.bimillog.domain.paper.entity.Message;

import java.util.List;
import java.util.Optional;

/**
 * <h2>PaperQueryPort</h2>
 * <p>
 * 롤링페이퍼 데이터 조회 기능을 담당하는 아웃바운드 포트입니다.
 * 헥사고날 아키텍처에서 롤링페이퍼 읽기 관련 외부 의존성을 추상화하여 도메인 로직의 순수성을 보장합니다.
 * CQRS 패턴에 따른 조회 전용 포트로 읽기 작업에 특화되어 있습니다.
 * </p>
 * <p>
 * 이 포트는 다양한 롤링페이퍼 조회 기능을 제공합니다:
 * - 개별 메시지 조회: ID를 통한 특정 메시지 검색
 * - 사용자별 메시지 조회: 본인 롤링페이퍼 메시지 목록 조회
 * - 방문용 메시지 조회: 타인의 롤링페이퍼 방문 시 메시지 조회
 * - 소유자 정보 조회: 메시지 삭제 권한 검증을 위한 소유자 ID 조회
 * </p>
 * <p>
 * 비즈니스 컨텍스트에서 이 포트가 필요한 이유:
 * 1. 사용자 경험 제공 - 본인 롤링페이퍼와 타인 롤링페이퍼 조회 기능
 * 2. 권한 검증 지원 - 메시지 소유권 확인을 통한 삭제 권한 검증
 * 3. 성능 최적화 - 필요한 데이터만 조회하는 효율적인 쿼리 제공
 * 4. 도메인 경계 유지 - Paper 도메인이 데이터베이스 구현에 직접 의존하지 않음
 * </p>
 * <p>
 * PaperQueryService에서 롤링페이퍼 조회 API 제공 시 사용됩니다.
 * PaperCommandService에서 메시지 삭제 권한 검증 시 사용됩니다.
 * </p>
 *
 * @author Jaeik
 * @version 2.0.0
 */
public interface PaperQueryPort {

    /**
     * <h3>메시지 ID로 메시지 조회</h3>
     * <p>특정 ID에 해당하는 롤링페이퍼 메시지를 조회합니다.</p>
     * <p>메시지 존재성 확인과 상세 정보 제공을 위해 사용됩니다.</p>
     * <p>PaperCommandService에서 메시지 삭제 전 존재성 검증 시 호출됩니다.</p>
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
     * <p>PaperQueryService에서 내 롤링페이퍼 조회 API 제공 시 호출됩니다.</p>
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
     * <p>PaperQueryService에서 타인의 롤링페이퍼 방문 조회 API 제공 시 호출됩니다.</p>
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
     * <p>PaperCommandService에서 메시지 삭제 권한 검증 시 호출됩니다.</p>
     *
     * @param messageId 조회할 메시지의 ID
     * @return Optional<Long> 롤링페이퍼 소유자의 사용자 ID (메시지가 존재하지 않으면 Optional.empty())
     * @author Jaeik
     * @since 2.0.0
     */
    Optional<Long> findOwnerIdByMessageId(Long messageId);
}