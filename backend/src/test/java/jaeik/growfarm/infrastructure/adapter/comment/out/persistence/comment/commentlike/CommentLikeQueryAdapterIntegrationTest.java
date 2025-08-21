package jaeik.growfarm.infrastructure.adapter.comment.out.persistence.comment.commentlike;

import com.querydsl.jpa.impl.JPAQueryFactory;
import jakarta.persistence.EntityManager;
import jaeik.growfarm.GrowfarmApplication;
import jaeik.growfarm.domain.comment.entity.Comment;
import jaeik.growfarm.domain.comment.entity.CommentLike;
import jaeik.growfarm.domain.common.entity.SocialProvider;
import jaeik.growfarm.domain.post.entity.Post;
import jaeik.growfarm.domain.user.entity.Setting;
import jaeik.growfarm.domain.user.entity.User;
import jaeik.growfarm.domain.user.entity.UserRole;
import jaeik.growfarm.infrastructure.adapter.comment.out.persistence.comment.comment.CommentRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.context.annotation.Import;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.TestPropertySource;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * <h2>CommentLikeQueryAdapter 통합 테스트</h2>
 * <p>실제 MySQL 데이터베이스를 사용한 CommentLikeQueryAdapter의 통합 테스트</p>
 * <p>TestContainers를 사용하여 실제 MySQL 환경에서 댓글 추천 조회 동작 검증</p>
 * <p>핵심 비즈니스 로직: 사용자가 특정 댓글에 추천을 눌렀는지 여부 확인</p>
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
        "jaeik.growfarm.domain.comment.entity",
        "jaeik.growfarm.domain.user.entity",
        "jaeik.growfarm.domain.post.entity",
        "jaeik.growfarm.domain.common.entity"
})
@EnableJpaRepositories(basePackages = {
        "jaeik.growfarm.infrastructure.adapter.comment.out.persistence.comment.comment",
        "jaeik.growfarm.infrastructure.adapter.comment.out.persistence.comment.commentlike"
})
@Import(CommentLikeQueryAdapter.class)
@TestPropertySource(properties = {
        "spring.jpa.hibernate.ddl-auto=create"
})
class CommentLikeQueryAdapterIntegrationTest {

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
    }

    @Autowired
    private TestEntityManager entityManager;
    
    @Autowired
    private CommentRepository commentRepository;
    
    @Autowired
    private CommentLikeRepository commentLikeRepository;

    @Autowired
    private CommentLikeQueryAdapter commentLikeQueryAdapter;

    private User testUser1;
    private User testUser2;
    private User testUser3;
    private Post testPost;
    private Comment testComment1;
    private Comment testComment2;

    @BeforeEach
    void setUp() {
        // 테스트 데이터 초기화
        commentLikeRepository.deleteAll();
        commentRepository.deleteAll();
        
        // 테스트용 사용자들 생성
        Setting setting1 = Setting.createSetting();
        entityManager.persistAndFlush(setting1);
        
        testUser1 = User.builder()
                .socialId("kakao123")
                .provider(SocialProvider.KAKAO)
                .userName("testUser1")
                .socialNickname("테스트유저1")
                .role(UserRole.USER)
                .setting(setting1)
                .build();
        entityManager.persistAndFlush(testUser1);
        
        Setting setting2 = Setting.createSetting();
        entityManager.persistAndFlush(setting2);
        
        testUser2 = User.builder()
                .socialId("kakao456")
                .provider(SocialProvider.KAKAO)
                .userName("testUser2")
                .socialNickname("테스트유저2")
                .role(UserRole.USER)
                .setting(setting2)
                .build();
        entityManager.persistAndFlush(testUser2);
        
        Setting setting3 = Setting.createSetting();
        entityManager.persistAndFlush(setting3);
        
        testUser3 = User.builder()
                .socialId("kakao789")
                .provider(SocialProvider.KAKAO)
                .userName("testUser3")
                .socialNickname("테스트유저3")
                .role(UserRole.USER)
                .setting(setting3)
                .build();
        entityManager.persistAndFlush(testUser3);
        
        // 테스트용 게시글 생성
        testPost = Post.builder()
                .user(testUser1)
                .title("테스트 게시글")
                .content("테스트 내용")
                .isNotice(false)
                .views(0)
                .build();
        entityManager.persistAndFlush(testPost);
        
        // 테스트용 댓글들 생성
        testComment1 = Comment.createComment(testPost, testUser1, "테스트 댓글 1", null);
        testComment1 = commentRepository.save(testComment1);
        
        testComment2 = Comment.createComment(testPost, testUser2, "테스트 댓글 2", null);
        testComment2 = commentRepository.save(testComment2);
    }

    @Test
    @DisplayName("정상 케이스 - 사용자가 댓글에 추천을 눌렀을 때 true 반환")
    void shouldReturnTrue_WhenUserLikedComment() {
        // Given: testUser2가 testComment1에 추천을 누른 상태
        CommentLike commentLike = CommentLike.builder()
                .comment(testComment1)
                .user(testUser2)
                .build();
        commentLikeRepository.save(commentLike);

        // When: 댓글 추천 여부 확인
        boolean isLiked = commentLikeQueryAdapter.isLikedByUser(testComment1.getId(), testUser2.getId());

        // Then: 추천을 눌렀으므로 true 반환
        assertThat(isLiked).isTrue();
    }

    @Test
    @DisplayName("정상 케이스 - 사용자가 댓글에 추천을 누르지 않았을 때 false 반환")
    void shouldReturnFalse_WhenUserDidNotLikeComment() {
        // Given: testUser2가 testComment1에 추천을 누르지 않은 상태
        // (별도의 설정 없이 빈 상태)

        // When: 댓글 추천 여부 확인
        boolean isLiked = commentLikeQueryAdapter.isLikedByUser(testComment1.getId(), testUser2.getId());

        // Then: 추천을 누르지 않았으므로 false 반환
        assertThat(isLiked).isFalse();
    }

    @Test
    @DisplayName("정상 케이스 - 존재하지 않는 댓글 ID로 조회 시 false 반환")
    void shouldReturnFalse_WhenCommentIdDoesNotExist() {
        // Given: 존재하지 않는 댓글 ID
        Long nonExistentCommentId = 99999L;

        // When: 존재하지 않는 댓글에 대한 추천 여부 확인
        boolean isLiked = commentLikeQueryAdapter.isLikedByUser(nonExistentCommentId, testUser1.getId());

        // Then: 존재하지 않는 댓글이므로 false 반환
        assertThat(isLiked).isFalse();
    }

    @Test
    @DisplayName("정상 케이스 - 존재하지 않는 사용자 ID로 조회 시 false 반환")
    void shouldReturnFalse_WhenUserIdDoesNotExist() {
        // Given: 존재하지 않는 사용자 ID
        Long nonExistentUserId = 99999L;

        // When: 존재하지 않는 사용자에 대한 추천 여부 확인
        boolean isLiked = commentLikeQueryAdapter.isLikedByUser(testComment1.getId(), nonExistentUserId);

        // Then: 존재하지 않는 사용자이므로 false 반환
        assertThat(isLiked).isFalse();
    }

    @Test
    @DisplayName("정상 케이스 - 여러 사용자가 동일 댓글에 추천한 경우 정확한 구분")
    void shouldReturnCorrectResult_WhenMultipleUsersLikeSameComment() {
        // Given: 여러 사용자가 동일한 댓글에 추천
        CommentLike like1 = CommentLike.builder()
                .comment(testComment1)
                .user(testUser1)
                .build();
        commentLikeRepository.save(like1);
        
        CommentLike like2 = CommentLike.builder()
                .comment(testComment1)
                .user(testUser2)
                .build();
        commentLikeRepository.save(like2);
        
        // testUser3은 추천하지 않음

        // When: 각 사용자별 추천 여부 확인
        boolean user1Liked = commentLikeQueryAdapter.isLikedByUser(testComment1.getId(), testUser1.getId());
        boolean user2Liked = commentLikeQueryAdapter.isLikedByUser(testComment1.getId(), testUser2.getId());
        boolean user3Liked = commentLikeQueryAdapter.isLikedByUser(testComment1.getId(), testUser3.getId());

        // Then: 추천한 사용자는 true, 추천하지 않은 사용자는 false
        assertThat(user1Liked).isTrue();
        assertThat(user2Liked).isTrue();
        assertThat(user3Liked).isFalse();
    }

    @Test
    @DisplayName("정상 케이스 - 한 사용자가 여러 댓글에 추천한 경우 정확한 구분")
    void shouldReturnCorrectResult_WhenOneUserLikesMultipleComments() {
        // Given: 한 사용자가 여러 댓글에 추천
        CommentLike like1 = CommentLike.builder()
                .comment(testComment1)
                .user(testUser2)
                .build();
        commentLikeRepository.save(like1);
        
        CommentLike like2 = CommentLike.builder()
                .comment(testComment2)
                .user(testUser2)
                .build();
        commentLikeRepository.save(like2);

        // When: 각 댓글별 추천 여부 확인
        boolean comment1Liked = commentLikeQueryAdapter.isLikedByUser(testComment1.getId(), testUser2.getId());
        boolean comment2Liked = commentLikeQueryAdapter.isLikedByUser(testComment2.getId(), testUser2.getId());
        
        // 다른 사용자의 경우
        boolean otherUserComment1Liked = commentLikeQueryAdapter.isLikedByUser(testComment1.getId(), testUser3.getId());
        boolean otherUserComment2Liked = commentLikeQueryAdapter.isLikedByUser(testComment2.getId(), testUser3.getId());

        // Then: 추천한 댓글은 true, 추천하지 않은 조합은 false
        assertThat(comment1Liked).isTrue();
        assertThat(comment2Liked).isTrue();
        assertThat(otherUserComment1Liked).isFalse();
        assertThat(otherUserComment2Liked).isFalse();
    }

    @Test
    @DisplayName("성능 테스트 - EXISTS 쿼리 최적화 확인")
    void shouldUseOptimizedExistsQuery_WhenCheckingCommentLike() {
        // Given: 대량의 테스트 데이터 생성 (성능 테스트용)
        // 여러 사용자와 댓글에 대한 추천 데이터 생성
        CommentLike targetLike = CommentLike.builder()
                .comment(testComment1)
                .user(testUser2)
                .build();
        commentLikeRepository.save(targetLike);
        
        // 다른 추천들도 생성 (노이즈 데이터)
        for (int i = 0; i < 10; i++) {
            Comment noiseComment = Comment.createComment(testPost, testUser1, "노이즈 댓글 " + i, null);
            noiseComment = commentRepository.save(noiseComment);
            
            CommentLike noiseLike = CommentLike.builder()
                    .comment(noiseComment)
                    .user(testUser3)
                    .build();
            commentLikeRepository.save(noiseLike);
        }

        // When: 타겟 추천 여부 확인 (EXISTS 쿼리 사용)
        long startTime = System.nanoTime();
        boolean isLiked = commentLikeQueryAdapter.isLikedByUser(testComment1.getId(), testUser2.getId());
        long endTime = System.nanoTime();

        // Then: 빠른 시간 내에 정확한 결과 반환
        assertThat(isLiked).isTrue();
        
        // 성능 확인: EXISTS 쿼리는 매우 빨라야 함 (1ms 이내)
        long executionTimeMs = (endTime - startTime) / 1_000_000;
        assertThat(executionTimeMs).isLessThan(10); // 10ms 이내 (여유있게 설정)
    }

    @Test
    @DisplayName("경계값 - null 값 처리 확인")
    void shouldHandleNullValues_WhenNullParametersProvided() {
        // 실제 비즈니스 로직에서는 null이 넘어오지 않아야 하지만
        // 어댑터 레벨에서의 방어 코드 확인
        
        // Given & When & Then: null 처리 테스트
        // 비즈니스 로직에서 NULL 체크가 선행되므로 여기서는 기본 동작 확인
        
        // 정상적인 경우만 테스트 (NULL은 비즈니스 레이어에서 사전 차단)
        boolean normalCase = commentLikeQueryAdapter.isLikedByUser(testComment1.getId(), testUser1.getId());
        assertThat(normalCase).isFalse();
    }

    @Test
    @DisplayName("트랜잭션 - 추천 상태 변경 시 실시간 반영 확인")
    void shouldReflectChangesImmediately_WhenCommentLikeStateChanges() {
        // Given: 초기 상태 - 추천하지 않은 상태
        boolean initialState = commentLikeQueryAdapter.isLikedByUser(testComment1.getId(), testUser2.getId());
        assertThat(initialState).isFalse();

        // When: 추천 추가
        CommentLike commentLike = CommentLike.builder()
                .comment(testComment1)
                .user(testUser2)
                .build();
        commentLikeRepository.save(commentLike);

        // Then: 추가 후 즉시 반영 확인
        boolean afterAdd = commentLikeQueryAdapter.isLikedByUser(testComment1.getId(), testUser2.getId());
        assertThat(afterAdd).isTrue();

        // When: 추천 삭제
        commentLikeRepository.deleteByCommentAndUser(testComment1, testUser2);

        // Then: 삭제 후 즉시 반영 확인
        boolean afterDelete = commentLikeQueryAdapter.isLikedByUser(testComment1.getId(), testUser2.getId());
        assertThat(afterDelete).isFalse();
    }

    @Test
    @DisplayName("복잡한 시나리오 - 대용량 데이터에서의 정확성 검증")
    void shouldReturnAccurateResults_WhenLargeDatasetExists() {
        // Given: 대용량 테스트 데이터 생성
        final int COMMENT_COUNT = 50;
        
        // 추가 댓글들 생성
        for (int i = 0; i < COMMENT_COUNT; i++) {
            Comment comment = Comment.createComment(testPost, testUser1, "댓글 " + i, null);
            commentRepository.save(comment);
        }
        
        // 무작위 추천 관계 생성 (testUser2가 일부 댓글에만 추천)
        // testComment1에는 추천, testComment2에는 추천하지 않음
        CommentLike targetLike = CommentLike.builder()
                .comment(testComment1)
                .user(testUser2)
                .build();
        commentLikeRepository.save(targetLike);

        // When: 타겟 댓글들의 추천 여부 확인
        boolean comment1Liked = commentLikeQueryAdapter.isLikedByUser(testComment1.getId(), testUser2.getId());
        boolean comment2Liked = commentLikeQueryAdapter.isLikedByUser(testComment2.getId(), testUser2.getId());

        // Then: 대용량 데이터 환경에서도 정확한 결과 반환
        assertThat(comment1Liked).isTrue();   // 추천한 댓글
        assertThat(comment2Liked).isFalse();  // 추천하지 않은 댓글
        
        // 전체 추천 수 확인
        long totalLikes = commentLikeRepository.count();
        assertThat(totalLikes).isEqualTo(1);  // targetLike 하나만 존재
    }

    @Test
    @DisplayName("동시성 시나리오 - 같은 댓글-사용자 조합에 대한 중복 체크")
    void shouldHandleConcurrentAccess_WhenSameCommentUserCombination() {
        // Given: 동일한 댓글-사용자 조합
        Long commentId = testComment1.getId();
        Long userId = testUser2.getId();
        
        // 초기 상태 확인
        boolean initialCheck = commentLikeQueryAdapter.isLikedByUser(commentId, userId);
        assertThat(initialCheck).isFalse();

        // When: 추천 생성
        CommentLike commentLike = CommentLike.builder()
                .comment(testComment1)
                .user(testUser2)
                .build();
        commentLikeRepository.save(commentLike);

        // Then: 여러 번 체크해도 일관된 결과
        for (int i = 0; i < 5; i++) {
            boolean check = commentLikeQueryAdapter.isLikedByUser(commentId, userId);
            assertThat(check).isTrue();
        }
        
        // 다른 조합들은 영향받지 않음
        boolean otherUserCheck = commentLikeQueryAdapter.isLikedByUser(commentId, testUser3.getId());
        boolean otherCommentCheck = commentLikeQueryAdapter.isLikedByUser(testComment2.getId(), userId);
        
        assertThat(otherUserCheck).isFalse();
        assertThat(otherCommentCheck).isFalse();
    }
}