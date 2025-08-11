package jaeik.growfarm.domain.paper.application.port.out;

import jaeik.growfarm.domain.paper.domain.Message;
import jaeik.growfarm.dto.paper.MessageDTO;
import jaeik.growfarm.dto.paper.VisitMessageDTO;

import java.util.List;
import java.util.Optional;

/**
 * <h2>롤링페이퍼 조회 포트</h2>
 * <p>
 * Secondary Port: 롤링페이퍼 데이터 조회를 위한 포트
 * 기존 PaperReadRepository의 모든 기능을 포함
 * </p>
 *
 * @author Jaeik
 * @version 2.0.0 (헥사고날 아키텍처 적용)
 */
public interface LoadPaperPort {

    Optional<Message> findMessageById(Long messageId);

    /**
     * <h3>사용자 ID로 메시지 조회</h3>
     * <p>
     * 기존 PaperReadRepository.findMessageDTOsByUserId() 메서드와 동일한 기능
     * QueryDSL을 사용한 복잡한 프로젝션 포함
     * </p>
     *
     * @param userId 조회할 사용자 ID
     * @return 해당 사용자의 롤링페이퍼 메시지 목록 (최신순 정렬)
     * @author Jaeik
     * @since 2.0.0
     */
    List<MessageDTO> findMessageDTOsByUserId(Long userId);

    /**
     * <h3>사용자명으로 방문용 메시지 조회</h3>
     * <p>
     * 기존 PaperReadRepository.findVisitMessageDTOsByUserName() 메서드와 동일한 기능
     * 방문자에게 노출할 최소한의 정보만 포함 (익명화)
     * </p>
     *
     * @param userName 조회할 사용자명
     * @return 방문용 메시지 목록 (content, anonymity 제외)
     * @author Jaeik
     * @since 2.0.0
     */
    List<VisitMessageDTO> findVisitMessageDTOsByUserName(String userName);
}