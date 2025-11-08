import { defineConfig } from "vite";
import react from "@vitejs/plugin-react";
import path from "path";

// ✅ 현재 환경 확인 (배포 시 NODE_ENV=production)
const isLocal = process.env.NODE_ENV !== "production";

export default defineConfig({
    plugins: [react()],
    resolve: {
        alias: {
            "@": path.resolve(__dirname, "src"),
        },
    },

    assetsInclude: ["**/*.ttf"],

    // ✅ 로컬에서는 proxy 적용, EC2 배포 시에는 자동 비활성화
    ...(isLocal && {
        server: {
            proxy: {
                "/api": {
                    target: "http://localhost:8081",
                    changeOrigin: true,
                    secure: false,
                },
                "/oauth2": {
                    target: "http://localhost:8081",
                    changeOrigin: true,
                    secure: false,
                },
            },
        },
    }),

    define: {
        global: "window",
    },
});
