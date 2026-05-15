package com.iata.search.service;

import com.iata.search.config.AiConfig.DocumentChunk;
import jakarta.annotation.PostConstruct;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

@Service
public class RagService {

    private static final Logger log = LoggerFactory.getLogger(RagService.class);

    private final List<DocumentChunk> documentStore;
    private final String pdfPath;

    public RagService(List<DocumentChunk> documentStore,
                      @Value("${rag.pdf.path:guidance-document-on-baggage-standards-for-interline.pdf}") String pdfPath) {
        this.documentStore = documentStore;
        this.pdfPath = pdfPath;
    }

    @PostConstruct
    public void loadDocument() {
        try {
            ClassPathResource resource = new ClassPathResource(pdfPath);
            if (!resource.exists()) {
                log.warn("PDF not found at classpath:{}. Skipping document ingestion.", pdfPath);
                return;
            }
            try (InputStream is = resource.getInputStream();
                 PDDocument doc = Loader.loadPDF(is.readAllBytes())) {
                PDFTextStripper stripper = new PDFTextStripper();
                String text = stripper.getText(doc);
                chunkAndStore(text);
                log.info("Ingested {} chunks from {}", documentStore.size(), pdfPath);
            }
        } catch (Exception e) {
            log.error("Failed to load PDF document: {}", pdfPath, e);
        }
    }

    private void chunkAndStore(String text) {
        int chunkSize = 500;
        int overlap = 50;
        int start = 0;
        int index = 0;
        while (start < text.length()) {
            int end = Math.min(start + chunkSize, text.length());
            String chunkText = text.substring(start, end);
            documentStore.add(new DocumentChunk(
                    "chunk-" + index,
                    chunkText,
                    "Section " + (index + 1),
                    new float[0]
            ));
            index++;
            start += chunkSize - overlap;
        }
    }

    public record ScoredChunk(String text, String sectionRef, double score) {}

    public List<ScoredChunk> retrieveRelevantChunks(String query, int topK) {
        if (documentStore.isEmpty()) {
            return List.of();
        }
        String queryLower = query.toLowerCase(Locale.ROOT);
        String[] queryTerms = queryLower.split("\\W+");

        List<ScoredChunk> scored = new ArrayList<>();
        for (DocumentChunk chunk : documentStore) {
            double score = computeRelevance(chunk.text().toLowerCase(Locale.ROOT), queryTerms);
            if (score > 0) {
                scored.add(new ScoredChunk(chunk.text(), chunk.sectionRef(), score));
            }
        }
        scored.sort(Comparator.comparingDouble(ScoredChunk::score).reversed());
        return scored.size() > topK ? scored.subList(0, topK) : scored;
    }

    private double computeRelevance(String chunkText, String[] queryTerms) {
        int matchCount = 0;
        for (String term : queryTerms) {
            if (term.length() < 3) continue;
            if (chunkText.contains(term)) {
                matchCount++;
            }
        }
        if (matchCount == 0) return 0;
        double termScore = (double) matchCount / queryTerms.length;
        double density = (double) matchCount / Math.max(1, chunkText.split("\\W+").length);
        return termScore + density;
    }
}
