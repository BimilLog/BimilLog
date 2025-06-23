import { Badge } from "@/components/ui/badge";
import { Button } from "@/components/ui/button";
import { Avatar, AvatarFallback } from "@/components/ui/avatar";
import { CardHeader, CardTitle } from "@/components/ui/card";
import { Eye, ThumbsUp, MessageSquare, Lock, Edit, Trash2 } from "lucide-react";
import { Post } from "@/lib/api";
import Link from "next/link";

interface PostHeaderProps {
  post: Post;
  commentCount: number;
  canModify: () => boolean;
  onDeleteClick: () => void;
}

export const PostHeader: React.FC<PostHeaderProps> = ({
  post,
  commentCount,
  canModify,
  onDeleteClick,
}) => {
  return (
    <CardHeader className="border-b">
      <div className="flex items-start justify-between">
        <div className="flex-1">
          <div className="flex items-center space-x-2 mb-2">
            {post.password && <Lock className="w-4 h-4 text-red-500" />}
            {post.notice && (
              <Badge className="bg-red-500 text-white">공지</Badge>
            )}
            {post.popularFlag && (
              <Badge className="bg-orange-500 text-white">
                {post.popularFlag === "REALTIME"
                  ? "실시간"
                  : post.popularFlag === "WEEKLY"
                  ? "주간"
                  : "레전드"}
              </Badge>
            )}
          </div>
          <CardTitle className="text-2xl font-bold text-gray-800 mb-3">
            {post.title}
          </CardTitle>
          <div className="flex items-center space-x-4 text-sm text-gray-600">
            <div className="flex items-center space-x-2">
              <Avatar className="w-6 h-6">
                <AvatarFallback className="bg-gradient-to-r from-pink-500 to-purple-600 text-white text-xs">
                  {post.userName?.charAt(0) || "?"}
                </AvatarFallback>
              </Avatar>
              <span>{post.userName}</span>
            </div>
            <span>{post.createdAt}</span>
            <div className="flex items-center space-x-1">
              <Eye className="w-4 h-4" />
              <span>{post.views}</span>
            </div>
            <div className="flex items-center space-x-1">
              <ThumbsUp className="w-4 h-4" />
              <span>{post.likes}</span>
            </div>
            <div className="flex items-center space-x-1">
              <MessageSquare className="w-4 h-4" />
              <span>{commentCount}</span>
            </div>
          </div>
        </div>
        {canModify() && (
          <div className="flex space-x-2">
            <Link href={`/board/post/${post.postId}/edit`}>
              <Button size="sm" variant="outline">
                <Edit className="w-4 h-4 mr-1" />
                수정
              </Button>
            </Link>
            <Button size="sm" variant="destructive" onClick={onDeleteClick}>
              <Trash2 className="w-4 h-4 mr-1" />
              삭제
            </Button>
          </div>
        )}
      </div>
    </CardHeader>
  );
};
