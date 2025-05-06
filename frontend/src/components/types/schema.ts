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

export interface KakaoFriendDTO {
    id: number;
    uuid: string;
    profileNickname: string;
    profileThumbnailImage: string;
}

export interface KakaoFriendListDTO {
    elements: KakaoFriendDTO[];
    totalCount: number;
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
    is_RealtimePopular: boolean;
    is_WeeklyPopular: boolean;
    is_HallOfFame: boolean;
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
    is_notice: boolean;
    is_RealtimePopular: boolean;
    is_WeeklyPopular: boolean;
    is_HallOfFame: boolean;
    createdAt: string;
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

export enum NotificationType {
    ADMIN = "ADMIN",
    FARM = "FARM",
    COMMENT = "COMMENT",
    POST_FEATURED = "POST_FEATURED",
    COMMENT_FEATURED = "COMMENT_FEATURED",
    INITIATE = "INITIATE"
}

export interface NotificationDTO {
    id: number;
    data: string;
    url: string;
    type: NotificationType;
    isRead: boolean;
    createdAt: string;
}

export interface UpdateNotificationDTO {
    readIds: number[];
    deletedIds: number[];
}

export enum DeviceType {
    MOBILE = "MOBILE",
    TABLET = "TABLET",
}

