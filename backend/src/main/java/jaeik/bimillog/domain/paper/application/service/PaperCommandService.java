package jaeik.bimillog.domain.paper.application.service;

import jaeik.bimillog.domain.paper.application.port.in.PaperCommandUseCase;
import jaeik.bimillog.domain.paper.application.port.out.PaperCommandPort;
import jaeik.bimillog.domain.paper.application.port.out.PaperQueryPort;
import jaeik.bimillog.domain.paper.entity.DecoType;
import jaeik.bimillog.domain.paper.entity.Message;
import jaeik.bimillog.domain.paper.event.RollingPaperEvent;
import jaeik.bimillog.domain.paper.exception.PaperCustomException;
import jaeik.bimillog.domain.paper.exception.PaperErrorCode;
import jaeik.bimillog.domain.user.entity.User;
import jaeik.bimillog.global.application.port.out.GlobalUserQueryPort;
import jaeik.bimillog.infrastructure.adapter.in.paper.web.PaperCommandController;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * <h2>롤링페이퍼 명령 서비스</h2>
 * <p>롤링페이퍼 도메인의 명령 작업을 담당하는 서비스입니다.</p>
 * <p>메시지 작성, 메시지 삭제</p>
 *
 * @author Jaeik
 * @version 2.0.0
 */
@Service
@RequiredArgsConstructor
public class PaperCommandService implements PaperCommandUseCase {

    private final PaperCommandPort paperCommandPort;
    private final PaperQueryPort paperQueryPort;
    private final GlobalUserQueryPort globalUserQueryPort;
    private final ApplicationEventPublisher eventPublisher;


    /**
     * <h3>내 롤링페이퍼 메시지 삭제</h3>
     * <p>롤링페이퍼 소유자가 자신의 메시지를 삭제합니다.</p>
     * <p>메시지 소유권을 검증하여 권한이 있는 사용자만 삭제할 수 있습니다.</p>
     * <p>{@link PaperCommandController}에서 메시지 삭제 요청 시 호출됩니다.</p>
     *
     * @param userId 현재 로그인한 롤링페이퍼 소유자 ID
     * @param messageId 삭제할 메시지 ID
     * @throws PaperCustomException 메시지가 존재하지 않거나 삭제 권한이 없는 경우
     * @author Jaeik
     * @since 2.0.0
     */
    @Override
    @Transactional
    public void deleteMessageInMyPaper(Long userId, Long messageId) {
        Long ownerId = paperQueryPort.findOwnerIdByMessageId(messageId)
                .orElseThrow(() -> new PaperCustomException(PaperErrorCode.MESSAGE_NOT_FOUND));

        if (!ownerId.equals(userId)) {
            throw new PaperCustomException(PaperErrorCode.MESSAGE_DELETE_FORBIDDEN);
        }
        paperCommandPort.deleteById(messageId);
    }

    /**
     * <h3>롤링페이퍼 메시지 작성</h3>
     * <p>특정 사용자의 롤링페이퍼에 익명 메시지를 작성합니다.</p>
     * <p>사용자 존재성을 검증하고 메시지를 저장한 뒤 알림 이벤트를 발행합니다.</p>
     * <p>{@link PaperCommandController}에서 메시지 작성 요청 시 호출됩니다.</p>
     *
     * @param userName 롤링페이퍼 소유자의 사용자명
     * @param decoType 메시지 장식 스타일
     * @param anonymity 익명 작성자 이름
     * @param content 메시지 내용
     * @param x 그리드 레이아웃에서의 메시지 x좌표
     * @param y 그리드 레이아웃에서의 메시지 y좌표
     * @throws PaperCustomException 대상 사용자가 존재하지 않거나 입력값이 유효하지 않은 경우
     * @author Jaeik
     * @since 2.0.0
     */
    @Override
    @Transactional
    public void writeMessage(String userName, DecoType decoType, String anonymity,
                           String content, int x, int y) {
        if (userName == null || userName.trim().isEmpty()) {
            throw new PaperCustomException(PaperErrorCode.INVALID_INPUT_VALUE);
        }
        
        User user = globalUserQueryPort.findByUserName(userName)
                .orElseThrow(() -> new PaperCustomException(PaperErrorCode.USERNAME_NOT_FOUND));

        Message message = Message.createMessage(user, decoType, anonymity, content, x, y);
        paperCommandPort.save(message);

        eventPublisher.publishEvent(new RollingPaperEvent(
                user.getId(),
                userName
        ));
    }
}