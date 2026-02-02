package jaeik.bimillog.domain.paper.dto;

import jaeik.bimillog.domain.paper.entity.DecoType;
import jaeik.bimillog.domain.paper.entity.Message;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class VisitMessageDTO {
    private DecoType decoType;
    private int x;
    private int y;

    public static List<VisitMessageDTO> getListVisitMessageDTO(List<Message> messageList) {
        List<VisitMessageDTO> visitMessageDTOS = new ArrayList<>();
        for (Message message : messageList) {
            VisitMessageDTO from = from(message);
            visitMessageDTOS.add(from);
        }
        return visitMessageDTOS;
    }

    private static VisitMessageDTO from(Message message) {
        return new VisitMessageDTO(
                message.getDecoType(),
                message.getX(),
                message.getY()
        );
    }
}
