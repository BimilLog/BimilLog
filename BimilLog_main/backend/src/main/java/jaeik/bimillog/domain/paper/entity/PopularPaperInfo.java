package jaeik.bimillog.domain.paper.entity;


import lombok.Getter;
import lombok.Setter;

/**
 * <h2>인기 롤링페이퍼 정보</h2>
 * <p>실시간 인기 롤링페이퍼의 상세 정보를 담는 DTO입니다.</p>
 * <p>Redis에서 조회한 점수 정보와 DB에서 조회한 추가 정보를 결합하여 사용합니다.</p>
 *
 * @author Jaeik
 * @version 2.0.0
 */
@Getter
@Setter
public class PopularPaperInfo {
    private Long memberId;
    private String memberName;
    private int rank; // 실시간 등수
    private double popularityScore; // 실시간 점수
    private int recentMessageCount; // 24시간 이내 메시지 수
}
