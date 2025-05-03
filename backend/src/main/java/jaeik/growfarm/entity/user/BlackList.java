package jaeik.growfarm.entity.user;

import jaeik.growfarm.repository.BaseEntity;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

// 블랙리스트 엔티티
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
