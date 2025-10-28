package jaeik.bimillog.domain.paper.application.port.out;

import java.util.List;
import java.util.Map;

/**
 * <h2>롤링페이퍼-사용자 도메인 연결 포트</h2>
 * <p>Paper 도메인과 Member 도메인을 연결하는 아웃바운드 포트입니다.</p>
 * <p>롤링페이퍼 도메인에서 사용자 도메인의 기능을 호출하는 중개 역할을 수행합니다.</p>
 *
 * @author Jaeik
 * @version 2.0.0
 */
public interface PaperToMemberPort {

    /**
     * <h3>여러 사용자 ID로 사용자명 배치 조회</h3>
     * <p>여러 사용자 ID에 해당하는 사용자명을 한 번에 조회합니다.</p>
     *
     * @param memberIds 조회할 사용자 ID 목록
     * @return Map&lt;Long, String&gt; 사용자 ID를 키로, 사용자명을 값으로 하는 맵
     * @author Jaeik
     * @since 2.0.0
     */
    Map<Long, String> findMemberNamesByIds(List<Long> memberIds);
}
