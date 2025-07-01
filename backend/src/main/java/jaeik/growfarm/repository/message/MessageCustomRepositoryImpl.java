package jaeik.growfarm.repository.message;

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
 * <h2>메시지 커스텀 저장소 구현 클래스</h2>
 * <p>
 * 메시지 관련 데이터베이스 작업을 수행하며 커스텀한 쿼리메소드가 포함되어 있습니다.
 * </p>
 *
 * @author Jaeik
 * @version 1.0.0
 */
@Repository
@RequiredArgsConstructor
public class MessageCustomRepositoryImpl implements MessageCustomRepository {

    private final JPAQueryFactory jpaQueryFactory;

    /**
     * <h3>사용자 ID로 메시지 DTO 리스트 조회</h3>
     *
     * <p>
     * 사용자 ID를 통해 해당 사용자의 메시지 DTO 리스트를 조회합니다.
     * </p>
     *
     * @param userId 사용자 ID
     * @return List<MessageDTO> 해당 사용자의 메시지 DTO 리스트
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
     * <h3>닉네임로 방문 메시지 DTO 리스트 조회</h3>
     *
     * <p>
     * 닉네임을 통해 해당 사용자의 방문 메시지 DTO 리스트를 조회합니다.
     * </p>
     * <P>
     * 다른 사람의 롤링페이퍼를 방문할 때 사용됩니다.
     * </P>
     *
     * @param userName 사용자 닉네임
     * @return List<VisitMessageDTO> 해당 사용자의 방문 메시지 DTO 리스트
     * @since 1.0.0
     * @author Jaeik
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
