package jaeik.growfarm.entity.user;

import static com.querydsl.core.types.PathMetadataFactory.*;

import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.PathMetadata;
import javax.annotation.processing.Generated;
import com.querydsl.core.types.Path;


/**
 * QBlackList is a Querydsl query type for BlackList
 */
@Generated("com.querydsl.codegen.DefaultEntitySerializer")
public class QBlackList extends EntityPathBase<BlackList> {

    private static final long serialVersionUID = 266351806L;

    public static final QBlackList blackList = new QBlackList("blackList");

    public final jaeik.growfarm.repository.QBaseEntity _super = new jaeik.growfarm.repository.QBaseEntity(this);

    //inherited
    public final DateTimePath<java.time.LocalDateTime> createdAt = _super.createdAt;

    public final NumberPath<Long> id = createNumber("id", Long.class);

    public final NumberPath<Long> kakaoId = createNumber("kakaoId", Long.class);

    //inherited
    public final DateTimePath<java.time.LocalDateTime> modifiedAt = _super.modifiedAt;

    public QBlackList(String variable) {
        super(BlackList.class, forVariable(variable));
    }

    public QBlackList(Path<? extends BlackList> path) {
        super(path.getType(), path.getMetadata());
    }

    public QBlackList(PathMetadata metadata) {
        super(BlackList.class, metadata);
    }

}

