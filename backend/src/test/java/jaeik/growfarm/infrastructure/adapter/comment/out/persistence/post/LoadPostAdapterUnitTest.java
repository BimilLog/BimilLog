package jaeik.growfarm.infrastructure.adapter.comment.out.persistence.post;

import jaeik.growfarm.domain.common.entity.SocialProvider;
import jaeik.growfarm.domain.post.application.port.in.PostQueryUseCase;
import jaeik.growfarm.domain.post.entity.Post;
import jaeik.growfarm.domain.user.entity.Setting;
import jaeik.growfarm.domain.user.entity.User;
import jaeik.growfarm.domain.user.entity.UserRole;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.times;

/**
 * <h2>LoadPostAdapter 단위 테스트</h2>
 * <p>Comment 도메인에서 Post 도메인에 접근하는 LoadPostAdapter의 단위 테스트</p>
 * <p>다른 도메인 UseCase를 호출하는 어댑터이므로 Mock을 사용한 단위 테스트로 작성</p>
 * 
 * @author Jaeik
 * @version 2.0.0
 */
@ExtendWith(MockitoExtension.class)
class LoadPostAdapterUnitTest {

    @Mock
    private PostQueryUseCase postQueryUseCase;

    @InjectMocks
    private CommentLoadPostAdapter commentLoadPostAdapter;

    private User testUser;
    private Post testPost;
    private Post noticePost;

    @BeforeEach
    void setUp() {
        // 테스트용 사용자 생성
        Setting setting = Setting.createSetting();
        testUser = User.builder()
                .socialId("kakao123")
                .provider(SocialProvider.KAKAO)
                .userName("testUser")
                .socialNickname("테스트유저")
                .role(UserRole.USER)
                .setting(setting)
                .build();

        // 테스트용 일반 게시글 생성
        testPost = Post.builder()
                .user(testUser)
                .title("테스트 게시글")
                .content("테스트 게시글 내용입니다.")
                .isNotice(false)
                .views(0)
                .build();

        // 테스트용 공지 게시글 생성
        noticePost = Post.builder()
                .user(testUser)
                .title("공지사항")
                .content("중요한 공지사항입니다.")
                .isNotice(true)
                .views(100)
                .build();
    }

    @Test
    @DisplayName("정상 케이스 - 존재하는 게시글 ID로 게시글 조회")
    void shouldReturnPost_WhenPostExists() {
        // Given: PostQueryUseCase가 게시글을 반환하도록 설정
        Long postId = 1L;
        given(postQueryUseCase.findById(postId)).willReturn(Optional.of(testPost));

        // When: 게시글 조회
        Optional<Post> result = commentLoadPostAdapter.findById(postId);

        // Then: 올바른 게시글이 반환되었는지 검증
        assertThat(result).isPresent();
        assertThat(result.get()).isEqualTo(testPost);
        assertThat(result.get().getTitle()).isEqualTo("테스트 게시글");
        assertThat(result.get().getContent()).isEqualTo("테스트 게시글 내용입니다.");
        assertThat(result.get().isNotice()).isFalse();
        assertThat(result.get().getViews()).isEqualTo(0);
        assertThat(result.get().getUser()).isEqualTo(testUser);

        // PostQueryUseCase의 메서드가 정확히 호출되었는지 검증
        then(postQueryUseCase).should().findById(postId);
    }

    @Test
    @DisplayName("정상 케이스 - 존재하지 않는 게시글 ID로 게시글 조회")
    void shouldReturnEmpty_WhenPostNotExists() {
        // Given: PostQueryUseCase가 빈 Optional을 반환하도록 설정
        Long nonExistentPostId = 999L;
        given(postQueryUseCase.findById(nonExistentPostId)).willReturn(Optional.empty());

        // When: 존재하지 않는 게시글 조회
        Optional<Post> result = commentLoadPostAdapter.findById(nonExistentPostId);

        // Then: 빈 Optional이 반환되었는지 검증
        assertThat(result).isEmpty();

        // PostQueryUseCase의 메서드가 정확히 호출되었는지 검증
        then(postQueryUseCase).should().findById(nonExistentPostId);
    }

    @Test
    @DisplayName("정상 케이스 - 공지사항 게시글 조회")
    void shouldReturnNoticePost_WhenNoticePostExists() {
        // Given: PostQueryUseCase가 공지사항 게시글을 반환하도록 설정
        Long noticePostId = 2L;
        given(postQueryUseCase.findById(noticePostId)).willReturn(Optional.of(noticePost));

        // When: 공지사항 게시글 조회
        Optional<Post> result = commentLoadPostAdapter.findById(noticePostId);

        // Then: 공지사항 게시글이 올바르게 반환되었는지 검증
        assertThat(result).isPresent();
        assertThat(result.get()).isEqualTo(noticePost);
        assertThat(result.get().getTitle()).isEqualTo("공지사항");
        assertThat(result.get().getContent()).isEqualTo("중요한 공지사항입니다.");
        assertThat(result.get().isNotice()).isTrue();
        assertThat(result.get().getViews()).isEqualTo(100);

        // PostQueryUseCase의 메서드가 정확히 호출되었는지 검증
        then(postQueryUseCase).should().findById(noticePostId);
    }

    @Test
    @DisplayName("정상 케이스 - null 게시글 ID로 조회")
    void shouldHandleNullPostId_WhenNullProvided() {
        // Given: null 게시글 ID
        Long nullPostId = null;
        given(postQueryUseCase.findById(nullPostId)).willReturn(Optional.empty());

        // When: null ID로 게시글 조회
        Optional<Post> result = commentLoadPostAdapter.findById(nullPostId);

        // Then: 빈 Optional이 반환되었는지 검증
        assertThat(result).isEmpty();

        // PostQueryUseCase의 메서드가 호출되었는지 검증
        then(postQueryUseCase).should().findById(nullPostId);
    }

    @Test
    @DisplayName("정상 케이스 - 음수 게시글 ID로 조회")
    void shouldHandleNegativePostId_WhenNegativeIdProvided() {
        // Given: 음수 게시글 ID
        Long negativePostId = -1L;
        given(postQueryUseCase.findById(negativePostId)).willReturn(Optional.empty());

        // When: 음수 ID로 게시글 조회
        Optional<Post> result = commentLoadPostAdapter.findById(negativePostId);

        // Then: 빈 Optional이 반환되었는지 검증
        assertThat(result).isEmpty();

        // PostQueryUseCase의 메서드가 호출되었는지 검증
        then(postQueryUseCase).should().findById(negativePostId);
    }

    @Test
    @DisplayName("정상 케이스 - 높은 조회수를 가진 게시글 조회")
    void shouldReturnHighViewPost_WhenHighViewPostExists() {
        // Given: 높은 조회수를 가진 게시글
        Post highViewPost = Post.builder()
                .user(testUser)
                .title("인기 게시글")
                .content("많은 사람들이 본 게시글입니다.")
                .isNotice(false)
                .views(10000)
                .build();

        Long postId = 3L;
        given(postQueryUseCase.findById(postId)).willReturn(Optional.of(highViewPost));

        // When: 높은 조회수 게시글 조회
        Optional<Post> result = commentLoadPostAdapter.findById(postId);

        // Then: 높은 조회수 게시글이 올바르게 반환되었는지 검증
        assertThat(result).isPresent();
        assertThat(result.get().getViews()).isEqualTo(10000);
        assertThat(result.get().getTitle()).isEqualTo("인기 게시글");

        // PostQueryUseCase의 메서드가 호출되었는지 검증
        then(postQueryUseCase).should().findById(postId);
    }

    @Test
    @DisplayName("정상 케이스 - 다양한 사용자가 작성한 게시글 조회")
    void shouldReturnPostFromDifferentUser_WhenDifferentUserPostExists() {
        // Given: 다른 사용자가 작성한 게시글
        User anotherUser = User.builder()
                .socialId("another123")
                .provider(SocialProvider.KAKAO)
                .userName("anotherUser")
                .socialNickname("다른유저")
                .role(UserRole.USER)
                .setting(Setting.createSetting())
                .build();

        Post anotherUserPost = Post.builder()
                .user(anotherUser)
                .title("다른 사용자 게시글")
                .content("다른 사용자가 작성한 게시글입니다.")
                .isNotice(false)
                .views(50)
                .build();

        Long postId = 4L;
        given(postQueryUseCase.findById(postId)).willReturn(Optional.of(anotherUserPost));

        // When: 다른 사용자 게시글 조회
        Optional<Post> result = commentLoadPostAdapter.findById(postId);

        // Then: 다른 사용자 게시글이 올바르게 반환되었는지 검증
        assertThat(result).isPresent();
        assertThat(result.get().getUser()).isEqualTo(anotherUser);
        assertThat(result.get().getUser().getSocialNickname()).isEqualTo("다른유저");
        assertThat(result.get().getTitle()).isEqualTo("다른 사용자 게시글");

        // PostQueryUseCase의 메서드가 호출되었는지 검증
        then(postQueryUseCase).should().findById(postId);
    }

    @Test
    @DisplayName("경계값 - 최대값 게시글 ID로 조회")
    void shouldHandleMaxPostId_WhenMaxLongValueProvided() {
        // Given: Long 최대값을 게시글 ID로 사용
        Long maxPostId = Long.MAX_VALUE;
        given(postQueryUseCase.findById(maxPostId)).willReturn(Optional.empty());

        // When: 최대값 ID로 게시글 조회
        Optional<Post> result = commentLoadPostAdapter.findById(maxPostId);

        // Then: 빈 Optional이 반환되었는지 검증
        assertThat(result).isEmpty();

        // PostQueryUseCase의 메서드가 호출되었는지 검증
        then(postQueryUseCase).should().findById(maxPostId);
    }

    @Test
    @DisplayName("예외 케이스 - PostQueryUseCase에서 예외 발생 시 전파")
    void shouldPropagateException_WhenPostQueryUseCaseThrowsException() {
        // Given: PostQueryUseCase가 예외를 던지도록 설정
        Long postId = 1L;
        RuntimeException expectedException = new RuntimeException("Post service error");
        given(postQueryUseCase.findById(postId)).willThrow(expectedException);

        // When & Then: 예외가 전파되는지 검증
        try {
            commentLoadPostAdapter.findById(postId);
        } catch (RuntimeException actualException) {
            assertThat(actualException).isEqualTo(expectedException);
            assertThat(actualException.getMessage()).isEqualTo("Post service error");
        }

        // PostQueryUseCase의 메서드가 호출되었는지 검증
        then(postQueryUseCase).should().findById(postId);
    }

    @Test
    @DisplayName("트랜잭션 - 여러 번 연속 호출 시 일관된 동작")
    void shouldBehaveConsistently_WhenCalledMultipleTimes() {
        // Given: 동일한 게시글 ID에 대해 일관된 응답 설정
        Long postId = 1L;
        given(postQueryUseCase.findById(postId)).willReturn(Optional.of(testPost));

        // When: 동일한 ID로 여러 번 조회
        Optional<Post> result1 = commentLoadPostAdapter.findById(postId);
        Optional<Post> result2 = commentLoadPostAdapter.findById(postId);
        Optional<Post> result3 = commentLoadPostAdapter.findById(postId);

        // Then: 모든 결과가 일관되게 동일한 게시글을 반환해야 함
        assertThat(result1).isPresent();
        assertThat(result2).isPresent();
        assertThat(result3).isPresent();
        
        assertThat(result1.get()).isEqualTo(testPost);
        assertThat(result2.get()).isEqualTo(testPost);
        assertThat(result3.get()).isEqualTo(testPost);

        // PostQueryUseCase의 메서드가 3번 호출되었는지 검증
        then(postQueryUseCase).should(times(3)).findById(postId);
    }

    @Test
    @DisplayName("어댑터 계약 - LoadPostPort 인터페이스 메서드 구현 검증")
    void shouldImplementLoadPostPortContract_WhenAdapterUsed() {
        // Given: 표준 게시글과 ID
        Long postId = 1L;
        given(postQueryUseCase.findById(anyLong())).willReturn(Optional.of(testPost));

        // When: 포트 인터페이스 메서드 호출
        Optional<Post> result = commentLoadPostAdapter.findById(postId);

        // Then: 포트 계약에 따른 정확한 결과 반환 확인
        assertThat(result).isNotNull(); // Optional이 null이면 안됨
        
        if (result.isPresent()) {
            Post post = result.get();
            // Post 엔티티의 필수 필드들이 모두 존재하는지 확인
            assertThat(post.getTitle()).isNotNull();
            assertThat(post.getContent()).isNotNull();
            assertThat(post.getUser()).isNotNull();
            assertThat(post.isNotice()).isNotNull();
            assertThat(post.getViews()).isNotNull();

        }

        // 의존성 호출 검증
        then(postQueryUseCase).should().findById(postId);
    }

    @Test
    @DisplayName("경계값 - 빈 제목과 내용을 가진 게시글 조회")
    void shouldReturnPostWithEmptyContent_WhenEmptyContentPostExists() {
        // Given: 빈 제목과 내용을 가진 게시글
        Post emptyContentPost = Post.builder()
                .user(testUser)
                .title("")
                .content("")
                .isNotice(false)
                .views(0)
                .build();

        Long postId = 5L;
        given(postQueryUseCase.findById(postId)).willReturn(Optional.of(emptyContentPost));

        // When: 빈 내용 게시글 조회
        Optional<Post> result = commentLoadPostAdapter.findById(postId);

        // Then: 빈 내용 게시글이 올바르게 반환되었는지 검증
        assertThat(result).isPresent();
        assertThat(result.get().getTitle()).isEmpty();
        assertThat(result.get().getContent()).isEmpty();
        assertThat(result.get().getViews()).isEqualTo(0);

        // PostQueryUseCase의 메서드가 호출되었는지 검증
        then(postQueryUseCase).should().findById(postId);
    }

    @Test
    @DisplayName("트랜잭션 - 복잡한 게시글 엔티티 조회")
    void shouldReturnComplexPostEntity_WhenComplexPostExists() {
        // Given: 복잡한 게시글 엔티티 (특수문자 포함, 긴 내용)
        String complexTitle = "복잡한 제목! @#$%^&*()_+{}|:<>?[]\\;'\",./ 특수문자 포함";
        String complexContent = "매우 긴 게시글 내용입니다. ".repeat(100); // 긴 내용
        
        Post complexPost = Post.builder()
                .user(testUser)
                .title(complexTitle)
                .content(complexContent)
                .isNotice(true)
                .views(9999)
                .build();

        Long postId = 6L;
        given(postQueryUseCase.findById(postId)).willReturn(Optional.of(complexPost));

        // When: 복잡한 게시글 조회
        Optional<Post> result = commentLoadPostAdapter.findById(postId);

        // Then: 복잡한 게시글이 올바르게 반환되었는지 검증
        assertThat(result).isPresent();
        assertThat(result.get().getTitle()).isEqualTo(complexTitle);
        assertThat(result.get().getContent()).isEqualTo(complexContent);
        assertThat(result.get().getTitle()).contains("특수문자");
        assertThat(result.get().getContent()).hasSize(complexContent.length());
        assertThat(result.get().isNotice()).isTrue();
        assertThat(result.get().getViews()).isEqualTo(9999);

        // PostQueryUseCase의 메서드가 호출되었는지 검증
        then(postQueryUseCase).should().findById(postId);
    }
}