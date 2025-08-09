package jaeik.growfarm.integration.repository.post;

import jaeik.growfarm.dto.post.FullPostResDTO;
import jaeik.growfarm.dto.post.SimplePostResDTO;
import jaeik.growfarm.entity.comment.Comment;
import jaeik.growfarm.entity.post.Post;
import jaeik.growfarm.entity.post.PostCacheFlag;
import jaeik.growfarm.entity.post.PostLike;
import jaeik.growfarm.entity.user.Users;

import jaeik.growfarm.repository.post.read.PostReadRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;


import java.util.List;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.*;

/**
 * <h2>PostReadRepository 통합 테스트</h2>
 * <p>
 * 실제 MySQL DB를 사용한 게시글 조회 레포지터리 통합 테스트
 * </p>
 *
 * @author Jaeik
 * @version 1.1.0
 */
@DataJpaTest
@Testcontainers
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import({
    jaeik.growfarm.repository.post.read.PostReadRepositoryImpl.class,
    jaeik.growfarm.repository.comment.CommentRepository.class,
    jaeik.growfarm.global.config.QueryDSLConfig.class
})
@DisplayName("PostReadRepository 통합 테스트")
class PostReadRepositoryIntegrationTest {

    @Container
    static MySQLContainer<?> mysql = new MySQLContainer<>("mysql:8.0")
            .withDatabaseName("testdb")
            .withUsername("test")
            .withPassword("test");

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", mysql::getJdbcUrl);
        registry.add("spring.datasource.username", mysql::getUsername);
        registry.add("spring.datasource.password", mysql::getPassword);
        registry.add("spring.datasource.driver-class-name", () -> "com.mysql.cj.jdbc.Driver");
        
        // JPA 설정
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "create-drop");
        registry.add("spring.jpa.show-sql", () -> "true");
        registry.add("spring.jpa.properties.hibernate.format_sql", () -> "true");
        registry.add("spring.jpa.properties.hibernate.use_sql_comments", () -> "true");
        registry.add("spring.jpa.database-platform", () -> "org.hibernate.dialect.MySQLDialect");
    }

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private PostReadRepository postReadRepository;



    private Users testUser1;
    private Users testUser2;
    private Post testPost1;
    private Post testPost2;
    private Post testPost3;

    @BeforeEach
    void setUp() {
        // 테스트 사용자 생성
        testUser1 = Users.builder()
                .userName("testUser1")
                .kakaoId(12345L)
                .build();
        entityManager.persistAndFlush(testUser1);

        testUser2 = Users.builder()
                .userName("testUser2")
                .kakaoId(67890L)
                .build();
        entityManager.persistAndFlush(testUser2);

        // 테스트 게시글 생성
        testPost1 = Post.builder()
                .title("첫 번째 테스트 게시글")
                .content("첫 번째 게시글 내용입니다.")
                .views(100)
                .isNotice(false)
                .postCacheFlag(null)
                .user(testUser1)
                .build();
        entityManager.persistAndFlush(testPost1);

        testPost2 = Post.builder()
                .title("두 번째 테스트 게시글")
                .content("두 번째 게시글 내용입니다.")
                .views(200)
                .isNotice(false)
                .postCacheFlag(PostCacheFlag.WEEKLY)
                .user(testUser2)
                .build();
        entityManager.persistAndFlush(testPost2);

        testPost3 = Post.builder()
                .title("공지사항")
                .content("공지사항 내용입니다.")
                .views(50)
                .isNotice(true)
                .postCacheFlag(null)
                .user(testUser1)
                .build();
        entityManager.persistAndFlush(testPost3);

        // 좋아요 생성 (testPost1에 testUser2가 좋아요)
        PostLike postLike = PostLike.builder()
                .post(testPost1)
                .user(testUser2)
                .build();
        entityManager.persistAndFlush(postLike);

        entityManager.clear();
    }

    @Test
    @DisplayName("게시글 목록 조회 - 성공")
    void findSimplePost_Success() {
        // Given
        Pageable pageable = PageRequest.of(0, 10);

        // When
        Page<SimplePostResDTO> result = postReadRepository.findSimplePost(pageable);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getContent()).hasSize(3); // 공지사항 포함 3개
        assertThat(result.getTotalElements()).isEqualTo(3);
        
        // 최신순 정렬 확인
        assertThat(result.getContent().get(0).getTitle()).isEqualTo("공지사항");
        
        // 좋아요 수 확인 (testPost1에 1개 좋아요)
        SimplePostResDTO firstPost = result.getContent().stream()
                .filter(post -> post.getTitle().equals("첫 번째 테스트 게시글"))
                .findFirst()
                .orElseThrow();
        assertThat(firstPost.getLikes()).isEqualTo(1);
    }

    @Test
    @DisplayName("게시글 목록 조회 - 빈 결과")
    void findSimplePost_EmptyResult() {
        // Given - 모든 게시글 삭제
        entityManager.getEntityManager().createQuery("DELETE FROM PostLike").executeUpdate();
        entityManager.getEntityManager().createQuery("DELETE FROM Post").executeUpdate();
        entityManager.flush();
        
        Pageable pageable = PageRequest.of(0, 10);

        // When
        Page<SimplePostResDTO> result = postReadRepository.findSimplePost(pageable);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getContent()).isEmpty();
        assertThat(result.getTotalElements()).isEqualTo(0);
    }

    @Test
    @DisplayName("게시글 상세 조회 - 성공 (로그인 사용자)")
    void findPostById_Success_LoggedInUser() {
        // Given
        Long postId = testPost1.getId();
        Long userId = testUser2.getId(); // 좋아요를 누른 사용자

        // When
        FullPostResDTO result = postReadRepository.findPostById(postId, userId);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getPostId()).isEqualTo(postId);
        assertThat(result.getTitle()).isEqualTo("첫 번째 테스트 게시글");
        assertThat(result.getContent()).isEqualTo("첫 번째 게시글 내용입니다.");
        assertThat(result.getViews()).isEqualTo(100);
        assertThat(result.getUserName()).isEqualTo("testUser1");
        assertThat(result.isUserLike()).isTrue(); // testUser2가 좋아요를 누름
    }

    @Test
    @DisplayName("게시글 상세 조회 - 성공 (비로그인 사용자)")
    void findPostById_Success_NonLoggedInUser() {
        // Given
        Long postId = testPost2.getId();
        Long userId = null; // 비로그인

        // When
        FullPostResDTO result = postReadRepository.findPostById(postId, userId);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getPostId()).isEqualTo(postId);
        assertThat(result.getTitle()).isEqualTo("두 번째 테스트 게시글");
        assertThat(result.getUserName()).isEqualTo("testUser2");
        assertThat(result.isUserLike()).isFalse(); // 비로그인 사용자는 좋아요 없음
    }

    @Test
    @DisplayName("게시글 상세 조회 - 게시글 없음")
    void findPostById_PostNotFound() {
        // Given
        Long nonExistentPostId = 99999L;
        Long userId = testUser1.getId();

        // When
        FullPostResDTO result = postReadRepository.findPostById(nonExistentPostId, userId);

        // Then
        assertThat(result).isNull();
    }

    @Test
    @DisplayName("게시글 상세 조회 - 익명 사용자 처리")
    void findPostById_AnonymousUser() {
        // Given - 익명 사용자 게시글 생성 (userName이 null인 경우)
        Users anonymousUser = Users.builder()
                .userName(null) // 익명
                .kakaoId(99999L)
                .build();
        entityManager.persistAndFlush(anonymousUser);

        Post anonymousPost = Post.builder()
                .title("익명 게시글")
                .content("익명 사용자의 게시글입니다.")
                .views(10)
                .isNotice(false)
                .user(anonymousUser)
                .build();
        entityManager.persistAndFlush(anonymousPost);
        entityManager.clear();

        // When
        FullPostResDTO result = postReadRepository.findPostById(anonymousPost.getId(), testUser1.getId());

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getUserName()).isEqualTo("익명");
        assertThat(result.getTitle()).isEqualTo("익명 게시글");
    }

    @Test
    @DisplayName("페이징 테스트")
    void findSimplePost_Paging() {
        // Given
        Pageable pageable = PageRequest.of(0, 2); // 첫 번째 페이지, 2개씩

        // When
        Page<SimplePostResDTO> result = postReadRepository.findSimplePost(pageable);

        // Then
        assertThat(result.getContent()).hasSize(2);
        assertThat(result.getTotalElements()).isEqualTo(3);
        assertThat(result.getTotalPages()).isEqualTo(2); // 3개 ÷ 2 = 2페이지
        assertThat(result.getNumber()).isEqualTo(0); // 첫 번째 페이지
        assertThat(result.hasNext()).isTrue();
    }

    @Test
    @DisplayName("댓글 수 계산 테스트")
    void findSimplePost_CommentCounts() {
        // Given - testPost1에 댓글 2개 추가
        Comment comment1 = Comment.builder()
                .content("첫 번째 댓글")
                .user(testUser1)
                .post(testPost1)
                .build();
        entityManager.persistAndFlush(comment1);

        Comment comment2 = Comment.builder()
                .content("두 번째 댓글")
                .user(testUser2)
                .post(testPost1)
                .build();
        entityManager.persistAndFlush(comment2);

        // testPost2에 댓글 1개 추가
        Comment comment3 = Comment.builder()
                .content("세 번째 댓글")
                .user(testUser1)
                .post(testPost2)
                .build();
        entityManager.persistAndFlush(comment3);

        entityManager.clear();

        Pageable pageable = PageRequest.of(0, 10);

        // When
        Page<SimplePostResDTO> result = postReadRepository.findSimplePost(pageable);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getContent()).hasSize(3);

        // testPost1의 댓글 수 확인
        SimplePostResDTO post1 = result.getContent().stream()
                .filter(post -> post.getTitle().equals("첫 번째 테스트 게시글"))
                .findFirst()
                .orElseThrow();
        assertThat(post1.getCommentCount()).isEqualTo(2);

        // testPost2의 댓글 수 확인
        SimplePostResDTO post2 = result.getContent().stream()
                .filter(post -> post.getTitle().equals("두 번째 테스트 게시글"))
                .findFirst()
                .orElseThrow();
        assertThat(post2.getCommentCount()).isEqualTo(1);

        // testPost3(공지사항)의 댓글 수 확인
        SimplePostResDTO post3 = result.getContent().stream()
                .filter(post -> post.getTitle().equals("공지사항"))
                .findFirst()
                .orElseThrow();
        assertThat(post3.getCommentCount()).isEqualTo(0);
    }

    @Test
    @DisplayName("게시글 정렬 순서 테스트 - 최신순")
    void findSimplePost_SortOrder() {
        // Given - 추가 게시글 생성 (시간 간격을 두고)
        try {
            Thread.sleep(100); // 시간 차이를 위한 대기
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        Post newestPost = Post.builder()
                .title("가장 최신 게시글")
                .content("가장 최신 게시글 내용")
                .views(10)
                .isNotice(false)
                .user(testUser1)
                .build();
        entityManager.persistAndFlush(newestPost);
        entityManager.clear();

        Pageable pageable = PageRequest.of(0, 10);

        // When
        Page<SimplePostResDTO> result = postReadRepository.findSimplePost(pageable);

        // Then
        assertThat(result.getContent()).hasSize(4);
        
        // 최신순 정렬 확인 (가장 나중에 생성된 게시글이 첫 번째)
        assertThat(result.getContent().get(0).getTitle()).isEqualTo("가장 최신 게시글");
        assertThat(result.getContent().get(1).getTitle()).isEqualTo("공지사항");
        assertThat(result.getContent().get(2).getTitle()).isEqualTo("두 번째 테스트 게시글");
        assertThat(result.getContent().get(3).getTitle()).isEqualTo("첫 번째 테스트 게시글");
    }

    @Test
    @DisplayName("여러 사용자의 좋아요 테스트")
    void findSimplePost_MultipleLikes() {
        // Given - testPost2에 추가 좋아요 생성
        PostLike postLike2 = PostLike.builder()
                .post(testPost2)
                .user(testUser1)
                .build();
        entityManager.persistAndFlush(postLike2);

        PostLike postLike3 = PostLike.builder()
                .post(testPost2)
                .user(testUser2)
                .build();
        entityManager.persistAndFlush(postLike3);

        entityManager.clear();

        Pageable pageable = PageRequest.of(0, 10);

        // When
        Page<SimplePostResDTO> result = postReadRepository.findSimplePost(pageable);

        // Then
        SimplePostResDTO post1 = result.getContent().stream()
                .filter(post -> post.getTitle().equals("첫 번째 테스트 게시글"))
                .findFirst()
                .orElseThrow();
        assertThat(post1.getLikes()).isEqualTo(1); // 기존 좋아요 1개

        SimplePostResDTO post2 = result.getContent().stream()
                .filter(post -> post.getTitle().equals("두 번째 테스트 게시글"))
                .findFirst()
                .orElseThrow();
        assertThat(post2.getLikes()).isEqualTo(2); // 새로 추가된 좋아요 2개

        SimplePostResDTO post3 = result.getContent().stream()
                .filter(post -> post.getTitle().equals("공지사항"))
                .findFirst()
                .orElseThrow();
        assertThat(post3.getLikes()).isEqualTo(0); // 좋아요 없음
    }

    @Test
    @DisplayName("null 값 처리 테스트")
    void findPostById_NullValueHandling() {
        // Given - views가 null인 게시글 생성
        Post nullViewsPost = Post.builder()
                .title("조회수 null 게시글")
                .content("조회수가 null인 게시글")
                .views(0) // 기본값
                .isNotice(false)
                .user(testUser1)
                .build();
        entityManager.persistAndFlush(nullViewsPost);
        entityManager.clear();

        // When
        FullPostResDTO result = postReadRepository.findPostById(nullViewsPost.getId(), testUser1.getId());

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getViews()).isEqualTo(0); // 기본값 0
        assertThat(result.getTitle()).isEqualTo("조회수 null 게시글");
    }

    @Test
    @DisplayName("존재하지 않는 사용자 ID로 게시글 조회")
    void findPostById_NonExistentUserId() {
        // Given
        Long postId = testPost1.getId();
        Long nonExistentUserId = 99999L;

        // When
        FullPostResDTO result = postReadRepository.findPostById(postId, nonExistentUserId);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getPostId()).isEqualTo(postId);
        assertThat(result.isUserLike()).isFalse(); // 존재하지 않는 사용자는 좋아요 안 함
    }

    @Test
    @DisplayName("대용량 데이터 페이징 테스트")
    void findSimplePost_LargeDataPaging() {
        // Given - 대량의 게시글 생성 (기존 3개 + 새로 100개 = 총 103개)
        List<Post> largePosts = IntStream.range(0, 100)
                .mapToObj(i -> Post.builder()
                        .title("대용량 테스트 게시글 " + i)
                        .content("대용량 테스트 게시글 내용 " + i)
                        .views(i)
                        .isNotice(false)
                        .user(i % 2 == 0 ? testUser1 : testUser2)
                        .build())
                .collect(java.util.stream.Collectors.toList());

        largePosts.forEach(entityManager::persistAndFlush);
        entityManager.clear();

        // When - 첫 번째 페이지 (20개씩)
        Pageable pageable1 = PageRequest.of(0, 20);
        Page<SimplePostResDTO> result1 = postReadRepository.findSimplePost(pageable1);

        // When - 마지막 페이지
        Pageable pageable2 = PageRequest.of(5, 20); // 103개 ÷ 20 = 6페이지 (마지막 페이지는 3개)
        Page<SimplePostResDTO> result2 = postReadRepository.findSimplePost(pageable2);

        // Then
        assertThat(result1.getContent()).hasSize(20);
        assertThat(result1.getTotalElements()).isEqualTo(103);
        assertThat(result1.getTotalPages()).isEqualTo(6);

        assertThat(result2.getContent()).hasSize(3); // 마지막 페이지는 3개
        assertThat(result2.getTotalElements()).isEqualTo(103);
        assertThat(result2.isLast()).isTrue();
    }

    @Test
    @DisplayName("페이지 크기 경계값 테스트")
    void findSimplePost_PageSizeBoundary() {
        // Given
        Pageable pageableMin = PageRequest.of(0, 1); // 최소 페이지 크기
        Pageable pageableMax = PageRequest.of(0, 1000); // 큰 페이지 크기

        // When
        Page<SimplePostResDTO> resultMin = postReadRepository.findSimplePost(pageableMin);
        Page<SimplePostResDTO> resultMax = postReadRepository.findSimplePost(pageableMax);

        // Then
        assertThat(resultMin.getContent()).hasSize(1);
        assertThat(resultMin.getTotalElements()).isEqualTo(3);
        assertThat(resultMin.getTotalPages()).isEqualTo(3);

        assertThat(resultMax.getContent()).hasSize(3); // 전체 데이터가 3개뿐
        assertThat(resultMax.getTotalElements()).isEqualTo(3);
        assertThat(resultMax.getTotalPages()).isEqualTo(1);
    }

    @Test
    @DisplayName("Popular Flag 다양한 값 테스트")
    void findPostById_PopularFlagVariations() {
        // Given - 다양한 PopularFlag를 가진 게시글들 생성
        Post realtimePopularPost = Post.builder()
                .title("실시간 인기 게시글")
                .content("실시간 인기 게시글 내용")
                .views(1000)
                .isNotice(false)
                .postCacheFlag(PostCacheFlag.REALTIME)
                .user(testUser1)
                .build();
        entityManager.persistAndFlush(realtimePopularPost);

        Post legendPopularPost = Post.builder()
                .title("레전드 인기 게시글")
                .content("레전드 인기 게시글 내용")
                .views(5000)
                .isNotice(false)
                .postCacheFlag(PostCacheFlag.LEGEND)
                .user(testUser2)
                .build();
        entityManager.persistAndFlush(legendPopularPost);

        entityManager.clear();

        // When
        FullPostResDTO realtimeResult = postReadRepository.findPostById(realtimePopularPost.getId(), null);
        FullPostResDTO weeklyResult = postReadRepository.findPostById(testPost2.getId(), null); // 기존에 WEEKLY 설정
        FullPostResDTO legendResult = postReadRepository.findPostById(legendPopularPost.getId(), null);

        // Then
        assertThat(realtimeResult.getPostCacheFlag()).isEqualTo(PostCacheFlag.REALTIME);
        assertThat(weeklyResult.getPostCacheFlag()).isEqualTo(PostCacheFlag.WEEKLY);
        assertThat(legendResult.getPostCacheFlag()).isEqualTo(PostCacheFlag.LEGEND);
    }

    @Test
    @DisplayName("공지사항과 일반 게시글 혼재 정렬 테스트")
    void findSimplePost_NoticeAndNormalPostSorting() {
        // Given - 공지사항 추가 생성
        try {
            Thread.sleep(100); // 시간 차이
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        Post additionalNotice = Post.builder()
                .title("추가 공지사항")
                .content("추가 공지사항 내용")
                .views(25)
                .isNotice(true)
                .user(testUser2)
                .build();
        entityManager.persistAndFlush(additionalNotice);

        try {
            Thread.sleep(100); // 시간 차이
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        Post normalPost = Post.builder()
                .title("최신 일반 게시글")
                .content("최신 일반 게시글 내용")
                .views(15)
                .isNotice(false)
                .user(testUser1)
                .build();
        entityManager.persistAndFlush(normalPost);

        entityManager.clear();

        Pageable pageable = PageRequest.of(0, 10);

        // When
        Page<SimplePostResDTO> result = postReadRepository.findSimplePost(pageable);

        // Then
        assertThat(result.getContent()).hasSize(5);
        
        // 최신순 정렬 확인 (공지사항도 최신순으로 정렬됨)
        List<String> titles = result.getContent().stream()
                .map(SimplePostResDTO::getTitle)
                .toList();
        
        assertThat(titles).containsExactly(
                "최신 일반 게시글",      // 가장 최신
                "추가 공지사항",        // 두 번째 최신
                "공지사항",            // 기존 공지사항
                "두 번째 테스트 게시글", // 기존 일반 게시글
                "첫 번째 테스트 게시글"  // 가장 오래된 게시글
        );
    }

    @Test
    @DisplayName("게시글 상세 조회 - 좋아요 수 정확성 테스트")
    void findPostById_LikeCountAccuracy() {
        // Given - testPost1에 여러 사용자가 좋아요
        Users additionalUser1 = Users.builder()
                .userName("추가사용자1")
                .kakaoId(11111L)
                .build();
        entityManager.persistAndFlush(additionalUser1);

        Users additionalUser2 = Users.builder()
                .userName("추가사용자2")
                .kakaoId(22222L)
                .build();
        entityManager.persistAndFlush(additionalUser2);

        PostLike like2 = PostLike.builder()
                .post(testPost1)
                .user(additionalUser1)
                .build();
        entityManager.persistAndFlush(like2);

        PostLike like3 = PostLike.builder()
                .post(testPost1)
                .user(additionalUser2)
                .build();
        entityManager.persistAndFlush(like3);

        entityManager.clear();

        // When
        FullPostResDTO result = postReadRepository.findPostById(testPost1.getId(), testUser1.getId());

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getLikes()).isEqualTo(3); // 기존 1개 + 새로 추가된 2개 = 총 3개
        assertThat(result.isUserLike()).isFalse(); // testUser1은 좋아요를 누르지 않음
    }
}