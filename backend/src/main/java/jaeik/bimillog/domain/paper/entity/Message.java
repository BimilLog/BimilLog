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
 * <p>
 * 롤링페이퍼의 메시지 정보를 담당하는 엔티티
 * </p>
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

    /**
     * <h3>메시지 생성 팩토리 메소드</h3>
     * <p>새로운 롤링페이퍼 메시지 엔티티를 생성합니다.</p>
     * <p>유효성 검증은 DTO 레벨에서 수행되므로 이미 검증된 데이터로 엔티티를 생성합니다.</p>
     * <p>
     * 사용자가 롤링페이퍼에 메시지를 작성하여 제출 버튼을 누른 상황에서,
     * PaperCommandService가 메시지 데이터를 받아 엔티티로 변환하기 위해 호출하는 메서드입니다.
     * 생성된 엔티티는 AES-256 암호화를 통해 content가 자동으로 암호화되어 저장됩니다.
     * </p>
     *
     * @param member 롤링페이퍼 소유자 사용자 엔티티
     * @param decoType 메시지 장식 스타일
     * @param anonymity 익명 작성자 이름
     * @param content 메시지 내용 (자동 암호화됨)
     * @param x 그리드 레이아웃에서의 x 좌표
     * @param y 그리드 레이아웃에서의 y 좌표
     * @return 생성된 메시지 엔티티
     * @author Jaeik
     * @since 2.0.0
     */
    public static Message createMessage(Member member, DecoType decoType, String anonymity,
                                        String content, int x, int y) {
        return Message.builder()
                .member(member)
                .decoType(decoType)
                .anonymity(anonymity)
                .content(content)
                .x(x)
                .y(y)
                .build();
    }

    /**
     * <h3>메시지 작성자 ID 조회</h3>
     * <p>메시지 작성자의 회원 ID를 반환합니다.</p>
     * <p>MessageDetail과 VisitMessageDetail에서 메시지 변환 시 호출되는 메서드</p>
     *
     * @return 작성자 회원 ID
     * @author Jaeik
     * @since 2.0.0
     */
    public Long getMemberId() {
        return this.member != null ? this.member.getId() : null;
    }
}

