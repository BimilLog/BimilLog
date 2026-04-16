package jaeik.bimillog.infrastructure.redis.member;

import jaeik.bimillog.domain.member.dto.SimpleMemberDTO;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.domain.Page;

@NoArgsConstructor
@AllArgsConstructor
@Getter
public class CacheMemberDTO {
    private Page<SimpleMemberDTO> simpleMemberDTOPage;
    private Double computeTTL;

    public static CacheMemberDTO from(Page<SimpleMemberDTO> simpleMemberDTOPage, Double computeTTL) {
        return new CacheMemberDTO(simpleMemberDTOPage, computeTTL);
    }
}
