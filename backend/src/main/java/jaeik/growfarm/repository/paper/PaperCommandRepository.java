package jaeik.growfarm.repository.paper;

import jaeik.growfarm.domain.message.domain.Message;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * <h2>롤링페이퍼 변경 레포지토리 인터페이스</h2>
 * <p>
 * 롤링페이퍼 변경(생성, 수정, 삭제) 관련 데이터베이스 작업을 정의합니다.
 * </p>
 *
 * @author Jaeik
 * @version 2.0.0
 */
@Repository
public interface PaperCommandRepository extends JpaRepository<Message, Long> {
}
