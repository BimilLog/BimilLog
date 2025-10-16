package jaeik.bimillog.domain.paper.application.port.out;

import jaeik.bimillog.domain.paper.application.service.PaperCommandService;
import jaeik.bimillog.domain.paper.application.service.PaperQueryService;
import jaeik.bimillog.domain.paper.entity.Message;
import jaeik.bimillog.domain.paper.entity.PopularPaperInfo;

import java.util.List;
import java.util.Optional;

/**
 * <h2>롤링페이퍼 조회 포트</h2>
 * <p>롤링페이퍼 도메인의 조회 작업을 담당하는 포트입니다.</p>
 * <p>메시지 ID 조회, 사용자별 메시지 조회, 방문용 메시지 조회, 소유자 ID 조회</p>
 *
 * @author Jaeik
 * @version 2.0.0
 */
public interface PaperQueryPort {

    /**
     * <h3>사용자 ID로 메시지 조회</h3>
     * <p>특정 사용자의 롤링페이퍼에 작성된 모든 메시지를 조회합니다.</p>
     * <p>내 롤링페이퍼 조회 시 사용됩니다.</p>
     * <p>{@link PaperQueryService#getMyPaper}에서 호출됩니다.</p>
     *
     * @param memberId 조회할 사용자 ID
     * @return List<Message> 해당 사용자의 롤링페이퍼 메시지 목록 (최신순 정렬)
     * @author Jaeik
     * @since 2.0.0
     */
    List<Message> findMessagesByUserId(Long memberId);

    /**
     * <h3>사용자명으로 방문용 메시지 조회</h3>
     * <p>특정 사용자명의 롤링페이퍼에 작성된 모든 메시지를 방문자용으로 조회합니다.</p>
     * <p>롤링페이퍼 방문 시 사용됩니다.</p>
     * <p>{@link PaperQueryService#visitPaper}에서 호출됩니다.</p>
     *
     * @param memberName 조회할 사용자명
     * @return List<Message> 방문용 메시지 목록
     * @author Jaeik
     * @since 2.0.0
     */
    List<Message> findMessagesByMemberName(String memberName);

    /**
     * <h3>메시지 소유자 ID 조회</h3>
     * <p>특정 메시지가 속한 롤링페이퍼의 소유자 ID를 조회합니다.</p>
     * <p>메시지 삭제 권한 검증 시 사용됩니다.</p>
     * <p>{@link PaperCommandService#deleteMessageInMyPaper}에서 권한 검증 시 호출됩니다.</p>
     *
     * @param messageId 조회할 메시지의 ID
     * @return Optional<Long> 롤링페이퍼 소유자의 사용자 ID (메시지가 존재하지 않으면 Optional.empty())
     * @author Jaeik
     * @since 2.0.0
     */
    Optional<Long> findOwnerIdByMessageId(Long messageId);

    /**
     * <h3>인기 롤링페이퍼 정보 보강</h3>
     * <p>memberId, memberName이 채워진 PopularPaperInfo 리스트에 24시간 이내 메시지 수를 채웁니다.</p>
     * <p>Message 테이블에서 최근 24시간 이내에 작성된 메시지 수를 계산하여 recentMessageCount를 설정합니다.</p>
     *
     * @param infos memberId, memberName, rank, popularityScore가 채워진 PopularPaperInfo 리스트
     * @author Jaeik
     * @since 2.0.0
     */
    void enrichPopularPaperInfos(List<PopularPaperInfo> infos);

}