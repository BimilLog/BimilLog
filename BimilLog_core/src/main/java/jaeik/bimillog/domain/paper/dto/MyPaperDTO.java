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
 * <h2>내 롤링페이퍼 조회용 DTO</h2>
 *
 * @author Jaeik
 * @version 2.8.0
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class MyPaperDTO {
    private Long memberId;
    private List<MyMessageDTO> myMessageDTOList;

    /**
     * 내 롤링페이퍼 DTO 생성
     */
    public static MyPaperDTO createMyPaperDTO(Long memberId, List<Message> messageList) {
        List<MyMessageDTO> myMessageDTOList = MyMessageDTO.getMyMessageDTOList(messageList);
        return new MyPaperDTO(memberId, myMessageDTOList);
    }

    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    static class MyMessageDTO {
        private Long id;
        private DecoType decoType;
        private String anonymity;
        private String content;
        private int x;
        private int y;
        private Instant createdAt;

        /**
         * DTO 리스트 생성
         */
        private static List<MyMessageDTO> getMyMessageDTOList(List<Message> messageList) {
            List<MyMessageDTO> myMessageDTOS = new ArrayList<>();
            for (Message message : messageList) {
                myMessageDTOS.add(new MyMessageDTO(
                        message.getId(),
                        message.getDecoType(),
                        message.getAnonymity(),
                        message.getContent(),
                        message.getX(),
                        message.getY(),
                        message.getCreatedAt()
                ));
            }
            return myMessageDTOS;
        }
    }
}
