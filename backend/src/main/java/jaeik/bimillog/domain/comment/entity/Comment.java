package jaeik.bimillog.domain.comment.entity;

import jaeik.bimillog.domain.global.entity.BaseEntity;
import jaeik.bimillog.domain.post.entity.Post;
import jaeik.bimillog.domain.member.entity.member.Member;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

/**
 * <h2>댓글 엔티티</h2>
 * <p>게시글에 대한 댓글 정보를 저장하는 엔티티입니다.</p>
 * <p>댓글 내용은 255자까지 허용합니다.</p>
 * <p>익명 댓글과 회원 댓글을 모두 지원하며, 계층 구조를 가집니다.</p>
 *
 * @author Jaeik
 * @version 2.0.0
 */
@Getter
@Entity
@SuperBuilder
@NoArgsConstructor
@Table(indexes = {
        @Index(name = "idx_comment_post_deleted", columnList = "post_id, deleted"),
        @Index(name = "idx_comment_post_created", columnList = "post_id, created_at DESC")
})
public class Comment extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "comment_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "post_id")
    private Post post;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id")
    private Member member;

    @NotNull
    @Column(nullable = false) // 255자 허용
    private String content;

    @NotNull
    @Column(nullable = false)
    private boolean deleted;

    private Integer password;

    /**
     * <h3>댓글 생성</h3>
     * <p>새로운 댓글을 생성합니다.</p>
     * <p>회원 댓글인 경우 password는 null, 익명 댓글인 경우 user는 null로 설정됩니다.</p>
     *
     * @param post     댓글이 달릴 게시글
     * @param member     댓글 작성자 (익명 댓글인 경우 null)
     * @param content  댓글 내용
     * @param password 댓글 비밀번호 (회원 댓글인 경우 null)
     * @return 생성된 댓글 엔티티
     * @author Jaeik
     * @since 2.0.0
     */
    public static Comment createComment(Post post, Member member, String content, Integer password) {
        return Comment.builder()
                .post(post)
                .member(member)
                .content(content)
                .deleted(false)
                .password(password)
                .build();
    }

    /**
     * <h3>댓글 수정</h3>
     * <p>댓글 내용을 수정합니다.</p>
     *
     * @param content 수정할 댓글 내용
     * @author Jaeik
     * @since 2.0.0
     */
    public void updateComment(String content) {
        this.content = content;
    }

    /**
     * <h3>댓글 소유자 확인</h3>
     * <p>로그인한 사용자가 이 댓글의 소유자인지 확인합니다.</p>
     * <p>익명 댓글의 경우 false를 반환합니다.</p>
     *
     * @param userId 확인할 사용자 ID
     * @return 소유자인 경우 true, 아닌 경우 false
     * @author Jaeik
     * @since 2.0.0
     */
    private boolean isOwner(Long userId) {
        return this.member != null && this.member.getId().equals(userId);
    }

    /**
     * <h3>댓글 비밀번호 일치 확인</h3>
     * <p>익명 댓글의 비밀번호가 일치하는지 확인합니다.</p>
     * <p>회원 댓글의 경우 false를 반환합니다.</p>
     *
     * @param password 확인할 비밀번호
     * @return 비밀번호가 일치하는 경우 true, 아닌 경우 false
     * @author Jaeik
     * @since 2.0.0
     */
    private boolean isPasswordMatch(Integer password) {
        return this.password != null && this.password.equals(password);
    }

    /**
     * <h3>댓글 소프트 삭제</h3>
     * <p>댓글을 소프트 삭제 처리합니다.</p>
     * <p>자손 댓글이 있는 경우 내용을 익명화하고 삭제 플래그를 설정합니다.</p>
     *
     * @author Jaeik
     * @since 2.0.0
     */
    public void softDelete() {
        this.deleted = true;
        this.content = "삭제된 댓글입니다";
    }

    /**
     * <h3>댓글 익명화 처리</h3>
     * <p>사용자 탈퇴 시 댓글을 익명화 처리합니다.</p>
     * <p>사용자 연관관계를 제거하고 내용을 익명화하며 소프트 삭제 처리합니다.</p>
     *
     * @author Jaeik
     * @since 2.0.0
     */
    public void anonymize() {
        this.member = null;
        this.content = "탈퇴한 사용자의 댓글입니다";
        this.deleted = true;
    }

    /**
     * <h3>댓글 수정/삭제 권한 확인</h3>
     * <p>댓글을 수정하거나 삭제할 수 있는 권한이 있는지 확인합니다.</p>
     * <p>회원 댓글: 소유자 확인, 익명 댓글: 비밀번호 확인</p>
     *
     * @param userId 사용자 ID (로그인한 경우)
     * @param password 비밀번호 (익명 댓글의 경우)
     * @return 권한이 있는 경우 true, 없는 경우 false
     * @author Jaeik
     * @since 2.0.0
     */
    public boolean canModify(Long userId, Integer password) {
        // 댓글이 원래 익명 댓글인지 회원 댓글인지 먼저 확인
        if (this.member == null) {
            // 익명 댓글: 비밀번호 검증
            return isPasswordMatch(password);
        } else {
            // 회원 댓글: 소유자 검증
            return isOwner(userId);
        }
    }
}

