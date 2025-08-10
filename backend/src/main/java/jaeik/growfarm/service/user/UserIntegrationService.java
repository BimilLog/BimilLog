package jaeik.growfarm.service.user;

import jaeik.growfarm.dto.admin.ReportDTO;
import jaeik.growfarm.dto.auth.KakaoFriendDTO;
import jaeik.growfarm.dto.auth.KakaoFriendsResponse;
import jaeik.growfarm.entity.report.Report;
import jaeik.growfarm.entity.user.Token;
import jaeik.growfarm.entity.user.Users;
import jaeik.growfarm.global.auth.CustomUserDetails;
import jaeik.growfarm.global.exception.CustomException;
import jaeik.growfarm.global.exception.ErrorCode;
import jaeik.growfarm.repository.admin.ReportRepository;
import jaeik.growfarm.repository.token.TokenRepository;
import jaeik.growfarm.repository.user.read.UserReadRepository;
import jaeik.growfarm.service.auth.strategy.KakaoLoginStrategy;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * <h2>사용자 통합 서비스</h2>
 * <p>
 * 외부 API 연동 및 기타 통합 기능을 담당
 * SRP: 외부 시스템과의 통합 기능만 담당 (카카오 API, 건의사항)
 * </p>
 *
 * @author Jaeik
 * @version 2.1.0
 * @since 2.0.0
 */
@Service
@RequiredArgsConstructor
public class UserIntegrationService {

    private final UserReadRepository userReadRepository;
    private final ReportRepository reportRepository;
    private final KakaoLoginStrategy kakaoLoginStrategy;
    private final TokenRepository tokenRepository;

    /**
     * <h3>건의 하기</h3>
     *
     * <p>
     * 비로그인은 유저 ID를 null로 처리하여 저장하고 로그인 한 사람의 경우에는 유저 ID를 저장한다.
     * </p>
     *
     * @param userDetails 현재 로그인한 사용자 정보
     * @param reportDTO   건의 내용 DTO
     * @throws CustomException DTO의 유저 ID와 Context의 유저 ID가 일치하지 않는 경우
     * @author Jaeik
     * @version 2.0.0
     * @since 2.0.0
     */
    @Transactional
    public void suggestion(CustomUserDetails userDetails, ReportDTO reportDTO) {
        Users user = reportDTO.getUserId() != null ? userReadRepository.findByIdWithSetting(userDetails.getUserId()).orElse(null) : null;
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
     * @version 2.1.0
     * @since 2.0.0
     */
    @Transactional(readOnly = true)
    public KakaoFriendsResponse getFriendList(CustomUserDetails userDetails, int offset) {
        Token token = tokenRepository.findById(userDetails.getTokenId())
                .orElseThrow(() -> new CustomException(ErrorCode.NOT_FIND_TOKEN));

        KakaoFriendsResponse friendsResponse = kakaoLoginStrategy.getFriendList(token.getAccessToken(), offset, 100);
        mapUserNamesToFriends(friendsResponse.getElements());

        return friendsResponse;
    }

    /**
     * <h3>친구 목록에 닉네임을 매핑한다.</h3>
     *
     * <p>
     * 카카오 친구 목록에 해당하는 사용자 닉네임을 매핑하는 내부 메서드
     * </p>
     *
     * @param friendList 친구 목록
     * @author Jaeik
     * @version 2.0.0
     * @since 2.0.0
     */
    private void mapUserNamesToFriends(List<KakaoFriendDTO> friendList) {
        if (friendList == null || friendList.isEmpty()) {
            return;
        }

        List<String> friendSocialIds = friendList.stream()
                .map(friend -> String.valueOf(friend.getId()))
                .toList();

        List<String> userNames = userReadRepository.findUserNamesInOrder(friendSocialIds);

        for (int i = 0; i < friendList.size(); i++) {
            friendList.get(i).setUserName(userNames.get(i));
        }
    }
}