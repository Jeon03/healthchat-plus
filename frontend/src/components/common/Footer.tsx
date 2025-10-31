export default function Footer() {
    return (
        <footer
            className="
                bg-gray-100 text-gray-600
                dark:bg-gray-900 dark:text-gray-400
                py-6 mt-16 border-t border-gray-200 dark:border-gray-700
                transition-colors duration-300
            "
        >
            <div className="max-w-6xl mx-auto text-center text-sm">
                © {new Date().getFullYear()}{" "}
                <span className="font-semibold text-blue-600 dark:text-blue-400">
                    HealthChat+
                </span>{" "}
                All rights reserved.
            </div>
        </footer>
    );
}
