package jaeik.bimillog.domain.paper.entity;

import jaeik.bimillog.global.entity.BaseEntity;
import jaeik.bimillog.domain.user.entity.User;
import jaeik.bimillog.infrastructure.security.MessageEncryptConverter;
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
 * @version 2.0.0
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
     * <p>유효성 검증은 DTO 레벨에서 수행됩니다.</p>
     *
     * @param user 사용자 엔티티
     * @param decoType 데코레이션 타입
     * @param anonymity 익명 이름
     * @param content 메시지 내용
     * @param width 메시지 너비
     * @param height 메시지 높이
     * @return 생성된 메시지 엔티티
     * @author Jaeik
     * @since 2.0.0
     */
    public static Message createMessage(User user, DecoType decoType, String anonymity, 
                                      String content, int width, int height) {
        return Message.builder()
                .user(user)
                .decoType(decoType)
                .anonymity(anonymity)
                .content(content)
                .width(width)
                .height(height)
                .build();
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

    /**
     * <h3>메시지 삭제 권한 확인</h3>
     * <p>주어진 사용자 ID가 이 메시지를 삭제할 권한이 있는지 확인합니다.</p>
     *
     * @param userId 삭제 요청 사용자 ID
     * @return 삭제 권한 여부
     * @author Jaeik
     * @since 2.0.0
     */
    public boolean canBeDeletedBy(Long userId) {
        return this.user != null && this.user.getId().equals(userId);
    }
}

