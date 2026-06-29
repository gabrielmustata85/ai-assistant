package com.ai.assistant.knowledge;

import com.ai.assistant.service.DocumentIngestionService;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

@Service
public class KnowledgeService {

    private final DocumentIngestionService ingestionService;
    private final KnowledgeDocumentRepository repository;

    public KnowledgeService(DocumentIngestionService ingestionService,
                            KnowledgeDocumentRepository repository) {
        this.ingestionService = ingestionService;
        this.repository = repository;
    }

    public KnowledgeDocument ingest(MultipartFile file) throws IOException {
        ingestionService.ingestLegislationPdf(file);
        KnowledgeDocument doc = new KnowledgeDocument();
        doc.setTitle(file.getOriginalFilename());
        doc.setSource("upload");
        doc.setNamespace("legislation");
        return repository.save(doc);
    }

    public List<KnowledgeDocument> list() {
        return repository.findAll();
    }
}
