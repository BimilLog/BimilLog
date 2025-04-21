package jaeik.growfarm.entity.crop;

import static com.querydsl.core.types.PathMetadataFactory.*;

import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.PathMetadata;
import javax.annotation.processing.Generated;
import com.querydsl.core.types.Path;
import com.querydsl.core.types.dsl.PathInits;


/**
 * QCrop is a Querydsl query type for Crop
 */
@Generated("com.querydsl.codegen.DefaultEntitySerializer")
public class QCrop extends EntityPathBase<Crop> {

    private static final long serialVersionUID = -993699126L;

    private static final PathInits INITS = PathInits.DIRECT2;

    public static final QCrop crop = new QCrop("crop");

    public final jaeik.growfarm.repository.QBaseEntity _super = new jaeik.growfarm.repository.QBaseEntity(this);

    //inherited
    public final DateTimePath<java.time.LocalDateTime> createdAt = _super.createdAt;

    public final EnumPath<CropType> cropType = createEnum("cropType", CropType.class);

    public final NumberPath<Integer> height = createNumber("height", Integer.class);

    public final NumberPath<Long> id = createNumber("id", Long.class);

    public final StringPath message = createString("message");

    //inherited
    public final DateTimePath<java.time.LocalDateTime> modifiedAt = _super.modifiedAt;

    public final StringPath nickname = createString("nickname");

    public final jaeik.growfarm.entity.user.QUsers users;

    public final NumberPath<Integer> width = createNumber("width", Integer.class);

    public QCrop(String variable) {
        this(Crop.class, forVariable(variable), INITS);
    }

    public QCrop(Path<? extends Crop> path) {
        this(path.getType(), path.getMetadata(), PathInits.getFor(path.getMetadata(), INITS));
    }

    public QCrop(PathMetadata metadata) {
        this(metadata, PathInits.getFor(metadata, INITS));
    }

    public QCrop(PathMetadata metadata, PathInits inits) {
        this(Crop.class, metadata, inits);
    }

    public QCrop(Class<? extends Crop> type, PathMetadata metadata, PathInits inits) {
        super(type, metadata, inits);
        this.users = inits.isInitialized("users") ? new jaeik.growfarm.entity.user.QUsers(forProperty("users"), inits.get("users")) : null;
    }

}

