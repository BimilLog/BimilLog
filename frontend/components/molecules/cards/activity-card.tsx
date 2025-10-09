import React from "react";
import Link from "next/link";
import { Card, CardContent, CardHeader } from "@/components";
import { Badge, TimeBadge } from "@/components";
import {
  Heart,
  MessageCircle,
  Eye,
  ThumbsUp,
  FileText,
  User,
  Calendar
} from "lucide-react";
import { SimplePost, SimpleComment } from "@/lib/api";
import { formatKoreanDate } from "@/lib/utils/date";
import { formatNumber } from "@/lib/utils";

interface ActivityCardProps {
  item: SimplePost | SimpleComment;
  type: "post" | "comment";
  isLiked?: boolean;
  className?: string;
}


const PostCard: React.FC<{ post: SimplePost; isLiked: boolean }> = React.memo(({ post, isLiked }) => (
  <Card variant="elevated" interactive={true} className="group hover:scale-[1.02]">
    <CardHeader className="pb-3">
      <div className="flex items-start justify-between">
        <div className="flex items-center space-x-3 flex-1">
          <div className="w-10 h-10 bg-gradient-to-r from-pink-500 to-purple-600 rounded-lg flex items-center justify-center">
            <FileText className="w-5 h-5 text-white" />
          </div>
          <div className="flex-1 min-w-0">
            <div className="flex items-center space-x-2 mb-1">
              <User className="w-4 h-4" />
              <span className="text-base md:text-sm font-medium text-brand-primary">
                {post.memberName}
              </span>
            </div>
            <TimeBadge dateString={post.createdAt} size="xs" />
          </div>
        </div>
        {isLiked && <Heart className="w-5 h-5 text-red-500 fill-current" />}
      </div>
    </CardHeader>

    <CardContent className="pt-0">
      <Link href={`/board/post/${post.id}`}>
        <div className="cursor-pointer group">
          <h3 className="font-bold text-lg text-brand-primary group-hover:text-purple-600 transition-colors mb-3 line-clamp-2">
            {post.title}
          </h3>

          <div className="grid grid-cols-2 gap-4 mt-4 p-3 bg-gray-50 rounded-lg">
            <div className="flex items-center space-x-2">
              <Eye className="w-4 h-4 text-blue-500" />
              <div className="text-base md:text-sm">
                <span className="text-brand-muted">조회수</span>
                <span className="font-semibold text-brand-primary ml-1">
                  {formatNumber(post.viewCount)}
                </span>
              </div>
            </div>

            <div className="flex items-center space-x-2">
              <Heart className="w-4 h-4 text-red-500" />
              <div className="text-base md:text-sm">
                <span className="text-brand-muted">추천</span>
                <span className="font-semibold text-brand-primary ml-1">
                  {formatNumber(post.likeCount)}
                </span>
              </div>
            </div>

            <div className="flex items-center space-x-2">
              <MessageCircle className="w-4 h-4 text-green-500" />
              <div className="text-base md:text-sm">
                <span className="text-brand-muted">댓글</span>
                <span className="font-semibold text-brand-primary ml-1">
                  {formatNumber(post.commentCount)}
                </span>
              </div>
            </div>

            <div className="flex items-center space-x-2">
              <Calendar className="w-4 h-4 text-purple-500" />
              <div className="text-base md:text-sm">
                <span className="text-brand-muted">작성일</span>
                <span className="font-semibold text-brand-primary ml-1">
                  {formatKoreanDate(post.createdAt)}
                </span>
              </div>
            </div>
          </div>
        </div>
      </Link>
    </CardContent>
  </Card>
));

PostCard.displayName = "PostCard";

const CommentCard: React.FC<{ comment: SimpleComment; isLiked: boolean }> = React.memo(({ comment, isLiked }) => (
  <Card variant="elevated" interactive={true} className="group hover:scale-[1.02]">
    <CardHeader className="pb-3">
      <div className="flex items-start justify-between">
        <div className="flex items-center space-x-3 flex-1">
          <div className="w-10 h-10 bg-gradient-to-r from-green-500 to-teal-600 rounded-lg flex items-center justify-center">
            <MessageCircle className="w-5 h-5 text-white" />
          </div>
          <div className="flex-1 min-w-0">
            <div className="flex items-center space-x-2 mb-1">
              <User className="w-4 h-4" />
              <span className="text-base md:text-sm font-medium text-brand-primary">
                {comment.memberName}
              </span>
            </div>
            <TimeBadge dateString={comment.createdAt} size="xs" />
          </div>
        </div>
        {isLiked && <ThumbsUp className="w-5 h-5 text-blue-500 fill-current" />}
      </div>
    </CardHeader>

    <CardContent className="pt-0">
      <Link href={`/board/post/${comment.postId}#comment-${comment.id}`}>
        <div className="cursor-pointer group">
          <div className="p-3 bg-gray-50 rounded-lg mb-3">
            <p className="text-brand-primary group-hover:text-purple-600 transition-colors line-clamp-3">
              {comment.content}
            </p>
          </div>

          <div className="flex items-center justify-between">
            <div className="flex items-center space-x-4">
              <div className="flex items-center space-x-2">
                <ThumbsUp className="w-4 h-4 text-blue-500" />
                <span className="text-base md:text-sm font-semibold text-brand-primary">
                  {formatNumber(comment.likeCount)}
                </span>
                <span className="text-sm md:text-xs text-brand-muted">추천</span>
              </div>

              {comment.userLike && !isLiked && (
                <Badge className="bg-blue-100 text-blue-700 border-blue-200 text-sm md:text-xs">
                  내가 추천한 댓글
                </Badge>
              )}
            </div>

            <div className="text-sm md:text-xs text-brand-secondary">
              게시글 #{comment.postId}
            </div>
          </div>
        </div>
      </Link>
    </CardContent>
  </Card>
));

CommentCard.displayName = "CommentCard";

export const ActivityCard: React.FC<ActivityCardProps> = ({
  item,
  type,
  isLiked = false,
  className
}) => {
  const cardClassName = className || "";

  if (type === "post") {
    return (
      <div className={cardClassName}>
        <PostCard post={item as SimplePost} isLiked={isLiked} />
      </div>
    );
  }

  return (
    <div className={cardClassName}>
      <CommentCard comment={item as SimpleComment} isLiked={isLiked} />
    </div>
  );
};