package jaeik.bimillog.infrastructure.adapter.paper.out.jpa;

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
}
