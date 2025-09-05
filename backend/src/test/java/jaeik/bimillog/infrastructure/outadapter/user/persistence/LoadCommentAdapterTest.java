package jaeik.bimillog.infrastructure.outadapter.user.persistence;

import jaeik.bimillog.domain.comment.application.port.in.CommentQueryUseCase;
import jaeik.bimillog.domain.comment.entity.SimpleCommentInfo;
import jaeik.bimillog.infrastructure.adapter.user.out.persistence.comment.LoadCommentAdapter;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.time.Instant;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

/**
 * <h2>LoadCommentAdapter 테스트</h2>
 * <p>사용자 도메인에서 댓글 도메인으로의 댓글 조회 어댁터 테스트</p>
 * <p>댓글 작성 목록 및 추천 댓글 목록 조회 기능 검증</p>
 * 
 * @author Jaeik
 * @version 2.0.0
 */
@ExtendWith(MockitoExtension.class)
class LoadCommentAdapterTest {

    @Mock
    private CommentQueryUseCase commentQueryUseCase;

    @InjectMocks
    private LoadCommentAdapter loadCommentAdapter;

    @Test
    @DisplayName("정상 케이스 - 사용자 작성 댓글 목록 조회")
    void shouldFindCommentsByUserId_WhenValidUserIdProvided() {
        // Given: 사용자 ID와 페이지 정보, 예상 댓글 목록
        Long userId = 1L;
        Pageable pageable = PageRequest.of(0, 10);
        
        List<SimpleCommentInfo> domainComments = Arrays.asList(
            new SimpleCommentInfo(1L, 1L, "작성자1", "첫 번째 댓글", Instant.now(), 0, false),
            new SimpleCommentInfo(2L, 2L, "작성자2", "두 번째 댓글", Instant.now(), 0, false)
        );
        
        Page<SimpleCommentInfo> domainPage = new PageImpl<>(domainComments, pageable, domainComments.size());
        given(commentQueryUseCase.getUserComments(eq(userId), any(Pageable.class))).willReturn(domainPage);

        // When: 사용자 작성 댓글 목록 조회 실행
        Page<SimpleCommentInfo> result = loadCommentAdapter.findCommentsByUserId(userId, pageable);

        // Then: 올바른 댓글 목록이 반환되고 UseCase가 호출되었는지 검증
        assertThat(result).isNotNull();
        assertThat(result.getContent()).hasSize(2);
        assertThat(result.getContent().get(0).getContent()).isEqualTo("첫 번째 댓글");
        assertThat(result.getContent().get(1).getContent()).isEqualTo("두 번째 댓글");
        verify(commentQueryUseCase).getUserComments(eq(userId), eq(pageable));
    }

    @Test
    @DisplayName("정상 케이스 - 사용자 추천 댓글 목록 조회")
    void shouldFindLikedCommentsByUserId_WhenValidUserIdProvided() {
        // Given: 사용자 ID와 페이지 정보, 예상 추천 댓글 목록
        Long userId = 1L;
        Pageable pageable = PageRequest.of(0, 5);
        
        List<SimpleCommentInfo> domainLikedComments = Arrays.asList(
            new SimpleCommentInfo(3L, 3L, "다른작성자1", "추천한 댓글 1", Instant.now(), 5, true),
            new SimpleCommentInfo(4L, 4L, "다른작성자2", "추천한 댓글 2", Instant.now(), 8, true)
        );
        
        Page<SimpleCommentInfo> domainPage = new PageImpl<>(domainLikedComments, pageable, domainLikedComments.size());
        given(commentQueryUseCase.getUserLikedComments(eq(userId), any(Pageable.class))).willReturn(domainPage);

        // When: 사용자 추천 댓글 목록 조회 실행
        Page<SimpleCommentInfo> result = loadCommentAdapter.findLikedCommentsByUserId(userId, pageable);

        // Then: 올바른 추천 댓글 목록이 반환되고 UseCase가 호출되었는지 검증
        assertThat(result).isNotNull();
        assertThat(result.getContent()).hasSize(2);
        assertThat(result.getContent().get(0).getContent()).isEqualTo("추천한 댓글 1");
        assertThat(result.getContent().get(1).getContent()).isEqualTo("추천한 댓글 2");
        verify(commentQueryUseCase).getUserLikedComments(eq(userId), eq(pageable));
    }

    @Test
    @DisplayName("경계값 - 빈 댓글 목록 조회")
    void shouldHandleEmptyComments_WhenNoCommentsFound() {
        // Given: 댓글이 없는 사용자 ID와 빈 페이지 결과
        Long userId = 999L;
        Pageable pageable = PageRequest.of(0, 10);
        Page<SimpleCommentInfo> emptyDomainPage = new PageImpl<>(Collections.emptyList(), pageable, 0);
        
        given(commentQueryUseCase.getUserComments(eq(userId), any(Pageable.class))).willReturn(emptyDomainPage);

        // When: 댓글이 없는 사용자의 댓글 목록 조회
        Page<SimpleCommentInfo> result = loadCommentAdapter.findCommentsByUserId(userId, pageable);

        // Then: 빈 페이지가 올바르게 반환되는지 검증
        assertThat(result).isNotNull();
        assertThat(result.getContent()).isEmpty();
        assertThat(result.getTotalElements()).isEqualTo(0);
        verify(commentQueryUseCase).getUserComments(eq(userId), eq(pageable));
    }

    @Test
    @DisplayName("경계값 - 빈 추천 댓글 목록 조회")
    void shouldHandleEmptyLikedComments_WhenNoLikedCommentsFound() {
        // Given: 추천 댓글이 없는 사용자 ID와 빈 페이지 결과
        Long userId = 999L;
        Pageable pageable = PageRequest.of(0, 10);
        Page<SimpleCommentInfo> emptyDomainPage = new PageImpl<>(Collections.emptyList(), pageable, 0);
        
        given(commentQueryUseCase.getUserLikedComments(eq(userId), any(Pageable.class))).willReturn(emptyDomainPage);

        // When: 추천 댓글이 없는 사용자의 추천 댓글 목록 조회
        Page<SimpleCommentInfo> result = loadCommentAdapter.findLikedCommentsByUserId(userId, pageable);

        // Then: 빈 페이지가 올바르게 반환되는지 검증
        assertThat(result).isNotNull();
        assertThat(result.getContent()).isEmpty();
        assertThat(result.getTotalElements()).isEqualTo(0);
        verify(commentQueryUseCase).getUserLikedComments(eq(userId), eq(pageable));
    }

    @Test
    @DisplayName("예외 케이스 - null 사용자 ID로 댓글 조회")
    void shouldHandleNullUserId_WhenNullUserIdProvided() {
        // Given: null 사용자 ID
        Long nullUserId = null;
        Pageable pageable = PageRequest.of(0, 10);

        // When: null 사용자 ID로 댓글 조회 실행
        loadCommentAdapter.findCommentsByUserId(nullUserId, pageable);

        // Then: UseCase에 null이 전달되는지 검증
        verify(commentQueryUseCase).getUserComments(eq(nullUserId), eq(pageable));
    }

    @Test
    @DisplayName("예외 케이스 - null 페이지로 댓글 조회")
    void shouldHandleNullPageable_WhenNullPageableProvided() {
        // Given: 정상 사용자 ID와 null 페이지 정보
        Long userId = 1L;
        Pageable nullPageable = null;

        // When: null 페이지 정보로 댓글 조회 실행
        loadCommentAdapter.findCommentsByUserId(userId, nullPageable);

        // Then: UseCase에 null 페이지가 전달되는지 검증
        verify(commentQueryUseCase).getUserComments(eq(userId), eq(nullPageable));
    }

    @Test
    @DisplayName("성능 - 대용량 댓글 목록 조회")
    void shouldHandleLargeCommentList_WhenLargePageRequested() {
        // Given: 대용량 페이지 요청
        Long userId = 1L;
        Pageable largePage = PageRequest.of(0, 1000);
        
        List<SimpleCommentInfo> largeCommentList = Collections.nCopies(1000, 
            new SimpleCommentInfo(1L, 1L, "테스트작성자", "대용량 테스트 댓글", Instant.now(), 0, false));
        
        Page<SimpleCommentInfo> largeDomainPage = new PageImpl<>(largeCommentList, largePage, 1000);
        given(commentQueryUseCase.getUserComments(eq(userId), any(Pageable.class))).willReturn(largeDomainPage);

        // When: 대용량 댓글 목록 조회
        Page<SimpleCommentInfo> result = loadCommentAdapter.findCommentsByUserId(userId, largePage);

        // Then: 대용량 데이터도 올바르게 처리되는지 검증
        assertThat(result).isNotNull();
        assertThat(result.getContent()).hasSize(1000);
        assertThat(result.getTotalElements()).isEqualTo(1000);
        verify(commentQueryUseCase).getUserComments(eq(userId), eq(largePage));
    }
}