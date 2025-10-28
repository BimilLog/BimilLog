package jaeik.bimillog.domain.comment.entity;

import jaeik.bimillog.domain.global.entity.BaseEntity;
import jaeik.bimillog.domain.member.entity.Member;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * <h2>댓글 추천 엔티티</h2>
 * <p>댓글에 대한 추천 정보를 저장하는 엔티티</p>
 * <p>사용자와 댓글 간의 관계를 나타냄</p>
 *
 * @author Jaeik
 * @version 2.0.0
 */
@Entity
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(
        indexes = {
                @Index(name = "idx_comment_like_member_comment", columnList = "comment_id, member_id")
        },
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_comment_like_member_comment", columnNames = {"comment_id", "member_id"})
        }
)
public class CommentLike extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "comment_like_id")
    private Long id;


    // DB 레벨 CASCADE : Member 삭제 시 CommentLike 자동 삭제
    // JPA cascade 없음: ManyToOne 관계로 Member가 CommentLike 생명주기 관리하지 않음
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id")
    private Member member;

    // DB 레벨 CASCADE : Comment 삭제 시 CommentLike 자동 삭제
    // JPA cascade 없음: ManyToOne 관계로 Comment가 CommentLike 생명주기 관리하지 않음
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "comment_id")
    private Comment comment;
}

