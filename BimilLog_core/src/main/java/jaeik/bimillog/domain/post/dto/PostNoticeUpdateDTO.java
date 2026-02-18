package jaeik.bimillog.domain.post.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;

@Getter
public class PostNoticeUpdateDTO {
    @NotNull
    Long postId;

    boolean isNotice;
}
