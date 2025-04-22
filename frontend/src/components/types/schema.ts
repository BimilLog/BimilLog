
export interface UserDTO {
    userId: number;
    kakaoId: number;
    tokenId: number;
    kakaoNickname: string;
    thumbnailImage: string;
    farmName: string;
    role: UserRole;
}

export enum UserRole {
    USER = "USER",
    ADMIN = "ADMIN"
}

export interface TokenDTO {
    kakaoAccessToken: string;
    kakaoRefreshToken: string;
    jwtAccessToken: string;
    jwtRefreshToken: string;
}

export interface KakaoInfoDTO {
    kakaoId: number;
    kakaoNickname: string;
    thumbnailImage: string;
}

export interface FarmNameRequestDTO {
    tokenId: number;
    farmName: string;
}


export interface CropDTO {
    id: number;
    farmName: string;
    cropType: CropType;
    nickname: string;
    message: string;
    width: number;
    height: number;
}

export const enum CropType {
    POTATO = "POTATO",
    CARROT = "CARROT",
    CABBAGE = "CABBAGE",
    TOMATO = "TOMATO",
    STRAWBERRY = "STRAWBERRY",
    WATERMELON = "WATERMELON",
    PUMPKIN = "PUMPKIN",
    APPLE = "APPLE",
    GRAPE = "GRAPE",
    BANANA = "BANANA",
    GOBLIN = "GOBLIN",
    SLIME = "SLIME",
    ORC = "ORC",
    DRAGON = "DRAGON",
    PHOENIX = "PHOENIX",
    WEREWOLF = "WEREWOLF",
    ZOMBIE = "ZOMBIE",
    KRAKEN = "KRAKEN",
    CYCLOPS = "CYCLOPS"
}

export interface ReportDTO {
    reportId: number;
    reportType: ReportType;
    userId: number;
    targetId: number;
    content: string;
}

export enum ReportType{
    POST = "POST",
    COMMENT = "COMMENT",
    BUG = "BUG",
    IMPROVEMENT = "IMPROVEMENT",
}

export interface PostDTO {
    postId: number;
    userId: number;
    farmName: string;
    title: string;
    content: string;
    views: number;
    likes: number;
    is_notice: boolean;
    is_featured: boolean;
    createdAt: string;
    comments: CommentDTO[];
    userLike: boolean;
}

export interface SimplePostDTO {
    postId: number;
    userId: number;
    farmName: string;
    title: string;
    commentCount: number;
    likes: number;
    views: number;
    is_featured: boolean;
    is_notice: boolean;
    createdAt: string;
}

export interface PostReqDTO {
    userId: number;
    farmName: string;
    title: string;
    content: string;
}

export interface CommentDTO {
    id: number;
    postId: number;
    userId: number;
    farmName: string;
    content: string;
    likes: number;
    createdAt: string;
    is_featured: boolean;
    userLike: boolean;
}


