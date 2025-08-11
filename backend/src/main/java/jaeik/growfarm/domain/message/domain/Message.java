package jaeik.growfarm.domain.message.domain;

import jaeik.growfarm.domain.user.domain.User;
import jaeik.growfarm.dto.paper.MessageDTO;
import jaeik.growfarm.entity.BaseEntity;
import jaeik.growfarm.global.security.MessageEncryptConverter;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

/**
 * <h2>메시지 엔티티</h2>
 * <p>롤링페이퍼의 메시지를 담당하는 엔티티</p>
 *
 * @author Jaeik
 * @since 1.0.0
 */
@Entity
@Getter
@SuperBuilder
@NoArgsConstructor
@Table(name = "message", uniqueConstraints =
        {@UniqueConstraint(name = "unique_user_x_y", columnNames = {"user_id", "width", "height"})})
public class Message extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "message_id")
    private Long id;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY)
    @OnDelete(action = OnDeleteAction.CASCADE)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private DecoType decoType;

    @NotNull
    @Column(nullable = false, length = 8) // 익명 닉네임 8자 까지 허용
    private String anonymity;

    @NotNull
    @Column(columnDefinition = "TEXT", nullable = false)
    @Convert(converter = MessageEncryptConverter.class)
    private String content; // 익명 메시지 255자 까지 지만 암호화 때문에 TEXT로 설정

    @NotNull
    @Column(nullable = false)
    private int width;

    @NotNull
    @Column(nullable = false)
    private int height;

    public static Message createMessage(User user, MessageDTO messageDTO) {
        return Message.builder()
                .user(user)
                .decoType(messageDTO.getDecoType())
                .anonymity(messageDTO.getAnonymity())
                .content(messageDTO.getContent())
                .width(messageDTO.getWidth())
                .height(messageDTO.getHeight())
                .build();
    }
}

