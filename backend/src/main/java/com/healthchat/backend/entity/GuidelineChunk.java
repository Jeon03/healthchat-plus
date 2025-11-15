package com.healthchat.backend.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Getter @Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Table(name = "guideline_chunks")
public class GuidelineChunk {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** KDRI / WHO 등 문서명 */
    @Column(nullable = false)
    private String source; // 예: "kdr-2020", "who-physical-activity"

    /** PDF 내 청크 index */
    private int chunkIndex;

    /** 청크 텍스트 본문 */
    @Lob
    @Column(nullable = false, columnDefinition = "LONGTEXT")
    private String text;

    /** 임베딩 벡터 (byte[]) */
    @Lob
    @Column(nullable = false, columnDefinition = "LONGBLOB")
    private byte[] embedding;
}
