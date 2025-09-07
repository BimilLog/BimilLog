package jaeik.bimillog.infrastructure.adapter.paper.dto;

import jaeik.bimillog.domain.paper.entity.DecoType;
import jaeik.bimillog.domain.paper.entity.MessageDetail;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;

/**
 * <h2>메시지 DTO</h2>
 * <p>
 * 메시지 작성 시 필요한 정보를 담는 DTO
 * </p>
 * <p>
 * 익명 이름은 최대 8자, 내용은 최대 255자까지 입력 가능하다.
 * </p>
 *
 * @author Jaeik
 * @version 2.0.0
 */
@Getter
@Setter
public class MessageDTO {

    private Long id;

    private Long userId;

    private DecoType decoType;

    @Size(max = 8, message = "익명 이름은 최대 8글자 까지 입력 가능합니다.")
    private String anonymity;

    @Size(max = 255, message = "내용은 최대 255자 까지 입력 가능합니다.")
    private String content;

    private int width;

    private int height;

    private Instant createdAt;

    /**
     * <h3>도메인 VO로부터 DTO 생성</h3>
     * <p>MessageDetail 도메인 값 객체를 DTO로 변환합니다.</p>
     *
     * @param messageDetail 도메인 값 객체
     * @return MessageDTO
     * @author Jaeik
     * @since 2.0.0
     */
    public static MessageDTO from(MessageDetail messageDetail) {
        MessageDTO dto = new MessageDTO();
        dto.id = messageDetail.id();
        dto.userId = messageDetail.userId();
        dto.decoType = messageDetail.decoType();
        dto.anonymity = messageDetail.anonymity();
        dto.content = messageDetail.content();
        dto.width = messageDetail.width();
        dto.height = messageDetail.height();
        dto.createdAt = messageDetail.createdAt();
        return dto;
    }

}
