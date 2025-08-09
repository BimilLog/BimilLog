package jaeik.growfarm.integration.repository.post;

import jaeik.growfarm.dto.post.SimplePostResDTO;
import jaeik.growfarm.entity.post.Post;
import jaeik.growfarm.entity.user.Users;
import jaeik.growfarm.repository.post.search.PostSearchRepository;
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

import static org.assertj.core.api.Assertions.*;

/**
 * <h2>PostSearchRepository 통합 테스트</h2>
 * <p>
 * 실제 MySQL DB를 사용한 게시글 검색 레포지터리 통합 테스트
 * </p>
 *
 * @author Jaeik
 * @version 1.1.0
 */
@DataJpaTest
@Testcontainers
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import({
    jaeik.growfarm.repository.post.search.PostSearchRepositoryImpl.class,
    jaeik.growfarm.repository.post.search.PostCustomFullTextRepository.class,
    jaeik.growfarm.repository.comment.CommentRepository.class
})
@DisplayName("PostSearchRepository 통합 테스트")
class PostSearchRepositoryIntegrationTest {

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
    private PostSearchRepository postSearchRepository;

    private Users testUser1;
    private Users testUser2;

    @BeforeEach
    void setUp() {
        // 테스트 사용자 생성
        testUser1 = Users.builder()
                .userName("홍길동")
                .kakaoId(12345L)
                .build();
        entityManager.persistAndFlush(testUser1);

        testUser2 = Users.builder()
                .userName("김철수")
                .kakaoId(67890L)
                .build();
        entityManager.persistAndFlush(testUser2);

        // 테스트 게시글 생성 (검색용)
        Post post1 = Post.builder()
                .title("스프링 부트 개발 가이드")
                .content("스프링 부트를 이용한 웹 애플리케이션 개발에 대한 내용입니다.")
                .views(100)
                .isNotice(false)
                .user(testUser1)
                .build();
        entityManager.persistAndFlush(post1);

        Post post2 = Post.builder()
                .title("자바 프로그래밍 기초")
                .content("자바 언어의 기본 문법과 객체지향 프로그래밍을 다룹니다.")
                .views(200)
                .isNotice(false)
                .user(testUser2)
                .build();
        entityManager.persistAndFlush(post2);

        Post post3 = Post.builder()
                .title("데이터베이스 최적화")
                .content("MySQL 데이터베이스 성능 향상을 위한 인덱스 활용법입니다.")
                .views(150)
                .isNotice(false)
                .user(testUser1)
                .build();
        entityManager.persistAndFlush(post3);

        Post post4 = Post.builder()
                .title("React 프론트엔드 개발")
                .content("React를 사용한 현대적인 프론트엔드 개발 방법론입니다.")
                .views(80)
                .isNotice(false)
                .user(testUser2)
                .build();
        entityManager.persistAndFlush(post4);

        entityManager.clear();
    }

    @Test
    @DisplayName("제목 검색 - 성공")
    void searchPosts_TitleSearch_Success() {
        // Given
        String keyword = "스프링";
        String searchType = "TITLE";
        Pageable pageable = PageRequest.of(0, 10);

        // When
        Page<SimplePostResDTO> result = postSearchRepository.searchPosts(keyword, searchType, pageable);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).getTitle()).contains("스프링");
        assertThat(result.getContent().get(0).getUserName()).isEqualTo("홍길동");
    }

    @Test
    @DisplayName("제목+내용 검색 - 성공")
    void searchPosts_TitleContentSearch_Success() {
        // Given
        String keyword = "프로그래밍";
        String searchType = "TITLE_CONTENT";
        Pageable pageable = PageRequest.of(0, 10);

        // When
        Page<SimplePostResDTO> result = postSearchRepository.searchPosts(keyword, searchType, pageable);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getContent()).hasSizeGreaterThanOrEqualTo(1);
        
        // 제목이나 내용에 "프로그래밍"이 포함된 게시글 확인
        boolean hasMatchingPost = result.getContent().stream()
                .anyMatch(post -> post.getTitle().contains("프로그래밍"));
        assertThat(hasMatchingPost).isTrue();
    }

    @Test
    @DisplayName("작성자 검색 - 성공")
    void searchPosts_AuthorSearch_Success() {
        // Given
        String keyword = "홍길동";
        String searchType = "AUTHOR";
        Pageable pageable = PageRequest.of(0, 10);

        // When
        Page<SimplePostResDTO> result = postSearchRepository.searchPosts(keyword, searchType, pageable);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getContent()).hasSize(2); // 홍길동이 작성한 게시글 2개
        
        // 모든 결과가 홍길동이 작성한 것인지 확인
        result.getContent().forEach(post -> 
            assertThat(post.getUserName()).isEqualTo("홍길동")
        );
    }

    @Test
    @DisplayName("검색 - 결과 없음")
    void searchPosts_NoResults() {
        // Given
        String keyword = "존재하지않는검색어";
        String searchType = "TITLE";
        Pageable pageable = PageRequest.of(0, 10);

        // When
        Page<SimplePostResDTO> result = postSearchRepository.searchPosts(keyword, searchType, pageable);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getContent()).isEmpty();
        assertThat(result.getTotalElements()).isEqualTo(0);
    }

    @Test
    @DisplayName("검색 - 빈 키워드")
    void searchPosts_EmptyKeyword() {
        // Given
        String keyword = "";
        String searchType = "TITLE";
        Pageable pageable = PageRequest.of(0, 10);

        // When
        Page<SimplePostResDTO> result = postSearchRepository.searchPosts(keyword, searchType, pageable);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getContent()).isEmpty();
        assertThat(result.getTotalElements()).isEqualTo(0);
    }

    @Test
    @DisplayName("검색 - null 키워드")
    void searchPosts_NullKeyword() {
        // Given
        String keyword = null;
        String searchType = "TITLE";
        Pageable pageable = PageRequest.of(0, 10);

        // When
        Page<SimplePostResDTO> result = postSearchRepository.searchPosts(keyword, searchType, pageable);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getContent()).isEmpty();
        assertThat(result.getTotalElements()).isEqualTo(0);
    }

    @Test
    @DisplayName("검색 - 잘못된 검색 타입")
    void searchPosts_InvalidSearchType() {
        // Given
        String keyword = "테스트";
        String searchType = "INVALID_TYPE";
        Pageable pageable = PageRequest.of(0, 10);

        // When & Then
        assertThatThrownBy(() -> 
            postSearchRepository.searchPosts(keyword, searchType, pageable)
        ).isInstanceOf(Exception.class); // CustomException 예상
    }

    @Test
    @DisplayName("검색 결과 페이징 - 성공")
    void searchPosts_Paging_Success() {
        // Given - 추가 게시글 생성 (총 6개가 되도록)
        for (int i = 1; i <= 2; i++) {
            Post additionalPost = Post.builder()
                    .title("개발 가이드 " + i)
                    .content("추가 개발 가이드 내용 " + i)
                    .views(10 * i)
                    .isNotice(false)
                    .user(testUser1)
                    .build();
            entityManager.persistAndFlush(additionalPost);
        }
        entityManager.clear();

        String keyword = "개발";
        String searchType = "TITLE";
        Pageable pageable = PageRequest.of(0, 2); // 첫 페이지, 2개씩

        // When
        Page<SimplePostResDTO> result = postSearchRepository.searchPosts(keyword, searchType, pageable);

        // Then
        assertThat(result.getContent()).hasSize(2);
        assertThat(result.getTotalElements()).isEqualTo(3); // "개발"이 포함된 게시글 총 3개
        assertThat(result.hasNext()).isTrue();
        assertThat(result.getNumber()).isEqualTo(0);
    }

    @Test
    @DisplayName("검색 결과 정렬 - 최신순")
    void searchPosts_OrderBy_CreatedAtDesc() {
        // Given
        String keyword = "개발";
        String searchType = "TITLE_CONTENT";
        Pageable pageable = PageRequest.of(0, 10);

        // When
        Page<SimplePostResDTO> result = postSearchRepository.searchPosts(keyword, searchType, pageable);

        // Then
        assertThat(result.getContent()).isNotEmpty();
        
        // 최신순 정렬 확인 (createdAt 기준 내림차순)
        if (result.getContent().size() > 1) {
            for (int i = 0; i < result.getContent().size() - 1; i++) {
                SimplePostResDTO current = result.getContent().get(i);
                SimplePostResDTO next = result.getContent().get(i + 1);
                assertThat(current.getCreatedAt()).isAfterOrEqualTo(next.getCreatedAt());
            }
        }
    }
}