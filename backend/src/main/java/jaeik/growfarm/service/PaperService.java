package jaeik.growfarm.service;

import jaeik.growfarm.dto.paper.MessageDTO;
import jaeik.growfarm.dto.paper.VisitMessageDTO;
import jaeik.growfarm.entity.message.Message;
import jaeik.growfarm.entity.user.Users;
import jaeik.growfarm.global.auth.CustomUserDetails;
import jaeik.growfarm.repository.message.MessageRepository;
import jaeik.growfarm.repository.user.UserRepository;
import jaeik.growfarm.util.PaperUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.List;

/**
 * <h2>PaperService 클래스</h2>
 * <p>롤링페이퍼 관련 서비스 클래스</p>
 * @author Jaeik
 * @version 1.0.0
 */
@Service
@RequiredArgsConstructor
public class PaperService {

    private final MessageRepository messageRepository;
    private final UserRepository userRepository;
    private final PaperUtil paperUtil;
    private final ApplicationEventPublisher eventPublisher;

    /**
     * <h3>내 롤링페이퍼 조회</h3>
     *
     * <p>
     * 사용자 ID를 통해 해당 사용자의 메시지 목록을 조회한다.
     * </p>
     * 
     * @since 1.0.0
     * @author Jaeik
     * @param userDetails 현재 로그인한 사용자 정보
     * @return 내 롤링페이퍼 메시지 리스트
     */
    public List<MessageDTO> myPaper(CustomUserDetails userDetails) {
        return messageRepository.findMessageDTOsByUserId(userDetails.getUserId());
    }

    /**
     * <h3>다른 농장 방문</h3>
     *
     * <p>
     * 닉네임을 통해 해당 농장의 농작물 목록을 조회한다.
     * </p>
     * 
     * @since 1.0.0
     * @author Jaeik
     * @param userName 닉네임
     * @return 방문 농장의 농작물 목록
     */
    public List<VisitMessageDTO> visitPaper(String userName) {
        Users user = userRepository.findByUserName(userName);

        if (user == null) {
            throw new IllegalArgumentException("해당 농장을 찾을 수 없습니다.");
        }

        List<Message> messages = messageRepository.findByUsersId(user.getId());
        return messages.stream().map(paperUtil::convertToVisitPaperDTO).toList();
    }

    /**
     * <h3>농작물 심기</h3>
     *
     * <p>
     * 다른 사용자의 농장에 농작물을 심고 농장 주인에게 알림을 발송한다.
     * 이벤트 기반 아키텍처로 SSE와 FCM 알림을 비동기 처리한다.
     * </p>
     * 
     * @since 1.0.0
     * @author Jaeik
     * @param userName 닉네임
     * @param messageDTO  심을 농작물 정보 DTO
     * @throws IOException FCM 메시지 발송 오류 시 발생
     */
    public void plantCrop(String userName, MessageDTO messageDTO) throws IOException {
        Users user = userRepository.findByUserName(userName);

        if (user == null) {
            throw new IllegalArgumentException("해당 농장을 찾을 수 없습니다.");
        }

        // 농작물 저장 (동기)
        Message message = paperUtil.convertToCrop(messageDTO, user);
        messageRepository.save(message);

        // 이벤트 발행 🚀 (알림은 이벤트 리스너에서 비동기로 처리)
        eventPublisher.publishEvent(new PaperPlantEvent(
                user.getId(),
                userName,
                user));
    }

    /**
     * <h3>농작물 삭제</h3>
     *
     * <p>
     * 농작물 소유자만 해당 농작물을 삭제할 수 있다.
     * </p>
     * 
     * @since 1.0.0
     * @author Jaeik
     * @param userDetails 현재 로그인한 사용자 정보
     * @param cropId      농작물 ID
     */
    public void deleteCrop(CustomUserDetails userDetails, Long cropId) {
        if (userDetails == null) {
            throw new RuntimeException("다시 로그인 해 주세요.");
        }

        Message message = messageRepository.findById(cropId)
                .orElseThrow(() -> new IllegalArgumentException("해당 농작물을 찾을 수 없습니다."));

        if (!message.getUsers().getId().equals(userDetails.getClientDTO().getUserId())) {
            throw new RuntimeException("본인 농장의 농작물만 삭제할 수 있습니다.");
        }

        messageRepository.delete(message);
    }
}
