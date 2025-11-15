package com.healthchat.backend.service.rag;

import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
public class ChunkSplitter {

    // 기본 청크 크기 (대략 350~600 tokens 정도를 가정)
    private static final int MAX_CHUNK_SIZE = 1200;    // 1200~1500자 정도
    private static final int MIN_CHUNK_SIZE = 600;
    private static final int OVERLAP_SIZE = 200;       // overlap 15~20%

    /**
     * 텍스트 → 문장 단위 → 의미 단위 청크로 나누기
     */
    public List<String> split(String text) {
        List<String> chunks = new ArrayList<>();

        // 1) 기본 정제
        text = clean(text);

        // 2) 문장 단위 분할
        String[] sentences = text.split("(?<=[.!?])\\s+|(?<=[.])[ ]+|(?<=\\n)");

        StringBuilder buffer = new StringBuilder();

        for (String sentence : sentences) {

            // 문장이 너무 길거나 공백이면 무시
            if (sentence.trim().isEmpty()) continue;

            // 현재 문장을 추가하면 청크 길이가 너무 커지면 새로운 청크 생성
            if (buffer.length() + sentence.length() > MAX_CHUNK_SIZE) {

                String chunk = buffer.toString().trim();
                if (chunk.length() > MIN_CHUNK_SIZE) {
                    chunks.add(chunk);

                    // Overlap 적용
                    buffer = new StringBuilder(
                            chunk.substring(
                                    Math.max(0, chunk.length() - OVERLAP_SIZE)
                            )
                    );
                } else {
                    // MIN 사이즈보다 작은 경우 이어붙인다
                    // (문장 단위 끊김을 최소화)
                }
            }

            buffer.append(" ").append(sentence);
        }

        // 마지막 청크 추가
        if (!buffer.isEmpty()) {
            chunks.add(buffer.toString().trim());
        }

        return chunks;
    }


    /**
     * 기본 cleaning (필요한 부분만 적용)
     */
    private String clean(String text) {
        if (text == null) return "";

        text = text.replaceAll("\r", "\n");
        text = text.replaceAll("\n{3,}", "\n\n");
        text = text.replaceAll("[ ]{2,}", " ");

        return text.trim();
    }
}
