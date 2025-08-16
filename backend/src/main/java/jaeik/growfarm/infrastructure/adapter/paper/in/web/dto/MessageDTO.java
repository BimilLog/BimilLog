package jaeik.growfarm.infrastructure.adapter.paper.in.web.dto;

import jaeik.growfarm.domain.paper.entity.DecoType;
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
 * @version 1.0.0
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
}
