package jaeik.bimillog.infrastructure.adapter.paper.dto;

import jaeik.bimillog.domain.paper.entity.DecoType;
import jaeik.bimillog.domain.paper.entity.MessageDetail;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;

/**
 * <h2>롤링페이퍼 메시지 DTO</h2>
 * <p>
 * 클라이언트와 서버 간 롤링페이퍼 메시지 데이터 전송을 위한 데이터 전송 객체입니다.
 * </p>
 * <p>
 * 메시지 작성 요청과 내 롤링페이퍼 조회 응답에서 공통으로 사용되며,
 * Jakarta Validation을 통해 익명 이름 8자, 내용 255자 제한을 강제합니다.
 * </p>
 * <p>
 * 프론트엔드의 메시지 작성 폼과 내 롤링페이퍼 표시에서 사용되며, MessageDetail VO와의 변환 메서드를 제공합니다.
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
     * <h3>MessageDetail VO에서 MessageDTO 변환</h3>
     * <p>도메인 계층의 MessageDetail 값 객체를 인프라스트럭처 계층의 DTO로 변환합니다.</p>
     * <p>복호화된 메시지 내용과 그리드 레이아웃 정보를 포함하여 클라이언트에 전달할 수 있는 형태로 매핑합니다.</p>
     * <p>내 롤링페이퍼 조회 API 응답 생성 시 PaperQueryController에서 호출되어 도메인 데이터를 HTTP 응답용 DTO로 변환합니다.</p>
     *
     * @param messageDetail 변환할 MessageDetail 도메인 값 객체 (복호화된 메시지 정보 포함)
     * @return MessageDTO 변환된 메시지 DTO (클라이언트 전달용)
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
