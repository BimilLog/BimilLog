package jaeik.bimillog.domain.paper.out;

import jaeik.bimillog.domain.paper.entity.Message;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * <h2>메시지 JPA 리포지토리</h2>
 *
 * @author Jaeik
 * @version 2.0.0
 */
@Repository
public interface MessageRepository extends JpaRepository<Message, Long> {

    /**
     * <h3>사용자 ID로 모든 메시지 삭제</h3>
     * <p>특정 사용자가 받은 모든 롤링페이퍼 메시지를 삭제합니다.</p>
     * <p>주로 사용자 탈퇴 시 해당 사용자의 모든 메시지를 정리하는데 사용됩니다.</p>
     * <p>{@link PaperCommandRepository#deleteMessage}에서 messageId가 null일 때 호출됩니다.</p>
     *
     * @param memberId 메시지를 삭제할 사용자 ID
     * @author Jaeik
     * @since 2.0.0
     */
    void deleteAllByMember_Id(Long memberId);
}
