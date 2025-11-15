package com.healthchat.backend.service.rag;

import com.healthchat.backend.config.GeminiClient;
import com.healthchat.backend.entity.GuidelineChunk;
import com.healthchat.backend.repository.GuidelineChunkRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class GuidelineImportService {

    private final PdfTextExtractor pdfExtractor;
    private final ChunkSplitter chunkSplitter;
    private final GeminiClient gemini;
    private final GuidelineChunkRepository repo;

    /**
     * PDF → 청크 → 임베딩 → DB 저장
     */
    public void importGuideline(String source, String pdfPath) {

        // 이미 import 완료된 source면 skip
        if (repo.existsBySource(source)) {
            log.info("📌 이미 임포트된 문서이므로 스킵: {}", source);
            return;
        }

        log.info("📥 PDF 불러오는 중: {}", pdfPath);
        String text = pdfExtractor.extractText(pdfPath);

        List<String> chunks = chunkSplitter.split(text);

        log.info("🧩 총 {}개 청크 생성됨", chunks.size());

        int idx = 0;

        for (String chunk : chunks) {
            float[] embedding = gemini.embed(chunk);

            GuidelineChunk entity = GuidelineChunk.builder()
                    .source(source)
                    .chunkIndex(idx)
                    .text(chunk)
                    .embedding(EmbeddingUtil.toBytes(embedding))
                    .build();

            repo.save(entity);

            if (idx % 10 == 0)
                log.info("  - 청크 {} 저장 완료", idx);

            idx++;
        }

        log.info("✅ {} 문서 DB 저장 완료!", source);
    }
}
