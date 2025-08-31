package jaeik.growfarm.domain.paper.application.port.in;

import jaeik.growfarm.domain.paper.entity.MessageCommand;
import jaeik.growfarm.infrastructure.exception.CustomException;

/**
 * <h2>롤링페이퍼 명령 유스케이스</h2>
 * <p>
 * Primary Port: 롤링페이퍼 생성/수정/삭제 관련 유스케이스를 정의
 * 기존 PaperWriteService, PaperDeleteService의 모든 기능을 포함
 * </p>
 *
 * @author Jaeik
 * @version 2.0.0
 */
public interface PaperCommandUseCase {

    /**
     * <h3>내 롤링페이퍼 메시지 삭제</h3>
     * <p>
     * 기존 PaperDeleteService.deleteMessageInMyPaper() 메서드와 동일한 기능
     * - 메시지 소유권 검증 (userId 일치 확인)
     * - 메시지 삭제 수행
     * </p>
     *
     * @param userId 현재 로그인한 사용자 ID
     * @param messageCommand 삭제할 메시지 정보
     * @throws CustomException 삭제 권한이 없는 경우 (MESSAGE_DELETE_FORBIDDEN)
     * @author Jaeik
     * @since 2.0.0
     */
    void deleteMessageInMyPaper(Long userId, MessageCommand messageCommand);

    /**
     * <h3>메시지 작성</h3>
     * <p>
     * 기존 PaperWriteService.writeMessage() 메서드와 동일한 기능
     * - 사용자 존재 여부 검증
     * - 메시지 생성 및 저장
     * - 알림 이벤트 발행 (MessageEvent)
     * </p>
     *
     * @param userName 롤링페이퍼 소유자의 사용자명
     * @param messageCommand 작성할 메시지 정보
     * @throws CustomException 사용자가 존재하지 않는 경우
     * @author Jaeik
     * @since 2.0.0
     */
    void writeMessage(String userName, MessageCommand messageCommand);
}