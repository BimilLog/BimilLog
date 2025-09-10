package jaeik.bimillog.infrastructure.adapter.comment.jpa;

import com.querydsl.jpa.impl.JPAQueryFactory;
import jaeik.bimillog.BimilLogApplication;
import jaeik.bimillog.domain.user.entity.SocialProvider;
import jaeik.bimillog.domain.comment.entity.Comment;
import jaeik.bimillog.domain.comment.entity.CommentClosure;
import jaeik.bimillog.domain.post.entity.Post;
import jaeik.bimillog.domain.user.entity.Setting;
import jaeik.bimillog.domain.user.entity.User;
import jaeik.bimillog.domain.user.entity.UserRole;
import jaeik.bimillog.infrastructure.adapter.comment.out.comment.CommentSaveAdapter;
import jaeik.bimillog.infrastructure.adapter.comment.out.comment.CommentDeleteAdapter;
import jaeik.bimillog.infrastructure.adapter.comment.out.jpa.CommentRepository;
import jaeik.bimillog.infrastructure.adapter.comment.out.jpa.CommentClosureRepository;
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
 * <h2>댓글 클로저 어댑터 통합 테스트</h2>
 * <p>실제 MySQL 데이터베이스를 사용한 CommentClosure 관련 어댑터들의 통합 테스트</p>
 * <p>TestContainers를 사용하여 실제 MySQL 환경에서 댓글 클로저 CRUD 및 조회 동작 검증</p>
 * <p>명령(저장/삭제) 작업과 조회 작업을 모두 포함</p>
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
        "jaeik.bimillog.infrastructure.adapter.comment.out.persistence.comment"
})
@Import({CommentSaveAdapter.class, CommentDeleteAdapter.class})
@TestPropertySource(properties = {
        "spring.jpa.hibernate.ddl-auto=create"
})
class CommentClosureAdapterIntegrationTest {

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
    private CommentRepository commentRepository;

    @Autowired
    private CommentSaveAdapter commentSaveAdapter;
    
    @Autowired
    private CommentDeleteAdapter commentDeleteAdapter;

    private User testUser;
    private Post testPost;
    private Comment parentComment;
    private Comment childComment;
    private Comment grandChildComment;

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

        // 테스트용 댓글들 생성 (3레벨 계층구조: 부모 -> 자식 -> 손자)
        parentComment = Comment.createComment(testPost, testUser, "부모 댓글", null);
        entityManager.persistAndFlush(parentComment);

        childComment = Comment.createComment(testPost, testUser, "자식 댓글", null);
        entityManager.persistAndFlush(childComment);

        grandChildComment = Comment.createComment(testPost, testUser, "손자 댓글", null);
        entityManager.persistAndFlush(grandChildComment);
    }

    // ============================================================================
    // 명령(Command) 테스트: save, delete 기능
    // ============================================================================

    @Test
    @DisplayName("정상 케이스 - 새로운 댓글 클로저 저장")
    void shouldSaveCommentClosure_WhenValidClosureProvided() {
        // Given
        CommentClosure commentClosure = CommentClosure.createCommentClosure(parentComment, childComment, 1);

        // When
        commentSaveAdapter.save(commentClosure);

        // Then
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
        // Given
        CommentClosure selfClosure = CommentClosure.createCommentClosure(parentComment, parentComment, 0);

        // When
        commentSaveAdapter.save(selfClosure);

        // Then
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
        // Given: 계층 구조: 부모 -> 자식 -> 손자
        CommentClosure closure1 = CommentClosure.createCommentClosure(parentComment, parentComment, 0);
        CommentClosure closure2 = CommentClosure.createCommentClosure(parentComment, childComment, 1);
        CommentClosure closure3 = CommentClosure.createCommentClosure(parentComment, grandChildComment, 2);
        CommentClosure closure4 = CommentClosure.createCommentClosure(childComment, childComment, 0);
        CommentClosure closure5 = CommentClosure.createCommentClosure(childComment, grandChildComment, 1);
        CommentClosure closure6 = CommentClosure.createCommentClosure(grandChildComment, grandChildComment, 0);

        // When
        commentSaveAdapter.save(closure1);
        commentSaveAdapter.save(closure2);
        commentSaveAdapter.save(closure3);
        commentSaveAdapter.save(closure4);
        commentSaveAdapter.save(closure5);
        commentSaveAdapter.save(closure6);

        // Then
        List<CommentClosure> allClosures = commentClosureRepository.findAll();
        assertThat(allClosures).hasSize(6);

        long depth0Count = allClosures.stream().filter(c -> c.getDepth() == 0).count();
        long depth1Count = allClosures.stream().filter(c -> c.getDepth() == 1).count();
        long depth2Count = allClosures.stream().filter(c -> c.getDepth() == 2).count();

        assertThat(depth0Count).isEqualTo(3); // 각 댓글의 자기 참조
        assertThat(depth1Count).isEqualTo(2); // 부모-자식, 자식-손자
        assertThat(depth2Count).isEqualTo(1); // 부모-손자
    }

    @Test
    @DisplayName("정상 케이스 - 자손 ID로 댓글 클로저 삭제")
    void shouldDeleteCommentClosuresByDescendantId_WhenValidDescendantIdProvided() {
        // Given
        CommentClosure closure1 = CommentClosure.createCommentClosure(parentComment, childComment, 1);
        CommentClosure closure2 = CommentClosure.createCommentClosure(childComment, childComment, 0);
        
        // 다른 자손을 가진 클로저
        Comment anotherChild = Comment.createComment(testPost, testUser, "다른 자식", null);
        entityManager.persistAndFlush(anotherChild);
        CommentClosure closure3 = CommentClosure.createCommentClosure(parentComment, anotherChild, 1);
        
        commentClosureRepository.save(closure1);
        commentClosureRepository.save(closure2);
        commentClosureRepository.save(closure3);

        // When
        commentRepository.deleteClosuresByDescendantId(childComment.getId());

        // Then
        List<CommentClosure> afterDeletion = commentClosureRepository.findAll();
        assertThat(afterDeletion).hasSize(1);
        assertThat(afterDeletion.get(0).getDescendant()).isEqualTo(anotherChild);
    }

    // ============================================================================
    // 조회(Query) 테스트: findByDescendantId 기능
    // ============================================================================

    @Test
    @DisplayName("정상 케이스 - 자손 ID로 클로저 목록 조회")
    void shouldFindClosuresByDescendantId_WhenValidDescendantIdProvided() {
        // Given: 계층 구조를 나타내는 클로저들 저장
        CommentClosure closure1 = CommentClosure.createCommentClosure(parentComment, parentComment, 0);
        CommentClosure closure2 = CommentClosure.createCommentClosure(parentComment, childComment, 1);
        CommentClosure closure3 = CommentClosure.createCommentClosure(parentComment, grandChildComment, 2);
        CommentClosure closure4 = CommentClosure.createCommentClosure(childComment, childComment, 0);
        CommentClosure closure5 = CommentClosure.createCommentClosure(childComment, grandChildComment, 1);
        CommentClosure closure6 = CommentClosure.createCommentClosure(grandChildComment, grandChildComment, 0);

        commentClosureRepository.save(closure1);
        commentClosureRepository.save(closure2);
        commentClosureRepository.save(closure3);
        commentClosureRepository.save(closure4);
        commentClosureRepository.save(closure5);
        commentClosureRepository.save(closure6);

        // When: 손자 댓글을 자손으로 하는 클로저들 조회
        Optional<List<CommentClosure>> result = commentClosureRepository
                .findByDescendantId(grandChildComment.getId());

        // Then: 손자 댓글과 관련된 모든 클로저 반환
        assertThat(result).isPresent();
        List<CommentClosure> closures = result.get();
        assertThat(closures).hasSize(3); // 부모->손자, 자식->손자, 손자->손자

        long depth0Count = closures.stream().filter(c -> c.getDepth() == 0).count();
        long depth1Count = closures.stream().filter(c -> c.getDepth() == 1).count();
        long depth2Count = closures.stream().filter(c -> c.getDepth() == 2).count();

        assertThat(depth0Count).isEqualTo(1); // 손자->손자
        assertThat(depth1Count).isEqualTo(1); // 자식->손자
        assertThat(depth2Count).isEqualTo(1); // 부모->손자
    }

    @Test
    @DisplayName("정상 케이스 - 중간 노드 자손 ID로 클로저 목록 조회")
    void shouldFindClosuresForMiddleNode_WhenMiddleNodeDescendantIdProvided() {
        // Given
        CommentClosure closure1 = CommentClosure.createCommentClosure(parentComment, parentComment, 0);
        CommentClosure closure2 = CommentClosure.createCommentClosure(parentComment, childComment, 1);
        CommentClosure closure3 = CommentClosure.createCommentClosure(childComment, childComment, 0);

        commentClosureRepository.save(closure1);
        commentClosureRepository.save(closure2);
        commentClosureRepository.save(closure3);

        // When: 중간 노드(자식 댓글)를 자손으로 하는 클로저들 조회
        Optional<List<CommentClosure>> result = commentClosureRepository
                .findByDescendantId(childComment.getId());

        // Then: 자식 댓글과 관련된 클로저들 반환
        assertThat(result).isPresent();
        List<CommentClosure> closures = result.get();
        assertThat(closures).hasSize(2); // 부모->자식, 자식->자식

        boolean hasParentToChild = closures.stream()
                .anyMatch(c -> c.getAncestor().equals(parentComment) && c.getDepth() == 1);
        boolean hasSelfReference = closures.stream()
                .anyMatch(c -> c.getAncestor().equals(childComment) && c.getDepth() == 0);

        assertThat(hasParentToChild).isTrue();
        assertThat(hasSelfReference).isTrue();
    }

    @Test
    @DisplayName("정상 케이스 - 루트 노드 자손 ID로 클로저 조회")
    void shouldFindClosureForRootNode_WhenRootNodeDescendantIdProvided() {
        // Given: 루트 노드의 자기 참조 클로저만 저장
        CommentClosure selfClosure = CommentClosure.createCommentClosure(parentComment, parentComment, 0);
        commentClosureRepository.save(selfClosure);

        // When: 루트 노드(부모 댓글)를 자손으로 하는 클로저들 조회
        Optional<List<CommentClosure>> result = commentClosureRepository
                .findByDescendantId(parentComment.getId());

        // Then: 자기 참조 클로저만 반환
        assertThat(result).isPresent();
        List<CommentClosure> closures = result.get();
        assertThat(closures).hasSize(1);

        CommentClosure closure = closures.getFirst();
        assertThat(closure.getAncestor()).isEqualTo(parentComment);
        assertThat(closure.getDescendant()).isEqualTo(parentComment);
        assertThat(closure.getDepth()).isEqualTo(0);
    }

    @Test
    @DisplayName("경계값 - 존재하지 않는 자손 ID로 클로저 조회")
    void shouldReturnEmpty_WhenDescendantIdNotExists() {
        // Given: 기존 클로저들과 존재하지 않는 자손 ID
        CommentClosure existingClosure = CommentClosure.createCommentClosure(parentComment, childComment, 1);
        commentClosureRepository.save(existingClosure);

        Long nonExistentDescendantId = 999L;

        // When: 존재하지 않는 자손 ID로 클로저 조회
        Optional<List<CommentClosure>> result = commentClosureRepository
                .findByDescendantId(nonExistentDescendantId);

        // Then: 빈 Optional 또는 빈 리스트 반환
        if (result.isPresent()) {
            assertThat(result.get()).isEmpty();
        } else {
            assertThat(result).isEmpty();
        }
    }

    @Test
    @DisplayName("경계값 - 빈 데이터베이스에서 클로저 조회")
    void shouldReturnEmpty_WhenDatabaseIsEmptyForQuery() {
        // Given: 빈 데이터베이스

        // When: 빈 데이터베이스에서 클로저 조회
        Optional<List<CommentClosure>> result = commentClosureRepository
                .findByDescendantId(childComment.getId());

        // Then: 빈 결과 반환
        if (result.isPresent()) {
            assertThat(result.get()).isEmpty();
        } else {
            assertThat(result).isEmpty();
        }
    }

    // ============================================================================
    // 통합 테스트: 명령과 조회 연계
    // ============================================================================

    @Test
    @DisplayName("통합 테스트 - 클로저 저장 후 조회")
    void shouldSaveAndQuery_WhenIntegratedOperationPerformed() {
        // Given: 클로저 저장
        CommentClosure closure = CommentClosure.createCommentClosure(parentComment, childComment, 1);
        commentSaveAdapter.save(closure);

        // When: 저장된 클로저 조회
        Optional<List<CommentClosure>> result = commentClosureRepository
                .findByDescendantId(childComment.getId());

        // Then: 저장한 클로저가 조회됨
        assertThat(result).isPresent();
        List<CommentClosure> closures = result.get();
        assertThat(closures).hasSize(1);
        assertThat(closures.get(0).getAncestor()).isEqualTo(parentComment);
        assertThat(closures.get(0).getDescendant()).isEqualTo(childComment);
        assertThat(closures.get(0).getDepth()).isEqualTo(1);
    }

    @Test
    @DisplayName("통합 테스트 - 클로저 저장 후 삭제")
    void shouldSaveAndDelete_WhenIntegratedOperationPerformed() {
        // Given: 클로저 저장
        CommentClosure closure = CommentClosure.createCommentClosure(parentComment, childComment, 1);
        commentSaveAdapter.save(closure);
        
        // 저장 확인
        assertThat(commentClosureRepository.findAll()).hasSize(1);

        // When: 저장된 클로저 삭제
        commentRepository.deleteClosuresByDescendantId(childComment.getId());

        // Then: 클로저가 삭제됨
        assertThat(commentClosureRepository.findAll()).isEmpty();
    }
}