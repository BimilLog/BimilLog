package jaeik.bimillog.domain.paper.dto;

import jaeik.bimillog.domain.paper.entity.DecoType;
import jaeik.bimillog.domain.paper.entity.Message;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * <h2>내 메시지 조회용 DTO</h2>
 * @version 2.6.0
 * @author Jaeik
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class MyPaperDTO {
    private Long id;
    private DecoType decoType;
    private String anonymity;
    private String content;
    private int x;
    private int y;
    private Instant createdAt;

    public static List<MyPaperDTO> getListMyMessageDTO(List<Message> messageList) {
        List<MyPaperDTO> myPaperDTOS = new ArrayList<>();
        for (Message message : messageList) {
            MyPaperDTO from = from(message);
            myPaperDTOS.add(from);
        }
        return myPaperDTOS;
    }

    private static MyPaperDTO from(Message message) {
        return new MyPaperDTO(
                message.getId(),
                message.getDecoType(),
                message.getAnonymity(),
                message.getContent(),
                message.getX(),
                message.getY(),
                message.getCreatedAt()
        );
    }
}
