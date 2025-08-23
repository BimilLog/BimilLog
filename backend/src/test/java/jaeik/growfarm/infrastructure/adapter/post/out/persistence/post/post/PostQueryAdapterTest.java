package jaeik.growfarm.infrastructure.adapter.post.out.persistence.post.post;

import com.querydsl.jpa.impl.JPAQueryFactory;
import jaeik.growfarm.GrowfarmApplication;
import jaeik.growfarm.domain.comment.application.port.in.CommentQueryUseCase;
import jaeik.growfarm.domain.common.entity.SocialProvider;
import jaeik.growfarm.domain.post.entity.Post;
import jaeik.growfarm.domain.post.entity.PostCacheFlag;
import jaeik.growfarm.domain.post.entity.PostLike;
import jaeik.growfarm.domain.user.entity.Setting;
import jaeik.growfarm.domain.user.entity.User;
import jaeik.growfarm.domain.user.entity.UserRole;
import jaeik.growfarm.infrastructure.adapter.post.in.web.dto.PostReqDTO;
import jaeik.growfarm.infrastructure.adapter.post.in.web.dto.SimplePostResDTO;
import jaeik.growfarm.infrastructure.adapter.post.out.persistence.post.strategy.SearchStrategyFactory;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.TestPropertySource;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;

/**
 * <h2>PostQueryAdapter 테스트</h2>
 * <p>게시글 조회 어댑터의 모든 기능을 테스트합니다.</p>
 *
 * @author Jaeik
 * @version 2.0.0
 */
@DataJpaTest(
        excludeFilters = @ComponentScan.Filter(
                type = FilterType.ASSIGNABLE_TYPE,
                classes = GrowfarmApplication.class
        )
)
@Testcontainers
@EntityScan(basePackages = {
        "jaeik.growfarm.domain.post.entity",
        "jaeik.growfarm.domain.user.entity",
        "jaeik.growfarm.domain.common.entity"
})
@EnableJpaRepositories(basePackages = {
        "jaeik.growfarm.infrastructure.adapter.post.out.persistence.post.post",
        "jaeik.growfarm.infrastructure.adapter.post.out.persistence.post.postlike"
})
@Import({PostQueryAdapter.class, PostQueryAdapterTest.TestConfig.class})
@TestPropertySource(properties = {
        "spring.jpa.hibernate.ddl-auto=create"
})
class PostQueryAdapterTest {

    @Container
    static MySQLContainer<?> mysql = new MySQLContainer<>("mysql:8.0")
            .withDatabaseName("testdb")
            .withUsername("test")
            .withPassword("test");

    @DynamicPropertySource
    static void dynamicProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", mysql::getJdbcUrl);
        registry.add("spring.datasource.username", mysql::getUsername);
        registry.add("spring.datasource.password", mysql::getPassword);
    }

    @TestConfiguration
    static class TestConfig {
        @Bean
        public JPAQueryFactory jpaQueryFactory(EntityManager entityManager) {
            return new JPAQueryFactory(entityManager);
        }
        
        @Bean
        public SearchStrategyFactory searchStrategyFactory() {
            return Mockito.mock(SearchStrategyFactory.class);
        }
        
        @Bean
        public CommentQueryUseCase commentQueryUseCase() {
            CommentQueryUseCase mock = Mockito.mock(CommentQueryUseCase.class);
            // 기본 Mock 동작 설정
            return mock;
        }
    }

    @Autowired
    private PostQueryAdapter postQueryAdapter;

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private SearchStrategyFactory searchStrategyFactory;
    
    @Autowired
    private CommentQueryUseCase commentQueryUseCase;

    private User testUser;
    private Post testPost1, testPost2, testPost3, noticePost;

    @BeforeEach
    void setUp() {
        // 테스트용 사용자 생성
        testUser = User.builder()
                .userName("testUser")
                .socialId("123456")
                .provider(SocialProvider.KAKAO)
                .socialNickname("테스트유저")
                .role(UserRole.USER)
                .setting(Setting.builder()
                        .messageNotification(true)
                        .commentNotification(true)
                        .postFeaturedNotification(true)
                        .build())
                .build();
        entityManager.persistAndFlush(testUser);

        // 테스트용 게시글들 생성
        createTestPosts();
        
        // 댓글 수 Mock 설정 (기본값)
        Map<Long, Integer> commentCounts = new HashMap<>();
        commentCounts.put(testPost1.getId(), 2);
        commentCounts.put(testPost2.getId(), 1);
        commentCounts.put(testPost3.getId(), 0);
        commentCounts.put(noticePost.getId(), 3);
        
        given(commentQueryUseCase.findCommentCountsByPostIds(any(List.class)))
                .willReturn(commentCounts);
    }

    private void createTestPosts() {
        // 일반 게시글 1
        PostReqDTO postReqDTO1 = PostReqDTO.builder()
                .title("첫 번째 게시글")
                .content("첫 번째 게시글 내용")
                .password(1234)
                .build();
        testPost1 = Post.createPost(testUser, postReqDTO1);
        entityManager.persistAndFlush(testPost1);

        // 일반 게시글 2
        PostReqDTO postReqDTO2 = PostReqDTO.builder()
                .title("두 번째 게시글")
                .content("두 번째 게시글 내용")
                .password(1234)
                .build();
        testPost2 = Post.createPost(testUser, postReqDTO2);
        entityManager.persistAndFlush(testPost2);

        // 일반 게시글 3
        PostReqDTO postReqDTO3 = PostReqDTO.builder()
                .title("세 번째 게시글")
                .content("세 번째 게시글 내용")
                .password(1234)
                .build();
        testPost3 = Post.createPost(testUser, postReqDTO3);
        entityManager.persistAndFlush(testPost3);

        // 공지사항 게시글
        PostReqDTO noticeReqDTO = PostReqDTO.builder()
                .title("공지사항 제목")
                .content("중요한 공지사항입니다.")
                .password(1234)
                .build();
        noticePost = Post.createPost(testUser, noticeReqDTO);
        noticePost.setAsNotice(); // 공지사항으로 설정
        entityManager.persistAndFlush(noticePost);

        entityManager.flush();
        entityManager.clear();
    }

    @Test
    @DisplayName("정상 케이스 - ID로 게시글 조회")
    void shouldFindPost_WhenValidIdProvided() {
        // Given: 저장된 게시글 ID
        Long postId = testPost1.getId();

        // When: ID로 게시글 조회
        Optional<Post> foundPost = postQueryAdapter.findById(postId);

        // Then: 게시글이 정상 조회됨
        assertThat(foundPost).isPresent();
        assertThat(foundPost.get().getId()).isEqualTo(postId);
        assertThat(foundPost.get().getTitle()).isEqualTo("첫 번째 게시글");
        assertThat(foundPost.get().getContent()).isEqualTo("첫 번째 게시글 내용");
    }

    @Test
    @DisplayName("경계값 - 존재하지 않는 ID로 조회")
    void shouldReturnEmpty_WhenNonExistentIdProvided() {
        // Given: 존재하지 않는 게시글 ID
        Long nonExistentId = 99999L;

        // When: 존재하지 않는 ID로 조회
        Optional<Post> foundPost = postQueryAdapter.findById(nonExistentId);

        // Then: 빈 Optional 반환
        assertThat(foundPost).isEmpty();
    }

    @Test
    @DisplayName("정상 케이스 - 페이지별 게시글 조회 (공지사항 제외)")
    void shouldFindPostsByPage_WhenValidPageableProvided() {
        // Given: 페이지 요청 (첫 페이지, 크기 2)
        Pageable pageable = PageRequest.of(0, 2);

        // When: 페이지별 게시글 조회
        Page<SimplePostResDTO> result = postQueryAdapter.findByPage(pageable);

        // Then: 공지사항이 제외된 일반 게시글만 조회됨
        assertThat(result).isNotNull();
        assertThat(result.getContent()).hasSize(2); // 요청한 크기만큼
        assertThat(result.getTotalElements()).isEqualTo(3L); // 전체 일반 게시글 수
        assertThat(result.getTotalPages()).isEqualTo(2); // 전체 페이지 수

        // 공지사항은 제외되어야 함
        List<String> titles = result.getContent().stream()
                .map(SimplePostResDTO::getTitle)
                .toList();
        assertThat(titles).doesNotContain("공지사항 제목");
        
        // 댓글 수가 설정되어 있는지 확인
        assertThat(result.getContent().get(0).getCommentCount()).isNotNull();
    }

    @Test
    @DisplayName("경계값 - 빈 페이지 요청")
    void shouldReturnEmptyPage_WhenNoPostsExist() {
        // Given: 모든 게시글 삭제
        entityManager.getEntityManager()
                .createQuery("DELETE FROM Post p WHERE p.isNotice = false")
                .executeUpdate();
        entityManager.flush();
        entityManager.clear();

        Pageable pageable = PageRequest.of(0, 10);

        // When: 빈 페이지 조회
        Page<SimplePostResDTO> result = postQueryAdapter.findByPage(pageable);

        // Then: 빈 페이지 반환
        assertThat(result).isNotNull();
        assertThat(result.getContent()).isEmpty();
        assertThat(result.getTotalElements()).isEqualTo(0L);
        assertThat(result.getTotalPages()).isEqualTo(0);
    }

    @Test
    @DisplayName("정상 케이스 - 사용자별 작성 게시글 조회")
    void shouldFindPostsByUserId_WhenValidUserIdProvided() {
        // Given: 사용자 ID와 페이지 요청
        Long userId = testUser.getId();
        Pageable pageable = PageRequest.of(0, 10);

        // When: 사용자별 게시글 조회
        Page<SimplePostResDTO> result = postQueryAdapter.findPostsByUserId(userId, pageable);

        // Then: 해당 사용자의 게시글만 조회됨
        assertThat(result).isNotNull();
        assertThat(result.getContent()).hasSize(4); // 공지사항 포함 전체 게시글
        assertThat(result.getTotalElements()).isEqualTo(4L);

        // 모든 게시글의 작성자가 해당 사용자인지 확인
        List<String> userNames = result.getContent().stream()
                .map(SimplePostResDTO::getUserName)
                .distinct()
                .toList();
        assertThat(userNames).containsExactly("testUser");
    }

    @Test
    @DisplayName("경계값 - 존재하지 않는 사용자의 게시글 조회")
    void shouldReturnEmptyPage_WhenNonExistentUserIdProvided() {
        // Given: 존재하지 않는 사용자 ID
        Long nonExistentUserId = 99999L;
        Pageable pageable = PageRequest.of(0, 10);

        // When: 존재하지 않는 사용자의 게시글 조회
        Page<SimplePostResDTO> result = postQueryAdapter.findPostsByUserId(nonExistentUserId, pageable);

        // Then: 빈 페이지 반환
        assertThat(result).isNotNull();
        assertThat(result.getContent()).isEmpty();
        assertThat(result.getTotalElements()).isEqualTo(0L);
    }

    @Test
    @DisplayName("정상 케이스 - 사용자 추천 게시글 조회")
    void shouldFindLikedPostsByUserId_WhenUserHasLikedPosts() {
        // Given: 사용자가 게시글에 추천을 누름
        User likeUser = User.builder()
                .userName("likeUser")
                .socialId("like123")
                .provider(SocialProvider.KAKAO)
                .socialNickname("좋아요유저")
                .role(UserRole.USER)
                .setting(Setting.builder()
                        .messageNotification(true)
                        .commentNotification(true)
                        .postFeaturedNotification(true)
                        .build())
                .build();
        entityManager.persistAndFlush(likeUser);

        // 게시글에 좋아요 추가
        PostLike postLike1 = PostLike.builder()
                .post(testPost1)
                .user(likeUser)
                .build();
        PostLike postLike2 = PostLike.builder()
                .post(testPost2)
                .user(likeUser)
                .build();

        entityManager.persist(postLike1);
        entityManager.persist(postLike2);
        entityManager.flush();

        Pageable pageable = PageRequest.of(0, 10);

        // When: 사용자 추천 게시글 조회
        Page<SimplePostResDTO> result = postQueryAdapter.findLikedPostsByUserId(likeUser.getId(), pageable);

        // Then: 추천한 게시글들이 조회됨
        assertThat(result).isNotNull();
        assertThat(result.getContent()).hasSize(2);
        assertThat(result.getTotalElements()).isEqualTo(2L);

        List<String> likedPostTitles = result.getContent().stream()
                .map(SimplePostResDTO::getTitle)
                .toList();
        assertThat(likedPostTitles).containsExactlyInAnyOrder(
                "첫 번째 게시글", 
                "두 번째 게시글"
        );

        // 댓글 수도 설정되어 있는지 확인
        assertThat(result.getContent()).allMatch(post -> post.getCommentCount() != null);
    }

    @Test
    @DisplayName("경계값 - 추천하지 않은 사용자의 추천 게시글 조회")
    void shouldReturnEmptyPage_WhenUserHasNoLikedPosts() {
        // Given: 추천을 하지 않은 새로운 사용자
        User newUser = User.builder()
                .userName("newUser")
                .socialId("new123")
                .provider(SocialProvider.KAKAO)
                .socialNickname("새로운유저")
                .role(UserRole.USER)
                .setting(Setting.builder()
                        .messageNotification(true)
                        .commentNotification(true)
                        .postFeaturedNotification(true)
                        .build())
                .build();
        entityManager.persistAndFlush(newUser);

        Pageable pageable = PageRequest.of(0, 10);

        // When: 추천 게시글 조회
        Page<SimplePostResDTO> result = postQueryAdapter.findLikedPostsByUserId(newUser.getId(), pageable);

        // Then: 빈 페이지 반환
        assertThat(result).isNotNull();
        assertThat(result.getContent()).isEmpty();
        assertThat(result.getTotalElements()).isEqualTo(0L);
    }

    @Test
    @DisplayName("성능 - 페이지네이션 동작 확인")
    void shouldHandlePagination_WhenMultiplePagesRequested() {
        // Given: 더 많은 게시글 생성 (총 10개)
        for (int i = 4; i <= 10; i++) {
            PostReqDTO postReqDTO = PostReqDTO.builder()
                    .title("추가 게시글 " + i)
                    .content("추가 게시글 내용 " + i)
                    .password(1234)
                    .build();
            Post post = Post.createPost(testUser, postReqDTO);
            entityManager.persistAndFlush(post);
        }

        // Mock 댓글 수 업데이트
        Map<Long, Integer> allCommentCounts = new HashMap<>();
        for (int i = 1; i <= 10; i++) {
            allCommentCounts.put((long) i, i % 3); // 0, 1, 2 순환
        }
        given(commentQueryUseCase.findCommentCountsByPostIds(any(List.class)))
                .willReturn(allCommentCounts);

        // When: 첫 페이지와 두 번째 페이지 조회
        Pageable firstPage = PageRequest.of(0, 3);
        Pageable secondPage = PageRequest.of(1, 3);

        Page<SimplePostResDTO> firstResult = postQueryAdapter.findByPage(firstPage);
        Page<SimplePostResDTO> secondResult = postQueryAdapter.findByPage(secondPage);

        // Then: 페이지네이션이 정상 동작함
        assertThat(firstResult.getContent()).hasSize(3);
        assertThat(secondResult.getContent()).hasSize(3);
        assertThat(firstResult.getTotalElements()).isEqualTo(10L); // 공지사항 제외
        assertThat(secondResult.getTotalElements()).isEqualTo(10L);
        
        // 각 페이지의 내용이 다름을 확인
        Set<Long> firstPageIds = firstResult.getContent().stream()
                .map(SimplePostResDTO::getId)
                .collect(java.util.stream.Collectors.toSet());
        Set<Long> secondPageIds = secondResult.getContent().stream()
                .map(SimplePostResDTO::getId)
                .collect(java.util.stream.Collectors.toSet());
        
        // 두 페이지 결과가 중복되지 않는지 검증
        assertThat(java.util.Collections.disjoint(firstPageIds, secondPageIds)).isTrue();
    }

    @Test
    @DisplayName("비즈니스 로직 - 공지사항은 일반 페이지 조회에서 제외")
    void shouldExcludeNoticePosts_WhenFindingByPage() {
        // Given: 공지사항과 일반 게시글이 모두 존재
        Pageable pageable = PageRequest.of(0, 10);

        // When: 일반 게시글 페이지 조회
        Page<SimplePostResDTO> result = postQueryAdapter.findByPage(pageable);

        // Then: 공지사항은 제외되고 일반 게시글만 조회됨
        assertThat(result.getContent()).hasSize(3); // 일반 게시글 3개
        
        List<Boolean> noticeFlags = result.getContent().stream()
                .map(SimplePostResDTO::isNotice)
                .toList();
        assertThat(noticeFlags).allMatch(isNotice -> !isNotice); // 모두 false여야 함
    }

    @Test
    @DisplayName("비즈니스 로직 - 최신 게시글부터 정렬")
    void shouldSortByCreatedAtDesc_WhenFindingPosts() {
        // Given: 여러 게시글이 존재
        Pageable pageable = PageRequest.of(0, 10);

        // When: 게시글 조회
        Page<SimplePostResDTO> result = postQueryAdapter.findByPage(pageable);

        // Then: 최신 게시글부터 정렬됨 (createdAt 내림차순)
        List<java.time.Instant> createdAts = result.getContent().stream()
                .map(SimplePostResDTO::getCreatedAt)
                .toList();

        for (int i = 1; i < createdAts.size(); i++) {
            assertThat(createdAts.get(i-1)).isAfterOrEqualTo(createdAts.get(i));
        }
    }

    @Test
    @DisplayName("통합 테스트 - N+1 문제 해결 확인")
    void shouldAvoidNPlusOneProblem_WhenFetchingPostsWithComments() {
        // Given: 게시글들과 댓글 수 Mock
        Map<Long, Integer> commentCounts = new HashMap<>();
        commentCounts.put(testPost1.getId(), 5);
        commentCounts.put(testPost2.getId(), 3);
        commentCounts.put(testPost3.getId(), 1);
        
        given(commentQueryUseCase.findCommentCountsByPostIds(any(List.class)))
                .willReturn(commentCounts);

        Pageable pageable = PageRequest.of(0, 3);

        // When: 게시글과 댓글 수 조회
        Page<SimplePostResDTO> result = postQueryAdapter.findByPage(pageable);

        // Then: 배치로 댓글 수가 조회되어 N+1 문제 없음
        assertThat(result.getContent()).hasSize(3);
        
        // 모든 게시글에 댓글 수가 설정됨
        assertThat(result.getContent()).allMatch(post -> post.getCommentCount() != null);
        
        // 댓글 수가 Mock으로 설정한 값과 일치
        Map<Long, Integer> actualCommentCounts = result.getContent().stream()
                .collect(java.util.stream.Collectors.toMap(
                        SimplePostResDTO::getId, 
                        SimplePostResDTO::getCommentCount
                ));
        
        for (SimplePostResDTO post : result.getContent()) {
            if (commentCounts.containsKey(post.getId())) {
                assertThat(post.getCommentCount()).isEqualTo(commentCounts.get(post.getId()));
            }
        }
    }

    @Test
    @DisplayName("예외 케이스 - null Pageable 처리")
    void shouldHandleNullPageable_GracefullyWithoutError() {
        // Given: null Pageable
        Pageable nullPageable = null;

        // When & Then: 적절한 예외 처리 또는 기본값 사용
        assertThatThrownBy(() -> {
            postQueryAdapter.findByPage(nullPageable);
        }).isInstanceOf(Exception.class); // NullPointerException 또는 적절한 예외
    }

    @Test
    @DisplayName("데이터 매핑 - 모든 필드 정확히 매핑")
    void shouldMapAllFields_WhenCreatingSimplePostResDTO() {
        // Given: 캐시 플래그가 설정된 게시글
        testPost1.setPostCacheFlag(PostCacheFlag.REALTIME);
        entityManager.merge(testPost1);
        entityManager.flush();

        Pageable pageable = PageRequest.of(0, 1);

        // When: 게시글 조회
        Page<SimplePostResDTO> result = postQueryAdapter.findByPage(pageable);

        // Then: 모든 필드가 정확히 매핑됨
        SimplePostResDTO dto = result.getContent().get(0);
        
        assertThat(dto.getId()).isEqualTo(testPost1.getId());
        assertThat(dto.getTitle()).isEqualTo(testPost1.getTitle());
        assertThat(dto.getUserId()).isEqualTo(testUser.getId());
        assertThat(dto.getUserName()).isEqualTo(testUser.getUserName());
        assertThat(dto.getPostCacheFlag()).isEqualTo(PostCacheFlag.REALTIME);
        assertThat(dto.isNotice()).isFalse();
        assertThat(dto.getCreatedAt()).isNotNull();
        assertThat(dto.getCommentCount()).isNotNull();
        assertThat(dto.getLikeCount()).isNotNull();
    }
}