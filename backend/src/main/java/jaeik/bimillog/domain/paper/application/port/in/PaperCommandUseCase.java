package jaeik.bimillog.domain.paper.application.port.in;

import jaeik.bimillog.domain.paper.entity.DecoType;
import jaeik.bimillog.domain.paper.exception.PaperCustomException;
import jaeik.bimillog.infrastructure.adapter.paper.in.web.PaperCommandController;

/**
 * <h2>롤링페이퍼 명령 유스케이스</h2>
 * <p>롤링페이퍼 도메인의 명령 작업을 담당하는 유스케이스입니다.</p>
 * <p>메시지 작성, 메시지 삭제</p>
 *
 * @author Jaeik
 * @version 2.0.0
 */
public interface PaperCommandUseCase {

    /**
     * <h3>내 롤링페이퍼 메시지 삭제</h3>
     * <p>사용자가 자신의 롤링페이퍼에서 메시지를 삭제합니다.</p>
     * <p>메시지 소유권을 검증한 후 삭제를 수행합니다.</p>
     * <p>{@link PaperCommandController}에서 메시지 삭제 요청 시 호출됩니다.</p>
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
     * <p>지정된 사용자의 롤링페이퍼에 새로운 메시지를 작성합니다.</p>
     * <p>메시지 작성 완료 후 알림 이벤트를 발행합니다.</p>
     * <p>{@link PaperCommandController}에서 메시지 작성 요청 시 호출됩니다.</p>
     *
     * @param userName 롤링페이퍼 소유자의 사용자명
     * @param decoType 데코레이션 타입
     * @param anonymity 익명 이름
     * @param content 메시지 내용
     * @param x 메시지 x좌표
     * @param y 메시지 y좌표
     * @throws PaperCustomException 사용자가 존재하지 않거나 유효성 검증 실패 시
     * @author Jaeik
     * @since 2.0.0
     */
    void writeMessage(String userName, DecoType decoType, String anonymity, 
                     String content, int x, int y);
}