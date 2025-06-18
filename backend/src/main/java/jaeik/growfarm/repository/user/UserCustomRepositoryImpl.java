package jaeik.growfarm.repository.user;

import com.querydsl.core.Tuple;
import com.querydsl.jpa.impl.JPAQueryFactory;
import jaeik.growfarm.entity.user.QUsers;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * <h2>사용자 커스텀 저장소 구현 클래스</h2>
 * <p>
 * 사용자 관련 데이터베이스 작업을 수행하며 커스텀한 쿼리메소드가 포함되어 있습니다.
 * </p>
 *
 * @author Jaeik
 * @version 1.0.0
 */
@Repository
@RequiredArgsConstructor
public class UserCustomRepositoryImpl implements UserCustomRepository {

    private final JPAQueryFactory jpaQueryFactory;

    /**
     * <h3>카카오 ID 목록으로 농장 이름 조회</h3>
     * <p>
     * 카카오 ID 목록의 순서대로 농장 이름을 조회한다.
     * </p>
     *
     * @param ids 카카오 ID 목록
     * @return 농장 이름 리스트
     * @author Jaeik
     * @since 1.0.0
     */
    @Override
    public List<String> findFarmNamesInOrder(List<Long> ids) {
        if (ids.isEmpty()) {
            return Collections.emptyList();
        }

        QUsers user = QUsers.users;

        List<Tuple> results = jpaQueryFactory
                .select(user.kakaoId, user.userName)
                .from(user)
                .where(user.kakaoId.in(ids))
                .fetch();

        Map<Long, String> kakaoIdToFarmName = results.stream()
                .collect(Collectors.toMap(
                        tuple -> tuple.get(user.kakaoId),
                        tuple -> Optional.ofNullable(tuple.get(user.userName)).orElse(""),
                        (existing, replacement) -> existing // 중복 키 처리
                ));

        return ids.stream()
                .map(id -> kakaoIdToFarmName.getOrDefault(id, ""))
                .collect(Collectors.toList());
    }
}
