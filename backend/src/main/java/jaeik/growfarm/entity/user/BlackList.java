package jaeik.growfarm.entity.user;

import jaeik.growfarm.repository.BaseEntity;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
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
    @GeneratedValue(strategy = GenerationType.IDENTITY) // PK 번호
    @Column(name = "blacklist_id")
    private Long id;

    @NotNull
    @Column(name = "kakao_id", unique = true, nullable = false)
    private Long kakaoId;
}
