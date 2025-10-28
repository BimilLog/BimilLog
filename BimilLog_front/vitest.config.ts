import { defineConfig } from "vitest/config";
import path from "path";

export default defineConfig({
  test: {
    environment: "jsdom",
    setupFiles: ["./vitest.setup.ts"],
    globals: true,
    include: ["tests/**/*.test.{ts,tsx}", "tests/**/*.spec.{ts,tsx}"],
    css: true,
  },
  resolve: {
    alias: {
      "@": path.resolve(__dirname, "."),
      "@/app": path.resolve(__dirname, "./app"),
      "@/components": path.resolve(__dirname, "./components"),
      "@/hooks": path.resolve(__dirname, "./hooks"),
      "@/lib": path.resolve(__dirname, "./lib"),
      "@/providers": path.resolve(__dirname, "./providers"),
      "@/stores": path.resolve(__dirname, "./stores"),
      "@/types": path.resolve(__dirname, "./types"),
    },
  },
});
