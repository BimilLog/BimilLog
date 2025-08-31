package jaeik.growfarm.domain.paper.application.service;

import jaeik.growfarm.domain.paper.application.port.in.PaperCommandUseCase;
import jaeik.growfarm.domain.paper.application.port.out.LoadUserPort;
import jaeik.growfarm.domain.paper.application.port.out.PaperCommandPort;
import jaeik.growfarm.domain.paper.application.port.out.PaperQueryPort;
import jaeik.growfarm.domain.paper.entity.Message;
import jaeik.growfarm.domain.paper.entity.MessageCommand;
import jaeik.growfarm.domain.paper.event.RollingPaperEvent;
import jaeik.growfarm.domain.user.entity.User;
import jaeik.growfarm.infrastructure.exception.CustomException;
import jaeik.growfarm.infrastructure.exception.ErrorCode;
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
    private final LoadUserPort loadUserPort;
    private final ApplicationEventPublisher eventPublisher;


    /**
     * <h3>메시지 삭제</h3>
     * 
     * <ul>
     *   <li>메시지 소유권 검증 (userId 일치 확인)</li>
     *   <li>동일한 예외 처리 (MESSAGE_DELETE_FORBIDDEN)</li>
     *   <li>메시지 ID로 삭제 수행</li>
     * </ul>
     * @author Jaeik
     * @since 2.0.0
     */
    @Override
    public void deleteMessageInMyPaper(Long userId, MessageCommand messageCommand) {
        if (messageCommand == null) {
            throw new CustomException(ErrorCode.INVALID_INPUT_VALUE);
        }
        
        Long ownerId = paperQueryPort.findOwnerIdByMessageId(messageCommand.id())
                .orElseThrow(() -> new CustomException(ErrorCode.MESSAGE_NOT_FOUND));

        if (!ownerId.equals(userId)) {
            throw new CustomException(ErrorCode.MESSAGE_DELETE_FORBIDDEN);
        }
        paperCommandPort.deleteById(messageCommand.id());
    }

    /**
     * <h3>메시지 작성</h3>
     *
     * <ul>
     *   <li>사용자 존재 여부 검증 (null 체크)</li>
     *   <li>동일한 예외 처리 (USERNAME_NOT_FOUND)</li>
     *   <li>Message.createMessage() 팩토리 메서드 사용</li>
     *   <li>메시지 저장 후 MessageEvent 이벤트 발행</li>
     *   <li>동일한 이벤트 데이터 (userId, userName)</li>
     * </ul>
     *
     * @author Jaeik
     * @since 2.0.0
     */
    @Override
    public void writeMessage(String userName, MessageCommand messageCommand) {
        if (messageCommand == null) {
            throw new CustomException(ErrorCode.INVALID_INPUT_VALUE);
        }
        
        User user = loadUserPort.findByUserName(userName)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));

        Message message = Message.createMessage(user, messageCommand);
        paperCommandPort.save(message);

        eventPublisher.publishEvent(new RollingPaperEvent(
                this,
                user.getId(),
                userName
        ));
    }
}