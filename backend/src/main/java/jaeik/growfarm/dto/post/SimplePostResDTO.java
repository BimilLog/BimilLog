package jaeik.growfarm.dto.post;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

import java.time.Instant;
import java.time.LocalDateTime;

@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class SimplePostResDTO {
    private Long id;
    private String title;
    private String content;
    private String writer;
    private int views;
    private int likes;
    private boolean isNotice;
    private Instant createdAt;
}
