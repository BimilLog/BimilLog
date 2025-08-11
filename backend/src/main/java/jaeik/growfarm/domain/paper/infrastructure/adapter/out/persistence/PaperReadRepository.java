package jaeik.growfarm.domain.paper.infrastructure.adapter.out.persistence;

import jaeik.growfarm.dto.paper.MessageDTO;
import jaeik.growfarm.dto.paper.VisitMessageDTO;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * <h2>롤링페이퍼 조회 레포지토리 인터페이스</h2>
 * <p>
 * 롤링페이퍼 조회 관련 데이터베이스 작업을 정의합니다.
 * </p>
 *
 * @author Jaeik
 * @version 2.0.0
 */
@Repository
public interface PaperReadRepository {

    List<MessageDTO> findMessageDTOsByUserId(Long userId);

    List<VisitMessageDTO> findVisitMessageDTOsByUserName(String userName);
}
