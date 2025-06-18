package jaeik.growfarm.util;

import jaeik.growfarm.dto.paper.MessageDTO;
import jaeik.growfarm.dto.paper.VisitMessageDTO;
import jaeik.growfarm.entity.message.Message;
import jaeik.growfarm.entity.user.Users;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/*
 * PaperUtil 클래스
 * 농장 관련 유틸리티 클래스
 * 수정일 : 2025-05-03
 */
@Component
@RequiredArgsConstructor
public class PaperUtil {

    /**
     * <h3>Crop 엔티티를 CropDTO로 변환</h3>
     *
     * <p>
     * Crop 엔티티를 CropDTO 객체로 변환한다.
     * </p>
     * 
     * @since 1.0.0
     * @author Jaeik
     * @param message Crop 엔티티
     * @return 농작물 DTO
     */
    public MessageDTO convertToCropDTO(Message message) {
        MessageDTO messageDTO = new MessageDTO();
        messageDTO.setId(message.getId());
        messageDTO.setUserName(message.getUsers().getUserName());
        messageDTO.setDecoType(message.getDecoType());
        messageDTO.setUserName(message.getAnonymity());
        messageDTO.setMessage(message.getMessage());
        messageDTO.setWidth(message.getWidth());
        messageDTO.setHeight(message.getHeight());
        return messageDTO;
    }

    /**
     * <h3>CropDTO를 Crop 엔티티로 변환</h3>
     *
     * <p>
     * CropDTO 객체를 Crop 엔티티로 변환한다.
     * </p>
     * 
     * @since 1.0.0
     * @author Jaeik
     * @param messageDTO 농작물 DTO
     * @param user    농장 소유자 정보
     * @return Crop 엔티티
     */
    public Message convertToCrop(MessageDTO messageDTO, Users user) {
        return Message.builder()
                .users(user)
                .decoType(messageDTO.getDecoType())
                .anonymity(messageDTO.getAnonymity())
                .message(messageDTO.getMessage())
                .width(messageDTO.getWidth())
                .height(messageDTO.getHeight())
                .build();
    }

    /**
     * <h3>Crop 엔티티를 VisitCropDTO로 변환</h3>
     *
     * <p>
     * 농장 방문 시 사용하는 농작물 DTO로 변환한다.
     * </p>
     * 
     * @since 1.0.0
     * @author Jaeik
     * @param message Crop 엔티티
     * @return 방문 농장 농작물 DTO
     */
    public VisitMessageDTO convertToVisitPaperDTO(Message message) {
        VisitMessageDTO visitMessageDTO = new VisitMessageDTO();
        visitMessageDTO.setId(message.getId());
        visitMessageDTO.setUserName(message.getUsers().getUserName());
        visitMessageDTO.setDecoType(message.getDecoType());
        visitMessageDTO.setWidth(message.getWidth());
        visitMessageDTO.setHeight(message.getHeight());
        return visitMessageDTO;
    }
}
