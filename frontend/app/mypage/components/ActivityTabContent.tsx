import { useState, useEffect } from "react";
import { Card, CardContent } from "@/components/ui/card";
import Link from "next/link";

interface ActivityTabContentProps {
  fetchData: () => Promise<any[]>;
}

export const ActivityTabContent: React.FC<ActivityTabContentProps> = ({
  fetchData,
}) => {
  const [items, setItems] = useState<any[]>([]);
  const [isLoading, setIsLoading] = useState(true);

  useEffect(() => {
    fetchData()
      .then((data) => setItems(data))
      .catch((err) => console.error("Failed to fetch activity data:", err))
      .finally(() => setIsLoading(false));
  }, [fetchData]);

  if (isLoading)
    return <div className="p-4 text-center">데이터를 불러오는 중...</div>;
  if (items.length === 0)
    return <div className="p-4 text-center">활동 내역이 없습니다.</div>;

  return (
    <div className="space-y-4 mt-4">
      {items.map((item) => (
        <Card
          key={item.id || item.postId}
          className="bg-white/80 backdrop-blur-sm"
        >
          <CardContent className="p-4">
            <Link href={`/board/post/${item.postId || item.id}`}>
              <div className="hover:text-purple-600 transition-colors">
                {item.title && <p className="font-semibold">{item.title}</p>}
                {item.content && (
                  <p className="text-gray-600">"{item.content}"</p>
                )}
                <p className="text-sm text-gray-400 mt-2">
                  {new Date(item.createdAt).toLocaleDateString()}
                </p>
              </div>
            </Link>
          </CardContent>
        </Card>
      ))}
    </div>
  );
};
