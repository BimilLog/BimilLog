package jaeik.growfarm.service.user;

import jaeik.growfarm.dto.admin.ReportDTO;
import jaeik.growfarm.dto.comment.SimpleCommentDTO;
import jaeik.growfarm.dto.kakao.KakaoCheckConsentDTO;
import jaeik.growfarm.dto.kakao.KakaoFriendDTO;
import jaeik.growfarm.dto.kakao.KakaoFriendListDTO;
import jaeik.growfarm.dto.post.SimplePostDTO;
import jaeik.growfarm.dto.user.SettingDTO;
import jaeik.growfarm.entity.report.Report;
import jaeik.growfarm.entity.user.Setting;
import jaeik.growfarm.entity.user.Token;
import jaeik.growfarm.entity.user.Users;
import jaeik.growfarm.global.auth.CustomUserDetails;
import jaeik.growfarm.global.exception.CustomException;
import jaeik.growfarm.global.exception.ErrorCode;
import jaeik.growfarm.repository.admin.ReportRepository;
import jaeik.growfarm.repository.comment.CommentRepository;
import jaeik.growfarm.repository.post.PostRepository;
import jaeik.growfarm.repository.token.TokenRepository;
import jaeik.growfarm.repository.user.SettingRepository;
import jaeik.growfarm.repository.user.UserRepository;
import jaeik.growfarm.service.auth.AuthService;
import jaeik.growfarm.service.kakao.KakaoService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.List;

/**
 * <h2>사용자 서비스 클래스</h2>
 * <p>
 * 사용자 관련 비즈니스 로직을 처리하는 서비스 클래스
 * </p>
 *
 * @author Jaeik
 * @version 1.0.0
 */
@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final PostRepository postRepository;
    private final CommentRepository commentRepository;
    private final ReportRepository reportRepository;
    private final KakaoService kakaoService;
    private final TokenRepository tokenRepository;
    private final SettingRepository settingRepository;
    private final UserUpdateService userUpdateService;
    private final AuthService authService;

    /**
     * <h3>유저 작성 글 목록 조회</h3>
     *
     * <p>
     * 해당 유저의 작성 글 목록을 페이지네이션으로 반환한다.
     * </p>
     *
     * @param page        페이지 번호
     * @param size        페이지 크기
     * @param userDetails 현재 로그인한 사용자 정보
     * @return 작성 글 목록 페이지
     * @author Jaeik
     * @since 1.0.0
     */
    public Page<SimplePostDTO> getPostList(int page, int size, CustomUserDetails userDetails) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        return postRepository.findPostsByUserId(userDetails.getUserId(), pageable);
    }

    /**
     * <h3>유저 작성 댓글 목록 조회</h3>
     *
     * <p>
     * 해당 유저의 작성 댓글 목록을 페이지네이션으로 반환한다.
     * </p>
     *
     * @param page        페이지 번호
     * @param size        페이지 크기
     * @param userDetails 현재 로그인한 사용자 정보
     * @return 작성 댓글 목록 페이지
     * @author Jaeik
     * @since 1.0.0
     */
    public Page<SimpleCommentDTO> getCommentList(int page, int size, CustomUserDetails userDetails) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        return commentRepository.findCommentsByUserId(userDetails.getUserId(), pageable);
    }

    /**
     * <h3>사용자 추천한 글 목록 조회</h3>
     *
     * <p>
     * 해당 유저가 좋아요한 글 목록을 페이지네이션으로 반환한다.
     * </p>
     *
     * @param page        페이지 번호
     * @param size        페이지 크기
     * @param userDetails 현재 로그인한 사용자 정보
     * @return 좋아요한 글 목록 페이지
     * @author Jaeik
     * @since 1.0.0
     */
    public Page<SimplePostDTO> getLikedPosts(int page, int size, CustomUserDetails userDetails) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        return postRepository.findLikedPostsByUserId(userDetails.getUserId(), pageable);
    }

    /**
     * <h3>사용자 추천한 댓글 목록 조회</h3>
     *
     * <p>
     * 해당 유저가 추천한 댓글 목록을 페이지네이션으로 반환한다.
     * </p>
     *
     * @param page        페이지 번호
     * @param size        페이지 크기
     * @param userDetails 현재 로그인한 사용자 정보
     * @return 추천한 댓글 목록 페이지
     * @author Jaeik
     * @since 1.0.0
     */
    public Page<SimpleCommentDTO> getLikedComments(int page, int size, CustomUserDetails userDetails) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        return commentRepository.findLikedCommentsByUserId(userDetails.getUserId(), pageable);
    }

    /**
     * <h3>닉네임 변경</h3>
     *
     * <p>
     * Dirty Read의 발생을 막기 위해 커밋된 읽기로 격리 수준 조정
     * </p>
     * <p>
     * 유니크 컬럼이기 때문에 Non-repeatable read 발생해도 문제 없음
     * </p>
     *
     * @param userName    새로운 닉네임
     * @param userDetails 현재 로그인한 사용자 정보
     * @author Jaeik
     * @since 1.0.0
     */
    public void updateUserName(String userName, CustomUserDetails userDetails) {
        isUserNameAvailable(userName);
        Users user = userRepository.findById(userDetails.getClientDTO().getUserId())
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));

        userUpdateService.userNameUpdate(userName, user);
    }

    /**
     * <h3>닉네임 중복 확인</h3>
     *
     * <p>
     * 주어진 닉네임이 이미 존재하는지 확인한다.
     * </p>
     *
     * @param userName 닉네임
     * @return 닉네임이 사용 가능한 경우 true
     * @throws CustomException 닉네임이 이미 존재하는 경우
     * @author Jaeik
     * @since 1.0.0
     */
    public boolean isUserNameAvailable(String userName) {
        if (userRepository.existsByUserName(userName)) {
            throw new CustomException(ErrorCode.EXISTED_NICKNAME);
        }
        return true;
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
    public void suggestion(CustomUserDetails userDetails, ReportDTO reportDTO) {
        Users user = reportDTO.getUserId() != null ? userRepository.getReferenceById(userDetails.getUserId()) : null;
        Report report = Report.DtoToReport(reportDTO, user);
        reportRepository.save(report);
    }

    /**
     * <h3>카카오 친구 목록 조회</h3>
     *
     * <p>
     * 카카오 API를 통해 친구 목록을 가져오고 닉네임을 매핑하여 반환한다.
     * </p>
     *
     * @param userDetails 현재 로그인한 사용자 정보
     * @param offset      페이지 오프셋
     * @return 카카오 친구 목록 DTO
     * @author Jaeik
     * @since 1.0.20
     */
    public KakaoFriendListDTO getFriendList(CustomUserDetails userDetails, int offset) {
        Token token = tokenRepository.findById(userDetails.getTokenId())
                .orElseThrow(() -> new CustomException(ErrorCode.NOT_FIND_TOKEN));
        String accessToken = authService.validateKakaoConsent(token);

        KakaoFriendListDTO friendListDTO = kakaoService.getFriendList(accessToken, offset);
        mapUserNamesToFriends(friendListDTO.getElements());

        return friendListDTO;
    }

    /**
     * <h3>사용자 설정 조회</h3>
     *
     * <p>
     * 사용자의 현재 설정 정보를 조회한다.
     * </p>
     *
     * @param userDetails 현재 로그인 한 사용자 정보
     * @return 설정 정보 DTO
     * @author Jaeik
     * @since 1.0.0
     */
    public SettingDTO getSetting(CustomUserDetails userDetails) {
        Setting setting = settingRepository.findById(userDetails.getClientDTO().getSettingId())
                .orElseThrow(() -> new CustomException(ErrorCode.SETTINGS_NOT_FOUND));

        return new SettingDTO(setting);
    }

    /**
     * <h3>사용자 설정 업데이트</h3>
     *
     * <p>
     * 사용자의 알림 설정을 업데이트한다.
     * </p>
     *
     * @param settingDTO  설정 정보 DTO
     * @param userDetails 현재 로그인한 사용자 정보
     * @author Jaeik
     * @since 1.0.0
     */
    public void updateSetting(SettingDTO settingDTO, CustomUserDetails userDetails) {
        Setting setting = settingRepository.findById(userDetails.getClientDTO().getSettingId())
                .orElseThrow(() -> new CustomException(ErrorCode.SETTINGS_NOT_FOUND));

        userUpdateService.settingUpdate(settingDTO, setting);
    }


    /**
     * <h3>친구 목록에 닉네임을 매핑한다.</h3>
     *
     * @param friendList 친구 목록
     * @author Jaeik
     * @since 1.0.0
     */
    private void mapUserNamesToFriends(List<KakaoFriendDTO> friendList) {
        if (friendList.isEmpty()) {
            return;
        }

        List<Long> friendIds = friendList.stream()
                .map(KakaoFriendDTO::getId)
                .toList();

        List<String> userNames = userRepository.findUserNamesInOrder(friendIds);

        for (int i = 0; i < friendList.size(); i++) {
            friendList.get(i).setUserName(userNames.get(i));
        }
    }
}
