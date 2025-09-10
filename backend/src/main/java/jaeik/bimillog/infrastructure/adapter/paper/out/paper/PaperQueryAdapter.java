package jaeik.bimillog.infrastructure.adapter.paper.out.paper;

import com.querydsl.jpa.impl.JPAQueryFactory;
import jaeik.bimillog.domain.paper.application.port.out.PaperQueryPort;
import jaeik.bimillog.domain.paper.application.service.PaperCommandService;
import jaeik.bimillog.domain.paper.application.service.PaperQueryService;
import jaeik.bimillog.domain.paper.entity.Message;
import jaeik.bimillog.domain.paper.entity.QMessage;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

/**
 * <h2>롤링페이퍼 조회 어댑터</h2>
 * <p>롤링페이퍼 도메인의 조회 작업을 담당하는 어댑터입니다.</p>
 * <p>메시지 ID로 조회, 사용자 ID로 조회, 사용자명으로 조회, 소유자 ID 조회</p>
 *
 * @author Jaeik
 * @version 2.0.0
 */
@Repository
@RequiredArgsConstructor
public class PaperQueryAdapter implements PaperQueryPort {

    private final JPAQueryFactory jpaQueryFactory;

    /**
     * <h3>메시지 ID로 메시지 조회</h3>
     * <p>특정 ID에 해당하는 메시지 엔티티를 QueryDSL을 통해 조회합니다.</p>
     * <p>메시지 존재 여부 확인과 권한 검증에 필요한 기본 정보를 제공하며, null 체크를 통해 안전한 조회를 보장합니다.</p>
     * <p>{@link PaperCommandService}에서 메시지 삭제 전 존재성 검증 시 호출됩니다.</p>
     *
     * @param messageId 조회할 메시지의 고유 식별자
     * @return Optional<Message> 조회된 메시지 엔티티 (존재하지 않으면 빈 Optional)
     * @author Jaeik
     * @since 2.0.0
     */
    @Override
    public Optional<Message> findMessageById(Long messageId) {
        if (messageId == null) {
            return Optional.empty();
        }
        
        QMessage message = QMessage.message;
        return Optional.ofNullable(
                jpaQueryFactory
                        .selectFrom(message)
                        .where(message.id.eq(messageId))
                        .fetchOne()
        );
    }

    /**
     * <h3>사용자 ID로 내 롤링페이퍼 메시지 목록 조회</h3>
     * <p>특정 사용자가 소유한 롤링페이퍼의 모든 메시지를 최신순으로 조회합니다.</p>
     * <p>기존 PaperReadRepositoryImpl.findMessageDTOsByUserId() 메서드의 QueryDSL 쿼리 구조를 완전히 보존하되,
     * Message 엔티티를 반환하여 서비스 계층에서 VO 변환을 담당하도록 개선했습니다.</p>
     * <p>{@link PaperQueryService}에서 내 롤링페이퍼 조회 시 호출되어 사용자의 모든 메시지를 최신 작성일 순으로 반환합니다.</p>
     *
     * @param userId 롤링페이퍼 소유자의 사용자 ID
     * @return List<Message> 해당 사용자의 메시지 목록 (최신순 정렬, 비어있을 수 있음)
     * @author Jaeik
     * @since 2.0.0
     */
    @Override
    public List<Message> findMessagesByUserId(Long userId) {
        if (userId == null) {
            return Collections.emptyList();
        }
        
        QMessage message = QMessage.message;

        return jpaQueryFactory
                .selectFrom(message)
                .where(message.user.id.eq(userId))
                .orderBy(message.createdAt.desc())
                .fetch();
    }

    /**
     * <h3>사용자명으로 방문 메시지 목록 조회</h3>
     * <p>특정 사용자명의 롤링페이퍼에 있는 모든 메시지를 조회합니다.</p>
     * <p>기존 PaperReadRepositoryImpl.findVisitMessageDTOsByUserName() 메서드의 QueryDSL 쿼리 구조와 JOIN 조건을 완전히 보존하되,
     * Message 엔티티를 반환하여 서비스 계층에서 VisitMessageDetail VO 변환을 담당하도록 개선했습니다.</p>
     * <p>{@link PaperQueryService}에서 롤링페이퍼 방문 시 호출되어 해당 사용자의 모든 메시지를 그리드 레이아웃 표시용으로 반환합니다.</p>
     *
     * @param userName 방문할 롤링페이퍼 소유자의 사용자명
     * @return List<Message> 해당 사용자의 메시지 목록 (비어있을 수 있음)
     * @author Jaeik
     * @since 2.0.0
     */
    @Override
    public List<Message> findMessagesByUserName(String userName) {
        if (userName == null || userName.trim().isEmpty()) {
            return Collections.emptyList();
        }
        
        QMessage message = QMessage.message;

        return jpaQueryFactory
                .selectFrom(message)
                .where(message.user.userName.eq(userName))
                .fetch();
    }

    /**
     * <h3>메시지 ID로 롤링페이퍼 소유자 ID 조회</h3>
     * <p>특정 메시지의 롤링페이퍼 소유자 ID만 효율적으로 조회합니다.</p>
     * <p>전체 Message 엔티티를 로드하지 않고 필요한 userId만 select하여 메모리 사용량과 네트워크 트래픽을 최적화합니다.</p>
     * <p>{@link PaperCommandService}에서 메시지 삭제 권한 검증 시 호출되어 해당 메시지의 소유자 ID를 반환합니다.</p>
     *
     * @param messageId 소유자를 확인할 메시지의 고유 식별자
     * @return Optional<Long> 롤링페이퍼 소유자의 사용자 ID (메시지가 존재하지 않으면 빈 Optional)
     * @author Jaeik
     * @since 2.0.0
     */
    @Override
    public Optional<Long> findOwnerIdByMessageId(Long messageId) {
        if (messageId == null) {
            return Optional.empty();
        }
        
        QMessage message = QMessage.message;
        
        Long ownerId = jpaQueryFactory
                .select(message.user.id)
                .from(message)
                .where(message.id.eq(messageId))
                .fetchOne();
                
        return Optional.ofNullable(ownerId);
    }
}