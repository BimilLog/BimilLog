package jaeik.growfarm.repository.message;

import com.querydsl.core.types.Projections;
import com.querydsl.jpa.impl.JPAQueryFactory;
import jaeik.growfarm.dto.paper.MessageDTO;
import jaeik.growfarm.entity.message.QMessage;
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
                .select(Projections.bean(MessageDTO.class,
                        message.id,
                        message.decoType,
                        message.anonymity,
                        message.content,
                        message.width,
                        message.height
                ))
                .from(message)
                .where(message.users.id.eq(userId))
                .orderBy(message.createdAt.desc())
                .fetch();
    }

}
