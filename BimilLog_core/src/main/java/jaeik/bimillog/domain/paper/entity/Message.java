package jaeik.bimillog.domain.paper.entity;

import jaeik.bimillog.domain.global.entity.BaseEntity;
import jaeik.bimillog.domain.member.entity.Member;
import jaeik.bimillog.infrastructure.security.MessageEncryptConverter;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

/**
 * <h2>메시지 엔티티</h2>
 * <p>롤링페이퍼의 메시지 정보를 담당하는 엔티티</p>
 * <p>롤링페이퍼에 작성되는 익명 메시지의 모든 정보를 저장</p>
 * <p>메시지 내용은 AES-256으로 암호화되어 저장됨</p>
 * <p>그리드 좌표(x, y)를 통해 롤링페이퍼 상의 위치 관리</p>
 *
 * @author Jaeik
 * @version 2.0.0
 */
@Entity
@Getter
@SuperBuilder
@NoArgsConstructor
@Table(name = "message", uniqueConstraints =
        {@UniqueConstraint(name = "unique_member_x_y", columnNames = {"member_id", "x", "y"})})
public class Message extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "message_id")
    private Long id;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id", nullable = false)
    private Member member;

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
    private int x;

    @NotNull
    @Column(nullable = false)
    private int y;

}

