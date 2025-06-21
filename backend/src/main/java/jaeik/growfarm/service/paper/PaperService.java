package jaeik.growfarm.service.paper;

import jaeik.growfarm.dto.paper.MessageDTO;
import jaeik.growfarm.dto.paper.VisitMessageDTO;
import jaeik.growfarm.entity.message.Message;
import jaeik.growfarm.entity.user.Users;
import jaeik.growfarm.global.auth.CustomUserDetails;
import jaeik.growfarm.global.event.MessageEvent;
import jaeik.growfarm.global.exception.CustomException;
import jaeik.growfarm.global.exception.ErrorCode;
import jaeik.growfarm.repository.message.MessageRepository;
import jaeik.growfarm.repository.user.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * <h2>PaperService 클래스</h2>
 * <p>롤링페이퍼 관련 서비스 클래스</p>
 *
 * @author Jaeik
 * @version 1.0.0
 */
@Service
@RequiredArgsConstructor
public class PaperService {

    private final MessageRepository messageRepository;
    private final UserRepository userRepository;
    private final ApplicationEventPublisher eventPublisher;
    private final PaperUpdateService paperUpdateService;

    /**
     * <h3>내 롤링페이퍼 조회</h3>
     *
     * <p>
     * 사용자 ID를 통해 해당 사용자의 메시지 목록을 조회한다.
     * </p>
     *
     * @param userDetails 현재 로그인한 사용자 정보
     * @return 내 롤링페이퍼 메시지 리스트
     * @author Jaeik
     * @since 1.0.0
     */
    public List<MessageDTO> myPaper(CustomUserDetails userDetails) {
        return messageRepository.findMessageDTOsByUserId(userDetails.getUserId());
    }

    /**
     * <h3>다른 롤링페이퍼 방문</h3>
     *
     * <p>
     * 닉네임을 통해 해당 롤링페이퍼의 메시지 목록을 조회한다.
     * </p>
     *
     * @param userName 닉네임
     * @return 방문 롤링페이퍼의 메시지 리스트
     * @author Jaeik
     * @since 1.0.0
     */
    public List<VisitMessageDTO> visitPaper(String userName) {
        boolean exists = userRepository.existsByUserName(userName);
        if (!exists) {
            throw new CustomException(ErrorCode.USERNAME_NOT_FOUND);
        }
        return messageRepository.findVisitMessageDTOsByUserName(userName);
    }

    /**
     * <h3>메시지 남기기</h3>
     *
     * <p>
     * 다른 사용자의 롤링페이퍼에 메시지를 남기고 롤링페이퍼 주인에게 알림을 발송한다.
     * </p>
     *
     * @param userName   닉네임
     * @param messageDTO 심을 농작물 정보 DTO
     * @author Jaeik
     * @since 1.0.0
     */
    public void writeMessage(String userName, MessageDTO messageDTO) {
        Users user = userRepository.findByUserName(userName);

        if (user == null) {
            throw new CustomException(ErrorCode.USERNAME_NOT_FOUND);
        }

        Message message = Message.createMessage(user, messageDTO);
        paperUpdateService.saveMessage(message);

        eventPublisher.publishEvent(new MessageEvent(
                user.getId(),
                userName,
                user));
    }


    /**
     * <h3>메시지 삭제</h3>
     *
     * <p>
     * 자신의 롤링페이퍼에 있는 메시지를 삭제합니다.
     * </p>
     *
     * @param userDetails 현재 로그인한 사용자 정보
     * @param messageDTO  삭제할 메시지 정보
     * @author Jaeik
     * @since 1.0.0
     */
    public void deleteMessageInMyPaper(CustomUserDetails userDetails, MessageDTO messageDTO) {
        if (!messageDTO.getUserId().equals(userDetails.getUserId())) {
            throw new CustomException(ErrorCode.MESSAGE_DELETE_FORBIDDEN);
        }
        paperUpdateService.deleteMessage(messageDTO);
    }
}
