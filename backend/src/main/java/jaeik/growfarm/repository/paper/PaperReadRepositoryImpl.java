package jaeik.growfarm.repository.paper;

import com.querydsl.core.types.ExpressionUtils;
import com.querydsl.core.types.Projections;
import com.querydsl.jpa.impl.JPAQueryFactory;
import jaeik.growfarm.domain.message.domain.QMessage;
import jaeik.growfarm.domain.user.domain.QUser;
import jaeik.growfarm.dto.paper.MessageDTO;
import jaeik.growfarm.dto.paper.VisitMessageDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
@RequiredArgsConstructor
public class PaperReadRepositoryImpl implements PaperReadRepository {

    private final JPAQueryFactory jpaQueryFactory;
    
    @Override
    public List<MessageDTO> findMessageDTOsByUserId(Long userId) {
        QMessage message = QMessage.message;

        return jpaQueryFactory
                .select(Projections.fields(MessageDTO.class,
                        message.id,
                        message.user.id.as("userId"), // 실제 userId
                        message.decoType,
                        message.anonymity,
                        message.content,
                        message.width,
                        message.height,
                        message.createdAt
                ))
                .from(message)
                .where(message.user.id.eq(userId))
                .orderBy(message.createdAt.desc())
                .fetch();
    }
    
    @Override
    public List<VisitMessageDTO> findVisitMessageDTOsByUserName(String userName) {
        QMessage message = QMessage.message;
        QUser user = QUser.user;

        return jpaQueryFactory
                .select(Projections.fields(VisitMessageDTO.class,
                        message.id,
                        ExpressionUtils.as(message.user.id, "userId"),
                        message.decoType,
                        message.width,
                        message.height
                ))
                .from(message)
                .join(message.user, user)
                .where(user.userName.eq(userName))
                .fetch();
    }
}
