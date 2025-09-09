package jaeik.bimillog.domain.paper.application.port.out;

import jaeik.bimillog.domain.paper.entity.Message;

/**
 * <h2>PaperCommandPort</h2>
 * <p>
 * 롤링페이퍼 메시지 생성과 삭제를 담당하는 아웃바운드 포트입니다.
 * 헥사고날 아키텍처에서 롤링페이퍼 쓰기 관련 외부 의존성을 추상화하여 도메인 로직의 순수성을 보장합니다.
 * CQRS 패턴에 따른 명령 전용 포트로 쓰기 작업에 특화되어 있습니다.
 * </p>
 * <p>
 * 이 포트는 다음과 같은 롤링페이퍼 명령 기능을 제공합니다:
 * - 메시지 저장: 새로운 롤링페이퍼 메시지 생성
 * - 메시지 삭제: 기존 롤링페이퍼 메시지 삭제
 * </p>
 * <p>
 * 비즈니스 컨텍스트에서 이 포트가 필요한 이유:
 * 1. 사용자 경험 제공 - 익명 메시지 작성과 본인 롤링페이퍼 관리 기능
 * 2. 데이터 무결성 보장 - 메시지 생성과 삭제 시 트랜잭션 관리
 * 3. 도메인 경계 유지 - Paper 도메인이 데이터베이스 구현에 직접 의존하지 않음
 * </p>
 * <p>
 * PaperCommandService에서 새 메시지 저장과 기존 메시지 삭제 시 사용됩니다.
 * </p>
 *
 * @author Jaeik
 * @version 2.0.0
 */
public interface PaperCommandPort {
    /**
     * <h3>메시지 ID로 메시지 삭제</h3>
     * <p>특정 ID에 해당하는 롤링페이퍼 메시지를 삭제합니다.</p>
     * <p>사용자가 자신의 롤링페이퍼에서 불필요한 메시지를 제거할 때 사용됩니다.</p>
     * <p>PaperCommandService에서 메시지 소유권 검증 후 메시지 삭제 시 호출됩니다.</p>
     *
     * @param messageId 삭제할 메시지의 ID
     * @author Jaeik
     * @since 2.0.0
     */
    void deleteById(Long messageId);

    /**
     * <h3>메시지 저장</h3>
     * <p>새로운 롤링페이퍼 메시지를 데이터베이스에 저장합니다.</p>
     * <p>사용자가 다른 사용자의 롤링페이퍼에 익명 메시지를 작성할 때 사용됩니다.</p>
     * <p>PaperCommandService에서 메시지 생성과 이벤트 발행을 위해 호출됩니다.</p>
     *
     * @param message 저장할 메시지 엔티티
     * @return Message 저장된 메시지 엔티티 (ID가 할당됨)
     * @author Jaeik
     * @since 2.0.0
     */
    Message save(Message message);
}
