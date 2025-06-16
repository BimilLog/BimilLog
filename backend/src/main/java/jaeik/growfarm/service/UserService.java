package jaeik.growfarm.service;

import jaeik.growfarm.dto.admin.ReportDTO;
import jaeik.growfarm.dto.board.CommentDTO;
import jaeik.growfarm.dto.board.SimplePostDTO;
import jaeik.growfarm.dto.kakao.KakaoCheckConsentDTO;
import jaeik.growfarm.dto.kakao.KakaoFriendDTO;
import jaeik.growfarm.dto.kakao.KakaoFriendListDTO;
import jaeik.growfarm.dto.user.SettingDTO;
import jaeik.growfarm.entity.comment.Comment;
import jaeik.growfarm.entity.post.Post;
import jaeik.growfarm.entity.report.Report;
import jaeik.growfarm.entity.user.Setting;
import jaeik.growfarm.entity.user.Users;
import jaeik.growfarm.global.auth.CustomUserDetails;
import jaeik.growfarm.global.exception.CustomException;
import jaeik.growfarm.global.exception.ErrorCode;
import jaeik.growfarm.repository.admin.ReportRepository;
import jaeik.growfarm.repository.comment.CommentLikeRepository;
import jaeik.growfarm.repository.comment.CommentRepository;
import jaeik.growfarm.repository.post.PostLikeRepository;
import jaeik.growfarm.repository.post.PostRepository;
import jaeik.growfarm.repository.user.UserRepository;
import jaeik.growfarm.service.comment.CommentService;
import jaeik.growfarm.util.BoardUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;

/*
 * UserService 클래스
 * 사용자 관련 서비스 클래스
 * 수정일 : 2025-05-03
 */
@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final PostRepository postRepository;
    private final CommentRepository commentRepository;
    private final BoardUtil boardUtil;
    private final ReportRepository reportRepository;
    private final PostLikeRepository postLikeRepository;
    private final CommentLikeRepository commentLikeRepository;
    private final KakaoService kakaoService;

    /**
     * <h3>사용자 작성 글 목록 조회</h3>
     *
     * <p>
     * 해당 유저의 작성 글 목록을 페이지네이션으로 반환한다.
     * </p>
     * 
     * @since 1.0.0
     * @author Jaeik
     * @param page        페이지 번호
     * @param size        페이지 크기
     * @param userDetails 현재 로그인한 사용자 정보
     * @return 작성 글 목록 페이지
     */
    public Page<SimplePostDTO> getPostList(int page, int size, CustomUserDetails userDetails) {

        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<Post> posts = postRepository.findByUserId(userDetails.getClientDTO().getUserId(), pageable);

        return posts.map(
                post -> boardUtil.postToSimpleDTO(post, commentRepository.countByPostId(post.getId()),
                        postLikeRepository.countByPostId(post.getId())));
    }

    /**
     * <h3>사용자 작성 댓글 목록 조회</h3>
     *
     * <p>
     * 해당 유저의 작성 댓글 목록을 페이지네이션으로 반환한다.
     * </p>
     *
     * @since 1.0.0
     * @author Jaeik
     * @param page        페이지 번호
     * @param size        페이지 크기
     * @param userDetails 현재 로그인한 사용자 정보
     * @return 작성 댓글 목록 페이지
     */
    public Page<CommentDTO> getCommentList(int page, int size, CustomUserDetails userDetails) {

        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<Comment> comments = commentRepository.findByUserId(userDetails.getClientDTO().getUserId(), pageable);

        return comments.map(
                comment -> boardUtil.commentToDTO(comment, commentLikeRepository.countByCommentId(comment.getId()),
                        false));
    }

    /**
     * <h3>농장이름 변경</h3>
     *
     * <p>
     * Dirty Read의 발생을 막기 위해 커밋된 읽기로 격리 수준 조정
     * 유니크 컬럼이기 때문에 Non-repeatable read 발생해도 문제 없음
     * </p>
     *
     * @param farmName    새로운 농장이름
     * @param userDetails 현재 로그인한 사용자 정보
     * @throws IllegalArgumentException 이미 존재하는 농장이름인 경우
     * @author Jaeik
     * @since 1.0.0
     */
    @Transactional(isolation = Isolation.READ_COMMITTED)
    public void updateFarmName(String farmName, CustomUserDetails userDetails) {
        if (userRepository.existsByFarmName(farmName)) {
            throw new IllegalArgumentException("이미 존재하는 농장이름입니다.");
        }

        Users user = userRepository.findById(userDetails.getClientDTO().getUserId())
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));

        user.updateFarmName(farmName);
    }

    /**
     * <h3>사용자 좋아요한 글 목록 조회</h3>
     *
     * <p>
     * 해당 유저가 좋아요한 글 목록을 페이지네이션으로 반환한다.
     * </p>
     * 
     * @since 1.0.0
     * @author Jaeik
     * @param page        페이지 번호
     * @param size        페이지 크기
     * @param userDetails 현재 로그인한 사용자 정보
     * @return 좋아요한 글 목록 페이지
     */
    public Page<SimplePostDTO> getLikedPosts(int page, int size, CustomUserDetails userDetails) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<Post> posts = postRepository.findByLikedPosts(userDetails.getClientDTO().getUserId(), pageable);

        return posts.map(
                post -> boardUtil.postToSimpleDTO(post, commentRepository.countByPostId(post.getId()),
                        postLikeRepository.countByPostId(post.getId())));
    }

    /**
     * <h3>사용자 좋아요한 댓글 목록 조회</h3>
     *
     * <p>
     * 해당 유저가 좋아요한 댓글 목록을 페이지네이션으로 반환한다.
     * </p>
     * 
     * @since 1.0.0
     * @author Jaeik
     * @param page        페이지 번호
     * @param size        페이지 크기
     * @param userDetails 현재 로그인한 사용자 정보
     * @return 좋아요한 댓글 목록 페이지
     */
    public Page<CommentDTO> getLikedComments(int page, int size, CustomUserDetails userDetails) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<Comment> comments = commentRepository.findByLikedComments(userDetails.getClientDTO().getUserId(), pageable);

        return comments.map(
                comment -> boardUtil.commentToDTO(comment, commentLikeRepository.countByCommentId(comment.getId()),
                        true));
    }

    /**
     * <h3>건의 하기</h3>
     *
     * <p>
     * 비로그인은 유저 ID를 null로 처리하여 저장하고 로그인 한 사람의 경우에는 유저 ID를 저장한다.
     * </p>
     *
     * @param reportDTO 건의 내용 DTO
     * @throws CustomException DTO의 유저 ID와 Context의 유저 ID가 일치하지 않는 경우
     * @author Jaeik
     * @since 1.0.0
     */
    public void suggestion(ReportDTO reportDTO) {
        Users user = null;

        if (reportDTO.getUserId() != null) {
            SecurityContext securityContext = SecurityContextHolder.getContext();
            CustomUserDetails userDetails = (CustomUserDetails) securityContext.getAuthentication().getPrincipal();

            if (!Objects.equals(reportDTO.getUserId(), userDetails.getUserId())) {
                throw new CustomException(ErrorCode.INVALID_USER_ID);
            }

            user = userRepository.getReferenceById(reportDTO.getUserId());
        }
        Report report = Report.DtoToReport(reportDTO, user);
        reportRepository.save(report);
    }

    /**
     * <h3>카카오 친구 목록 조회</h3>
     *
     * <p>
     * 카카오 API를 통해 친구 목록을 가져오고 농장 이름을 매핑하여 반환한다.
     * </p>
     * 
     * @since 1.0.0
     * @author Jaeik
     * @param userDetails 현재 로그인한 사용자 정보
     * @param offset      페이지 오프셋
     * @return 카카오 친구 목록 DTO
     */
    public KakaoFriendListDTO getFriendList(CustomUserDetails userDetails, int offset) {
        Users user = userRepository.findById(userDetails.getClientDTO().getUserId())
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));

        KakaoCheckConsentDTO kakaoCheckConsentDTO = kakaoService.checkConsent(user.getToken().getKakaoAccessToken());
        boolean hasNotAgreed = Arrays.stream(kakaoCheckConsentDTO.getScopes())
                .anyMatch(scope -> !scope.isAgreed());

        if (hasNotAgreed) {
            throw new CustomException(ErrorCode.KAKAO_FRIEND_CONSENT_FAIL);
        }

        KakaoFriendListDTO kakaoFriendListDTO = kakaoService.getFriendList(user.getToken().getKakaoAccessToken(),
                offset);
        List<KakaoFriendDTO> friendList = kakaoFriendListDTO.getElements();
        List<Long> friendIds = friendList.stream()
                .map(KakaoFriendDTO::getId)
                .toList();

        List<String> farmNames = userRepository.findFarmNamesInOrder(friendIds);

        for (int i = 0; i < friendList.size(); i++) {
            friendList.get(i).setFarmName(farmNames.get(i));
        }

        return kakaoFriendListDTO;
    }

    /**
     * <h3>사용자 설정 업데이트</h3>
     *
     * <p>
     * 사용자의 알림 설정을 업데이트한다.
     * </p>
     * 
     * @since 1.0.0
     * @author Jaeik
     * @param settingDTO 설정 정보 DTO
     * @param userId     사용자 ID
     */
    @Transactional
    public void updateSetting(SettingDTO settingDTO, Long userId) {

        Users user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));

        Setting setting = user.getSetting();
        setting.updateSetting(settingDTO.farmNotification(),
                settingDTO.commentNotification(),
                settingDTO.postFeaturedNotification(),
                settingDTO.commentFeaturedNotification());

    }

    /**
     * <h3>사용자 설정 조회</h3>
     *
     * <p>
     * 사용자의 현재 설정 정보를 조회한다.
     * </p>
     * 
     * @since 1.0.0
     * @author Jaeik
     * @param userId 사용자 ID
     * @return 설정 정보 DTO
     */
    public SettingDTO getSetting(Long userId) {
        Users user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));

        return userUtil.settingToSettingDTO(user.getSetting());
    }
}
