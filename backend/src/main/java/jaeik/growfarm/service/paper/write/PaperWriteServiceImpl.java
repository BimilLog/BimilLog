package jaeik.growfarm.service.paper.write;

import jaeik.growfarm.dto.paper.MessageDTO;
import jaeik.growfarm.entity.message.Message;
import jaeik.growfarm.entity.user.Users;
import jaeik.growfarm.global.event.MessageEvent;
import jaeik.growfarm.global.exception.CustomException;
import jaeik.growfarm.global.exception.ErrorCode;
import jaeik.growfarm.repository.paper.PaperCommandRepository;
import jaeik.growfarm.repository.user.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * <h2>PaperWriteService 구현 클래스</h2>
 * <p>롤링페이퍼 작성 관련 서비스 클래스</p>
 *
 * @author Jaeik
 * @version 2.0.0
 */
@Service
@Transactional
@RequiredArgsConstructor
public class PaperWriteServiceImpl implements PaperWriteService {

    private final UserRepository userRepository;
    private final PaperCommandRepository paperCommandRepository;
    private final ApplicationEventPublisher eventPublisher;

    /**
     * {@inheritDoc}
     */
    @Override
    public void writeMessage(String userName, MessageDTO messageDTO) {
        Users user = userRepository.findByUserName(userName);

        if (user == null) {
            throw new CustomException(ErrorCode.USERNAME_NOT_FOUND);
        }

        Message message = Message.createMessage(user, messageDTO);
        paperCommandRepository.save(message);

        eventPublisher.publishEvent(new MessageEvent(
                this,
                user.getId(),
                userName));
    }
}
