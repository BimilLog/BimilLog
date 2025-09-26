package jaeik.bimillog.adapter.out.comment;

import jaeik.bimillog.BimilLogApplication;
import jaeik.bimillog.domain.comment.entity.Comment;
import jaeik.bimillog.domain.comment.entity.CommentClosure;
import jaeik.bimillog.domain.post.entity.Post;
import jaeik.bimillog.domain.user.entity.User;
import jaeik.bimillog.infrastructure.adapter.out.comment.CommentSaveAdapter;
import jaeik.bimillog.infrastructure.adapter.out.comment.jpa.CommentClosureRepository;
import jaeik.bimillog.infrastructure.adapter.out.comment.jpa.CommentRepository;
import jaeik.bimillog.testutil.CommentTestDataBuilder;
import jaeik.bimillog.testutil.H2TestConfiguration;
import jaeik.bimillog.testutil.PostTestDataBuilder;
import jaeik.bimillog.testutil.TestUsers;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * <h2>댓글 저장 어댑터 통합 테스트</h2>
 * <p>CommentSaveAdapter의 댓글 저장 동작을 검증하는 통합 테스트</p>
 * <p>H2 데이터베이스를 사용하여 클로저 테이블 관련 기능 포함하여 검증</p>
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
@ActiveProfiles("h2test")
@Import({CommentSaveAdapter.class, H2TestConfiguration.class})
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@DisplayName("댓글 저장 어댑터 통합 테스트")
@Tag("integration")
class CommentSaveAdapterIntegrationTest {

    @Autowired
    private TestEntityManager entityManager;
    
    @Autowired
    private CommentRepository commentRepository;
    
    @Autowired
    private CommentClosureRepository commentClosureRepository;

    @Autowired
    private CommentSaveAdapter commentSaveAdapter;

    private User testUser;
    private Post testPost;
    private Comment parentComment;
    private Comment childComment;

    @BeforeEach
    void setUp() {
        // 테스트 데이터 초기화
        commentClosureRepository.deleteAll();
        commentRepository.deleteAll();
        
        // 테스트용 사용자 생성 - TestUsers 활용
        testUser = TestUsers.createUnique();
        entityManager.persistAndFlush(testUser.getSetting());
        entityManager.persistAndFlush(testUser);

        // 테스트용 게시글 생성 - CommentTestDataBuilder 활용
        testPost = PostTestDataBuilder.createPost(testUser, "테스트 게시글", "테스트 게시글 내용입니다.");
        entityManager.persistAndFlush(testPost);
        
        // 부모 댓글 생성 - CommentTestDataBuilder 활용
        parentComment = CommentTestDataBuilder.createComment(testUser, testPost, "부모 댓글");
        parentComment = commentRepository.save(parentComment);
        
        // 자식 댓글 생성 - CommentTestDataBuilder 활용
        childComment = CommentTestDataBuilder.createComment(testUser, testPost, "자식 댓글");
        childComment = commentRepository.save(childComment);
    }

    @Test
    @DisplayName("정상 케이스 - 새로운 댓글 저장")
    void shouldSaveNewComment_WhenValidCommentProvided() {
        // Given
        Comment newComment = Comment.createComment(testPost, testUser, "테스트 댓글 내용", null);

        // When
        Comment result = commentSaveAdapter.save(newComment);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getId()).isNotNull();
        assertThat(result.getContent()).isEqualTo("테스트 댓글 내용");
        assertThat(result.getPost()).isEqualTo(testPost);
        assertThat(result.getUser()).isEqualTo(testUser);
        assertThat(result.getPassword()).isNull();
        
        // 데이터베이스에 실제로 저장되었는지 검증
        Optional<Comment> savedComment = commentRepository.findById(result.getId());
        assertThat(savedComment).isPresent();
        assertThat(savedComment.get().getContent()).isEqualTo("테스트 댓글 내용");
    }

    @Test
    @DisplayName("정상 케이스 - 익명 댓글(비밀번호) 저장")
    void shouldSaveAnonymousComment_WhenPasswordProvided() {
        // Given
        Comment anonymousComment = Comment.createComment(testPost, null, "익명 댓글 내용", 1234);

        // When
        Comment result = commentSaveAdapter.save(anonymousComment);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getId()).isNotNull();
        assertThat(result.getContent()).isEqualTo("익명 댓글 내용");
        assertThat(result.getUser()).isNull();
        assertThat(result.getPassword()).isEqualTo(1234);
        
        // 데이터베이스에 실제로 저장되었는지 검증
        Optional<Comment> savedComment = commentRepository.findById(result.getId());
        assertThat(savedComment).isPresent();
        assertThat(savedComment.get().getUser()).isNull();
        assertThat(savedComment.get().getPassword()).isEqualTo(1234);
    }

    @Test
    @DisplayName("정상 케이스 - 댓글 수정 저장")
    void shouldUpdateComment_WhenCommentModified() {
        // Given
        Comment existingComment = Comment.createComment(testPost, testUser, "원본 댓글", null);
        existingComment = commentRepository.save(existingComment);
        
        // 댓글 수정
        existingComment.updateComment("수정된 댓글");

        // When
        Comment result = commentSaveAdapter.save(existingComment);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(existingComment.getId());
        assertThat(result.getContent()).isEqualTo("수정된 댓글");
        
        // 데이터베이스에서 수정 확인
        Optional<Comment> updatedComment = commentRepository.findById(result.getId());
        assertThat(updatedComment).isPresent();
        assertThat(updatedComment.get().getContent()).isEqualTo("수정된 댓글");
    }

    // ============================================================================
    // 클로저 테이블 관련 기능 테스트
    // ============================================================================

    @Test
    @DisplayName("정상 케이스 - 단일 댓글 클로저 저장")
    void shouldSaveCommentClosure_WhenValidClosureProvided() {
        // Given
        CommentClosure commentClosure = CommentClosure.createCommentClosure(parentComment, childComment, 1);

        // When
        commentSaveAdapter.save(commentClosure);

        // Then
        List<CommentClosure> allClosures = commentClosureRepository.findAll();
        assertThat(allClosures).hasSize(1);

        CommentClosure savedClosure = allClosures.getFirst();
        assertThat(savedClosure.getId()).isNotNull();
        assertThat(savedClosure.getAncestor()).isEqualTo(parentComment);
        assertThat(savedClosure.getDescendant()).isEqualTo(childComment);
        assertThat(savedClosure.getDepth()).isEqualTo(1);
    }

    @Test
    @DisplayName("정상 케이스 - 다중 댓글 클로저 일괄 저장")
    void shouldSaveAllCommentClosures_WhenMultipleClosuresProvided() {
        // Given
        Comment grandChildComment = Comment.createComment(testPost, testUser, "손자 댓글", null);
        grandChildComment = commentRepository.save(grandChildComment);
        
        List<CommentClosure> closures = List.of(
                CommentClosure.createCommentClosure(parentComment, parentComment, 0),
                CommentClosure.createCommentClosure(parentComment, childComment, 1),
                CommentClosure.createCommentClosure(parentComment, grandChildComment, 2),
                CommentClosure.createCommentClosure(childComment, childComment, 0),
                CommentClosure.createCommentClosure(childComment, grandChildComment, 1),
                CommentClosure.createCommentClosure(grandChildComment, grandChildComment, 0)
        );

        // When
        commentSaveAdapter.saveAll(closures);

        // Then
        List<CommentClosure> allClosures = commentClosureRepository.findAll();
        assertThat(allClosures).hasSize(6);

        long depth0Count = allClosures.stream().filter(c -> c.getDepth() == 0).count();
        long depth1Count = allClosures.stream().filter(c -> c.getDepth() == 1).count();
        long depth2Count = allClosures.stream().filter(c -> c.getDepth() == 2).count();

        assertThat(depth0Count).isEqualTo(3); // 자기 참조 3개
        assertThat(depth1Count).isEqualTo(2); // 1단계 관계 2개
        assertThat(depth2Count).isEqualTo(1); // 2단계 관계 1개
    }

    @Test
    @DisplayName("정상 케이스 - 부모 댓글의 조상 클로저 관계 조회")
    void shouldGetParentClosures_WhenParentCommentHasAncestors() {
        // Given: 3레벨 계층 구조 설정
        final Comment grandParentComment = commentRepository.save(
                Comment.createComment(testPost, testUser, "조부모 댓글", null));
        
        // 클로저 관계 저장
        List<CommentClosure> closures = List.of(
                CommentClosure.createCommentClosure(grandParentComment, grandParentComment, 0),
                CommentClosure.createCommentClosure(grandParentComment, parentComment, 1),
                CommentClosure.createCommentClosure(parentComment, parentComment, 0)
        );
        commentClosureRepository.saveAll(closures);

        // When: 부모 댓글의 조상 클로저 관계 조회
        Optional<List<CommentClosure>> result = commentSaveAdapter.getParentClosures(parentComment.getId());

        // Then: 부모 댓글과 관련된 모든 클로저 반환
        assertThat(result).isPresent();
        List<CommentClosure> parentClosures = result.get();
        assertThat(parentClosures).hasSize(2); // 조부모->부모, 부모->부모

        boolean hasGrandParentToParent = parentClosures.stream()
                .anyMatch(c -> c.getAncestor().equals(grandParentComment) && c.getDepth() == 1);
        boolean hasSelfReference = parentClosures.stream()
                .anyMatch(c -> c.getAncestor().equals(parentComment) && c.getDepth() == 0);

        assertThat(hasGrandParentToParent).isTrue();
        assertThat(hasSelfReference).isTrue();
    }

    @Test
    @DisplayName("정상 케이스 - 최상위 댓글의 조상 클로저 관계 조회")
    void shouldGetEmptyParentClosures_WhenRootCommentHasNoAncestors() {
        // Given: 최상위 댓글 (조상이 없음)
        Comment rootComment = Comment.createComment(testPost, testUser, "최상위 댓글", null);
        rootComment = commentRepository.save(rootComment);

        // When: 최상위 댓글의 조상 클로저 관계 조회
        Optional<List<CommentClosure>> result = commentSaveAdapter.getParentClosures(rootComment.getId());

        // Then: 빈 결과 반환
        if (result.isPresent()) {
            assertThat(result.get()).isEmpty();
        } else {
            assertThat(result).isEmpty();
        }
    }

    @Test
    @DisplayName("정상 케이스 - 존재하지 않는 댓글 ID로 조상 클로저 조회")
    void shouldGetEmptyParentClosures_WhenCommentIdNotExists() {
        // Given: 존재하지 않는 댓글 ID
        Long nonExistentCommentId = 999L;

        // When: 존재하지 않는 댓글 ID로 조상 클로저 조회
        Optional<List<CommentClosure>> result = commentSaveAdapter.getParentClosures(nonExistentCommentId);

        // Then: 빈 결과 반환
        if (result.isPresent()) {
            assertThat(result.get()).isEmpty();
        } else {
            assertThat(result).isEmpty();
        }
    }

    // ============================================================================
    // 통합 워크플로우 테스트
    // ============================================================================

    @Test
    @DisplayName("통합 워크플로우 - 댓글 저장 후 클로저 관계 설정")
    void shouldSaveCommentAndClosure_WhenIntegratedWorkflowPerformed() {
        // Given: 새로운 댓글 저장
        Comment newComment = Comment.createComment(testPost, testUser, "통합 테스트 댓글", null);
        Comment savedComment = commentSaveAdapter.save(newComment);

        // When: 저장된 댓글을 위한 클로저 관계 생성
        CommentClosure selfClosure = CommentClosure.createCommentClosure(savedComment, savedComment, 0);
        commentSaveAdapter.save(selfClosure);

        // Then: 댓글과 클로저가 모두 올바르게 저장됨
        Optional<Comment> foundComment = commentRepository.findById(savedComment.getId());
        assertThat(foundComment).isPresent();
        assertThat(foundComment.get().getContent()).isEqualTo("통합 테스트 댓글");

        List<CommentClosure> closures = commentClosureRepository.findAll();
        assertThat(closures.stream()
                .anyMatch(c -> c.getAncestor().equals(savedComment) && c.getDescendant().equals(savedComment))
        ).isTrue();
    }

    @Test
    @DisplayName("통합 워크플로우 - 다단계 댓글 계층 구조 생성")
    void shouldCreateMultiLevelCommentHierarchy_WhenComplexStructureBuilt() {
        // Given: 복잡한 댓글 계층 구조
        final Comment level1 = commentSaveAdapter.save(
            Comment.createComment(testPost, testUser, "1단계 댓글", null));
        final Comment level2 = commentSaveAdapter.save(
            Comment.createComment(testPost, testUser, "2단계 댓글", null));
        final Comment level3 = commentSaveAdapter.save(
            Comment.createComment(testPost, testUser, "3단계 댓글", null));

        // When: 계층 구조 클로저 관계 일괄 생성
        List<CommentClosure> hierarchyClosures = List.of(
                // 1단계 자기 참조
                CommentClosure.createCommentClosure(level1, level1, 0),
                // 1단계 -> 2단계
                CommentClosure.createCommentClosure(level1, level2, 1),
                // 1단계 -> 3단계
                CommentClosure.createCommentClosure(level1, level3, 2),
                // 2단계 자기 참조
                CommentClosure.createCommentClosure(level2, level2, 0),
                // 2단계 -> 3단계
                CommentClosure.createCommentClosure(level2, level3, 1),
                // 3단계 자기 참조
                CommentClosure.createCommentClosure(level3, level3, 0)
        );
        commentSaveAdapter.saveAll(hierarchyClosures);

        // Then: 전체 계층 구조가 올바르게 생성됨
        List<CommentClosure> allClosures = commentClosureRepository.findAll();
        assertThat(allClosures).hasSize(6);
        
        // 최상위 -> 최하위 관계 확인
        boolean hasLevel1ToLevel3 = allClosures.stream()
                .anyMatch(c -> c.getAncestor().equals(level1) && 
                              c.getDescendant().equals(level3) && 
                              c.getDepth() == 2);
        assertThat(hasLevel1ToLevel3).isTrue();
        
        // 3단계 댓글의 조상 클로저 관계 확인
        Optional<List<CommentClosure>> level3Ancestors = commentSaveAdapter.getParentClosures(level3.getId());
        assertThat(level3Ancestors).isPresent();
        assertThat(level3Ancestors.get()).hasSize(3); // 1단계->3단계, 2단계->3단계, 3단계->3단계
    }
}