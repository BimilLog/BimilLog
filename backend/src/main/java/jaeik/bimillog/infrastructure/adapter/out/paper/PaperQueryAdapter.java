package jaeik.bimillog.infrastructure.adapter.out.paper;

import com.querydsl.jpa.impl.JPAQueryFactory;
import jaeik.bimillog.domain.paper.application.port.out.PaperQueryPort;
import jaeik.bimillog.domain.paper.application.service.PaperCommandService;
import jaeik.bimillog.domain.paper.application.service.PaperQueryService;
import jaeik.bimillog.domain.paper.entity.Message;
import jaeik.bimillog.domain.paper.entity.QMessage;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * <h2>롤링페이퍼 조회 어댑터</h2>
 * <p>롤링페이퍼 도메인의 조회 작업을 담당하는 어댑터입니다.</p>
 * <p>사용자 ID로 조회, 사용자명으로 조회, 소유자 ID 조회</p>
 *
 * @author Jaeik
 * @version 2.0.0
 */
@Repository
@RequiredArgsConstructor
public class PaperQueryAdapter implements PaperQueryPort {

    private final JPAQueryFactory jpaQueryFactory;

    /**
     * <h3>사용자 ID로 내 롤링페이퍼 메시지 목록 조회</h3>
     * <p>특정 사용자가 소유한 롤링페이퍼의 모든 메시지를 최신순으로 조회합니다.</p>
     * <p>{@link PaperQueryService#getMyPaper}에서 호출됩니다.</p>
     *
     * @param memberId 롤링페이퍼 소유자의 사용자 ID
     * @return List<Message> 해당 사용자의 메시지 목록
     * @author Jaeik
     * @since 2.0.0
     */
    @Override
    public List<Message> findMessagesByUserId(Long memberId) {
        QMessage message = QMessage.message;

        return jpaQueryFactory
                .selectFrom(message)
                .where(message.member.id.eq(memberId))
                .orderBy(message.createdAt.desc())
                .fetch();
    }

    /**
     * <h3>사용자명으로 방문 메시지 목록 조회</h3>
     * <p>특정 사용자명의 롤링페이퍼에 있는 모든 메시지를 조회합니다.</p>
     * <p>{@link PaperQueryService#visitPaper}에서 호출됩니다.</p>
     *
     * @param memberName 방문할 롤링페이퍼 소유자의 사용자명
     * @return List<Message> 해당 사용자의 메시지 목록
     * @author Jaeik
     * @since 2.0.0
     */
    @Override
    public List<Message> findMessagesByMemberName(String memberName) {
        QMessage message = QMessage.message;

        return jpaQueryFactory
                .selectFrom(message)
                .where(message.member.memberName.eq(memberName))
                .fetch();
    }

    /**
     * <h3>메시지 ID로 롤링페이퍼 소유자 ID 조회</h3>
     * <p>특정 메시지의 롤링페이퍼 소유자 ID만 조회합니다.</p>
     * <p>필요한 memberId만 select하여 효율적입니다.</p>
     * <p>{@link PaperCommandService#deleteMessageInMyPaper}에서 권한 검증 시 호출됩니다.</p>
     *
     * @param messageId 소유자를 확인할 메시지의 ID
     * @return Optional<Long> 롤링페이퍼 소유자의 사용자 ID
     * @author Jaeik
     * @since 2.0.0
     */
    @Override
    public Optional<Long> findOwnerIdByMessageId(Long messageId) {
        QMessage message = QMessage.message;
        
        Long ownerId = jpaQueryFactory
                .select(message.member.id)
                .from(message)
                .where(message.id.eq(messageId))
                .fetchOne();
                
        return Optional.ofNullable(ownerId);
    }
}