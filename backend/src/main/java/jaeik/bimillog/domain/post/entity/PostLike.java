package jaeik.bimillog.domain.post.entity;

import jaeik.bimillog.domain.member.entity.member.Member;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * <h2>게시글 추천 엔티티</h2>
 * <p>사용자와 게시글 간의 추천 관계를 나타내는 연결 엔티티입니다.</p>
 * <p>중복 추천 방지를 위해 user_id + post_id 묶음 인덱스를 사용합니다.</p>
 * <p>CASCADE 옵션으로 사용자나 게시글 삭제 시 자동 삭제됩니다.</p>
 *
 * @author Jaeik
 * @version 2.0.0
 */
@Getter
@Entity
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(indexes = {
        @Index(name = "idx_postlike_member_post", columnList = "member_id, post_id")
})
public class PostLike {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "post_like_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id")
    private Member member;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "post_id")
    private Post post;
}
