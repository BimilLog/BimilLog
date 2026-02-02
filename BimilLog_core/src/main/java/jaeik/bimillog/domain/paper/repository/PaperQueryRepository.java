package jaeik.bimillog.domain.paper.repository;

import com.querydsl.core.Tuple;
import com.querydsl.jpa.impl.JPAQueryFactory;
import jaeik.bimillog.domain.paper.entity.Message;
import jaeik.bimillog.domain.paper.entity.PopularPaperInfo;
import jaeik.bimillog.domain.paper.entity.QMessage;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * <h2>롤링페이퍼 조회 리포지터리</h2>
 * <p>롤링페이퍼 도메인의 조회 작업을 담당하는 리포지터리.</p>
 *
 * @author Jaeik
 * @version 2.6.0
 */
@Repository
@RequiredArgsConstructor
public class PaperQueryRepository {
    private final JPAQueryFactory jpaQueryFactory;
    private final QMessage message = QMessage.message;

    public List<Message> getMyMessageList(Long memberId) {
        return jpaQueryFactory.select(message)
                .from(message)
                .where(message.member.id.eq(memberId))
                .orderBy(message.createdAt.desc())
                .fetch();
    }



    /**
     * <h3>인기 롤링페이퍼 정보 보강</h3>
     * <p>memberId, memberName이 채워진 PopularPaperInfo 리스트에 24시간 이내 메시지 수를 채웁니다.</p>
     * <p>Message 테이블에서 최근 24시간 이내에 작성된 메시지 수를 계산하여 recentMessageCount를 설정합니다.</p>
     *
     * @param infos memberId, memberName, rank, popularityScore가 채워진 PopularPaperInfo 리스트
     * @author Jaeik
     * @since 2.0.0
     */
    public void enrichPopularPaperInfos(List<PopularPaperInfo> infos) {
        if (infos == null || infos.isEmpty()) {
            return;
        }

        // 1. infos에서 memberIds 추출
        List<Long> memberIds = infos.stream()
                .map(PopularPaperInfo::getMemberId)
                .collect(Collectors.toList());

        // 2. 24시간 이내 메시지 수 조회
        Instant twentyFourHoursAgo = Instant.now().minus(24, ChronoUnit.HOURS);

        List<Tuple> results = jpaQueryFactory
                .select(message.member.id, message.count())
                .from(message)
                .where(
                        message.member.id.in(memberIds),
                        message.createdAt.goe(twentyFourHoursAgo)
                )
                .groupBy(message.member.id)
                .fetch();

        // 3. Map<Long, Integer> 형태로 변환
        Map<Long, Integer> messageCountMap = results.stream()
                .collect(Collectors.toMap(
                        tuple -> tuple.get(message.member.id),
                        tuple -> tuple.get(message.count()).intValue(),
                        (existing, replacement) -> existing
                ));

        // 4. infos를 순회하며 recentMessageCount 설정
        infos.forEach(info ->
                info.setRecentMessageCount(messageCountMap.getOrDefault(info.getMemberId(), 0))
        );
    }
}