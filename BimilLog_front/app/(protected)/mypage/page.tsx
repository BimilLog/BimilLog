import { getMyPageInfoServer, getMyRollingPaperServer } from "@/lib/api/server";
import MyPageClient from "./MyPageClient";

export default async function MyPage() {
  const [mypageData, paperData] = await Promise.all([
    getMyPageInfoServer(0, 10),
    getMyRollingPaperServer(),
  ]);

  return (
    <MyPageClient
      initialMyPageData={mypageData?.data ?? null}
      initialPaperData={paperData?.data ?? null}
    />
  );
}
