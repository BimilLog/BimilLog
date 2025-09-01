package jaeik.bimillog.infrastructure.adapter.user.in.web;

import jaeik.bimillog.domain.comment.entity.SimpleCommentInfo;
import jaeik.bimillog.domain.post.entity.PostSearchResult;
import jaeik.bimillog.domain.user.application.port.in.SettingQueryUseCase;
import jaeik.bimillog.domain.user.application.port.in.UserActivityUseCase;
import jaeik.bimillog.domain.user.application.port.in.UserIntegrationUseCase;
import jaeik.bimillog.domain.user.application.port.in.UserQueryUseCase;
import jaeik.bimillog.domain.user.entity.SettingVO;
import jaeik.bimillog.infrastructure.adapter.comment.in.web.dto.SimpleCommentDTO;
import jaeik.bimillog.infrastructure.adapter.post.in.web.PostResponseMapper;
import jaeik.bimillog.infrastructure.adapter.post.in.web.dto.SimplePostResDTO;
import jaeik.bimillog.infrastructure.adapter.user.in.web.dto.KakaoFriendsResponse;
import jaeik.bimillog.infrastructure.adapter.user.in.web.dto.SettingDTO;
import jaeik.bimillog.infrastructure.auth.CustomUserDetails;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

/**
 * <h2>사용자 조회 컨트롤러</h2>
 * <p>사용자 관련 조회 요청을 처리하는 컨트롤러</p>
 *
 * @author Jaeik
 * @version 2.0.0
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/user")
public class UserQueryController {

    private final UserQueryUseCase userQueryUseCase;
    private final UserActivityUseCase userActivityUseCase;
    private final SettingQueryUseCase settingQueryUseCase;
    private final UserIntegrationUseCase userIntegrationUseCase;
    private final PostResponseMapper postResponseMapper;

    /**
     * <h3>닉네임 중복 확인 API</h3>
     * <p>사용자의 닉네임이 이미 사용 중인지 확인하는 요청을 처리</p>
     *
     * @param userName 닉네임
     * @return 닉네임 사용 가능 여부
     * @since 2.0.0
     * @author Jaeik
     */
    @GetMapping("/username/check")
    public ResponseEntity<Boolean> checkUserName(@RequestParam String userName) {
        boolean isAvailable = !userQueryUseCase.existsByUserName(userName);
        return ResponseEntity.ok(isAvailable);
    }

    /**
     * <h3>사용자 설정 조회 API </h3>
     * <p>JWT 토큰의 settingId를 활용하여 효율적으로 설정 정보를 조회</p>
     * <p>User 전체 조회 없이 Setting만 직접 조회하여 성능 최적화</p>
     *
     * @param userDetails 사용자 인증 정보 (JWT에서 settingId 포함)
     * @return 사용자 설정 DTO
     * @since 2.0.0
     * @author Jaeik
     */
    @GetMapping("/setting")
    public ResponseEntity<SettingDTO> getSetting(@AuthenticationPrincipal CustomUserDetails userDetails) {
        SettingVO settingVO = settingQueryUseCase.findBySettingId(userDetails.getSettingId());
        return ResponseEntity.ok(SettingDTO.fromSettingVO(settingVO));
    }

    /**
     * <h3>사용자가 작성한 게시글 목록 조회 API</h3>
     * <p>현재 로그인한 사용자가 작성한 게시글 목록을 페이지네이션으로 조회합니다.</p>
     *
     * @param page        페이지 번호
     * @param size        페이지 크기  
     * @param userDetails 현재 로그인한 사용자 정보
     * @return 작성 게시글 목록 페이지
     * @since 2.0.0
     * @author Jaeik
     */
    @GetMapping("/posts")
    public ResponseEntity<Page<SimplePostResDTO>> getUserPosts(@RequestParam int page,
                                                              @RequestParam int size,
                                                              @AuthenticationPrincipal CustomUserDetails userDetails) {
        PageRequest pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<PostSearchResult> postList = userActivityUseCase.getUserPosts(userDetails.getUserId(), pageable);
        Page<SimplePostResDTO> dtoList = postList.map(postResponseMapper::convertToSimplePostResDTO);
        return ResponseEntity.ok(dtoList);
    }

    /**
     * <h3>사용자가 추천한 게시글 목록 조회 API</h3>
     * <p>현재 로그인한 사용자가 추천한 게시글 목록을 페이지네이션으로 조회합니다.</p>
     *
     * @param page        페이지 번호
     * @param size        페이지 크기
     * @param userDetails 현재 로그인한 사용자 정보
     * @return 추천한 게시글 목록 페이지
     * @since 2.0.0
     * @author Jaeik
     */
    @GetMapping("/likeposts")
    public ResponseEntity<Page<SimplePostResDTO>> getUserLikedPosts(@RequestParam int page,
                                                                   @RequestParam int size,
                                                                   @AuthenticationPrincipal CustomUserDetails userDetails) {
        PageRequest pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<PostSearchResult> likedPosts = userActivityUseCase.getUserLikedPosts(userDetails.getUserId(), pageable);
        Page<SimplePostResDTO> dtoList = likedPosts.map(postResponseMapper::convertToSimplePostResDTO);
        return ResponseEntity.ok(dtoList);
    }

    /**
     * <h3>사용자가 작성한 댓글 목록 조회 API</h3>
     * <p>현재 로그인한 사용자가 작성한 댓글 목록을 페이지네이션으로 조회합니다.</p>
     *
     * @param page        페이지 번호
     * @param size        페이지 크기
     * @param userDetails 현재 로그인한 사용자 정보
     * @return 작성 댓글 목록 페이지
     * @since 2.0.0
     * @author Jaeik
     */
    @GetMapping("/comments")
    public ResponseEntity<Page<SimpleCommentDTO>> getUserComments(@RequestParam int page,
                                                                 @RequestParam int size,
                                                                 @AuthenticationPrincipal CustomUserDetails userDetails) {
        PageRequest pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<SimpleCommentInfo> commentInfoList = userActivityUseCase.getUserComments(userDetails.getUserId(), pageable);
        Page<SimpleCommentDTO> commentList = commentInfoList.map(this::convertToSimpleCommentDTO);
        return ResponseEntity.ok(commentList);
    }

    /**
     * <h3>사용자가 추천한 댓글 목록 조회 API</h3>
     * <p>현재 로그인한 사용자가 추천한 댓글 목록을 페이지네이션으로 조회합니다.</p>
     *
     * @param page        페이지 번호
     * @param size        페이지 크기
     * @param userDetails 현재 로그인한 사용자 정보
     * @return 추천한 댓글 목록 페이지
     * @since 2.0.0
     * @author Jaeik
     */
    @GetMapping("/likecomments")
    public ResponseEntity<Page<SimpleCommentDTO>> getUserLikedComments(@RequestParam int page,
                                                                      @RequestParam int size,
                                                                      @AuthenticationPrincipal CustomUserDetails userDetails) {
        PageRequest pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<SimpleCommentInfo> likedCommentsInfo = userActivityUseCase.getUserLikedComments(userDetails.getUserId(), pageable);
        Page<SimpleCommentDTO> likedComments = likedCommentsInfo.map(this::convertToSimpleCommentDTO);
        return ResponseEntity.ok(likedComments);
    }

    /**
     * <h3>카카오 친구 목록 조회 API</h3>
     * <p>현재 로그인한 사용자의 카카오 친구 목록을 조회하고 비밀로그 가입 여부를 포함하여 반환합니다.</p>
     *
     * @param offset      조회 시작 위치 (기본값: 0)
     * @param limit       조회할 친구 수 (기본값: 10, 최대: 100)
     * @param userDetails 현재 로그인한 사용자 정보
     * @return Mono<ResponseEntity<KakaoFriendsResponse>> 카카오 친구 목록 (비밀로그 가입 여부 포함)
     * @since 2.0.0
     * @author Jaeik
     */
    @GetMapping("/friendlist")
    public Mono<ResponseEntity<KakaoFriendsResponse>> getKakaoFriendList(@RequestParam(defaultValue = "0") Integer offset,
                                                                   @RequestParam(defaultValue = "10") Integer limit,
                                                                   @AuthenticationPrincipal CustomUserDetails userDetails) {
        return userIntegrationUseCase.getKakaoFriendList(
                userDetails.getUserId(),
                userDetails.getTokenId(), // JWT에서 파싱된 현재 기기의 토큰 ID
                offset,
                limit
        )
        .map(friendsResponseVO -> {
            KakaoFriendsResponse friendsResponse = KakaoFriendsResponse.fromVO(friendsResponseVO);
            return ResponseEntity.ok(friendsResponse);
        });
    }

    /**
     * <h3>SimpleCommentInfo를 SimpleCommentDTO로 변환</h3>
     *
     * @param commentInfo 변환할 도메인 객체
     * @return SimpleCommentDTO 응답 DTO
     * @author jaeik
     * @since 2.0.0
     */
    public SimpleCommentDTO convertToSimpleCommentDTO(SimpleCommentInfo commentInfo) {
        return new SimpleCommentDTO(
                commentInfo.getId(),
                commentInfo.getPostId(),
                commentInfo.getUserName(),
                commentInfo.getContent(),
                commentInfo.getCreatedAt(),
                commentInfo.getLikeCount(),
                commentInfo.isUserLike()
        );
    }
}