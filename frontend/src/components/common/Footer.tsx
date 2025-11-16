export default function Footer() {
    return (
        <footer
            id="app-footer"
            className="
                mt-20 py-10
                bg-gradient-to-b from-gray-100/50 to-gray-200/70
                dark:from-gray-900/40 dark:to-gray-900/70
                border-t border-gray-300/30 dark:border-gray-700/40
                backdrop-blur-xl
                transition-all duration-300
            "
        >
            <div className="max-w-5xl mx-auto px-6 text-center space-y-4">

                {/* 서비스 소개 */}
                <p className="text-[15px] text-gray-700 dark:text-gray-300 leading-relaxed">
                    <span className="font-bold text-blue-600 dark:text-blue-400">
                        HealthChat+
                    </span>{" "}
                    는 AI 기반으로 식단·운동·감정을 분석하여
                    일상을 더 건강하게 만드는 개인 맞춤 헬스 코칭 플랫폼입니다.
                </p>

                {/* 섬세한 구분선 */}
                <div className="h-px w-20 mx-auto bg-gray-300/60 dark:bg-gray-600/60 rounded"></div>

                {/* 저작권 */}
                <p className="text-sm text-gray-600 dark:text-gray-400 tracking-tight">
                    © {new Date().getFullYear()}{" "}
                    <span className="font-semibold text-blue-600 dark:text-blue-400">
                        HealthChat+
                    </span>{" "}
                    | Designed & Built with ❤ for a Healthier Life
                </p>

                {/* 추가 링크 (원하면 클릭 가능하게 변경 가능) */}
                <div className="flex justify-center gap-6 text-sm text-gray-500 dark:text-gray-400">
                    <span className="hover:text-blue-500 cursor-pointer">이용약관</span>
                    <span className="hover:text-blue-500 cursor-pointer">개인정보 처리방침</span>
                    <span className="hover:text-blue-500 cursor-pointer">문의하기</span>
                </div>
            </div>
        </footer>
    );
}
