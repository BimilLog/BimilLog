package jaeik.bimillog.domain.post.entity.jpa;

import jaeik.bimillog.domain.global.entity.BaseEntity;
import jaeik.bimillog.domain.member.entity.Member;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * <h2>게시글 추천 엔티티</h2>
 * <p>사용자와 게시글 간의 추천 관계를 나타내는 연결 엔티티입니다.</p>
 * <p>중복 추천 방지를 위해 member_id + post_id UNIQUE 제약조건을 사용합니다.</p>
 * <p>DB CASCADE (V2.5): Post 삭제 시 자동 삭제, Member FK 제거로 통계 보존</p>
 *
 * @author Jaeik
 * @version 2.0.0
 */
@Getter
@Entity
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(uniqueConstraints = {@UniqueConstraint(name = "uk_postlike_member_post", columnNames = {"member_id", "post_id"})})
public class PostLike extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "post_like_id")
    private Long id;

    // DB 레벨 CASCADE : Member 삭제 시 PostLike 자동 삭제
    // JPA cascade 없음 : ManyToOne 관계로 Member가 PostLike 생명주기 관리하지 않음
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id")
    private Member member;

    // DB 레벨 CASCADE : Post 삭제 시 PostLike 자동 삭제
    // JPA cascade 없음 : ManyToOne 관계로 Post가 PostLike 생명주기 관리하지 않음
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "post_id")
    private Post post;
}
