package jaeik.growfarm.domain.paper.application.service;

import jaeik.growfarm.domain.paper.application.port.in.PaperCommandUseCase;
import jaeik.growfarm.domain.paper.application.port.in.PaperQueryUseCase;
import jaeik.growfarm.domain.paper.application.port.out.LoadUserPort;
import jaeik.growfarm.domain.paper.application.port.out.PaperCommandPort;
import jaeik.growfarm.domain.paper.application.port.out.PublishEventPort;
import jaeik.growfarm.domain.paper.entity.Message;
import jaeik.growfarm.domain.user.entity.User;
import jaeik.growfarm.dto.paper.MessageDTO;
import jaeik.growfarm.global.event.MessageEvent;
import jaeik.growfarm.global.exception.CustomException;
import jaeik.growfarm.global.exception.ErrorCode;
import jaeik.growfarm.infrastructure.auth.CustomUserDetails;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * <h2>롤링페이퍼 삭제 서비스</h2>
 * <p>
 * Use Case Implementation: 롤링페이퍼 메시지 삭제 관련 비즈니스 로직 구현
 * 기존 PaperDeleteServiceImpl의 모든 로직을 완전히 보존하여 이전
 * </p>
 *
 * @author Jaeik
 * @version 2.0.0 (헥사고날 아키텍처 적용)
 */
@Service
@Transactional
@RequiredArgsConstructor
public class PaperCommandService implements PaperCommandUseCase {

    private final PaperCommandPort paperCommandPort;
    private final PaperQueryUseCase paperQueryUseCase;
    private final LoadUserPort loadUserPort;
    private final PublishEventPort publishEventPort;




    /**
     * {@inheritDoc}
     * 
     * <p>기존 PaperDeleteServiceImpl.deleteMessageInMyPaper() 메서드의 로직을 완전히 보존:</p>
     * <ul>
     *   <li>메시지 소유권 검증 (userId 일치 확인)</li>
     *   <li>동일한 예외 처리 (MESSAGE_DELETE_FORBIDDEN)</li>
     *   <li>메시지 ID로 삭제 수행</li>
     * </ul>
     */
    @Override
    public void deleteMessageInMyPaper(CustomUserDetails userDetails, MessageDTO messageDTO) {
        Message message = paperQueryUseCase.findMessageById(messageDTO.getId())
                .orElseThrow(() -> new CustomException(ErrorCode.MESSAGE_NOT_FOUND));

        if (!message.isOwner(userDetails.getUserId())) {
            throw new CustomException(ErrorCode.MESSAGE_DELETE_FORBIDDEN);
        }
        paperCommandPort.deleteById(message.getId());
    }

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
        User user = loadUserPort.findByUserName(userName)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));

        Message message = Message.createMessage(user, messageDTO);
        paperCommandPort.save(message);

        publishEventPort.publishMessageEvent(new MessageEvent(
                this,
                user.getId(),
                userName));
    }
}