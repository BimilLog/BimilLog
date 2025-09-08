package jaeik.bimillog.domain.comment.service;

import jaeik.bimillog.domain.comment.application.port.in.CommentCommandUseCase;
import jaeik.bimillog.domain.comment.entity.Comment;
import jaeik.bimillog.infrastructure.adapter.comment.out.persistence.comment.comment.CommentRepository;
import jaeik.bimillog.infrastructure.adapter.comment.out.persistence.comment.commentclosure.CommentClosureRepository;
import jaeik.bimillog.testutil.TestContainersConfiguration;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.junit.jupiter.Testcontainers;

/**
 * <h2>댓글 삭제 쿼리 분석 테스트</h2>
 * <p>댓글 삭제 로직의 쿼리 패턴을 분석하기 위한 테스트</p>
 * <p>SQL 로그를 활성화하여 실제 실행되는 쿼리 확인</p>
 *
 * @author Jaeik
 * @version 2.0.0
 */
@SpringBootTest
@Testcontainers
@Import(TestContainersConfiguration.class)
@ActiveProfiles("test")
@TestPropertySource(properties = {
    "logging.level.org.hibernate.SQL=DEBUG",
    "logging.level.org.hibernate.type.descriptor.sql.BasicBinder=TRACE",
    "spring.jpa.show-sql=true",
    "spring.jpa.properties.hibernate.format_sql=true",
    "spring.jpa.properties.hibernate.use_sql_comments=true"
})
@DisplayName("댓글 삭제 쿼리 분석 테스트")
class CommentDeleteQueryAnalysisTest {

    @Autowired
    private CommentCommandUseCase commentCommandUseCase;
    
    @Autowired
    private CommentRepository commentRepository;
    
    @Autowired
    private CommentClosureRepository commentClosureRepository;

    @Test
    @DisplayName("단순 댓글 삭제 - 쿼리 패턴 확인")
    @Transactional
    void shouldAnalyzeQueryPattern_WhenDeletingSingleComment() {
        // Given: 기존 테스트에서 생성된 댓글이 있다고 가정 (ID = 1)
        Long existingCommentId = 1L;
        
        Comment.Request deleteRequest = Comment.Request.builder()
                .id(existingCommentId)
                .build();
        
        System.out.println("=== 댓글 삭제 쿼리 분석 시작 ===");
        System.out.println("삭제 대상 댓글 ID: " + existingCommentId);
        
        try {
            // When: 댓글 삭제 실행
            commentCommandUseCase.deleteComment(1L, deleteRequest);
            System.out.println("댓글 삭제 완료");
            
        } catch (Exception e) {
            System.out.println("댓글 삭제 실패: " + e.getMessage());
            // 실패해도 쿼리 패턴은 확인 가능
        }
        
        System.out.println("=== 댓글 삭제 쿼리 분석 완료 ===");
    }

    @Test
    @DisplayName("댓글 개수 확인 - 현재 DB 상태")
    @Transactional(readOnly = true)
    void shouldCheckCurrentState_CommentsAndClosures() {
        System.out.println("=== 현재 DB 상태 확인 ===");
        
        long commentCount = commentRepository.count();
        long closureCount = commentClosureRepository.count();
        
        System.out.println("총 댓글 수: " + commentCount);
        System.out.println("총 클로저 수: " + closureCount);
        
        if (commentCount > 0) {
            System.out.println("첫 번째 댓글로 삭제 테스트 가능");
        } else {
            System.out.println("댓글이 없음 - 먼저 댓글을 생성해야 함");
        }
    }

    @Test
    @DisplayName("hasDescendants 쿼리 분석")
    @Transactional(readOnly = true)  
    void shouldAnalyzeHasDescendantsQuery() {
        System.out.println("=== hasDescendants 쿼리 분석 ===");
        
        // hasDescendants 메서드가 제거되어 이 테스트 코드도 제거됨
        try {
            System.out.println("hasDescendants 메서드는 더 이상 사용되지 않습니다.");
            
        } catch (Exception e) {
            System.out.println("hasDescendants 조회 실패: " + e.getMessage());
        }
    }

    @Test
    @DisplayName("클로저 삭제 쿼리 분석")
    @Transactional
    void shouldAnalyzeClosureDeleteQuery() {
        System.out.println("=== 클로저 삭제 쿼리 분석 ===");
        
        try {
            // 존재하지 않는 댓글 ID로도 삭제 쿼리 확인 가능
            commentClosureRepository.deleteByDescendantId(999L);
            System.out.println("클로저 삭제 쿼리 실행 완료");
            
        } catch (Exception e) {
            System.out.println("클로저 삭제 실패: " + e.getMessage());
        }
    }
}