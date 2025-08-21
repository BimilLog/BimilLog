package jaeik.growfarm.infrastructure.adapter.comment.out.persistence.comment.commentclosure;

import com.querydsl.jpa.impl.JPAQueryFactory;
import jakarta.persistence.EntityManager;
import jaeik.growfarm.GrowfarmApplication;
import jaeik.growfarm.domain.comment.entity.Comment;
import jaeik.growfarm.domain.comment.entity.CommentClosure;
import jaeik.growfarm.domain.common.entity.SocialProvider;
import jaeik.growfarm.domain.post.entity.Post;
import jaeik.growfarm.domain.user.entity.Setting;
import jaeik.growfarm.domain.user.entity.User;
import jaeik.growfarm.domain.user.entity.UserRole;
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
 * <h2>CommentClosureQueryAdapter 통합 테스트</h2>
 * <p>실제 MySQL 데이터베이스를 사용한 CommentClosureQueryAdapter의 통합 테스트</p>
 * <p>TestContainers를 사용하여 실제 MySQL 환경에서 댓글 클로저 조회 동작 검증</p>
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
        "jaeik.growfarm.infrastructure.adapter.comment.out.persistence.comment.commentclosure"
})
@Import(CommentClosureQueryAdapter.class)
@TestPropertySource(properties = {
        "spring.jpa.hibernate.ddl-auto=create"
})
class CommentClosureQueryAdapterIntegrationTest {

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
    private CommentClosureQueryAdapter commentClosureQueryAdapter;

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

    @Test
    @DisplayName("정상 케이스 - 자손 ID로 클로저 목록 조회")
    void shouldFindClosuresByDescendantId_WhenValidDescendantIdProvided() {
        // Given: 계층 구조를 나타내는 클로저들 저장
        // 부모 -> 자식 -> 손자 계층 구조
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
        Optional<List<CommentClosure>> result = commentClosureQueryAdapter
                .findByDescendantId(grandChildComment.getId());

        // Then: 손자 댓글과 관련된 모든 클로저 반환
        assertThat(result).isPresent();
        List<CommentClosure> closures = result.get();
        assertThat(closures).hasSize(3); // 부모->손자, 자식->손자, 손자->손자

        // 깊이별로 검증
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
        // Given: 계층 구조의 클로저들 저장
        CommentClosure closure1 = CommentClosure.createCommentClosure(parentComment, parentComment, 0);
        CommentClosure closure2 = CommentClosure.createCommentClosure(parentComment, childComment, 1);
        CommentClosure closure3 = CommentClosure.createCommentClosure(childComment, childComment, 0);

        commentClosureRepository.save(closure1);
        commentClosureRepository.save(closure2);
        commentClosureRepository.save(closure3);

        // When: 중간 노드(자식 댓글)를 자손으로 하는 클로저들 조회
        Optional<List<CommentClosure>> result = commentClosureQueryAdapter
                .findByDescendantId(childComment.getId());

        // Then: 자식 댓글과 관련된 클로저들 반환
        assertThat(result).isPresent();
        List<CommentClosure> closures = result.get();
        assertThat(closures).hasSize(2); // 부모->자식, 자식->자식

        // 조상들 검증
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
        Optional<List<CommentClosure>> result = commentClosureQueryAdapter
                .findByDescendantId(parentComment.getId());

        // Then: 자기 참조 클로저만 반환
        assertThat(result).isPresent();
        List<CommentClosure> closures = result.get();
        assertThat(closures).hasSize(1);

        CommentClosure closure = closures.get(0);
        assertThat(closure.getAncestor()).isEqualTo(parentComment);
        assertThat(closure.getDescendant()).isEqualTo(parentComment);
        assertThat(closure.getDepth()).isEqualTo(0);
    }

    @Test
    @DisplayName("정상 케이스 - 자손이 있는 댓글의 자손 존재 여부 확인")
    void shouldReturnTrue_WhenCommentHasDescendants() {
        // Given: 자손이 있는 계층 구조 클로저들 저장
        // 부모가 자식과 손자를 자손으로 가지는 구조
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

        // When: 부모 댓글의 자손 존재 여부 확인
        boolean hasDescendants = commentClosureQueryAdapter.hasDescendants(parentComment.getId());

        // Then: 자손이 존재하므로 true 반환
        assertThat(hasDescendants).isTrue();
    }

    @Test
    @DisplayName("정상 케이스 - 중간 노드의 자손 존재 여부 확인")
    void shouldReturnTrue_WhenMiddleNodeHasDescendants() {
        // Given: 중간 노드가 자손을 가지는 구조
        CommentClosure closure1 = CommentClosure.createCommentClosure(parentComment, childComment, 1);
        CommentClosure closure2 = CommentClosure.createCommentClosure(childComment, childComment, 0);
        CommentClosure closure3 = CommentClosure.createCommentClosure(childComment, grandChildComment, 1);
        CommentClosure closure4 = CommentClosure.createCommentClosure(grandChildComment, grandChildComment, 0);

        commentClosureRepository.save(closure1);
        commentClosureRepository.save(closure2);
        commentClosureRepository.save(closure3);
        commentClosureRepository.save(closure4);

        // When: 중간 노드(자식 댓글)의 자손 존재 여부 확인
        boolean hasDescendants = commentClosureQueryAdapter.hasDescendants(childComment.getId());

        // Then: 자손이 존재하므로 true 반환
        assertThat(hasDescendants).isTrue();
    }

    @Test
    @DisplayName("정상 케이스 - 말단 노드의 자손 존재 여부 확인")
    void shouldReturnFalse_WhenLeafNodeHasNoDescendants() {
        // Given: 말단 노드(자기 자신만 참조)
        CommentClosure selfClosure = CommentClosure.createCommentClosure(grandChildComment, grandChildComment, 0);
        commentClosureRepository.save(selfClosure);

        // When: 말단 노드(손자 댓글)의 자손 존재 여부 확인
        boolean hasDescendants = commentClosureQueryAdapter.hasDescendants(grandChildComment.getId());

        // Then: 자손이 없으므로 false 반환
        assertThat(hasDescendants).isFalse();
    }

    @Test
    @DisplayName("경계값 - 존재하지 않는 자손 ID로 클로저 조회")
    void shouldReturnEmpty_WhenDescendantIdNotExists() {
        // Given: 기존 클로저들과 존재하지 않는 자손 ID
        CommentClosure existingClosure = CommentClosure.createCommentClosure(parentComment, childComment, 1);
        commentClosureRepository.save(existingClosure);

        Long nonExistentDescendantId = 999L;

        // When: 존재하지 않는 자손 ID로 클로저 조회
        Optional<List<CommentClosure>> result = commentClosureQueryAdapter
                .findByDescendantId(nonExistentDescendantId);

        // Then: 빈 Optional 또는 빈 리스트 반환
        if (result.isPresent()) {
            assertThat(result.get()).isEmpty();
        } else {
            assertThat(result).isEmpty();
        }
    }

    @Test
    @DisplayName("경계값 - 존재하지 않는 댓글 ID의 자손 존재 여부 확인")
    void shouldReturnFalse_WhenCommentIdNotExists() {
        // Given: 기존 클로저와 존재하지 않는 댓글 ID
        CommentClosure existingClosure = CommentClosure.createCommentClosure(parentComment, childComment, 1);
        commentClosureRepository.save(existingClosure);

        Long nonExistentCommentId = 999L;

        // When: 존재하지 않는 댓글 ID의 자손 존재 여부 확인
        boolean hasDescendants = commentClosureQueryAdapter.hasDescendants(nonExistentCommentId);

        // Then: 자손이 없으므로 false 반환
        assertThat(hasDescendants).isFalse();
    }

    @Test
    @DisplayName("경계값 - 빈 데이터베이스에서 자손 존재 여부 확인")
    void shouldReturnFalse_WhenDatabaseIsEmpty() {
        // Given: 빈 데이터베이스 (setUp에서 이미 deleteAll() 실행됨)

        // When: 빈 데이터베이스에서 자손 존재 여부 확인
        boolean hasDescendants = commentClosureQueryAdapter.hasDescendants(parentComment.getId());

        // Then: 자손이 없으므로 false 반환
        assertThat(hasDescendants).isFalse();
    }

    @Test
    @DisplayName("경계값 - 빈 데이터베이스에서 클로저 조회")
    void shouldReturnEmpty_WhenDatabaseIsEmptyForQuery() {
        // Given: 빈 데이터베이스

        // When: 빈 데이터베이스에서 클로저 조회
        Optional<List<CommentClosure>> result = commentClosureQueryAdapter
                .findByDescendantId(childComment.getId());

        // Then: 빈 결과 반환
        if (result.isPresent()) {
            assertThat(result.get()).isEmpty();
        } else {
            assertThat(result).isEmpty();
        }
    }

    @Test
    @DisplayName("트랜잭션 - 복잡한 계층 구조에서 정확한 클로저 조회")
    void shouldQueryComplexHierarchy_WhenMultipleBranchesExist() {
        // Given: 복잡한 계층 구조 (부모 -> 자식1, 자식2 -> 손자1, 손자2)
        Comment child2Comment = Comment.createComment(testPost, testUser, "자식2 댓글", null);
        Comment grandChild2Comment = Comment.createComment(testPost, testUser, "손자2 댓글", null);
        entityManager.persistAndFlush(child2Comment);
        entityManager.persistAndFlush(grandChild2Comment);

        // 복잡한 클로저 구조 생성
        CommentClosure[] closures = {
            CommentClosure.createCommentClosure(parentComment, parentComment, 0),
            CommentClosure.createCommentClosure(parentComment, childComment, 1),
            CommentClosure.createCommentClosure(parentComment, child2Comment, 1),
            CommentClosure.createCommentClosure(parentComment, grandChildComment, 2),
            CommentClosure.createCommentClosure(parentComment, grandChild2Comment, 2),
            CommentClosure.createCommentClosure(childComment, childComment, 0),
            CommentClosure.createCommentClosure(childComment, grandChildComment, 1),
            CommentClosure.createCommentClosure(child2Comment, child2Comment, 0),
            CommentClosure.createCommentClosure(child2Comment, grandChild2Comment, 1),
            CommentClosure.createCommentClosure(grandChildComment, grandChildComment, 0),
            CommentClosure.createCommentClosure(grandChild2Comment, grandChild2Comment, 0)
        };

        for (CommentClosure closure : closures) {
            commentClosureRepository.save(closure);
        }

        // When: 각 노드의 자손 존재 여부 확인
        boolean parentHasDescendants = commentClosureQueryAdapter.hasDescendants(parentComment.getId());
        boolean child1HasDescendants = commentClosureQueryAdapter.hasDescendants(childComment.getId());
        boolean child2HasDescendants = commentClosureQueryAdapter.hasDescendants(child2Comment.getId());
        boolean grandChild1HasDescendants = commentClosureQueryAdapter.hasDescendants(grandChildComment.getId());
        boolean grandChild2HasDescendants = commentClosureQueryAdapter.hasDescendants(grandChild2Comment.getId());

        // Then: 계층 구조에 따른 정확한 결과 반환
        assertThat(parentHasDescendants).isTrue(); // 4개의 자손
        assertThat(child1HasDescendants).isTrue(); // 1개의 자손
        assertThat(child2HasDescendants).isTrue(); // 1개의 자손
        assertThat(grandChild1HasDescendants).isFalse(); // 자손 없음
        assertThat(grandChild2HasDescendants).isFalse(); // 자손 없음

        // 부모 노드의 모든 자손 클로저 조회 검증
        Optional<List<CommentClosure>> parentDescendants = commentClosureQueryAdapter
                .findByDescendantId(parentComment.getId());
        assertThat(parentDescendants).isPresent();
        assertThat(parentDescendants.get()).hasSize(1); // 자기 자신만
    }

    @Test
    @DisplayName("트랜잭션 - 깊이별 클로저 조회 검증")
    void shouldQueryByDepth_WhenHierarchicalStructureExists() {
        // Given: 4레벨 깊이의 계층 구조
        Comment level4Comment = Comment.createComment(testPost, testUser, "4레벨 댓글", null);
        entityManager.persistAndFlush(level4Comment);

        // 깊이별 클로저 저장
        CommentClosure depth0 = CommentClosure.createCommentClosure(parentComment, parentComment, 0);
        CommentClosure depth1 = CommentClosure.createCommentClosure(parentComment, childComment, 1);
        CommentClosure depth2 = CommentClosure.createCommentClosure(parentComment, grandChildComment, 2);
        CommentClosure depth3 = CommentClosure.createCommentClosure(parentComment, level4Comment, 3);

        commentClosureRepository.save(depth0);
        commentClosureRepository.save(depth1);
        commentClosureRepository.save(depth2);
        commentClosureRepository.save(depth3);

        // When: 최하위 레벨 댓글의 조상들 조회
        Optional<List<CommentClosure>> ancestors = commentClosureQueryAdapter
                .findByDescendantId(level4Comment.getId());

        // Then: 모든 조상이 올바른 깊이로 조회됨
        assertThat(ancestors).isPresent();
        List<CommentClosure> closures = ancestors.get();
        assertThat(closures).hasSize(1); // 직접 조상만 (부모->level4)

        CommentClosure ancestorClosure = closures.get(0);
        assertThat(ancestorClosure.getDepth()).isEqualTo(3);
        assertThat(ancestorClosure.getAncestor()).isEqualTo(parentComment);
        assertThat(ancestorClosure.getDescendant()).isEqualTo(level4Comment);
    }
}