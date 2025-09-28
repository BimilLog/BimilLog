package jaeik.bimillog.domain.auth.entity;

import jaeik.bimillog.domain.global.entity.BaseEntity;
import jaeik.bimillog.domain.user.entity.SocialProvider;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

/**
 * <h2>블랙리스트 엔티티</h2>
 * <p>사용자를 블랙리스트에 추가하는 엔티티</p>
 * <p>카카오 ID를 기반으로 블랙리스트에 등록</p>
 *
 * @author Jaeik
 * @since 2.0.0
 */
@Entity
@Getter
@SuperBuilder
@NoArgsConstructor
@Table(name = "blacklist", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"provider", "social_id"})
})
public class BlackList extends BaseEntity{

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "provider", nullable = false)
    @Enumerated(EnumType.STRING)
    private SocialProvider provider;

    @Column(name = "social_id", nullable = false)
    private String socialId;

    /**
     * <h2>블랙리스트 엔티티 생성</h2>
     * <p>소셜 ID와 제공자 정보를 기반으로 블랙리스트 엔티티를 생성</p>
     *
     * @param socialId 소셜 ID
     * @param provider 제공자
     * @return 생성된 블랙리스트 엔티티
     */
    public static BlackList createBlackList(String socialId, SocialProvider provider) {
        return BlackList.builder()
                .socialId(socialId)
                .provider(provider)
                .build();
    }
}