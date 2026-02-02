package jaeik.bimillog.domain.paper.dto;

import jaeik.bimillog.domain.paper.entity.DecoType;
import jaeik.bimillog.domain.paper.entity.Message;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

/**
 * <h2>방문 조회용 메시지 DTO</h2>
 *
 * @author Jaeik
 * @version 2.6.0
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class VisitPaperDTO {
    private Long ownerId;
    private List<VisitMessageDTO> visitMessageDTOList;

    /**
     * 방문 DTO 생성
     */
    public static VisitPaperDTO createVisitPaperDTO(Long ownerId, List<Message> messageList) {
        List<VisitMessageDTO> visitMessageDtoList = VisitMessageDTO.getVisitMessageDtoList(messageList);
        return new VisitPaperDTO(ownerId, visitMessageDtoList);
    }

    @NoArgsConstructor
    @AllArgsConstructor
    static class VisitMessageDTO {
        private DecoType decoType;
        private int x;
        private int y;

        /**
         * DTO리스트 생성
         */
        private static List<VisitMessageDTO> getVisitMessageDtoList(List<Message> messageList) {
            List<VisitMessageDTO> visitMessageDTOS = new ArrayList<>();
            for (Message message : messageList) {
                VisitMessageDTO from = new VisitMessageDTO(message.getDecoType(), message.getX(), message.getY());
                visitMessageDTOS.add(from);
            }
            return visitMessageDTOS;
        }
    }
}