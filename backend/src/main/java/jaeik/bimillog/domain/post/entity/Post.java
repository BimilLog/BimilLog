package jaeik.bimillog.domain.post.entity;

import jaeik.bimillog.global.entity.BaseEntity;
import jaeik.bimillog.domain.user.entity.User;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

/**
 * <h2>게시글 엔티티</h2>
 * <p>
 * 커뮤니티 게시판의 게시글 정보를 저장하는 핵심 엔티티
 * </p>
 * <p>제목, 내용, 작성자, 조회수, 공지 여부, 캐시 플래그를 포함한 게시글의 모든 정보를 관리합니다.</p>
 * <p>MySQL 전문검색을 위한 인덱스와 성능 최적화를 위한 캐시 플래그를 지원합니다.</p>
 * <p>게시글 작성, 수정, 삭제 시 PostCommandController에서 생성되어 사용됩니다.</p>
 * <p>게시글 조회 시 PostQueryController와 PostCacheController에서 조회됩니다.</p>
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
        @Index(name = "idx_post_created_at_popular", columnList = "created_at, post_cache_flag"),
        @Index(name = "idx_post_created", columnList = "created_at"),
        @Index(name = "idx_post_popular_flag", columnList = "post_cache_flag"),
})
public class Post extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "post_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @OnDelete(action = OnDeleteAction.CASCADE)
    @JoinColumn(name = "user_id")
    private User user;

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

    @Enumerated(EnumType.STRING)
    @Column(name = "post_cache_flag")
    private PostCacheFlag postCacheFlag;

    private Integer password;

    /**
     * <h3>게시글 생성</h3>
     * <p>새로운 게시글을 생성하는 정적 팩토리 메서드입니다.</p>
     * <p>기본적으로 조회수는 0, 공지사항은 false, 캐시플래그는 null로 초기화됩니다.</p>
     * <p>PostCommandController에서 게시글 작성 요청 시 호출됩니다.</p>
     * <p>PostCommandService의 게시글 생성 로직에서 사용됩니다.</p>
     *
     * @param user       작성자 정보
     * @param title      게시글 제목 (1-30자)
     * @param content    게시글 내용 (1-1000자)
     * @param password   게시글 비밀번호 (선택적)
     * @return 생성된 Post 엔티티
     * @throws IllegalArgumentException 제목이나 내용이 null이거나 빈 문자열인 경우
     * @author Jaeik
     * @since 2.0.0
     */
    public static Post createPost(User user, String title, String content, Integer password) {
        if (title == null || title.trim().isEmpty()) {
            throw new IllegalArgumentException("게시글 제목은 필수입니다.");
        }
        if (content == null || content.trim().isEmpty()) {
            throw new IllegalArgumentException("게시글 내용은 필수입니다.");
        }
        
        return Post.builder()
                .user(user)
                .title(title)
                .content(content)
                .views(0)
                .isNotice(false)
                .password(password)
                .postCacheFlag(null)
                .build();
    }

    /**
     * <h3>게시글 정보 업데이트</h3>
     * <p>게시글의 제목과 내용을 업데이트합니다.</p>
     * <p>PostCommandController에서 게시글 수정 요청 시 호출됩니다.</p>
     * <p>PostCommandService의 게시글 수정 로직에서 권한 검증 후 실행됩니다.</p>
     *
     * @param title   새로운 게시글 제목 (1-30자)
     * @param content 새로운 게시글 내용 (1-1000자)
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
     * <p>AdminCommandController에서 관리자가 공지사항 등록 시 호출됩니다.</p>
     * <p>AdminCommandService의 공지사항 설정 로직에서 관리자 권한 검증 후 실행됩니다.</p>
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
     * <p>AdminCommandController에서 관리자가 공지사항 해제 시 호출됩니다.</p>
     * <p>AdminCommandService의 공지사항 해제 로직에서 관리자 권한 검증 후 실행됩니다.</p>
     *
     * @author Jaeik
     * @since 2.0.0
     */
    public void unsetAsNotice() {
        this.isNotice = false;
    }

    /**
     * <h3>게시글 캐시 플래그 설정</h3>
     * <p>게시글의 캐시 플래그를 설정합니다.</p>
     * <p>PostCacheController에서 인기글 캐시 업데이트 시 호출됩니다.</p>
     * <p>PostCommandService의 배치 작업에서 주간/전설 인기글 선정 시 실행됩니다.</p>
     * <p>REALTIME, WEEKLY, LEGEND, NOTICE 중 하나로 설정하여 게시글 분류에 사용됩니다.</p>
     *
     * @param postCacheFlag 설정할 캐시 플래그 (REALTIME/WEEKLY/LEGEND/NOTICE)
     * @author Jaeik
     * @since 2.0.0
     */
    public void updatePostCacheFlag(PostCacheFlag postCacheFlag) {
        this.postCacheFlag = postCacheFlag;
    }

    /**
     * <h3>작성자 권한 확인</h3>
     * <p>현재 사용자가 게시글의 작성자인지 확인합니다.</p>
     * <p>PostCommandController에서 게시글 수정/삭제 요청 시 권한 검증을 위해 호출됩니다.</p>
     * <p>PostCommandService의 게시글 수정/삭제 로직에서 작성자 검증에 사용됩니다.</p>
     *
     * @param userId 확인할 사용자 ID
     * @return 작성자인 경우 true, 아니면 false
     * @author Jaeik
     * @since 2.0.0
     */
    public boolean isAuthor(Long userId) {
        return this.user != null && this.user.getId().equals(userId);
    }

}
