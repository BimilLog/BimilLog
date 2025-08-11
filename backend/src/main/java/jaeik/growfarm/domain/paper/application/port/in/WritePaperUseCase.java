package jaeik.growfarm.domain.paper.application.port.in;

import jaeik.growfarm.dto.paper.MessageDTO;

/**
 * <h2>롤링페이퍼 작성 유스케이스</h2>
 * <p>
 * Primary Port: 롤링페이퍼 메시지 작성 관련 유스케이스를 정의
 * 기존 PaperWriteService의 모든 기능을 포함
 * </p>
 *
 * @author Jaeik
 * @version 2.0.0 (헥사고날 아키텍처 적용)
 */
public interface WritePaperUseCase {

    /**
     * <h3>메시지 작성</h3>
     * <p>
     * 기존 PaperWriteService.writeMessage() 메서드와 동일한 기능
     * - 사용자 존재 여부 검증
     * - 메시지 생성 및 저장
     * - 알림 이벤트 발행 (MessageEvent)
     * </p>
     *
     * @param userName   롤링페이퍼 소유자의 사용자명
     * @param messageDTO 작성할 메시지 정보
     * @throws jaeik.growfarm.global.exception.CustomException 사용자가 존재하지 않는 경우
     * @author Jaeik
     * @since 2.0.0
     */
    void writeMessage(String userName, MessageDTO messageDTO);
}