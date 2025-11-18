import { motion, AnimatePresence } from "framer-motion";

interface Props {
    open: boolean;
    onCancel: () => void;
    onConfirm: () => void;
    message: string;
}

export default function DeleteConfirmModal({ open, onCancel, onConfirm, message }: Props) {
    return (
        <AnimatePresence>
            {open && (
                <motion.div
                    className="fixed inset-0 z-50 flex items-center justify-center bg-black/40 backdrop-blur-sm"
                    initial={{ opacity: 0 }}
                    animate={{ opacity: 1 }}
                    exit={{ opacity: 0 }}
                >
                    {/* Modal Box */}
                    <motion.div
                        initial={{ opacity: 0, scale: 0.85 }}
                        animate={{ opacity: 1, scale: 1 }}
                        exit={{ opacity: 0, scale: 0.85 }}
                        transition={{ duration: 0.18 }}
                        className="
                            bg-white dark:bg-gray-800
                            rounded-2xl shadow-xl p-6 w-[90%] max-w-md
                            border border-gray-200 dark:border-gray-700
                        "
                    >
                        <h2 className="text-lg font-semibold text-gray-900 dark:text-gray-100 mb-2">
                            정말 삭제할까요?
                        </h2>

                        <p className="text-gray-600 dark:text-gray-300 mb-6 leading-relaxed">
                            {message}
                        </p>

                        <div className="flex justify-end gap-3">
                            <button
                                onClick={onCancel}
                                className="
                                    px-4 py-2 rounded-lg
                                    bg-gray-200 hover:bg-gray-300
                                    dark:bg-gray-700 dark:hover:bg-gray-600
                                    text-gray-800 dark:text-gray-200
                                    transition-all
                                "
                            >
                                취소
                            </button>

                            <button
                                onClick={onConfirm}
                                className="
                                    px-4 py-2 rounded-lg
                                    bg-red-500 hover:bg-red-600 text-white
                                    shadow-md shadow-red-500/30
                                    transition-all
                                "
                            >
                                삭제하기
                            </button>
                        </div>
                    </motion.div>
                </motion.div>
            )}
        </AnimatePresence>
    );
}
