package jaeik.bimillog.infrastructure.outadapter.comment.persistence.comment.commentclosure;

import com.querydsl.jpa.impl.JPAQueryFactory;
import jaeik.bimillog.BimilLogApplication;
import jaeik.bimillog.domain.user.entity.SocialProvider;
import jaeik.bimillog.domain.comment.entity.Comment;
import jaeik.bimillog.domain.comment.entity.CommentClosure;
import jaeik.bimillog.domain.post.entity.Post;
import jaeik.bimillog.domain.user.entity.Setting;
import jaeik.bimillog.domain.user.entity.User;
import jaeik.bimillog.domain.user.entity.UserRole;
import jaeik.bimillog.infrastructure.adapter.comment.out.persistence.comment.CommentSaveAdapter;
import jaeik.bimillog.infrastructure.adapter.comment.out.persistence.comment.CommentDeleteAdapter;
import jaeik.bimillog.infrastructure.adapter.comment.out.persistence.comment.commentclosure.CommentClosureRepository;
import jakarta.persistence.EntityManager;
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

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * <h2>CommentClosureCommandAdapter 통합 테스트</h2>
 * <p>실제 MySQL 데이터베이스를 사용한 CommentClosureCommandAdapter의 통합 테스트</p>
 * <p>TestContainers를 사용하여 실제 MySQL 환경에서 댓글 클로저 CRUD 동작 검증</p>
 * 
 * @author Jaeik
 * @version 2.0.0
 */
@DataJpaTest(
        excludeFilters = @ComponentScan.Filter(
                type = FilterType.ASSIGNABLE_TYPE,
                classes = BimilLogApplication.class
        )
)
@Testcontainers
@EntityScan(basePackages = {
        "jaeik.bimillog.domain.comment.entity",
        "jaeik.bimillog.domain.user.entity",
        "jaeik.bimillog.domain.post.entity",
        "jaeik.bimillog.domain.global.entity"
})
@EnableJpaRepositories(basePackages = {
        "jaeik.bimillog.infrastructure.adapter.comment.out.persistence.comment.commentclosure"
})
@Import({CommentSaveAdapter.class, CommentDeleteAdapter.class})
@TestPropertySource(properties = {
        "spring.jpa.hibernate.ddl-auto=create"
})
class CommentClosureCommandAdapterIntegrationTest {

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
    private CommentClosureRepository commentClosureRepository;

    @Autowired
    private CommentSaveAdapter commentSaveAdapter;
    
    @Autowired
    private CommentDeleteAdapter commentDeleteAdapter;

    private User testUser;
    private Post testPost;
    private Comment parentComment;
    private Comment childComment;

    @BeforeEach
    void setUp() {
        // 테스트 데이터 초기화
        commentClosureRepository.deleteAll();
        
        // 테스트용 사용자 생성
        Setting setting = Setting.createSetting();
        entityManager.persistAndFlush(setting);
        
        testUser = User.builder()
                .socialId("kakao123")
                .provider(SocialProvider.KAKAO)
                .userName("testUser")
                .socialNickname("테스트유저")
                .role(UserRole.USER)
                .setting(setting)
                .build();
        entityManager.persistAndFlush(testUser);
        
        // 테스트용 게시글 생성
        testPost = Post.builder()
                .user(testUser)
                .title("테스트 게시글")
                .content("테스트 내용")
                .isNotice(false)
                .views(0)
                .build();
        entityManager.persistAndFlush(testPost);

        // 테스트용 댓글 생성 (부모 댓글)
        parentComment = Comment.createComment(
                testPost, 
                testUser, 
                "부모 댓글", 
                null
        );
        entityManager.persistAndFlush(parentComment);

        // 테스트용 댓글 생성 (자식 댓글)
        childComment = Comment.createComment(
                testPost, 
                testUser, 
                "자식 댓글", 
                null
        );
        entityManager.persistAndFlush(childComment);
    }

    @Test
    @DisplayName("정상 케이스 - 새로운 댓글 클로저 저장")
    void shouldSaveCommentClosure_WhenValidClosureProvided() {
        // Given: 새로운 댓글 클로저 엔티티
        CommentClosure commentClosure = CommentClosure.createCommentClosure(
                parentComment, 
                childComment, 
                1
        );

        // When: 댓글 클로저 저장
        commentSaveAdapter.save(commentClosure);

        // Then: 댓글 클로저가 올바르게 저장되었는지 검증
        List<CommentClosure> allClosures = commentClosureRepository.findAll();
        assertThat(allClosures).hasSize(1);

        CommentClosure savedClosure = allClosures.get(0);
        assertThat(savedClosure.getId()).isNotNull();
        assertThat(savedClosure.getAncestor()).isEqualTo(parentComment);
        assertThat(savedClosure.getDescendant()).isEqualTo(childComment);
        assertThat(savedClosure.getDepth()).isEqualTo(1);
    }

    @Test
    @DisplayName("정상 케이스 - 자기 자신을 가리키는 클로저 저장 (depth=0)")
    void shouldSaveSelfReferencingClosure_WhenSameCommentProvided() {
        // Given: 자기 자신을 가리키는 댓글 클로저 (depth=0)
        CommentClosure selfClosure = CommentClosure.createCommentClosure(
                parentComment, 
                parentComment, 
                0
        );

        // When: 자기 참조 클로저 저장
        commentSaveAdapter.save(selfClosure);

        // Then: 자기 참조 클로저가 올바르게 저장되었는지 검증
        List<CommentClosure> allClosures = commentClosureRepository.findAll();
        assertThat(allClosures).hasSize(1);

        CommentClosure savedClosure = allClosures.get(0);
        assertThat(savedClosure.getAncestor()).isEqualTo(parentComment);
        assertThat(savedClosure.getDescendant()).isEqualTo(parentComment);
        assertThat(savedClosure.getDepth()).isEqualTo(0);
    }

    @Test
    @DisplayName("정상 케이스 - 다중 레벨 클로저 저장")
    void shouldSaveMultipleLevelClosures_WhenDeepHierarchyProvided() {
        // Given: 깊은 계층 구조를 위한 추가 댓글 생성
        Comment grandChildComment = Comment.createComment(
                testPost, 
                testUser, 
                "손자 댓글", 
                null
        );
        entityManager.persistAndFlush(grandChildComment);

        // 계층 구조: 부모 -> 자식 -> 손자
        CommentClosure closure1 = CommentClosure.createCommentClosure(parentComment, parentComment, 0);
        CommentClosure closure2 = CommentClosure.createCommentClosure(parentComment, childComment, 1);
        CommentClosure closure3 = CommentClosure.createCommentClosure(parentComment, grandChildComment, 2);
        CommentClosure closure4 = CommentClosure.createCommentClosure(childComment, childComment, 0);
        CommentClosure closure5 = CommentClosure.createCommentClosure(childComment, grandChildComment, 1);
        CommentClosure closure6 = CommentClosure.createCommentClosure(grandChildComment, grandChildComment, 0);

        // When: 모든 클로저 저장
        commentSaveAdapter.save(closure1);
        commentSaveAdapter.save(closure2);
        commentSaveAdapter.save(closure3);
        commentSaveAdapter.save(closure4);
        commentSaveAdapter.save(closure5);
        commentSaveAdapter.save(closure6);

        // Then: 모든 클로저가 올바르게 저장되었는지 검증
        List<CommentClosure> allClosures = commentClosureRepository.findAll();
        assertThat(allClosures).hasSize(6);

        // 깊이별로 클로저 검증
        long depth0Count = allClosures.stream().filter(c -> c.getDepth() == 0).count();
        long depth1Count = allClosures.stream().filter(c -> c.getDepth() == 1).count();
        long depth2Count = allClosures.stream().filter(c -> c.getDepth() == 2).count();

        assertThat(depth0Count).isEqualTo(3); // 각 댓글의 자기 참조
        assertThat(depth1Count).isEqualTo(2); // 부모-자식, 자식-손자
        assertThat(depth2Count).isEqualTo(1); // 부모-손자
    }

    @Test
    @DisplayName("정상 케이스 - 댓글 클로저 삭제")
    void shouldDeleteCommentClosure_WhenValidClosureProvided() {
        // Given: 기존 댓글 클로저 생성 및 저장
        CommentClosure commentClosure = CommentClosure.createCommentClosure(
                parentComment, 
                childComment, 
                1
        );
        commentClosureRepository.save(commentClosure);
        
        // 저장 확인
        List<CommentClosure> beforeDeletion = commentClosureRepository.findAll();
        assertThat(beforeDeletion).hasSize(1);

        // When: 댓글 클로저 삭제
        commentDeleteAdapter.delete(commentClosure);

        // Then: 댓글 클로저가 삭제되었는지 검증
        List<CommentClosure> afterDeletion = commentClosureRepository.findAll();
        assertThat(afterDeletion).isEmpty();
    }

    @Test
    @DisplayName("정상 케이스 - 자손 ID로 댓글 클로저 삭제")
    void shouldDeleteCommentClosuresByDescendantId_WhenValidDescendantIdProvided() {
        // Given: 동일한 자손을 가진 여러 클로저 생성
        CommentClosure closure1 = CommentClosure.createCommentClosure(parentComment, childComment, 1);
        CommentClosure closure2 = CommentClosure.createCommentClosure(childComment, childComment, 0);
        
        // 다른 자손을 가진 클로저
        Comment anotherChild = Comment.createComment(testPost, testUser, "다른 자식", null);
        entityManager.persistAndFlush(anotherChild);
        CommentClosure closure3 = CommentClosure.createCommentClosure(parentComment, anotherChild, 1);
        
        commentClosureRepository.save(closure1);
        commentClosureRepository.save(closure2);
        commentClosureRepository.save(closure3);

        // 저장 확인
        List<CommentClosure> beforeDeletion = commentClosureRepository.findAll();
        assertThat(beforeDeletion).hasSize(3);

        // When: 특정 자손 ID로 클로저 삭제
        commentDeleteAdapter.deleteByDescendantId(childComment.getId());

        // Then: 해당 자손을 가진 클로저만 삭제되었는지 검증
        List<CommentClosure> afterDeletion = commentClosureRepository.findAll();
        assertThat(afterDeletion).hasSize(1);
        assertThat(afterDeletion.get(0).getDescendant()).isEqualTo(anotherChild);
    }

    @Test
    @DisplayName("경계값 - 최대 깊이 클로저 저장")
    void shouldSaveMaxDepthClosure_WhenMaxDepthProvided() {
        // Given: 최대 깊이(예: 10) 클로저
        int maxDepth = 10;
        CommentClosure maxDepthClosure = CommentClosure.createCommentClosure(
                parentComment, 
                childComment, 
                maxDepth
        );

        // When: 최대 깊이 클로저 저장
        commentSaveAdapter.save(maxDepthClosure);

        // Then: 최대 깊이 클로저가 올바르게 저장되었는지 검증
        List<CommentClosure> allClosures = commentClosureRepository.findAll();
        assertThat(allClosures).hasSize(1);
        assertThat(allClosures.get(0).getDepth()).isEqualTo(maxDepth);
    }

    @Test
    @DisplayName("경계값 - 존재하지 않는 자손 ID로 삭제 시도")
    void shouldDoNothing_WhenDeletingByNonExistentDescendantId() {
        // Given: 기존 클로저와 존재하지 않는 자손 ID
        CommentClosure existingClosure = CommentClosure.createCommentClosure(
                parentComment, 
                childComment, 
                1
        );
        commentClosureRepository.save(existingClosure);
        
        Long nonExistentDescendantId = 999L;

        // When: 존재하지 않는 자손 ID로 삭제 시도
        commentDeleteAdapter.deleteByDescendantId(nonExistentDescendantId);

        // Then: 기존 클로저는 삭제되지 않아야 함
        List<CommentClosure> allClosures = commentClosureRepository.findAll();
        assertThat(allClosures).hasSize(1);
        assertThat(allClosures.get(0).getDescendant()).isEqualTo(childComment);
    }

    @Test
    @DisplayName("트랜잭션 - 복잡한 클로저 엔티티 저장 및 조회")
    void shouldSaveComplexClosureEntity_WhenAllFieldsProvided() {
        // Given: 복잡한 계층 구조의 클로저들
        CommentClosure complexClosure = CommentClosure.createCommentClosure(
                parentComment, 
                childComment, 
                5
        );

        // When: 복잡한 클로저 저장
        commentSaveAdapter.save(complexClosure);

        // Then: 모든 필드가 올바르게 저장되었는지 검증
        Optional<CommentClosure> foundClosure = commentClosureRepository.findById(complexClosure.getId());
        assertThat(foundClosure).isPresent();

        CommentClosure dbClosure = foundClosure.get();
        assertThat(dbClosure.getAncestor()).isEqualTo(parentComment);
        assertThat(dbClosure.getDescendant()).isEqualTo(childComment);
        assertThat(dbClosure.getDepth()).isEqualTo(5);
        assertThat(dbClosure.getId()).isNotNull();

        // 연관된 엔티티들도 올바르게 조회되는지 확인
        assertThat(dbClosure.getAncestor().getContent()).isEqualTo("부모 댓글");
        assertThat(dbClosure.getDescendant().getContent()).isEqualTo("자식 댓글");
    }

    @Test
    @DisplayName("트랜잭션 - 배치 클로저 삭제")
    void shouldDeleteMultipleClosures_WhenBatchDeletionRequested() {
        // Given: 동일한 자손을 가진 여러 클로저들
        Comment grandParentComment = Comment.createComment(testPost, testUser, "할아버지 댓글", null);
        entityManager.persistAndFlush(grandParentComment);

        CommentClosure closure1 = CommentClosure.createCommentClosure(grandParentComment, childComment, 2);
        CommentClosure closure2 = CommentClosure.createCommentClosure(parentComment, childComment, 1);
        CommentClosure closure3 = CommentClosure.createCommentClosure(childComment, childComment, 0);
        
        commentClosureRepository.save(closure1);
        commentClosureRepository.save(closure2);
        commentClosureRepository.save(closure3);

        // 저장 확인
        assertThat(commentClosureRepository.findAll()).hasSize(3);

        // When: 특정 자손의 모든 클로저 삭제
        commentDeleteAdapter.deleteByDescendantId(childComment.getId());

        // Then: 해당 자손의 모든 클로저가 삭제되었는지 검증
        List<CommentClosure> remainingClosures = commentClosureRepository.findAll();
        assertThat(remainingClosures).isEmpty();
    }
}