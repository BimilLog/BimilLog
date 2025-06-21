package jaeik.growfarm.entity.user;

import jaeik.growfarm.entity.BaseEntity;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

/**
 * <h2>블랙리스트 엔티티</h2>
 * <p>사용자를 블랙리스트에 추가하는 엔티티</p>
 * <p>카카오 ID를 기반으로 블랙리스트에 등록</p>
 *
 * @author Jaeik
 * @since 1.0.0
 */
@Entity
@Getter
@SuperBuilder
@NoArgsConstructor
public class BlackList extends BaseEntity {

    @Id
    private Long kakaoId;

    /**
     * <h2>블랙리스트 엔티티 생성</h2>
     * <p>카카오 ID를 기반으로 블랙리스트 엔티티를 생성</p>
     *
     * @param kakaoId 카카오 ID
     * @return 생성된 블랙리스트 엔티티
     */
    public static BlackList createBlackList(Long kakaoId) {
        return BlackList.builder()
                .kakaoId(kakaoId)
                .build();
    }
}