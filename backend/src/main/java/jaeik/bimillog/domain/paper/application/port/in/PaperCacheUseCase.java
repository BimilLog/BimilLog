package jaeik.bimillog.domain.paper.application.port.in;

import jaeik.bimillog.domain.paper.entity.PopularPaperInfo;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

/**
 * <h2>롤링페이퍼 캐시 유스케이스</h2>
 * <p>Paper 도메인의 캐시 데이터 조회 작업을 담당하는 유스케이스입니다.</p>
 * <p>실시간 인기 롤링페이퍼 조회</p>
 *
 * @author Jaeik
 * @version 2.0.0
 */
public interface PaperCacheUseCase {

    /**
     * <h3>실시간 인기 롤링페이퍼 조회</h3>
     * <p>실시간 인기 롤링페이퍼 목록을 페이지네이션으로 조회합니다.</p>
     *
     * @param pageable 페이지 정보 (페이지 번호, 크기)
     * @return Page<PopularPaperInfo> 실시간 인기 롤링페이퍼 목록 페이지 (전체 개수 포함)
     * @author Jaeik
     * @since 2.0.0
     */
    Page<PopularPaperInfo> getRealtimePapers(Pageable pageable);
}
