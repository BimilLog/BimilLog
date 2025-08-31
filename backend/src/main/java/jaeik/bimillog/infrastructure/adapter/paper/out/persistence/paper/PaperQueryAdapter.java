package jaeik.bimillog.infrastructure.adapter.paper.out.persistence.paper;

import com.querydsl.jpa.impl.JPAQueryFactory;
import jaeik.bimillog.domain.paper.application.port.out.PaperQueryPort;
import jaeik.bimillog.domain.paper.entity.Message;
import jaeik.bimillog.domain.paper.entity.QMessage;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

/**
 * <h2>롤링페이퍼 JPA 어댑터</h2>
 * <p>
 * Secondary Adapter: 롤링페이퍼 데이터 조회를 위한 JPA/QueryDSL 구현
 * 기존 PaperReadRepositoryImpl의 모든 로직을 완전히 보존하여 이전
 * </p>
 *
 * @author Jaeik
 * @version 2.0.0
 */
@Repository
@RequiredArgsConstructor
public class PaperQueryAdapter implements PaperQueryPort {

    private final JPAQueryFactory jpaQueryFactory;

    /**
     * {@inheritDoc}
     * 
     * <p>QueryDSL을 사용하여 메시지 ID로 메시지 엔티티를 조회합니다.</p>
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
     * {@inheritDoc}
     * 
     * <p>기존 PaperReadRepositoryImpl.findMessageDTOsByUserId() 메서드의 로직을 완전히 보존:</p>
     * <ul>
     *   <li>동일한 QueryDSL 쿼리 구조</li>
     *   <li>Message 엔티티 반환으로 변경하여 Service에서 VO 변환</li>
     *   <li>동일한 정렬 조건 (createdAt desc)</li>
     * </ul>
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
     * {@inheritDoc}
     * 
     * <p>기존 PaperReadRepositoryImpl.findVisitMessageDTOsByUserName() 메서드의 로직을 완전히 보존:</p>
     * <ul>
     *   <li>동일한 QueryDSL 쿼리 구조</li>
     *   <li>Message 엔티티 반환으로 변경하여 Service에서 VisitMessageDetail VO 변환</li>
     *   <li>동일한 JOIN 조건</li>
     * </ul>
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
     * {@inheritDoc}
     * 
     * <p>QueryDSL을 사용하여 메시지 ID로 롤링페이퍼 소유자 ID만 효율적으로 조회합니다.</p>
     * <p>전체 엔티티를 로드하지 않고 userId만 조회하여 성능을 최적화합니다.</p>
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