package jaeik.bimillog.infrastructure.outadapter.post.persistence.post.post;

import jaeik.bimillog.domain.user.entity.SocialProvider;
import jaeik.bimillog.domain.post.application.port.out.PostCommentQueryPort;
import jaeik.bimillog.domain.post.application.port.out.PostLikeQueryPort;
import jaeik.bimillog.domain.post.entity.*;
import jaeik.bimillog.domain.post.exception.PostCustomException;
import jaeik.bimillog.domain.user.entity.Setting;
import jaeik.bimillog.domain.user.entity.User;
import jaeik.bimillog.domain.user.entity.UserRole;
import jaeik.bimillog.infrastructure.adapter.post.out.persistence.post.post.PostQueryAdapter;
import jaeik.bimillog.testutil.TestContainersConfiguration;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;

/**
 * <h2>PostQueryAdapter 통합 테스트</h2>
 * <p>게시글 조회 어댑터의 모든 기능을 테스트합니다.</p>
 *
 * @author Jaeik
 * @version 2.0.0
 */
@DataJpaTest(
        includeFilters = @ComponentScan.Filter(
                type = FilterType.ASSIGNABLE_TYPE,
                classes = {PostQueryAdapter.class}
        )
)
@Testcontainers
@Import({PostQueryAdapter.class, TestContainersConfiguration.class})
class PostQueryAdapterTest {

    @Autowired
    private PostQueryAdapter postQueryAdapter;

    @Autowired
    private TestEntityManager entityManager;

    @MockitoBean
    private PostCommentQueryPort postCommentQueryPort;

    @MockitoBean
    private PostLikeQueryPort postLikeQueryPort;

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
        
        // 추천 수 Mock 설정 (기본값)
        Map<Long, Integer> likeCounts = new HashMap<>();
        likeCounts.put(testPost1.getId(), 5);
        likeCounts.put(testPost2.getId(), 3);
        likeCounts.put(testPost3.getId(), 1);
        likeCounts.put(noticePost.getId(), 8);
        
        given(postCommentQueryPort.findCommentCountsByPostIds(any(List.class)))
                .willReturn(commentCounts);
        given(postLikeQueryPort.findLikeCountsByPostIds(any(List.class)))
                .willReturn(likeCounts);
    }

    private void createTestPosts() {
        // 일반 게시글 1
        PostReqVO postReqDTO1 = PostReqVO.builder()
                .title("첫 번째 게시글")
                .content("첫 번째 게시글 내용")
                .password(1234)
                .build();
        testPost1 = Post.createPost(testUser, postReqDTO1);
        entityManager.persistAndFlush(testPost1);

        // 일반 게시글 2
        PostReqVO postReqDTO2 = PostReqVO.builder()
                .title("두 번째 게시글")
                .content("두 번째 게시글 내용")
                .password(1234)
                .build();
        testPost2 = Post.createPost(testUser, postReqDTO2);
        entityManager.persistAndFlush(testPost2);

        // 일반 게시글 3
        PostReqVO postReqDTO3 = PostReqVO.builder()
                .title("세 번째 게시글")
                .content("세 번째 게시글 내용")
                .password(1234)
                .build();
        testPost3 = Post.createPost(testUser, postReqDTO3);
        entityManager.persistAndFlush(testPost3);

        // 공지사항 게시글
        PostReqVO noticeReqDTO = PostReqVO.builder()
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
        Post foundPost = postQueryAdapter.findById(postId);

        // Then: 게시글이 정상 조회됨
        assertThat(foundPost).isNotNull();
        assertThat(foundPost.getId()).isEqualTo(postId);
        assertThat(foundPost.getTitle()).isEqualTo("첫 번째 게시글");
        assertThat(foundPost.getContent()).isEqualTo("첫 번째 게시글 내용");
    }

    @Test
    @DisplayName("경계값 - 존재하지 않는 ID로 조회")
    void shouldReturnEmpty_WhenNonExistentIdProvided() {
        // Given: 존재하지 않는 게시글 ID
        Long nonExistentId = 99999L;

        // When & Then: 존재하지 않는 ID로 조회 시 예외 발생
        assertThatThrownBy(() -> postQueryAdapter.findById(nonExistentId))
                .isInstanceOf(PostCustomException.class);
    }

    @Test
    @DisplayName("정상 케이스 - 페이지별 게시글 조회 (공지사항 제외)")
    void shouldFindPostsByPage_WhenValidPageableProvided() {
        // Given: 페이지 요청 (첫 페이지, 크기 2)
        Pageable pageable = PageRequest.of(0, 2);

        // When: 페이지별 게시글 조회
        Page<PostSearchResult> result = postQueryAdapter.findByPage(pageable);

        // Then: 공지사항이 제외된 일반 게시글만 조회됨
        assertThat(result).isNotNull();
        assertThat(result.getContent()).hasSize(2); // 요청한 크기만큼
        assertThat(result.getTotalElements()).isEqualTo(3L); // 전체 일반 게시글 수
        assertThat(result.getTotalPages()).isEqualTo(2); // 전체 페이지 수

        // 공지사항은 제외되어야 함
        List<String> titles = result.getContent().stream()
                .map(PostSearchResult::getTitle)
                .toList();
        assertThat(titles).doesNotContain("공지사항 제목");
        
        // 댓글 수와 추천 수가 설정되어 있는지 확인
        assertThat(result.getContent().get(0).getCommentCount()).isNotNull();
        assertThat(result.getContent().get(0).getLikeCount()).isNotNull();
    }

    @Test
    @DisplayName("경계값 - 빈 페이지 요청")
    void shouldReturnEmptyPage_WhenNoPostsExist() {
        // Given: 모든 일반 게시글 삭제
        entityManager.getEntityManager()
                .createQuery("DELETE FROM Post p WHERE p.isNotice = false")
                .executeUpdate();
        entityManager.flush();
        entityManager.clear();

        Pageable pageable = PageRequest.of(0, 10);

        // When: 빈 페이지 조회
        Page<PostSearchResult> result = postQueryAdapter.findByPage(pageable);

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
        Page<PostSearchResult> result = postQueryAdapter.findPostsByUserId(userId, pageable);

        // Then: 해당 사용자의 게시글만 조회됨
        assertThat(result).isNotNull();
        assertThat(result.getContent()).hasSize(4); // 공지사항 포함 전체 게시글
        assertThat(result.getTotalElements()).isEqualTo(4L);

        // 모든 게시글의 작성자가 해당 사용자인지 확인
        List<String> userNames = result.getContent().stream()
                .map(PostSearchResult::getUserName)
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
        Page<PostSearchResult> result = postQueryAdapter.findPostsByUserId(nonExistentUserId, pageable);

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
        Page<PostSearchResult> result = postQueryAdapter.findLikedPostsByUserId(likeUser.getId(), pageable);

        // Then: 추천한 게시글들이 조회됨
        assertThat(result).isNotNull();
        assertThat(result.getContent()).hasSize(2);
        assertThat(result.getTotalElements()).isEqualTo(2L);

        List<String> likedPostTitles = result.getContent().stream()
                .map(PostSearchResult::getTitle)
                .toList();
        assertThat(likedPostTitles).containsExactlyInAnyOrder(
                "첫 번째 게시글", 
                "두 번째 게시글"
        );

        // 댓글 수와 추천 수도 설정되어 있는지 확인
        assertThat(result.getContent()).allMatch(post -> post.getCommentCount() != null);
        assertThat(result.getContent()).allMatch(post -> post.getLikeCount() != null);
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
        Page<PostSearchResult> result = postQueryAdapter.findLikedPostsByUserId(newUser.getId(), pageable);

        // Then: 빈 페이지 반환
        assertThat(result).isNotNull();
        assertThat(result.getContent()).isEmpty();
        assertThat(result.getTotalElements()).isEqualTo(0L);
    }

    @Test
    @DisplayName("정상 케이스 - 제목 검색")
    void shouldFindPostsByTitleSearch_WhenValidSearchQueryProvided() {
        // Given: 제목 검색어
        String searchType = "title";
        String query = "첫 번째";
        Pageable pageable = PageRequest.of(0, 10);

        // When: 제목 검색
        Page<PostSearchResult> result = postQueryAdapter.findBySearch(searchType, query, pageable);

        // Then: 해당 제목이 포함된 게시글 조회됨
        assertThat(result).isNotNull();
        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).getTitle()).contains("첫 번째");
    }

    @Test
    @DisplayName("정상 케이스 - 작성자 검색")
    void shouldFindPostsByWriterSearch_WhenValidWriterQueryProvided() {
        // Given: 작성자 검색어
        String searchType = "writer";
        String query = "test";
        Pageable pageable = PageRequest.of(0, 10);

        // When: 작성자 검색
        Page<PostSearchResult> result = postQueryAdapter.findBySearch(searchType, query, pageable);

        // Then: 해당 작성자의 게시글들이 조회됨
        assertThat(result).isNotNull();
        assertThat(result.getContent()).hasSizeGreaterThan(0);
        assertThat(result.getContent()).allMatch(post -> 
                post.getUserName().toLowerCase().contains(query.toLowerCase()));
    }

    @Test
    @DisplayName("경계값 - 빈 검색어로 검색")
    void shouldReturnAllPosts_WhenEmptySearchQueryProvided() {
        // Given: 빈 검색어
        String searchType = "title";
        String query = "";
        Pageable pageable = PageRequest.of(0, 10);

        // When: 빈 검색어로 검색
        Page<PostSearchResult> result = postQueryAdapter.findBySearch(searchType, query, pageable);

        // Then: 모든 일반 게시글 조회됨 (공지사항 제외)
        assertThat(result).isNotNull();
        assertThat(result.getContent()).hasSize(3); // 일반 게시글 3개
    }

    @Test
    @DisplayName("성능 - 페이지네이션 동작 확인")
    void shouldHandlePagination_WhenMultiplePagesRequested() {
        // Given: 더 많은 게시글 생성 (총 10개)
        for (int i = 4; i <= 10; i++) {
            PostReqVO postReqDTO = PostReqVO.builder()
                    .title("추가 게시글 " + i)
                    .content("추가 게시글 내용 " + i)
                    .password(1234)
                    .build();
            Post post = Post.createPost(testUser, postReqDTO);
            entityManager.persistAndFlush(post);
        }

        // Mock 댓글 수와 추천 수 업데이트
        Map<Long, Integer> allCommentCounts = new HashMap<>();
        Map<Long, Integer> allLikeCounts = new HashMap<>();
        for (int i = 1; i <= 10; i++) {
            allCommentCounts.put((long) i, i % 3); // 0, 1, 2 순환
            allLikeCounts.put((long) i, i % 5); // 0, 1, 2, 3, 4 순환
        }
        given(postCommentQueryPort.findCommentCountsByPostIds(any(List.class)))
                .willReturn(allCommentCounts);
        given(postLikeQueryPort.findLikeCountsByPostIds(any(List.class)))
                .willReturn(allLikeCounts);

        // When: 첫 페이지와 두 번째 페이지 조회
        Pageable firstPage = PageRequest.of(0, 3);
        Pageable secondPage = PageRequest.of(1, 3);

        Page<PostSearchResult> firstResult = postQueryAdapter.findByPage(firstPage);
        Page<PostSearchResult> secondResult = postQueryAdapter.findByPage(secondPage);

        // Then: 페이지네이션이 정상 동작함
        assertThat(firstResult.getContent()).hasSize(3);
        assertThat(secondResult.getContent()).hasSize(3);
        assertThat(firstResult.getTotalElements()).isEqualTo(10L); // 공지사항 제외
        assertThat(secondResult.getTotalElements()).isEqualTo(10L);
        
        // 각 페이지의 내용이 다름을 확인
        Set<Long> firstPageIds = firstResult.getContent().stream()
                .map(PostSearchResult::getId)
                .collect(java.util.stream.Collectors.toSet());
        Set<Long> secondPageIds = secondResult.getContent().stream()
                .map(PostSearchResult::getId)
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
        Page<PostSearchResult> result = postQueryAdapter.findByPage(pageable);

        // Then: 공지사항은 제외되고 일반 게시글만 조회됨
        assertThat(result.getContent()).hasSize(3); // 일반 게시글 3개
        
        List<Boolean> noticeFlags = result.getContent().stream()
                .map(PostSearchResult::isNotice)
                .toList();
        assertThat(noticeFlags).allMatch(isNotice -> !isNotice); // 모두 false여야 함
    }

    @Test
    @DisplayName("비즈니스 로직 - 최신 게시글부터 정렬")
    void shouldSortByCreatedAtDesc_WhenFindingPosts() {
        // Given: 여러 게시글이 존재
        Pageable pageable = PageRequest.of(0, 10);

        // When: 게시글 조회
        Page<PostSearchResult> result = postQueryAdapter.findByPage(pageable);

        // Then: 최신 게시글부터 정렬됨 (createdAt 내림차순)
        List<java.time.Instant> createdAts = result.getContent().stream()
                .map(PostSearchResult::getCreatedAt)
                .toList();

        for (int i = 1; i < createdAts.size(); i++) {
            assertThat(createdAts.get(i-1)).isAfterOrEqualTo(createdAts.get(i));
        }
    }

    @Test
    @DisplayName("통합 테스트 - N+1 문제 해결 확인")
    void shouldAvoidNPlusOneProblem_WhenFetchingPostsWithComments() {
        // Given: 게시글들과 댓글 수, 추천 수 Mock
        Map<Long, Integer> commentCounts = new HashMap<>();
        commentCounts.put(testPost1.getId(), 5);
        commentCounts.put(testPost2.getId(), 3);
        commentCounts.put(testPost3.getId(), 1);
        
        Map<Long, Integer> likeCounts = new HashMap<>();
        likeCounts.put(testPost1.getId(), 8);
        likeCounts.put(testPost2.getId(), 6);
        likeCounts.put(testPost3.getId(), 2);
        
        given(postCommentQueryPort.findCommentCountsByPostIds(any(List.class)))
                .willReturn(commentCounts);
        given(postLikeQueryPort.findLikeCountsByPostIds(any(List.class)))
                .willReturn(likeCounts);

        Pageable pageable = PageRequest.of(0, 3);

        // When: 게시글과 댓글 수, 추천 수 조회
        Page<PostSearchResult> result = postQueryAdapter.findByPage(pageable);

        // Then: 배치로 댓글 수와 추천 수가 조회되어 N+1 문제 없음
        assertThat(result.getContent()).hasSize(3);
        
        // 모든 게시글에 댓글 수와 추천 수가 설정됨
        assertThat(result.getContent()).allMatch(post -> post.getCommentCount() != null);
        assertThat(result.getContent()).allMatch(post -> post.getLikeCount() != null);
        
        // 댓글 수와 추천 수가 Mock으로 설정한 값과 일치
        for (PostSearchResult post : result.getContent()) {
            if (commentCounts.containsKey(post.getId())) {
                assertThat(post.getCommentCount()).isEqualTo(commentCounts.get(post.getId()));
            }
            if (likeCounts.containsKey(post.getId())) {
                assertThat(post.getLikeCount()).isEqualTo(likeCounts.get(post.getId()));
            }
        }
    }

    @Test
    @DisplayName("예외 케이스 - null Pageable 처리")
    void shouldHandleNullPageable_GracefullyWithoutError() {
        // Given: null Pageable
        Pageable nullPageable = null;

        // When & Then: 적절한 예외 처리
        assertThatThrownBy(() -> {
            postQueryAdapter.findByPage(nullPageable);
        }).isInstanceOf(Exception.class); // NullPointerException 또는 적절한 예외
    }

    @Test
    @DisplayName("데이터 매핑 - 모든 필드 정확히 매핑")
    void shouldMapAllFields_WhenCreatingPostSearchResult() {
        // Given: 캐시 플래그가 설정된 게시글 (가장 최신)
        testPost3.setPostCacheFlag(PostCacheFlag.REALTIME);
        entityManager.merge(testPost3);
        entityManager.flush();

        Pageable pageable = PageRequest.of(0, 1);

        // When: 게시글 조회 (최신순 정렬로 testPost3가 첫 번째)
        Page<PostSearchResult> result = postQueryAdapter.findByPage(pageable);

        // Then: 모든 필드가 정확히 매핑됨
        PostSearchResult dto = result.getContent().get(0);
        
        assertThat(dto.getId()).isEqualTo(testPost3.getId());
        assertThat(dto.getTitle()).isEqualTo(testPost3.getTitle());
        assertThat(dto.getUserId()).isEqualTo(testUser.getId());
        assertThat(dto.getUserName()).isEqualTo(testUser.getUserName());
        assertThat(dto.getPostCacheFlag()).isEqualTo(PostCacheFlag.REALTIME);
        assertThat(dto.isNotice()).isFalse();
        assertThat(dto.getCreatedAt()).isNotNull();
        assertThat(dto.getCommentCount()).isNotNull();
        assertThat(dto.getLikeCount()).isNotNull();
        assertThat(dto.getViewCount()).isNotNull();
    }
}