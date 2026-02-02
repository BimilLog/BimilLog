import { getUserSettingsServer } from "@/lib/api/server";
import SettingsClient from "./SettingsClient";

export default async function SettingsPage() {
  const settingsData = await getUserSettingsServer();

  return <SettingsClient initialSettings={settingsData?.data ?? null} />;
}
