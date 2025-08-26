package jaeik.growfarm.domain.paper.entity;

import jaeik.growfarm.domain.user.entity.User;
import jaeik.growfarm.domain.common.entity.BaseEntity;
import jaeik.growfarm.infrastructure.security.MessageEncryptConverter;
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

    /**
     * <h3>메시지 생성 팩토리 메소드</h3>
     * <p>새로운 롤링페이퍼 메시지 엔티티를 생성합니다.</p>
     *
     * @param user 사용자 엔티티
     * @param messageCommand 메시지 명령
     * @return 생성된 메시지 엔티티
     * @author Jaeik
     * @since 2.0.0
     */
    public static Message createMessage(User user, MessageCommand messageCommand) {
        return Message.builder()
                .user(user)
                .decoType(messageCommand.decoType())
                .anonymity(messageCommand.anonymity())
                .content(messageCommand.content())
                .width(messageCommand.width())
                .height(messageCommand.height())
                .build();
    }

    /**
     * <h3>메시지 소유자 확인</h3>
     * <p>주어진 사용자 ID가 메시지의 소유자인지 확인합니다.</p>
     *
     * @param userId 확인할 사용자의 ID
     * @return 소유자이면 true, 아니면 false
     * @author Jaeik
     * @since 2.0.0
     */
    public boolean isOwner(Long userId) {
        if (this.user == null || userId == null) {
            return false;
        }
        return this.user.getId().equals(userId);
    }

    /**
     * <h3>메시지 작성자 ID 조회</h3>
     * <p>메시지 작성자의 사용자 ID를 반환합니다.</p>
     *
     * @return 작성자 사용자 ID
     * @author Jaeik
     * @since 2.0.0
     */
    public Long getUserId() {
        return this.user != null ? this.user.getId() : null;
    }
}

