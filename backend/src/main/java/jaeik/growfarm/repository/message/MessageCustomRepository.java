package jaeik.growfarm.repository.message;

import jaeik.growfarm.dto.paper.MessageDTO;
import jaeik.growfarm.dto.paper.VisitMessageDTO;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * <h2>메시지 커스텀 저장소 인터페이스</h2>
 * <p>
 * 메시지 관련 데이터베이스 작업을 수행하며 커스텀한 쿼리메소드가 포함되어 있습니다.
 * </p>
 *
 * @author Jaeik
 * @version 1.0.0
 */
@Repository
public interface MessageCustomRepository {

    List<MessageDTO> findMessageDTOsByUserId(Long userId);

    List<VisitMessageDTO> findVisitMessageDTOsByUserName(String userName);

}
