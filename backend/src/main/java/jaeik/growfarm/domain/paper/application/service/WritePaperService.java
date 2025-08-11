package jaeik.growfarm.domain.paper.application.service;

import jaeik.growfarm.domain.paper.domain.Message;
import jaeik.growfarm.domain.paper.application.port.in.WritePaperUseCase;
import jaeik.growfarm.domain.paper.application.port.out.LoadUserPort;
import jaeik.growfarm.domain.paper.application.port.out.PublishEventPort;
import jaeik.growfarm.domain.paper.application.port.out.SavePaperPort;
import jaeik.growfarm.domain.user.domain.User;
import jaeik.growfarm.dto.paper.MessageDTO;
import jaeik.growfarm.global.event.MessageEvent;
import jaeik.growfarm.global.exception.CustomException;
import jaeik.growfarm.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * <h2>롤링페이퍼 작성 서비스</h2>
 * <p>
 * Use Case Implementation: 롤링페이퍼 메시지 작성 관련 비즈니스 로직 구현
 * 기존 PaperWriteServiceImpl의 모든 로직을 완전히 보존하여 이전
 * </p>
 *
 * @author Jaeik
 * @version 2.0.0 (헥사고날 아키텍처 적용)
 */
@Service
@Transactional
@RequiredArgsConstructor
public class WritePaperService implements WritePaperUseCase {

    private final LoadUserPort loadUserPort;
    private final SavePaperPort savePaperPort;
    private final PublishEventPort publishEventPort;

    /**
     * {@inheritDoc}
     * 
     * <p>기존 PaperWriteServiceImpl.writeMessage() 메서드의 로직을 완전히 보존:</p>
     * <ul>
     *   <li>사용자 존재 여부 검증 (null 체크)</li>
     *   <li>동일한 예외 처리 (USERNAME_NOT_FOUND)</li>
     *   <li>Message.createMessage() 팩토리 메서드 사용</li>
     *   <li>메시지 저장 후 MessageEvent 이벤트 발행</li>
     *   <li>동일한 이벤트 데이터 (userId, userName)</li>
     * </ul>
     */
    @Override
    public void writeMessage(String userName, MessageDTO messageDTO) {
        User user = loadUserPort.findByUserName(userName);

        if (user == null) {
            throw new CustomException(ErrorCode.USERNAME_NOT_FOUND);
        }

        Message message = Message.createMessage(user, messageDTO);
        savePaperPort.save(message);

        publishEventPort.publishMessageEvent(new MessageEvent(
                this,
                user.getId(),
                userName));
    }
}