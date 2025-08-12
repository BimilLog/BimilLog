package jaeik.growfarm.domain.paper.infrastructure.adapter.out.persistence;

import jaeik.growfarm.domain.paper.entity.Message;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * <h2>메시지 리포지토리 인터페이스</h2>
 * <p>
 * Message 엔티티에 대한 데이터베이스 작업을 정의합니다.
 * </p>
 *
 * @author Jaeik
 * @version 2.1.0
 */
@Repository
public interface MessageRepository extends JpaRepository<Message, Long> {
}
