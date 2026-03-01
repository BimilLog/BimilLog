package jaeik.bimillog.domain.friend.dto;

import lombok.Builder;
import lombok.Getter;

import java.util.Map;

@Getter
@Builder
public class InteractionRebuildDTO {
    Long memberId;
    Map<Long, Double> scores;

    public static InteractionRebuildDTO createDTO(Long memberId, Map<Long, Double> scores) {
        return builder().memberId(memberId).scores(scores).build();
    }
}
