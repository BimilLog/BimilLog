import { Card, CardContent } from "@/components/ui/card";
import { Mail, FileText, MessageCircle, Heart, ThumbsUp } from "lucide-react";

interface UserStatsProps {
  stats: {
    totalMessages: number;
    totalPosts: number;
    totalComments: number;
    totalLikedPosts: number;
    totalLikedComments: number;
  };
}

export const UserStats: React.FC<UserStatsProps> = ({ stats }) => {
  return (
    <div className="grid grid-cols-2 md:grid-cols-5 gap-4 mb-8">
      <Card className="bg-white/80 backdrop-blur-sm border-0 shadow-lg">
        <CardContent className="p-4 text-center">
          <Mail className="w-8 h-8 text-pink-600 mx-auto mb-2" />
          <p className="text-2xl font-bold text-gray-800">
            {stats.totalMessages}
          </p>
          <p className="text-sm text-gray-600">받은 메시지</p>
        </CardContent>
      </Card>
      <Card className="bg-white/80 backdrop-blur-sm border-0 shadow-lg">
        <CardContent className="p-4 text-center">
          <FileText className="w-8 h-8 text-blue-600 mx-auto mb-2" />
          <p className="text-2xl font-bold text-gray-800">{stats.totalPosts}</p>
          <p className="text-sm text-gray-600">작성한 글</p>
        </CardContent>
      </Card>
      <Card className="bg-white/80 backdrop-blur-sm border-0 shadow-lg">
        <CardContent className="p-4 text-center">
          <MessageCircle className="w-8 h-8 text-green-600 mx-auto mb-2" />
          <p className="text-2xl font-bold text-gray-800">
            {stats.totalComments}
          </p>
          <p className="text-sm text-gray-600">작성한 댓글</p>
        </CardContent>
      </Card>
      <Card className="bg-white/80 backdrop-blur-sm border-0 shadow-lg">
        <CardContent className="p-4 text-center">
          <Heart className="w-8 h-8 text-red-600 mx-auto mb-2" />
          <p className="text-2xl font-bold text-gray-800">
            {stats.totalLikedPosts}
          </p>
          <p className="text-sm text-gray-600">추천한 글</p>
        </CardContent>
      </Card>
      <Card className="bg-white/80 backdrop-blur-sm border-0 shadow-lg">
        <CardContent className="p-4 text-center">
          <ThumbsUp className="w-8 h-8 text-purple-600 mx-auto mb-2" />
          <p className="text-2xl font-bold text-gray-800">
            {stats.totalLikedComments}
          </p>
          <p className="text-sm text-gray-600">추천한 댓글</p>
        </CardContent>
      </Card>
    </div>
  );
};
