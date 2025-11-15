package com.healthchat.backend;

import com.healthchat.backend.service.rag.GuidelineImportService;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class GuidelineImportRunner implements CommandLineRunner {

    private final GuidelineImportService importer;

    @Override
    public void run(String... args) throws Exception {

        // 최초 한 번 import
        importer.importGuideline("kdr-2020", "guidelines/kdri-2020.pdf");
        importer.importGuideline("korean-guidelines", "guidelines/korean-dietary-guidelines.pdf");
        importer.importGuideline("who-obesity", "guidelines/who-obesity-overweight.pdf");
        importer.importGuideline("who-activity", "guidelines/who-physical-activity.pdf");
        importer.importGuideline("who-stress", "guidelines/who-stress-management.pdf");
    }
}
