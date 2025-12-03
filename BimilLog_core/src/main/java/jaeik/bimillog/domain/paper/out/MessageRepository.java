package jaeik.bimillog.domain.paper.out;

import jaeik.bimillog.domain.paper.entity.Message;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

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
     *
     * @param memberId 메시지를 삭제할 사용자 ID
     * @author Jaeik
     * @since 2.0.0
     */
    void deleteAllByMember_Id(Long memberId);

    /**
     * 특정 사용자(롤링페이퍼 소유자) ID로 해당 롤링페이퍼에 작성된 메시지 목록을
     * 최신순(createdAt 기준 내림차순)으로 조회합니다.
     *
     * @param memberId 롤링페이퍼 소유자의 사용자 ID (Message.member.id)
     * @return 해당 사용자의 메시지 목록 (List<Message>)
     */
    List<Message> findByMemberIdOrderByCreatedAtDesc(Long memberId);

    /**
     * 특정 사용자명(롤링페이퍼 소유자)을 기준으로
     * 해당 롤링페이퍼에 작성된 모든 메시지 목록을 조회합니다.
     *
     * @param memberName 롤링페이퍼 소유자의 사용자명 (Message.member.memberName)
     * @return 해당 사용자의 메시지 목록 (List<Message>)
     */
    List<Message> findByMemberMemberName(String memberName);

    /**
     * 특정 메시지 ID를 사용하여 해당 메시지의 소유자 ID만 조회합니다
     *
     * @param messageId 소유자를 확인할 메시지의 ID
     * @return 롤링페이퍼 소유자의 사용자 ID (Optional<Long>)
     */
    @Query("SELECT m.member.id FROM Message m WHERE m.id = :messageId")
    Optional<Long> findOwnerIdByMessageId(@Param("messageId") Long messageId);
}
