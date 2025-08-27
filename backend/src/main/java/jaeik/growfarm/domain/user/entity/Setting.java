package jaeik.growfarm.domain.user.entity;

import jakarta.persistence.*;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

/**
 * <h2>설정 엔티티</h2>
 * <p>
 * 사용자의 알림 설정을 저장하는 엔티티
 * </p>
 * <p>
 * 농장, 댓글, 게시글 추천, 댓글 추천에 대한 알림 설정을 포함
 * </p>
 *
 * @author Jaeik
 * @since 2.0.0
 */
@Entity
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Builder
public class Setting {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY) // PK 번호
    @Column(name = "setting_id")
    private Long id;

    // 롤링페이퍼에 메시지가 달렸을 때 FCM 알림 여부 SSE는 항상 전송됨
    @Builder.Default
    @Column(nullable = false, columnDefinition = "TINYINT(1) DEFAULT 1")
    private boolean messageNotification = true;

    // 글에 댓글이 달렸을 때 FCM 알림 여부 SSE는 항상 전송됨
    @Builder.Default
    @Column(nullable = false, columnDefinition = "TINYINT(1) DEFAULT 1")
    private boolean commentNotification = true;

    // 글이 인기글이 되었을 때 FCM 알림 여부 SSE는 항상 전송됨 (실시간 인기글은 해당 안됨, 주간, 전설 인기글만 전송됨)
    @Builder.Default
    @Column(nullable = false, columnDefinition = "TINYINT(1) DEFAULT 1")
    private boolean postFeaturedNotification = true;

    /**
     * <h3>설정 업데이트</h3>
     * <p>주어진 VO를 사용하여 알림 설정을 업데이트합니다.</p>
     *
     * @param settingVO 업데이트할 설정 정보 값 객체
     * @author Jaeik
     * @since 2.0.0
     */
    public void updateSetting(SettingVO settingVO) {
        messageNotification = settingVO.messageNotification();
        commentNotification = settingVO.commentNotification();
        postFeaturedNotification = settingVO.postFeaturedNotification();
    }

    /**
     * <h3>SettingVO로 변환</h3>
     * <p>현재 설정 엔티티를 SettingVO로 변환합니다.</p>
     *
     * @return 설정 정보를 담은 SettingVO 객체
     * @author Jaeik  
     * @since 2.0.0
     */
    public SettingVO toSettingVO() {
        return SettingVO.of(messageNotification, commentNotification, postFeaturedNotification);
    }

    /**
     * <h3>기본 설정 생성</h3>
     * <p>기본값(모두 true)으로 새로운 설정 엔티티를 생성합니다.</p>
     *
     * @return 기본 설정 엔티티
     * @author Jaeik
     * @since 2.0.0
     */
    public static Setting createSetting() {
        return Setting.builder()
                .messageNotification(true)
                .commentNotification(true)
                .postFeaturedNotification(true)
                .build();
    }
}
