package jaeik.bimillog.domain.comment.application.service;

import jaeik.bimillog.domain.comment.application.port.out.*;
import jaeik.bimillog.domain.comment.entity.Comment;
import jaeik.bimillog.domain.comment.entity.CommentClosure;
import jaeik.bimillog.domain.comment.entity.CommentRequest;
import jaeik.bimillog.domain.post.entity.Post;
import jaeik.bimillog.domain.user.entity.User;
import jaeik.bimillog.domain.user.entity.Setting;
import jaeik.bimillog.domain.user.entity.UserRole;
import jaeik.bimillog.domain.auth.entity.SocialProvider;
import jaeik.bimillog.util.TestContainersConfiguration;
import jaeik.bimillog.infrastructure.adapter.comment.out.persistence.comment.comment.CommentRepository;
import jaeik.bimillog.infrastructure.adapter.comment.out.persistence.comment.commentclosure.CommentClosureRepository;
import org.hibernate.SessionFactory;
import org.hibernate.stat.Statistics;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.junit.jupiter.Testcontainers;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * <h2>댓글 삭제 성능 테스트</h2>
 * <p>댓글 삭제 로직에서 N+1 쿼리 문제 및 성능 이슈를 검증하는 테스트</p>
 * <p>실제 데이터베이스 환경에서 쿼리 횟수와 성능을 측정</p>
 *
 * @author Jaeik
 * @version 2.0.0
 */
@SpringBootTest
@Testcontainers
@Import(TestContainersConfiguration.class)
@ActiveProfiles("test")
@DisplayName("댓글 삭제 성능 테스트")
class CommentDeletePerformanceTest {

    @Autowired
    private CommentCommandService commentCommandService;
    
    @Autowired
    private CommentCommandPort commentCommandPort;
    
    @Autowired
    private CommentQueryPort commentQueryPort;
    
    @Autowired
    private CommentClosureCommandPort commentClosureCommandPort;
    
    @Autowired
    private CommentClosureQueryPort commentClosureQueryPort;
    
    @Autowired
    private CommentToPostPort commentToPostPort;
    
    @Autowired
    private CommentToUserPort commentToUserPort;
    
    @Autowired
    private CommentRepository commentRepository;
    
    @Autowired 
    private CommentClosureRepository commentClosureRepository;
    
    @Autowired
    private EntityManagerFactory entityManagerFactory;
    
    @Autowired
    private EntityManager entityManager;

    private User testUser;
    private Post testPost;
    private Statistics hibernateStats;

    @BeforeEach
    void setUp() {
        // Hibernate 통계 초기화 및 활성화
        SessionFactory sessionFactory = entityManagerFactory.unwrap(SessionFactory.class);
        hibernateStats = sessionFactory.getStatistics();
        hibernateStats.setStatisticsEnabled(true);
        hibernateStats.clear();
        
        // 테스트 데이터 준비
        setupTestData();
    }

    @Test
    @DisplayName("단일 댓글 삭제 - 쿼리 수 측정")
    @Transactional
    void shouldMeasureQueryCount_WhenDeletingSingleComment() {
        // Given: 자손이 없는 단일 댓글
        Comment singleComment = createAndSaveComment("단일 댓글");
        CommentClosure selfClosure = CommentClosure.createCommentClosure(singleComment, singleComment, 0);
        commentClosureCommandPort.save(selfClosure);
        
        CommentRequest deleteRequest = CommentRequest.builder()
                .id(singleComment.getId())
                .build();
        
        // 통계 초기화
        hibernateStats.clear();
        entityManager.clear();
        
        // When: 댓글 삭제 실행
        commentCommandService.deleteComment(testUser.getId(), deleteRequest);
        entityManager.flush();
        
        // Then: 쿼리 수 검증
        long queryCount = hibernateStats.getQueryExecutionCount();
        long deleteCount = hibernateStats.getEntityDeleteCount();
        
        System.out.println("=== 단일 댓글 삭제 성능 측정 ===");
        System.out.println("총 쿼리 실행 수: " + queryCount);
        System.out.println("삭제된 엔티티 수: " + deleteCount);
        System.out.println("예상 쿼리: SELECT(댓글조회) + SELECT(자손체크) + DELETE(클로저) + DELETE(댓글) = 4개");
        
        // 성능 검증: 4-6개 쿼리 정도가 적정선
        assertThat(queryCount).isLessThanOrEqualTo(6);
    }

    @Test
    @DisplayName("계층 댓글 삭제 - N+1 쿼리 문제 확인")
    @Transactional
    void shouldCheckNPlusOneQuery_WhenDeletingHierarchicalComments() {
        // Given: 계층 구조 댓글 생성 (부모 -> 자식 -> 손자)
        Comment parentComment = createAndSaveComment("부모 댓글");
        Comment childComment = createAndSaveComment("자식 댓글");
        Comment grandChildComment = createAndSaveComment("손자 댓글");
        
        // 클로저 테이블 구성
        setupCommentHierarchy(parentComment, childComment, grandChildComment);
        
        CommentRequest deleteRequest = CommentRequest.builder()
                .id(parentComment.getId())
                .build();
        
        // 통계 초기화
        hibernateStats.clear();
        entityManager.clear();
        
        // When: 부모 댓글 삭제 (자손이 있으므로 소프트 삭제)
        commentCommandService.deleteComment(testUser.getId(), deleteRequest);
        entityManager.flush();
        
        // Then: 쿼리 수 분석
        long queryCount = hibernateStats.getQueryExecutionCount();
        long updateCount = hibernateStats.getEntityUpdateCount();
        
        System.out.println("=== 계층 댓글 삭제 성능 측정 ===");
        System.out.println("총 쿼리 실행 수: " + queryCount);
        System.out.println("업데이트된 엔티티 수: " + updateCount);
        System.out.println("예상 쿼리: SELECT(댓글조회) + SELECT(자손체크) + UPDATE(소프트삭제) = 3개");
        
        // 성능 검증: 소프트 삭제는 더 효율적이어야 함
        assertThat(queryCount).isLessThanOrEqualTo(4);
        
        // 소프트 삭제 확인
        Optional<Comment> deletedComment = commentQueryPort.findById(parentComment.getId());
        assertThat(deletedComment).isPresent();
        assertThat(deletedComment.get().isDeleted()).isTrue();
        assertThat(deletedComment.get().getContent()).isEqualTo("삭제된 댓글 입니다.");
    }

    @Test
    @DisplayName("대량 댓글 삭제 성능 - 스케일링 테스트")
    @Transactional
    void shouldPerformWell_WhenDeletingManyComments() {
        // Given: 10개의 독립적인 댓글 생성 (자손 없음)
        for (int i = 0; i < 10; i++) {
            Comment comment = createAndSaveComment("댓글 " + i);
            CommentClosure selfClosure = CommentClosure.createCommentClosure(comment, comment, 0);
            commentClosureCommandPort.save(selfClosure);
        }
        
        List<Comment> allComments = commentRepository.findAll();
        assertThat(allComments).hasSize(10);
        
        // 통계 초기화
        hibernateStats.clear();
        entityManager.clear();
        
        long startTime = System.currentTimeMillis();
        
        // When: 모든 댓글을 순차적으로 삭제
        for (Comment comment : allComments) {
            CommentRequest deleteRequest = CommentRequest.builder()
                    .id(comment.getId())
                    .build();
            commentCommandService.deleteComment(testUser.getId(), deleteRequest);
        }
        entityManager.flush();
        
        long endTime = System.currentTimeMillis();
        long executionTime = endTime - startTime;
        
        // Then: 성능 측정 및 분석
        long queryCount = hibernateStats.getQueryExecutionCount();
        long deleteCount = hibernateStats.getEntityDeleteCount();
        
        System.out.println("=== 대량 댓글 삭제 성능 측정 ===");
        System.out.println("댓글 수: 10개");
        System.out.println("총 쿼리 실행 수: " + queryCount);
        System.out.println("삭제된 엔티티 수: " + deleteCount);
        System.out.println("실행 시간: " + executionTime + "ms");
        System.out.println("댓글당 평균 쿼리 수: " + (queryCount / 10.0));
        
        // 성능 기준: 댓글당 6개 이하의 쿼리 (적정선)
        // 실제로는 N+1 문제로 더 많을 수 있음을 확인
        assertThat(queryCount / 10.0).describedAs("댓글당 평균 쿼리 수").isLessThanOrEqualTo(8.0);
        assertThat(executionTime).describedAs("10개 댓글 삭제 시간").isLessThan(5000); // 5초 이내
    }

    @Test
    @DisplayName("복잡한 계층 구조 삭제 - 최악의 시나리오")
    @Transactional 
    void shouldAnalyzeWorstCaseScenario_WhenDeletingDeepHierarchy() {
        // Given: 깊은 계층 구조 (5단계 깊이)
        Comment level1 = createAndSaveComment("1단계 댓글");
        Comment level2 = createAndSaveComment("2단계 댓글");
        Comment level3 = createAndSaveComment("3단계 댓글");
        Comment level4 = createAndSaveComment("4단계 댓글");
        Comment level5 = createAndSaveComment("5단계 댓글");
        
        // 복잡한 클로저 구조 생성
        setupDeepHierarchy(List.of(level1, level2, level3, level4, level5));
        
        // 통계 초기화
        hibernateStats.clear();
        entityManager.clear();
        
        // When: 최상위 댓글 삭제 (모든 하위 댓글 때문에 소프트 삭제)
        CommentRequest deleteRequest = CommentRequest.builder()
                .id(level1.getId())
                .build();
        
        long startTime = System.currentTimeMillis();
        commentCommandService.deleteComment(testUser.getId(), deleteRequest);
        entityManager.flush();
        long endTime = System.currentTimeMillis();
        
        // Then: 최악의 시나리오 성능 분석
        long queryCount = hibernateStats.getQueryExecutionCount();
        long executionTime = endTime - startTime;
        
        System.out.println("=== 복잡한 계층 구조 삭제 성능 측정 ===");
        System.out.println("계층 깊이: 5단계");
        System.out.println("총 쿼리 실행 수: " + queryCount);
        System.out.println("실행 시간: " + executionTime + "ms");
        
        // 성능 기준: 계층이 깊어도 쿼리 수는 선형적으로만 증가해야 함
        assertThat(queryCount).describedAs("복잡한 계층 구조 쿼리 수").isLessThan(15);
        assertThat(executionTime).describedAs("복잡한 계층 구조 실행 시간").isLessThan(3000); // 3초 이내
    }

    @Test
    @DisplayName("쿼리 최적화 확인 - 배치 삭제 vs 개별 삭제")
    @Transactional
    void shouldComparePerformance_BatchVsIndividualDelete() {
        // Given: 동일한 게시글에 10개 댓글 생성
        for (int i = 0; i < 10; i++) {
            Comment comment = createAndSaveComment("배치 테스트 댓글 " + i);
            CommentClosure selfClosure = CommentClosure.createCommentClosure(comment, comment, 0);
            commentClosureCommandPort.save(selfClosure);
        }
        
        // 통계 초기화
        hibernateStats.clear();
        entityManager.clear();
        
        // When: 개별 삭제 방식으로 모든 댓글 삭제
        List<Comment> comments = commentRepository.findAll();
        
        long startTime = System.currentTimeMillis();
        for (Comment comment : comments) {
            CommentRequest deleteRequest = CommentRequest.builder()
                    .id(comment.getId())
                    .build();
            commentCommandService.deleteComment(testUser.getId(), deleteRequest);
        }
        entityManager.flush();
        long endTime = System.currentTimeMillis();
        
        // Then: 개별 삭제 성능 분석
        long individualDeleteQueries = hibernateStats.getQueryExecutionCount();
        long individualDeleteTime = endTime - startTime;
        
        System.out.println("=== 개별 삭제 vs 배치 삭제 성능 비교 ===");
        System.out.println("개별 삭제 - 쿼리 수: " + individualDeleteQueries + ", 시간: " + individualDeleteTime + "ms");
        
        // 성능 임계값: 10개 댓글에 대해 과도한 쿼리 실행 확인
        // N+1 문제가 있다면 쿼리 수가 선형적으로 증가할 것
        double queriesPerComment = (double) individualDeleteQueries / 10;
        System.out.println("댓글당 평균 쿼리 수: " + queriesPerComment);
        
        // 경고: 댓글당 4개 이상의 쿼리가 실행되면 최적화 필요
        if (queriesPerComment > 4.0) {
            System.out.println("⚠️ 성능 경고: 댓글당 평균 " + queriesPerComment + "개 쿼리 실행됨");
            System.out.println("⚠️ N+1 쿼리 문제 가능성이 높습니다.");
        }
        
        assertThat(queriesPerComment).describedAs("댓글당 쿼리 수가 과도함").isLessThan(10.0);
    }

    @Test
    @DisplayName("클로저 테이블 삭제 성능 - 조회 후 삭제 패턴 분석")
    @Transactional
    void shouldAnalyzeClosureTablePerformance_WhenDeletingWithoutDescendants() {
        // Given: 자손이 없는 댓글 (완전 삭제 대상)
        Comment leafComment = createAndSaveComment("말단 댓글");
        CommentClosure selfClosure = CommentClosure.createCommentClosure(leafComment, leafComment, 0);
        commentClosureCommandPort.save(selfClosure);
        
        CommentRequest deleteRequest = CommentRequest.builder()
                .id(leafComment.getId())
                .build();
        
        // 통계 초기화
        hibernateStats.clear();
        entityManager.clear();
        
        // When: 말단 댓글 삭제 (hasDescendants = false)
        long startTime = System.currentTimeMillis();
        commentCommandService.deleteComment(testUser.getId(), deleteRequest);
        entityManager.flush();
        long endTime = System.currentTimeMillis();
        
        // Then: 클로저 테이블 삭제 패턴 분석
        long queryCount = hibernateStats.getQueryExecutionCount();
        long deleteCount = hibernateStats.getEntityDeleteCount();
        long executionTime = endTime - startTime;
        
        System.out.println("=== 클로저 테이블 삭제 성능 분석 ===");
        System.out.println("총 쿼리 수: " + queryCount);
        System.out.println("삭제된 엔티티 수: " + deleteCount);
        System.out.println("실행 시간: " + executionTime + "ms");
        
        // 기대값: SELECT(댓글) + SELECT(자손체크) + DELETE(클로저) + DELETE(댓글) = 4개
        // 실제로 더 많다면 최적화 필요
        if (queryCount > 5) {
            System.out.println("⚠️ 예상보다 많은 쿼리 실행: " + queryCount + "개");
            System.out.println("⚠️ hasDescendants 체크나 삭제 과정에서 추가 쿼리 발생");
        }
        
        assertThat(deleteCount).isEqualTo(2); // 클로저 1개 + 댓글 1개
        assertThat(queryCount).isLessThan(8); // 과도한 쿼리 방지
    }

    // === 헬퍼 메서드들 ===
    
    private void setupTestData() {
        // 테스트용 사용자 생성
        Setting setting = Setting.createSetting();
        entityManager.persist(setting);
        
        testUser = User.builder()
                .socialId("test123")
                .provider(SocialProvider.KAKAO)
                .userName("testUser")
                .socialNickname("테스트유저")
                .role(UserRole.USER)
                .setting(setting)
                .build();
        entityManager.persist(testUser);
        
        // 테스트용 게시글 생성  
        testPost = Post.builder()
                .user(testUser)
                .title("테스트 게시글")
                .content("테스트 내용")
                .isNotice(false)
                .views(0)
                .build();
        entityManager.persist(testPost);
        entityManager.flush();
    }
    
    private Comment createAndSaveComment(String content) {
        Comment comment = Comment.createComment(testPost, testUser, content, null);
        return commentCommandPort.save(comment);
    }
    
    private void setupCommentHierarchy(Comment parent, Comment child, Comment grandChild) {
        // 자기 자신 클로저 생성
        commentClosureCommandPort.save(CommentClosure.createCommentClosure(parent, parent, 0));
        commentClosureCommandPort.save(CommentClosure.createCommentClosure(child, child, 0));
        commentClosureCommandPort.save(CommentClosure.createCommentClosure(grandChild, grandChild, 0));
        
        // 계층 관계 클로저 생성
        commentClosureCommandPort.save(CommentClosure.createCommentClosure(parent, child, 1));
        commentClosureCommandPort.save(CommentClosure.createCommentClosure(parent, grandChild, 2));
        commentClosureCommandPort.save(CommentClosure.createCommentClosure(child, grandChild, 1));
    }
    
    private void setupDeepHierarchy(List<Comment> comments) {
        // 각 댓글의 자기 자신 클로저
        for (Comment comment : comments) {
            commentClosureCommandPort.save(CommentClosure.createCommentClosure(comment, comment, 0));
        }
        
        // 계층 관계 클로저 생성 (각 레벨이 하위 모든 레벨의 조상)
        for (int ancestorIdx = 0; ancestorIdx < comments.size(); ancestorIdx++) {
            for (int descendantIdx = ancestorIdx + 1; descendantIdx < comments.size(); descendantIdx++) {
                int depth = descendantIdx - ancestorIdx;
                Comment ancestor = comments.get(ancestorIdx);
                Comment descendant = comments.get(descendantIdx);
                commentClosureCommandPort.save(CommentClosure.createCommentClosure(ancestor, descendant, depth));
            }
        }
    }
}