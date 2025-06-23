import { Badge } from "@/components/ui/badge";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { Avatar, AvatarFallback } from "@/components/ui/avatar";
import { ThumbsUp } from "lucide-react";
import { Comment } from "@/lib/api";

interface PopularCommentsProps {
  popularComments: Comment[];
  onCommentClick: (commentId: number) => void;
}

export const PopularComments: React.FC<PopularCommentsProps> = ({
  popularComments,
  onCommentClick,
}) => {
  if (popularComments.length === 0) {
    return null;
  }

  return (
    <Card className="bg-white/80 backdrop-blur-sm border-0 shadow-lg mb-4">
      <CardHeader>
        <CardTitle className="flex items-center space-x-2">
          <ThumbsUp className="w-5 h-5 text-orange-600" />
          <span>인기 댓글 {popularComments.length}개</span>
        </CardTitle>
      </CardHeader>
      <CardContent>
        <div className="space-y-4">
          {popularComments.map((comment) => (
            <div
              key={comment.id}
              className="p-4 bg-gradient-to-r from-orange-50 to-yellow-50 rounded-lg border-l-4 border-orange-400 cursor-pointer hover:bg-gradient-to-r hover:from-orange-100 hover:to-yellow-100 transition-colors"
              onClick={() => onCommentClick(comment.id)}
            >
              <div className="flex items-start justify-between mb-2">
                <div className="flex items-center space-x-2">
                  <Avatar className="w-8 h-8">
                    <AvatarFallback>
                      {comment.userName?.charAt(0) || "?"}
                    </AvatarFallback>
                  </Avatar>
                  <div>
                    <div className="flex items-center space-x-2">
                      <p className="font-semibold">{comment.userName}</p>
                      <Badge className="bg-orange-100 text-orange-800 text-xs">
                        인기
                      </Badge>
                    </div>
                    <p className="text-xs text-gray-500">
                      {new Date(comment.createdAt).toLocaleString()}
                    </p>
                  </div>
                </div>
                <div className="flex items-center space-x-1 text-orange-600">
                  <ThumbsUp className="w-4 h-4" />
                  <span className="text-sm font-medium">{comment.likes}</span>
                </div>
              </div>
              <div
                className="prose max-w-none prose-sm"
                dangerouslySetInnerHTML={{
                  __html: comment.content,
                }}
              />
              <div className="mt-2 text-xs text-orange-600 font-medium">
                클릭하여 원본 댓글로 이동 →
              </div>
            </div>
          ))}
        </div>
      </CardContent>
    </Card>
  );
};
