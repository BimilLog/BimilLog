package jaeik.bimillog.infrastructure.adapter.out.paper;

import jaeik.bimillog.domain.paper.entity.Message;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * <h2>메시지 JPA 리포지토리</h2>
 * <p>
 * 롤링페이퍼 Message 엔티티에 대한 기본적인 CRUD 작업을 제공하는 Spring Data JPA 리포지토리입니다.
 * </p>
 * <p>
 * Spring Data JPA의 JpaRepository를 상속받아 기본적인 저장, 조회, 삭제 기능을 제공하며,
 * 복잡한 쿼리는 PaperQueryAdapter에서 QueryDSL로 별도 구현합니다.
 * </p>
 * <p>
 * 메시지 저장과 삭제 시 PaperCommandAdapter에서 호출되어 Message 엔티티의 기본적인 영속성 작업을 담당합니다.
 * </p>
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
     * <p>{@link PaperCommandAdapter#deleteMessage}에서 messageId가 null일 때 호출됩니다.</p>
     *
     * @param userId 메시지를 삭제할 사용자 ID
     * @author Jaeik
     * @since 2.0.0
     */
    void deleteAllByUserId(Long userId);

}
