package jaeik.growfarm.repository.paper;

import com.querydsl.core.types.ExpressionUtils;
import com.querydsl.core.types.Projections;
import com.querydsl.jpa.impl.JPAQueryFactory;
import jaeik.growfarm.dto.paper.MessageDTO;
import jaeik.growfarm.dto.paper.VisitMessageDTO;
import jaeik.growfarm.entity.message.QMessage;
import jaeik.growfarm.entity.user.QUsers;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * <h2>롤링페이퍼 조회 레포지토리 구현 클래스</h2>
 * <p>
 * QueryDSL을 사용하여 롤링페이퍼 조회 관련 데이터베이스 작업을 수행합니다.
 * </p>
 *
 * @author Jaeik
 * @version 1.0.0
 */
@Repository
@RequiredArgsConstructor
public class PaperReadRepositoryImpl implements PaperReadRepository {

    private final JPAQueryFactory jpaQueryFactory;

    /**
     * {@inheritDoc}
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
