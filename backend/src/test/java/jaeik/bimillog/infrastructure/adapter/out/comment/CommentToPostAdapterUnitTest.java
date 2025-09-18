package jaeik.bimillog.infrastructure.adapter.out.comment;

import jaeik.bimillog.domain.post.application.port.in.PostQueryUseCase;
import jaeik.bimillog.domain.post.entity.Post;
import jaeik.bimillog.domain.post.exception.PostCustomException;
import jaeik.bimillog.domain.post.exception.PostErrorCode;
import jaeik.bimillog.domain.user.entity.Setting;
import jaeik.bimillog.domain.user.entity.SocialProvider;
import jaeik.bimillog.domain.user.entity.User;
import jaeik.bimillog.domain.user.entity.UserRole;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.times;

/**
 * <h2>CommentToPostAdapterUnitTest 단위 테스트</h2>
 * <p>Comment 도메인에서 Post 도메인에 접근하는 CommentToPostAdapterUnitTest 단위 테스트</p>
 * <p>다른 도메인 UseCase를 호출하는 어댑터이므로 Mock을 사용한 단위 테스트로 작성</p>
 * 
 * @author Jaeik
 * @version 2.0.0
 */
@ExtendWith(MockitoExtension.class)
class CommentToPostAdapterUnitTest {

    @Mock
    private PostQueryUseCase postQueryUseCase;

    @InjectMocks
    private CommentToPostAdapter commentLoadPostAdapter;

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
        given(postQueryUseCase.findById(postId)).willReturn(testPost);

        // When: 게시글 조회
        Post result = commentLoadPostAdapter.findById(postId);

        // Then: 올바른 게시글이 반환되었는지 검증
        assertThat(result).isNotNull();
        assertThat(result).isEqualTo(testPost);
        assertThat(result.getTitle()).isEqualTo("테스트 게시글");
        assertThat(result.getContent()).isEqualTo("테스트 게시글 내용입니다.");
        assertThat(result.isNotice()).isFalse();
        assertThat(result.getViews()).isEqualTo(0);
        assertThat(result.getUser()).isEqualTo(testUser);

        // PostQueryUseCase의 메서드가 정확히 호출되었는지 검증
        then(postQueryUseCase).should().findById(postId);
    }

    @Test
    @DisplayName("정상 케이스 - 존재하지 않는 게시글 ID로 게시글 조회")
    void shouldThrowException_WhenPostNotExists() {
        // Given: PostQueryUseCase가 예외를 던지도록 설정
        Long nonExistentPostId = 999L;
        given(postQueryUseCase.findById(nonExistentPostId)).willThrow(new PostCustomException(PostErrorCode.POST_NOT_FOUND));

        // When & Then: 존재하지 않는 게시글 조회 시 예외 발생
        assertThatThrownBy(() -> commentLoadPostAdapter.findById(nonExistentPostId))
                .isInstanceOf(PostCustomException.class);

        // PostQueryUseCase의 메서드가 정확히 호출되었는지 검증
        then(postQueryUseCase).should().findById(nonExistentPostId);
    }

    @Test
    @DisplayName("정상 케이스 - 공지사항 게시글 조회")
    void shouldReturnNoticePost_WhenNoticePostExists() {
        // Given: PostQueryUseCase가 공지사항 게시글을 반환하도록 설정
        Long noticePostId = 2L;
        given(postQueryUseCase.findById(noticePostId)).willReturn(noticePost);

        // When: 공지사항 게시글 조회
        Post result = commentLoadPostAdapter.findById(noticePostId);

        // Then: 공지사항 게시글이 올바르게 반환되었는지 검증
        assertThat(result).isNotNull();
        assertThat(result).isEqualTo(noticePost);
        assertThat(result.getTitle()).isEqualTo("공지사항");
        assertThat(result.getContent()).isEqualTo("중요한 공지사항입니다.");
        assertThat(result.isNotice()).isTrue();
        assertThat(result.getViews()).isEqualTo(100);

        // PostQueryUseCase의 메서드가 정확히 호출되었는지 검증
        then(postQueryUseCase).should().findById(noticePostId);
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
        given(postQueryUseCase.findById(postId)).willReturn(highViewPost);

        // When: 높은 조회수 게시글 조회
        Post result = commentLoadPostAdapter.findById(postId);

        // Then: 높은 조회수 게시글이 올바르게 반환되었는지 검증
        assertThat(result).isNotNull();
        assertThat(result.getViews()).isEqualTo(10000);
        assertThat(result.getTitle()).isEqualTo("인기 게시글");

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
        given(postQueryUseCase.findById(postId)).willReturn(anotherUserPost);

        // When: 다른 사용자 게시글 조회
        Post result = commentLoadPostAdapter.findById(postId);

        // Then: 다른 사용자 게시글이 올바르게 반환되었는지 검증
        assertThat(result).isNotNull();
        assertThat(result.getUser()).isEqualTo(anotherUser);
        assertThat(result.getUser().getSocialNickname()).isEqualTo("다른유저");
        assertThat(result.getTitle()).isEqualTo("다른 사용자 게시글");

        // PostQueryUseCase의 메서드가 호출되었는지 검증
        then(postQueryUseCase).should().findById(postId);
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
        given(postQueryUseCase.findById(postId)).willReturn(testPost);

        // When: 동일한 ID로 여러 번 조회
        Post result1 = commentLoadPostAdapter.findById(postId);
        Post result2 = commentLoadPostAdapter.findById(postId);
        Post result3 = commentLoadPostAdapter.findById(postId);

        // Then: 모든 결과가 일관되게 동일한 게시글을 반환해야 함
        assertThat(result1).isEqualTo(testPost);
        assertThat(result2).isEqualTo(testPost);
        assertThat(result3).isEqualTo(testPost);

        // PostQueryUseCase의 메서드가 3번 호출되었는지 검증
        then(postQueryUseCase).should(times(3)).findById(postId);
    }

    @Test
    @DisplayName("어댑터 계약 - CommentToPostPort 인터페이스 메서드 구현 검증")
    void shouldImplementLoadPostPortContract_WhenAdapterUsed() {
        // Given: 표준 게시글과 ID
        Long postId = 1L;
        given(postQueryUseCase.findById(anyLong())).willReturn(testPost);

        // When: 포트 인터페이스 메서드 호출
        Post result = commentLoadPostAdapter.findById(postId);

        // Then: 포트 계약에 따른 정확한 결과 반환 확인
        assertThat(result).isNotNull(); // Optional이 null이면 안됨
        
        if (result != null) {
            Post post = result;
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
        given(postQueryUseCase.findById(postId)).willReturn(complexPost);

        // When: 복잡한 게시글 조회
        Post result = commentLoadPostAdapter.findById(postId);

        // Then: 복잡한 게시글이 올바르게 반환되었는지 검증
        assertThat(result).isNotNull();
        assertThat(result.getTitle()).isEqualTo(complexTitle);
        assertThat(result.getContent()).isEqualTo(complexContent);
        assertThat(result.getTitle()).contains("특수문자");
        assertThat(result.getContent()).hasSize(complexContent.length());
        assertThat(result.isNotice()).isTrue();
        assertThat(result.getViews()).isEqualTo(9999);

        // PostQueryUseCase의 메서드가 호출되었는지 검증
        then(postQueryUseCase).should().findById(postId);
    }
}