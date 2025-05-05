package jaeik.growfarm.entity.crop;

import jaeik.growfarm.entity.user.Users;
import jaeik.growfarm.repository.BaseEntity;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

// 농작물 엔티티
@Entity
@Getter
@SuperBuilder
@NoArgsConstructor
@Table(name = "crop", uniqueConstraints =
        {@UniqueConstraint(name = "unique_user_x_y", columnNames = {"user_id", "width", "height"})})
public class Crop extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "crop_id")
    private Long id;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY)
    @OnDelete(action = OnDeleteAction.CASCADE)
    @JoinColumn(name = "user_id", nullable = false)
    private Users users;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "crop_type", nullable = false)
    private CropType cropType;

    @NotNull
    @Column(nullable = false)
    private String nickname;

    @NotNull
    @Column(nullable = false)
    private String message;

    @NotNull
    @Column(nullable = false)
    private int width;

    @NotNull
    @Column(nullable = false)
    private int height;
}