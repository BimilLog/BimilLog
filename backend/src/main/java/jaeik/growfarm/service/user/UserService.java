package jaeik.growfarm.service.user;

import jaeik.growfarm.dto.admin.ReportDTO;
import jaeik.growfarm.dto.auth.KakaoFriendsResponse;
import jaeik.growfarm.dto.comment.SimpleCommentDTO;
import jaeik.growfarm.dto.post.SimplePostResDTO;
import jaeik.growfarm.dto.user.SettingDTO;
import jaeik.growfarm.global.auth.CustomUserDetails;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;

/**
 * <h2>사용자 서비스 파사드</h2>
 * <p>
 * 사용자 관련 서비스들을 조정하는 파사드 클래스
 * Facade Pattern: UserContentService, UserProfileService, UserIntegrationService를 조정
 * SRP: 서비스간 조정 및 통합 인터페이스 제공만 담당
 * </p>
 *
 * @author Jaeik
 * @version 2.0.0
 * @since 2.0.0
 */
@Service
@RequiredArgsConstructor
public class UserService {

    private final UserContentService userContentService;
    private final UserProfileService userProfileService;
    private final UserIntegrationService userIntegrationService;

    /**
     * <h3>유저 작성 글 목록 조회</h3>
     *
     * <p>
     * UserContentService로 위임하여 작성 글 목록을 조회한다.
     * </p>
     *
     * @param page        페이지 번호
     * @param size        페이지 크기
     * @param userDetails 현재 로그인한 사용자 정보
     * @return 작성 글 목록 페이지
     * @author Jaeik
     * @version 2.0.0
     * @since 2.0.0
     */
    public Page<SimplePostResDTO> getPostList(int page, int size, CustomUserDetails userDetails) {
        return userContentService.getPostList(page, size, userDetails);
    }

    /**
     * <h3>유저 작성 댓글 목록 조회</h3>
     *
     * <p>
     * UserContentService로 위임하여 작성 댓글 목록을 조회한다.
     * </p>
     *
     * @param page        페이지 번호
     * @param size        페이지 크기
     * @param userDetails 현재 로그인한 사용자 정보
     * @return 작성 댓글 목록 페이지
     * @author Jaeik
     * @version 2.0.0
     * @since 2.0.0
     */
    public Page<SimpleCommentDTO> getCommentList(int page, int size, CustomUserDetails userDetails) {
        return userContentService.getCommentList(page, size, userDetails);
    }

    /**
     * <h3>사용자 추천한 글 목록 조회</h3>
     *
     * <p>
     * UserContentService로 위임하여 좋아요한 글 목록을 조회한다.
     * </p>
     *
     * @param page        페이지 번호
     * @param size        페이지 크기
     * @param userDetails 현재 로그인한 사용자 정보
     * @return 좋아요한 글 목록 페이지
     * @author Jaeik
     * @version 2.0.0
     * @since 2.0.0
     */
    public Page<SimplePostResDTO> getLikedPosts(int page, int size, CustomUserDetails userDetails) {
        return userContentService.getLikedPosts(page, size, userDetails);
    }

    /**
     * <h3>사용자 추천한 댓글 목록 조회</h3>
     *
     * <p>
     * UserContentService로 위임하여 추천한 댓글 목록을 조회한다.
     * </p>
     *
     * @param page        페이지 번호
     * @param size        페이지 크기
     * @param userDetails 현재 로그인한 사용자 정보
     * @return 추천한 댓글 목록 페이지
     * @author Jaeik
     * @version 2.0.0
     * @since 2.0.0
     */
    public Page<SimpleCommentDTO> getLikedComments(int page, int size, CustomUserDetails userDetails) {
        return userContentService.getLikedComments(page, size, userDetails);
    }

    /**
     * <h3>닉네임 변경</h3>
     *
     * <p>
     * UserProfileService로 위임하여 닉네임을 변경한다.
     * </p>
     *
     * @param userName    새로운 닉네임
     * @param userDetails 현재 로그인한 사용자 정보
     * @author Jaeik
     * @version 2.0.0
     * @since 2.0.0
     */
    public void updateUserName(String userName, CustomUserDetails userDetails) {
        userProfileService.updateUserName(userName, userDetails);
    }

    /**
     * <h3>닉네임 중복 확인</h3>
     *
     * <p>
     * UserProfileService로 위임하여 닉네임 중복을 확인한다.
     * </p>
     *
     * @param userName 닉네임
     * @return 닉네임이 사용 가능한 경우 true
     * @throws CustomException 닉네임이 이미 존재하는 경우
     * @author Jaeik
     * @version 2.0.0
     * @since 2.0.0
     */
    public boolean isUserNameAvailable(String userName) {
        return userProfileService.isUserNameAvailable(userName);
    }

    /**
     * <h3>건의 하기</h3>
     *
     * <p>
     * UserIntegrationService로 위임하여 건의사항을 처리한다.
     * </p>
     *
     * @param userDetails 현재 로그인한 사용자 정보
     * @param reportDTO 건의 내용 DTO
     * @throws CustomException DTO의 유저 ID와 Context의 유저 ID가 일치하지 않는 경우
     * @author Jaeik
     * @version 2.0.0
     * @since 2.0.0
     */
    public void suggestion(CustomUserDetails userDetails, ReportDTO reportDTO) {
        userIntegrationService.suggestion(userDetails, reportDTO);
    }

    /**
     * <h3>카카오 친구 목록 조회</h3>
     *
     * <p>
     * UserIntegrationService로 위임하여 카카오 친구 목록을 조회한다.
     * </p>
     *
     * @param userDetails 현재 로그인한 사용자 정보
     * @param offset      페이지 오프셋
     * @return 카카오 친구 목록 DTO
     * @author Jaeik
     * @version 2.0.0
     * @since 2.0.0
     */
    public KakaoFriendsResponse getFriendList(CustomUserDetails userDetails, int offset) {
        return userIntegrationService.getFriendList(userDetails, offset);
    }

    /**
     * <h3>사용자 설정 조회</h3>
     *
     * <p>
     * UserProfileService로 위임하여 사용자 설정을 조회한다.
     * </p>
     *
     * @param userDetails 현재 로그인 한 사용자 정보
     * @return 설정 정보 DTO
     * @author Jaeik
     * @version 2.0.0
     * @since 2.0.0
     */
    public SettingDTO getSetting(CustomUserDetails userDetails) {
        return userProfileService.getSetting(userDetails);
    }

    /**
     * <h3>사용자 설정 업데이트</h3>
     *
     * <p>
     * UserProfileService로 위임하여 사용자 설정을 업데이트한다.
     * </p>
     *
     * @param settingDTO  설정 정보 DTO
     * @param userDetails 현재 로그인한 사용자 정보
     * @author Jaeik
     * @version 2.0.0
     * @since 2.0.0
     */
    public void updateSetting(SettingDTO settingDTO, CustomUserDetails userDetails) {
        userProfileService.updateSetting(settingDTO, userDetails);
    }
}
