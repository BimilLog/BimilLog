package jaeik.growfarm.service;

import jaeik.growfarm.dto.admin.ReportDTO;
import jaeik.growfarm.dto.board.CommentDTO;
import jaeik.growfarm.dto.board.SimplePostDTO;
import jaeik.growfarm.dto.kakao.KakaoFriendDTO;
import jaeik.growfarm.dto.kakao.KakaoFriendListDTO;
import jaeik.growfarm.dto.user.SettingDTO;
import jaeik.growfarm.entity.board.Comment;
import jaeik.growfarm.entity.board.Post;
import jaeik.growfarm.entity.report.Report;
import jaeik.growfarm.entity.user.Setting;
import jaeik.growfarm.entity.user.Users;
import jaeik.growfarm.global.auth.CustomUserDetails;
import jaeik.growfarm.repository.admin.ReportRepository;
import jaeik.growfarm.repository.comment.CommentLikeRepository;
import jaeik.growfarm.repository.comment.CommentRepository;
import jaeik.growfarm.repository.post.PostLikeRepository;
import jaeik.growfarm.repository.post.PostRepository;
import jaeik.growfarm.repository.user.UserRepository;
import jaeik.growfarm.util.BoardUtil;
import jaeik.growfarm.util.UserUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

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
    private final CommentService commentService;
    private final ReportRepository reportRepository;
    private final PostLikeRepository postLikeRepository;
    private final CommentLikeRepository commentLikeRepository;
    private final KakaoService kakaoService;
    private final UserUtil userUtil;

    // 해당 유저의 작성 글 목록 반환
    public Page<SimplePostDTO> getPostList(int page, int size, CustomUserDetails userDetails) {

        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<Post> posts = postRepository.findByUserId(userDetails.getUserDTO().getUserId(), pageable);

        return posts.map(
                post -> boardUtil.postToSimpleDTO(post, commentRepository.countByPostId(post.getId()),
                        postLikeRepository.countByPostId(post.getId())));
    }

    // 해당 유저의 작성 댓글 목록 반환
    public Page<CommentDTO> getCommentList(int page, int size, CustomUserDetails userDetails) {

        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<Comment> comments = commentRepository.findByUserId(userDetails.getUserDTO().getUserId(), pageable);

        return comments.map(
                comment -> boardUtil.commentToDTO(comment, commentLikeRepository.countByCommentId(comment.getId()), false));
    }

    // 농장이름 변경
    @Transactional
    public void updateFarmName(String farmName, CustomUserDetails userDetails) {
        // 이미 같은 농장이름이 있는지 검사
        if (userRepository.existsByFarmName(farmName)) {
            throw new IllegalArgumentException("이미 존재하는 농장이름입니다.");
        }

        // 영속성 컨텍스트에서 사용자 엔티티를 조회하여 직접 업데이트
        Users user = userRepository.findById(userDetails.getUserDTO().getUserId())
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));

        // 농장 이름 업데이트
        user.updateFarmName(farmName);
    }

    public Page<SimplePostDTO> getLikedPosts(int page, int size, CustomUserDetails userDetails) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<Post> posts = postRepository.findByLikedPosts(userDetails.getUserDTO().getUserId(), pageable);

        return posts.map(
                post -> boardUtil.postToSimpleDTO(post, commentRepository.countByPostId(post.getId()),
                        postLikeRepository.countByPostId(post.getId())));
    }

    public Page<CommentDTO> getLikedComments(int page, int size, CustomUserDetails userDetails) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<Comment> comments = commentRepository.findByLikedComments(userDetails.getUserDTO().getUserId(), pageable);

        return comments.map(
                comment -> boardUtil.commentToDTO(comment, commentLikeRepository.countByCommentId(comment.getId()), true));
    }

    public void suggestion(CustomUserDetails userDetails, ReportDTO reportDTO) {

        if (userDetails == null) {
            throw new RuntimeException("다시 로그인 해 주세요.");
        }

        // 신고 내용이 비어있지 않은지 확인
        if (reportDTO.getContent() == null || reportDTO.getContent().isEmpty()) {
            throw new IllegalArgumentException("신고 내용을 입력해주세요.");
        }

        // 신고 타입이 비어있지 않은지 확인
        if (reportDTO.getReportType() == null) {
            throw new IllegalArgumentException("신고 타입을 선택해주세요.");
        }

        // 신고 내용 저장
        Users user = userRepository.findById(reportDTO.getUserId())
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));

        Report report = Report.builder()
                .reportType(reportDTO.getReportType())
                .users(user)
                .targetId(reportDTO.getTargetId())
                .content(reportDTO.getContent())
                .build();

        reportRepository.save(report);
    }

    public KakaoFriendListDTO getFriendList(CustomUserDetails userDetails, int offset) {
        Users user = userRepository.findById(userDetails.getUserDTO().getUserId())
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));

        KakaoFriendListDTO kakaoFriendListDTO = kakaoService.getFriendList(user.getToken().getKakaoAccessToken(), offset);
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

    @Transactional
    public void updateSetting(SettingDTO settingDTO, Long userId) {

        Users user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));

        Setting setting = user.getSetting();
        setting.updateSetting(settingDTO.isFarmNotification(),
                settingDTO.isCommentNotification(),
                settingDTO.isPostFeaturedNotification(),
                settingDTO.isCommentFeaturedNotification());

    }

    public SettingDTO getSetting(Long userId) {
        Users user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));

        return userUtil.settingToSettingDTO(user.getSetting());
    }
}
