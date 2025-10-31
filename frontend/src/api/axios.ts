import axios from "axios";

const api = axios.create({
    baseURL: "/api",
    withCredentials: true,
});

// ✅ 401 처리: Access Token 만료 시 refresh 시도
api.interceptors.response.use(
    (res) => res,
    async (err) => {
        const original = err.config;
        if (err.response?.status === 401 && !original._retry) {
            original._retry = true;
            try {
                await api.post("/auth/refresh"); // 백엔드에서 새 access_token 쿠키 발급
                return api(original); // 요청 재시도
            } catch {
                console.warn("토큰 갱신 실패 — 로그인 필요");
                window.dispatchEvent(new Event("auth-logout"));
            }
        }
        return Promise.reject(err);
    }
);

export default api;