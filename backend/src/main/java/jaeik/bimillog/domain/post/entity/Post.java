package jaeik.bimillog.domain.post.entity;

import jaeik.bimillog.domain.global.entity.BaseEntity;
import jaeik.bimillog.domain.member.entity.Member;
import jaeik.bimillog.domain.post.application.service.PostAdminService;
import jaeik.bimillog.domain.post.application.service.PostCommandService;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

/**
 * <h2>게시글 엔티티</h2>
 * <p>커뮤니티 게시판의 게시글 정보를 저장하는 엔티티입니다.</p>
 * <p>제목, 내용, 작성자, 조회수, 공지 여부, 캐시 플래그를 관리합니다.</p>
 * <p>MySQL 전문검색 인덱스와 캐시 플래그를 지원합니다.</p>
 *
 * @author Jaeik
 * @version 2.0.0
 */
@Entity
@Getter
@NoArgsConstructor
@SuperBuilder
@Table(indexes = {
        @Index(name = "idx_post_notice_created", columnList = "is_notice, created_at DESC"),
        @Index(name = "idx_post_created", columnList = "created_at"),
})
public class Post extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "post_id")
    private Long id;

    // 익명 지원 Nullable FK 존재하지만 회원삭제시 글 삭제 안됨 명시적 삭제 필요
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id")
    private Member member;

    @NotNull
    @Column(nullable = false, length = 30) // 제목 30자 허용
    private String title;

    @NotNull
    @Column(columnDefinition = "TEXT", nullable = false) // 내용 1000자 허용
    private String content;

    @NotNull
    @Column(nullable = false)
    private int views;

    @NotNull
    @Column(name = "is_notice", nullable = false)
    private boolean isNotice;

    private Integer password;

    /**
     * <h3>게시글 생성</h3>
     * <p>새로운 게시글을 생성하는 정적 팩토리 메서드입니다.</p>
     * <p>조회수는 0, 공지사항은 false로 초기화됩니다.</p>
     * <p>{@link PostCommandService}에서 게시글 작성 시 호출됩니다.</p>
     *
     * @param member       작성자 정보
     * @param title      게시글 제목 (1-30자)
     * @param content    게시글 내용 (HTML 형식, 순수 텍스트 기준 1-1000자, HTML 태그 포함 최대 3000자)
     * @param password   게시글 비밀번호 (선택적)
     * @return 생성된 Post 엔티티
     * @throws IllegalArgumentException 제목이나 내용이 null이거나 빈 문자열인 경우
     * @author Jaeik
     * @since 2.0.0
     */
    public static Post createPost(Member member, String title, String content, Integer password) {
        if (title == null || title.trim().isEmpty()) {
            throw new IllegalArgumentException("게시글 제목은 필수입니다.");
        }
        if (content == null || content.trim().isEmpty()) {
            throw new IllegalArgumentException("게시글 내용은 필수입니다.");
        }

        return Post.builder()
                .member(member)
                .title(title)
                .content(content)
                .views(0)
                .isNotice(false)
                .password(password)
                .build();
    }

    /**
     * <h3>게시글 정보 업데이트</h3>
     * <p>게시글의 제목과 내용을 업데이트합니다.</p>
     * <p>{@link PostCommandService}에서 게시글 수정 시 권한 검증 후 호출됩니다.</p>
     *
     * @param title   새로운 게시글 제목 (1-30자)
     * @param content 새로운 게시글 내용 (HTML 형식, 순수 텍스트 기준 1-1000자, HTML 태그 포함 최대 3000자)
     * @throws IllegalArgumentException 제목이나 내용이 null이거나 빈 문자열인 경우
     * @author Jaeik
     * @since 2.0.0
     */
    public void updatePost(String title, String content) {
        if (title == null || title.trim().isEmpty()) {
            throw new IllegalArgumentException("게시글 제목은 필수입니다.");
        }
        if (content == null || content.trim().isEmpty()) {
            throw new IllegalArgumentException("게시글 내용은 필수입니다.");
        }
        
        this.title = title;
        this.content = content;
    }

    /**
     * <h3>공지사항 설정</h3>
     * <p>게시글을 공지사항으로 설정합니다.</p>
     * <p>{@link PostAdminService}에서 관리자 권한 검증 후 호출됩니다.</p>
     *
     * @author Jaeik
     * @since 2.0.0
     */
    public void setAsNotice() {
        this.isNotice = true;
    }

    /**
     * <h3>공지사항 해제</h3>
     * <p>게시글의 공지사항을 해제합니다.</p>
     * <p>{@link PostAdminService}에서 관리자 권한 검증 후 호출됩니다.</p>
     *
     * @author Jaeik
     * @since 2.0.0
     */
    public void unsetAsNotice() {
        this.isNotice = false;
    }

    /**
     * <h3>작성자 권한 확인</h3>
     * <p>현재 사용자가 게시글의 작성자인지 확인합니다.</p>
     * <p>{@link PostCommandService}에서 게시글 수정/삭제 시 권한 검증에 사용됩니다.</p>
     *
     * @param memberId 확인할 사용자 ID
     * @param password 확인할 password
     * @return 작성자인 경우 true, 아니면 false
     * @author Jaeik
     * @since 2.0.0
     */
    public boolean isAuthor(Long memberId, Integer password) {
        if (this.member != null) {
            return this.member.getId().equals(memberId);
        } else {
            return this.password != null && this.password.equals(password);
        }
    }
}
