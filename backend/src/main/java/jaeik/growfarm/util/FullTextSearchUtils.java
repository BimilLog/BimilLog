package jaeik.growfarm.util;

import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.core.types.dsl.Expressions;
import com.querydsl.core.types.dsl.StringPath;
import org.springframework.stereotype.Component;

/**
 * <h2>MySQL 전문검색 유틸리티</h2>
 * <p>
 * MySQL MATCH AGAINST 쿼리를 생성하는 유틸리티 클래스
 * </p>
 * 
 * @author Jaeik
 * @version 1.0
 */
@Component
public class FullTextSearchUtils {

    /**
     * <h3>제목과 내용 모두 전문검색</h3>
     *
     * @param titlePath   제목 필드
     * @param contentPath 내용 필드
     * @param keyword     검색어
     * @return BooleanExpression 전문검색 조건
     * @since 1.0.0
     * @author Jaeik
     */
    public static BooleanExpression matchAgainst(StringPath titlePath, StringPath contentPath, String keyword) {
        if (keyword == null || keyword.trim().isEmpty()) {
            return null;
        }

        String processedKeyword = preprocessKeyword(keyword);

        return Expressions.booleanTemplate(
                "MATCH({0}, {1}) AGAINST ({2} IN BOOLEAN MODE)",
                titlePath,
                contentPath,
                "+" + processedKeyword + "*");
    }

    /**
     * <h3>제목 전문검색</h3>
     *
     * @param titlePath 제목 필드
     * @param keyword   검색어
     * @return BooleanExpression 전문검색 조건
     * @since 1.0.0
     * @author Jaeik
     */
    public static BooleanExpression matchTitle(StringPath titlePath, String keyword) {
        if (keyword == null || keyword.trim().isEmpty()) {
            return null;
        }

        String processedKeyword = preprocessKeyword(keyword);

        return Expressions.booleanTemplate(
                "MATCH({0}) AGAINST ({1} IN BOOLEAN MODE)",
                titlePath,
                "+" + processedKeyword + "*");
    }

    /**
     * <h3>작성자 검색</h3>
     * <p>
     * 검색어 4자 이상 : LIKE '검색어%'로 인덱스를 사용한다.
     * </p>
     * <p>
     * 검색어 4자 미만 : LIKE '%검색어%'로 인덱스를 사용하지 않는다.
     * </p>
     *
     * @param usernamePath 사용자명 필드
     * @param keyword      검색어
     * @return BooleanExpression 최적화된 검색 조건
     * @since 1.0.0
     * @author Jaeik
     */
    public static BooleanExpression optimizedUsernameSearch(StringPath usernamePath, String keyword) {
        if (keyword == null || keyword.trim().isEmpty()) {
            return null;
        }

        String trimmedKeyword = keyword.trim();

        if (trimmedKeyword.length() >= 4) {
            return usernamePath.startsWithIgnoreCase(trimmedKeyword);
        }

        return usernamePath.containsIgnoreCase(trimmedKeyword);
    }

    /**
     * <h3>검색어 전처리</h3>
     * <p>
     * 검색어에서 특수문자를 제거하고 공백을 정리한다.
     * </p>
     *
     * @param keyword 검색어
     * @return 전처리된 검색어
     * @since 1.0.0
     * @author Jaeik
     */
    private static String preprocessKeyword(String keyword) {
        return keyword.trim()
                .replaceAll("[+\\-><()~*\"@]", "")
                .replaceAll("\\s+", " ");
    }
}