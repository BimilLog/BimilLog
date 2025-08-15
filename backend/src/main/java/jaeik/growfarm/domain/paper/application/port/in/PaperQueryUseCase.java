package jaeik.growfarm.domain.paper.application.port.in;

import jaeik.growfarm.domain.paper.entity.Message;
import jaeik.growfarm.dto.paper.MessageDTO;
import jaeik.growfarm.dto.paper.VisitMessageDTO;
import jaeik.growfarm.infrastructure.auth.CustomUserDetails;

import java.util.List;
import java.util.Optional;

/**
 * <h2>롤링페이퍼 조회 유스케이스</h2>
 * <p>
 * Primary Port: 롤링페이퍼 조회 관련 유스케이스를 정의
 * 기존 PaperReadService의 모든 기능을 포함
 * </p>
 *
 * @author Jaeik
 * @version 2.0.0 (헥사고날 아키텍처 적용)
 */
public interface PaperQueryUseCase {

    /**
     * <h3>내 롤링페이퍼 조회</h3>
     * <p>
     * 기존 PaperReadService.myPaper() 메서드와 동일한 기능
     * </p>
     *
     * @param userDetails 현재 로그인한 사용자 정보
     * @return 내 롤링페이퍼의 메시지 목록
     * @author Jaeik
     * @since 2.0.0
     */
    List<MessageDTO> getMyPaper(CustomUserDetails userDetails);

    /**
     * <h3>다른 사용자 롤링페이퍼 방문 조회</h3>
     * <p>
     * 기존 PaperReadService.visitPaper() 메서드와 동일한 기능
     * 사용자 존재 여부 검증 포함
     * </p>
     *
     * @param userName 방문할 사용자명
     * @return 방문한 롤링페이퍼의 메시지 목록 (익명화된 정보)
     * @throws jaeik.growfarm.global.exception.CustomException 사용자가 존재하지 않는 경우
     * @author Jaeik
     * @since 2.0.0
     */
    List<VisitMessageDTO> visitPaper(String userName);

    Optional<Message> findMessageById(Long messageId);
}