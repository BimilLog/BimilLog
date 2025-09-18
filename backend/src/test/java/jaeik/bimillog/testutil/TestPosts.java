package jaeik.bimillog.testutil;

import jaeik.bimillog.domain.post.entity.Post;
import jaeik.bimillog.domain.post.entity.PostCacheFlag;
import jaeik.bimillog.domain.user.entity.User;

/**
 * <h2>미리 정의된 테스트 게시글 인스턴스</h2>
 * <p>테스트에서 바로 사용할 수 있는 사전 정의된 게시글 객체들</p>
 * <p>성능 향상 및 코드 간소화를 위해 객체 재사용</p>
 *
 * @author Jaeik
 * @version 2.0.0
 */
public class TestPosts {

    // 미리 정의된 게시글 인스턴스들
    public static final Post NORMAL_POST;
    public static final Post POPULAR_POST;
    public static final Post NOTICE_POST;
    public static final Post PASSWORD_POST;
    public static final Post LEGEND_POST;
    public static final Post ADMIN_POST;
    public static final Post ANONYMOUS_POST;

    static {
        // 일반 게시글
        NORMAL_POST = Post.builder()
                .id(1L)
                .title("일반 게시글 제목")
                .content("일반 게시글 내용입니다.")
                .user(TestUsers.USER1)
                .views(10)
                .isNotice(false)
                .password(null)
                .postCacheFlag(null)
                .build();

        // 인기 게시글
        POPULAR_POST = Post.builder()
                .id(2L)
                .title("인기 게시글 제목")
                .content("인기 게시글 내용입니다. 많은 사람들이 좋아하는 글입니다.")
                .user(TestUsers.USER2)
                .views(1500)
                .isNotice(false)
                .password(null)
                .postCacheFlag(PostCacheFlag.WEEKLY)
                .build();

        // 공지사항 게시글
        NOTICE_POST = Post.builder()
                .id(3L)
                .title("[공지] 중요한 공지사항")
                .content("시스템 점검 안내입니다.")
                .user(TestUsers.ADMIN)
                .views(500)
                .isNotice(true)
                .password(null)
                .postCacheFlag(PostCacheFlag.NOTICE)
                .build();

        // 비밀번호 게시글
        PASSWORD_POST = Post.builder()
                .id(4L)
                .title("비밀번호가 있는 게시글")
                .content("비밀번호로 보호되는 게시글입니다.")
                .user(null) // 익명 게시글
                .views(25)
                .isNotice(false)
                .password(1234)
                .postCacheFlag(null)
                .build();

        // 전설적인 게시글
        LEGEND_POST = Post.builder()
                .id(5L)
                .title("전설의 게시글")
                .content("모든 시대를 통틀어 가장 인기있는 게시글입니다.")
                .user(TestUsers.USER3)
                .views(50000)
                .isNotice(false)
                .password(null)
                .postCacheFlag(PostCacheFlag.LEGEND)
                .build();

        // 관리자 게시글
        ADMIN_POST = Post.builder()
                .id(6L)
                .title("관리자 게시글")
                .content("관리자가 작성한 일반 게시글입니다.")
                .user(TestUsers.ADMIN)
                .views(100)
                .isNotice(false)
                .password(null)
                .postCacheFlag(null)
                .build();

        // 익명 게시글
        ANONYMOUS_POST = Post.builder()
                .id(7L)
                .title("익명 게시글")
                .content("익명으로 작성된 게시글입니다.")
                .user(null)
                .views(5)
                .isNotice(false)
                .password(null)
                .postCacheFlag(null)
                .build();
    }

    /**
     * 특정 ID를 가진 게시글 생성
     */
    public static Post withId(Long id) {
        return Post.builder()
                .id(id)
                .title(NORMAL_POST.getTitle())
                .content(NORMAL_POST.getContent())
                .user(NORMAL_POST.getUser())
                .views(NORMAL_POST.getViews())
                .isNotice(NORMAL_POST.isNotice())
                .password(NORMAL_POST.getPassword())
                .postCacheFlag(NORMAL_POST.getPostCacheFlag())
                .build();
    }

    /**
     * 기존 게시글을 복사하며 특정 ID 설정
     */
    public static Post copyWithId(Post post, Long id) {
        return Post.builder()
                .id(id)
                .title(post.getTitle())
                .content(post.getContent())
                .user(post.getUser())
                .views(post.getViews())
                .isNotice(post.isNotice())
                .password(post.getPassword())
                .postCacheFlag(post.getPostCacheFlag())
                .build();
    }

    /**
     * 특정 사용자의 게시글 생성
     */
    public static Post withUser(User user) {
        return Post.builder()
                .title(NORMAL_POST.getTitle())
                .content(NORMAL_POST.getContent())
                .user(user)
                .views(NORMAL_POST.getViews())
                .isNotice(NORMAL_POST.isNotice())
                .password(NORMAL_POST.getPassword())
                .postCacheFlag(NORMAL_POST.getPostCacheFlag())
                .build();
    }

    /**
     * 특정 제목과 내용을 가진 게시글 생성
     */
    public static Post withTitleAndContent(String title, String content) {
        return Post.builder()
                .title(title)
                .content(content)
                .user(NORMAL_POST.getUser())
                .views(0)
                .isNotice(false)
                .password(null)
                .postCacheFlag(null)
                .build();
    }

    /**
     * 특정 비밀번호를 가진 게시글 생성
     */
    public static Post withPassword(Integer password) {
        return Post.builder()
                .title("비밀번호 게시글")
                .content("비밀번호로 보호되는 게시글")
                .user(null)
                .views(0)
                .isNotice(false)
                .password(password)
                .postCacheFlag(null)
                .build();
    }

    /**
     * 특정 조회수를 가진 게시글 생성
     */
    public static Post withViews(int views) {
        return Post.builder()
                .title(NORMAL_POST.getTitle())
                .content(NORMAL_POST.getContent())
                .user(NORMAL_POST.getUser())
                .views(views)
                .isNotice(NORMAL_POST.isNotice())
                .password(NORMAL_POST.getPassword())
                .postCacheFlag(NORMAL_POST.getPostCacheFlag())
                .build();
    }

    /**
     * 공지사항 게시글 생성
     */
    public static Post asNotice() {
        return Post.builder()
                .title("[공지] 공지사항")
                .content("공지사항 내용")
                .user(TestUsers.ADMIN)
                .views(0)
                .isNotice(true)
                .password(null)
                .postCacheFlag(PostCacheFlag.NOTICE)
                .build();
    }

    /**
     * 특정 캐시 플래그를 가진 게시글 생성
     */
    public static Post withCacheFlag(PostCacheFlag cacheFlag) {
        return Post.builder()
                .title(NORMAL_POST.getTitle())
                .content(NORMAL_POST.getContent())
                .user(NORMAL_POST.getUser())
                .views(NORMAL_POST.getViews())
                .isNotice(NORMAL_POST.isNotice())
                .password(NORMAL_POST.getPassword())
                .postCacheFlag(cacheFlag)
                .build();
    }

    /**
     * 커스텀 게시글 생성 (모든 속성 지정)
     */
    public static Post custom(String title, String content, User user, int views,
                             boolean isNotice, Integer password, PostCacheFlag cacheFlag) {
        return Post.builder()
                .title(title)
                .content(content)
                .user(user)
                .views(views)
                .isNotice(isNotice)
                .password(password)
                .postCacheFlag(cacheFlag)
                .build();
    }

    // Private constructor to prevent instantiation
    private TestPosts() {}
}