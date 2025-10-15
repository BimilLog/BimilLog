package jaeik.bimillog.domain.paper.entity;


import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class PopularPaperInfo {
    private Long memberId;
    private String memberName;
    private int rank; // 실시간 등수
    private double popularityScore; // 실시간 점수
    private int recentMessageCount; // 24시간 이내 메시지 수
}
