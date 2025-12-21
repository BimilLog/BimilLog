package jaeik.bimillog.domain.member.dto;

import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.Instant;

/**
 * <h3>블랙리스트 조회 DTO</h3>
 * <p>사용자의 블랙리스트 목록을 조회할 때 사용하는 DTO</p>
 *
 * @author Jaeik
 * @since 2.1.0
 */
@Getter
@AllArgsConstructor
public class BlacklistDTO {

    /**
     * 블랙리스트 ID (삭제 시 사용)
     */
    private Long id;

    /**
     * 차단한 사용자의 이름
     */
    @Size(min = 1, max = 8, message = "사용자 이름은 1자 이상 8자 이하여야 합니다.")
    private String memberName;

    /**
     * 차단한 날짜
     */
    private Instant createdAt;
}
