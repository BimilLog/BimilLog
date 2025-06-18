package jaeik.growfarm.repository.message;

import jaeik.growfarm.entity.message.Message;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * <h2>메시지 저장소 인터페이스</h2>
 * <p>
 * 메시지 관련 데이터베이스 작업을 수행하는 Jpa레포지토리입니다.
 * </p>
 *
 * @author Jaeik
 * @version 1.0.0
 */
@Repository
public interface MessageRepository extends JpaRepository<Message, Long> , MessageCustomRepository {

}
