package jaeik.bimillog.domain.paper.application.port.in;

import jaeik.bimillog.domain.paper.entity.DecoType;
import jaeik.bimillog.domain.paper.exception.PaperCustomException;

/**
 * <h2>롤링페이퍼 명령 유스케이스</h2>
 * <p>
 * 인바운드 포트: 롤링페이퍼 생성/수정/삭제 관련 유스케이스를 정의
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
     * @param messageId 삭제할 메시지 ID
     * @throws PaperCustomException 삭제 권한이 없는 경우 (MESSAGE_DELETE_FORBIDDEN)
     * @author Jaeik
     * @since 2.0.0
     */
    void deleteMessageInMyPaper(Long userId, Long messageId);

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
     * @param decoType 데코레이션 타입
     * @param anonymity 익명 이름
     * @param content 메시지 내용
     * @param width 메시지 너비
     * @param height 메시지 높이
     * @throws PaperCustomException 사용자가 존재하지 않거나 유효성 검증 실패 시
     * @author Jaeik
     * @since 2.0.0
     */
    void writeMessage(String userName, DecoType decoType, String anonymity, 
                     String content, int width, int height);
}