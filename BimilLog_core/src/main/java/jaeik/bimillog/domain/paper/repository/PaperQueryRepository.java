package jaeik.bimillog.domain.paper.repository;

import com.querydsl.core.Tuple;
import com.querydsl.jpa.impl.JPAQueryFactory;
import jaeik.bimillog.domain.paper.entity.Message;

import jaeik.bimillog.domain.paper.entity.QMessage;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.time.Instant;

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

    /**
     * <h3>회원ID로 메시지 조회</h3>
     * @param memberId 회원 id
     * @return 메시지 리스트
     */
    public List<Message> getMessageList(Long memberId) {
        return jpaQueryFactory.select(message)
                .from(message)
                .where(message.member.id.eq(memberId))
                .orderBy(message.createdAt.desc())
                .fetch();
    }

    /**
     * <h3>회원별 최근 메시지 수 조회</h3>
     * <p>지정된 시간 이후에 작성된 메시지 수를 회원별로 집계하여 반환합니다.</p>
     *
     * @param memberIds 조회 대상 회원 ID 목록
     * @param since 이 시점 이후의 메시지만 카운트
     * @return Map&lt;memberId, messageCount&gt;
     */
    public Map<Long, Integer> enrichPopularPaperInfos(List<Long> memberIds, Instant twentyFourHoursAgo) {

        List<Tuple> results = jpaQueryFactory
                .select(message.member.id, message.count())
                .from(message)
                .where(
                        message.member.id.in(memberIds),
                        message.createdAt.goe(twentyFourHoursAgo)
                )
                .groupBy(message.member.id)
                .fetch();

        return results.stream()
                .collect(Collectors.toMap(
                        tuple -> tuple.get(message.member.id),
                        tuple -> tuple.get(message.count()).intValue(),
                        (existing, replacement) -> existing
                ));
    }
}