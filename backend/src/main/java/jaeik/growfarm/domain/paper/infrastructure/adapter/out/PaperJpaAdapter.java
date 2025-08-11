package jaeik.growfarm.domain.paper.infrastructure.adapter.out;

import com.querydsl.core.types.ExpressionUtils;
import com.querydsl.core.types.Projections;
import com.querydsl.jpa.impl.JPAQueryFactory;
import jaeik.growfarm.domain.paper.application.port.out.LoadPaperPort;
import jaeik.growfarm.dto.paper.MessageDTO;
import jaeik.growfarm.dto.paper.VisitMessageDTO;
import jaeik.growfarm.entity.message.QMessage;
import jaeik.growfarm.entity.user.QUsers;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * <h2>롤링페이퍼 JPA 어댑터</h2>
 * <p>
 * Secondary Adapter: 롤링페이퍼 데이터 조회를 위한 JPA/QueryDSL 구현
 * 기존 PaperReadRepositoryImpl의 모든 로직을 완전히 보존하여 이전
 * </p>
 *
 * @author Jaeik
 * @version 2.0.0 (헥사고날 아키텍처 적용)
 */
@Repository
@RequiredArgsConstructor
public class PaperJpaAdapter implements LoadPaperPort {

    private final JPAQueryFactory jpaQueryFactory;

    /**
     * {@inheritDoc}
     * 
     * <p>기존 PaperReadRepositoryImpl.findMessageDTOsByUserId() 메서드의 로직을 완전히 보존:</p>
     * <ul>
     *   <li>동일한 QueryDSL 쿼리 구조</li>
     *   <li>동일한 Projections.fields 매핑</li>
     *   <li>동일한 정렬 조건 (createdAt desc)</li>
     *   <li>동일한 필드 별칭 (userId)</li>
     * </ul>
     */
    @Override
    public List<MessageDTO> findMessageDTOsByUserId(Long userId) {
        QMessage message = QMessage.message;

        return jpaQueryFactory
                .select(Projections.fields(MessageDTO.class,
                        message.id,
                        message.users.id.as("userId"), // 실제 userId
                        message.decoType,
                        message.anonymity,
                        message.content,
                        message.width,
                        message.height,
                        message.createdAt
                ))
                .from(message)
                .where(message.users.id.eq(userId))
                .orderBy(message.createdAt.desc())
                .fetch();
    }

    /**
     * {@inheritDoc}
     * 
     * <p>기존 PaperReadRepositoryImpl.findVisitMessageDTOsByUserName() 메서드의 로직을 완전히 보존:</p>
     * <ul>
     *   <li>동일한 QueryDSL 쿼리 구조</li>
     *   <li>동일한 Projections.fields 매핑</li>
     *   <li>동일한 JOIN 조건</li>
     *   <li>동일한 ExpressionUtils.as 사용법</li>
     *   <li>방문자용 필드만 포함 (content, anonymity 제외)</li>
     * </ul>
     */
    @Override
    public List<VisitMessageDTO> findVisitMessageDTOsByUserName(String userName) {
        QMessage message = QMessage.message;
        QUsers user = QUsers.users;

        return jpaQueryFactory
                .select(Projections.fields(VisitMessageDTO.class,
                        message.id,
                        ExpressionUtils.as(message.users.id, "userId"),
                        message.decoType,
                        message.width,
                        message.height
                ))
                .from(message)
                .join(message.users, user)
                .where(user.userName.eq(userName))
                .fetch();
    }
}