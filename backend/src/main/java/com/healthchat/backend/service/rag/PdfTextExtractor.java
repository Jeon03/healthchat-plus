package com.healthchat.backend.service.rag;

import lombok.extern.slf4j.Slf4j;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import java.io.File;

@Slf4j
@Service
public class PdfTextExtractor {

    /**
     * PDF 파일 → 텍스트(문자열) 전체 추출
     */
    public String extractText(String classpathPdfPath) {
        try {
            // resources/** 에 있는 PDF 파일 로드
            ClassPathResource resource = new ClassPathResource(classpathPdfPath);
            File pdfFile = resource.getFile();

            try (PDDocument document = PDDocument.load(pdfFile)) {
                PDFTextStripper stripper = new PDFTextStripper();

                // 줄바꿈 유지 + 페이지 순서대로 추출
                stripper.setSortByPosition(true);
                stripper.setStartPage(1);
                stripper.setEndPage(document.getNumberOfPages());

                String raw = stripper.getText(document);

                // 텍스트 정리
                return cleanText(raw);
            }

        } catch (Exception e) {
            log.error("❌ PDF 텍스트 추출 실패: {}", classpathPdfPath, e);
            throw new RuntimeException("PDF 추출 실패: " + classpathPdfPath);
        }
    }

    /**
     * 텍스트 클리닝
     * - 과한 공백 제거
     * - 줄바꿈 정리
     */
    private String cleanText(String text) {
        if (text == null) return "";

        // 여러 줄로 나눠있는 경우 통합
        text = text.replaceAll("\r", "\n");

        // 3개 이상 연속 줄바꿈 → 2줄바꿈으로 통일
        text = text.replaceAll("\n{3,}", "\n\n");

        // 앞뒤 공백 제거
        text = text.trim();

        return text;
    }
}
