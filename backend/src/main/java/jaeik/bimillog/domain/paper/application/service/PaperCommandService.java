package jaeik.bimillog.domain.paper.application.service;

import jaeik.bimillog.domain.paper.application.port.in.PaperCommandUseCase;
import jaeik.bimillog.domain.paper.application.port.out.PaperCommandPort;
import jaeik.bimillog.domain.paper.application.port.out.PaperQueryPort;
import jaeik.bimillog.domain.paper.application.port.out.PaperToUserPort;
import jaeik.bimillog.domain.paper.entity.Message;
import jaeik.bimillog.domain.paper.entity.DecoType;
import jaeik.bimillog.domain.paper.event.RollingPaperEvent;
import jaeik.bimillog.domain.paper.exception.PaperCustomException;
import jaeik.bimillog.domain.paper.exception.PaperErrorCode;
import jaeik.bimillog.domain.user.entity.User;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * <h2>롤링페이퍼 명령 서비스</h2>
 * <p>
 * 롤링페이퍼 메시지 생성/삭제 관련 비즈니스 로직 구현
 * </p>
 *
 * @author Jaeik
 * @version 2.0.0
 */
@Service
@Transactional
@RequiredArgsConstructor
public class PaperCommandService implements PaperCommandUseCase {

    private final PaperCommandPort paperCommandPort;
    private final PaperQueryPort paperQueryPort;
    private final PaperToUserPort paperToUserPort;
    private final ApplicationEventPublisher eventPublisher;


    /**
     * <h3>내 롤링페이퍼 메시지 삭제</h3>
     * <p>사용자가 자신의 롤링페이퍼에서 메시지를 삭제합니다.</p>
     * <p>메시지 소유권을 검증한 후 삭제를 수행합니다.</p>
     *
     * @param userId 현재 로그인한 사용자 ID
     * @param messageId 삭제할 메시지 ID
     * @author Jaeik
     * @since 2.0.0
     */
    @Override
    public void deleteMessageInMyPaper(Long userId, Long messageId) {
        if (messageId == null || userId == null) {
            throw new PaperCustomException(PaperErrorCode.INVALID_INPUT_VALUE);
        }
        
        Long ownerId = paperQueryPort.findOwnerIdByMessageId(messageId)
                .orElseThrow(() -> new PaperCustomException(PaperErrorCode.MESSAGE_NOT_FOUND));

        if (!ownerId.equals(userId)) {
            throw new PaperCustomException(PaperErrorCode.MESSAGE_DELETE_FORBIDDEN);
        }
        paperCommandPort.deleteById(messageId);
    }

    /**
     * <h3>롤링페이퍼 메시지 작성</h3>
     * <p>지정된 사용자의 롤링페이퍼에 새로운 메시지를 작성합니다.</p>
     * <p>메시지 작성 완료 후 알림 이벤트를 발행합니다.</p>
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
    @Override
    public void writeMessage(String userName, DecoType decoType, String anonymity, 
                           String content, int width, int height) {
        if (userName == null || decoType == null) {
            throw new PaperCustomException(PaperErrorCode.INVALID_INPUT_VALUE);
        }
        
        User user = paperToUserPort.findByUserName(userName)
                .orElseThrow(() -> new PaperCustomException(PaperErrorCode.USERNAME_NOT_FOUND));

        Message message = Message.createMessage(user, decoType, anonymity, content, width, height);
        paperCommandPort.save(message);

        eventPublisher.publishEvent(new RollingPaperEvent(
                user.getId(),
                userName
        ));
    }
}