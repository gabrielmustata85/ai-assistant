package com.ai.assistant.service;

import com.ai.assistant.ai.EmbeddingClient;
import com.ai.assistant.client.EnhancedPineconeClient;
import com.ai.assistant.model.Vector;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.encryption.InvalidPasswordException;
import org.apache.pdfbox.text.PDFTextStripper;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class DocumentIngestionService {

    private static final Logger log = LoggerFactory.getLogger(DocumentIngestionService.class);

    private static final int BYTES_PER_MB = 1024 * 1024;
    private static final int TEXT_PREVIEW_LENGTH = 200;

    private final EnhancedPineconeClient pineconeClient;
    private final EmbeddingClient embeddingClient;
    private final int maxPdfSizeMb;
    private final int maxChunkTokens;

    @Autowired
    public DocumentIngestionService(
            EnhancedPineconeClient pineconeClient,
            EmbeddingClient embeddingClient,
            @Value("${app.pdf.max-size-mb:20}") int maxPdfSizeMb,
            @Value("${app.chunk.max-tokens:5000}") int maxChunkTokens) {
        this.pineconeClient = pineconeClient;
        this.embeddingClient = embeddingClient;
        this.maxPdfSizeMb = maxPdfSizeMb;
        this.maxChunkTokens = maxChunkTokens;
        log.info("Initialized Document processor with max {}MB files and {} tokens/chunk",
                maxPdfSizeMb, maxChunkTokens);
    }

    /**
     * Ingest PDF document
     */
    @Async
    public CompletableFuture<Void> ingestPdfDocument(MultipartFile file) throws IOException {
        try {
            // 1. Validate file first
            validatePdfFile(file);

            // 2. Extract text
            String text = extractTextFromPDF(file);
            log.debug("Extracted {} characters from {}", text.length(), file.getOriginalFilename());

            // 3. Process chunks
            List<String> chunks = chunkTextIntelligently(text);
            if (chunks.isEmpty()) {
                throw new IOException("No valid text chunks could be extracted");
            }

            // 4. Process each chunk
            for (int i = 0; i < chunks.size(); i++) {
                processPdfChunk(file, chunks.get(i), i, chunks.size());
            }

            return CompletableFuture.completedFuture(null);
        } catch (Exception e) {
            log.error("Failed to process PDF {}: {}", file.getOriginalFilename(), e.getMessage());
            throw new IOException("PDF processing failed: " + e.getMessage(), e);
        }
    }

    /**
     * Ingest SQL file
     */
    @Async
    public CompletableFuture<Void> ingestSqlFile(MultipartFile file) throws IOException {
        try {
            // 1. Validate file
            validateSqlFile(file);

            // 2. Extract SQL content
            String sqlContent = extractTextFromSqlFile(file);
            log.debug("Extracted {} characters from SQL file {}", sqlContent.length(), file.getOriginalFilename());

            // 3. Parse SQL statements
            List<SqlStatement> sqlStatements = parseSqlStatements(sqlContent);
            if (sqlStatements.isEmpty()) {
                throw new IOException("No valid SQL statements found");
            }

            // 4. Process each SQL statement
            for (int i = 0; i < sqlStatements.size(); i++) {
                processSqlStatement(file, sqlStatements.get(i), i, sqlStatements.size());
            }

            return CompletableFuture.completedFuture(null);
        } catch (Exception e) {
            log.error("Failed to process SQL file {}: {}", file.getOriginalFilename(), e.getMessage());
            throw new IOException("SQL file processing failed: " + e.getMessage(), e);
        }
    }

    private void validatePdfFile(MultipartFile file) throws IOException {
        if (file.isEmpty()) {
            throw new IOException("File is empty");
        }

        if (!"application/pdf".equals(file.getContentType())) {
            throw new IOException("Only PDF files are supported");
        }

        if (file.getSize() > maxPdfSizeMb * BYTES_PER_MB) {
            throw new IOException(String.format(
                    "File size exceeds %dMB limit (actual: %.2fMB)",
                    maxPdfSizeMb,
                    (double) file.getSize() / BYTES_PER_MB
            ));
        }
    }

    private void validateSqlFile(MultipartFile file) throws IOException {
        if (file.isEmpty()) {
            throw new IOException("File is empty");
        }

        String filename = file.getOriginalFilename();
        if (filename == null || !filename.toLowerCase().endsWith(".sql")) {
            throw new IOException("Only SQL files are supported");
        }

        if (file.getSize() > 10 * BYTES_PER_MB) {
            throw new IOException("SQL file size exceeds 10MB limit");
        }
    }

    private String extractTextFromPDF(MultipartFile file) throws IOException {
        try (InputStream is = file.getInputStream();
             PDDocument document = PDDocument.load(is)) {

            if (document.isEncrypted()) {
                throw new IOException("Password-protected PDFs are not supported");
            }

            PDFTextStripper stripper = new PDFTextStripper();
            stripper.setSortByPosition(true);
            return stripper.getText(document);
        } catch (InvalidPasswordException e) {
            throw new IOException("Password-protected PDFs are not supported", e);
        }
    }

    private String extractTextFromSqlFile(MultipartFile file) throws IOException {
        try (InputStream is = file.getInputStream();
             BufferedReader reader = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8))) {

            StringBuilder content = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                content.append(line).append("\n");
            }
            return content.toString();
        }
    }

    private List<String> chunkTextIntelligently(String text) {
        if (text == null || text.trim().isEmpty()) {
            return List.of();
        }

        List<String> chunks = new ArrayList<>();
        String[] paragraphs = text.split("(?<=\\n\\n)");
        StringBuilder currentChunk = new StringBuilder();

        for (String paragraph : paragraphs) {
            if (paragraph.trim().isEmpty()) {
                continue;
            }

            if (currentChunk.length() + paragraph.length() > estimateTokenSize(maxChunkTokens)) {
                String chunk = currentChunk.toString().trim();
                if (!chunk.isEmpty()) {
                    chunks.add(chunk);
                }
                currentChunk = new StringBuilder();
            }
            currentChunk.append(paragraph);
        }

        String lastChunk = currentChunk.toString().trim();
        if (!lastChunk.isEmpty()) {
            chunks.add(lastChunk);
        }

        return chunks;
    }

    private List<SqlStatement> parseSqlStatements(String sqlContent) {
        List<SqlStatement> statements = new ArrayList<>();

        // Pattern to match CREATE TABLE statements
        Pattern createTablePattern = Pattern.compile(
                "CREATE\\s+TABLE\\s+(?:IF\\s+NOT\\s+EXISTS\\s+)?([\\w.]+)\\s*\\((.*?)\\);",
                Pattern.CASE_INSENSITIVE | Pattern.DOTALL
        );

        Matcher matcher = createTablePattern.matcher(sqlContent);
        while (matcher.find()) {
            String tableName = matcher.group(1);
            String tableDefinition = matcher.group(0);
            statements.add(new SqlStatement(tableName, "CREATE_TABLE", tableDefinition));
        }

        // Pattern to match CREATE VIEW statements
        Pattern createViewPattern = Pattern.compile(
                "CREATE\\s+(?:OR\\s+REPLACE\\s+)?VIEW\\s+([\\w.]+)\\s+AS\\s+(.*?);",
                Pattern.CASE_INSENSITIVE | Pattern.DOTALL
        );

        matcher = createViewPattern.matcher(sqlContent);
        while (matcher.find()) {
            String viewName = matcher.group(1);
            String viewDefinition = matcher.group(0);
            statements.add(new SqlStatement(viewName, "CREATE_VIEW", viewDefinition));
        }

        return statements;
    }

    private void processPdfChunk(MultipartFile file, String chunk, int chunkIndex, int totalChunks) throws IOException {
        try {
            if (chunk == null || chunk.trim().isEmpty()) {
                log.warn("Skipping empty chunk {}/{} in {}", chunkIndex + 1, totalChunks, file.getOriginalFilename());
                return;
            }

            List<Float> embedding = embeddingClient.embed(chunk);
            if (embedding == null || embedding.isEmpty()) {
                throw new IOException("Failed to generate embedding for chunk");
            }

            JSONObject metadata = new JSONObject();
            metadata.put("documentId", file.getOriginalFilename());
            metadata.put("documentType", "PDF");
            metadata.put("chunkIndex", chunkIndex);
            metadata.put("totalChunks", totalChunks);
            metadata.put("textPreview", getTextPreview(chunk, TEXT_PREVIEW_LENGTH));
            metadata.put("chunkLength", chunk.length());
            metadata.put("uploadTimestamp", System.currentTimeMillis());

            Vector vector = new Vector(
                    generatePdfChunkId(file.getOriginalFilename(), chunkIndex),
                    embedding
            );

            pineconeClient.upsertPdfDocument(vector, metadata);
            log.debug("Processed PDF chunk {}/{} of {}", chunkIndex + 1, totalChunks, file.getOriginalFilename());
        } catch (Exception e) {
            throw new IOException(String.format(
                    "Failed to process PDF chunk %d/%d: %s",
                    chunkIndex + 1, totalChunks, e.getMessage()), e);
        }
    }

    private void processSqlStatement(MultipartFile file, SqlStatement statement, int index, int total) throws IOException {
        try {
            // Create rich text representation for embedding
            String embeddingText = String.format(
                    "SQL %s: %s\nDefinition: %s",
                    statement.getType(),
                    statement.getName(),
                    statement.getDefinition()
            );

            List<Float> embedding = embeddingClient.embed(embeddingText);
            if (embedding == null || embedding.isEmpty()) {
                throw new IOException("Failed to generate embedding for SQL statement");
            }

            JSONObject metadata = new JSONObject();
            metadata.put("fileName", file.getOriginalFilename());
            metadata.put("statementType", statement.getType());
            metadata.put("objectName", statement.getName());
            metadata.put("definition", statement.getDefinition());
            metadata.put("statementIndex", index);
            metadata.put("totalStatements", total);
            metadata.put("uploadTimestamp", System.currentTimeMillis());

            Vector vector = new Vector(
                    generateSqlStatementId(file.getOriginalFilename(), statement.getName(), index),
                    embedding
            );

            pineconeClient.upsertSqlSchema(vector, metadata);
            log.debug("Processed SQL statement {}/{}: {} {}",
                    index + 1, total, statement.getType(), statement.getName());
        } catch (Exception e) {
            throw new IOException(String.format(
                    "Failed to process SQL statement %d/%d: %s",
                    index + 1, total, e.getMessage()), e);
        }
    }

    /** Ingestă un PDF de legislație în namespace-ul de legislație (RAG). */
    public void ingestLegislationPdf(MultipartFile file) throws IOException {
        validatePdfFile(file);
        String text = extractTextFromPDF(file);
        List<String> chunks = chunkTextIntelligently(text);
        if (chunks.isEmpty()) {
            throw new IOException("No valid text chunks could be extracted");
        }
        for (int i = 0; i < chunks.size(); i++) {
            processLegislationChunk(file, chunks.get(i), i, chunks.size());
        }
    }

    private void processLegislationChunk(MultipartFile file, String chunk, int chunkIndex, int totalChunks)
            throws IOException {
        if (chunk == null || chunk.trim().isEmpty()) {
            return;
        }
        List<Float> embedding = embeddingClient.embed(chunk);
        JSONObject metadata = new JSONObject();
        metadata.put("documentId", file.getOriginalFilename());
        metadata.put("documentType", "LEGISLATION");
        metadata.put("chunkIndex", chunkIndex);
        metadata.put("totalChunks", totalChunks);
        metadata.put("textPreview", getTextPreview(chunk, TEXT_PREVIEW_LENGTH));
        metadata.put("uploadTimestamp", System.currentTimeMillis());

        Vector vector = new Vector(
                generatePdfChunkId("legis_" + file.getOriginalFilename(), chunkIndex),
                embedding);
        pineconeClient.upsertLegislation(vector, metadata);
    }

    private String generatePdfChunkId(String filename, int chunkIndex) {
        return String.format("pdf_%s_chunk_%d",
                filename.replaceAll("[^a-zA-Z0-9]", "_"), chunkIndex);
    }

    private String generateSqlStatementId(String filename, String objectName, int index) {
        return String.format("sql_%s_%s_%d",
                filename.replaceAll("[^a-zA-Z0-9]", "_"),
                objectName.replaceAll("[^a-zA-Z0-9]", "_"),
                index);
    }

    private String getTextPreview(String text, int maxLength) {
        if (text.length() <= maxLength) return text;
        return text.substring(0, maxLength) + "...";
    }

    private int estimateTokenSize(int desiredTokenCount) {
        return desiredTokenCount * 4;
    }

    // Helper class for SQL statements
    private static class SqlStatement {
        private final String name;
        private final String type;
        private final String definition;

        public SqlStatement(String name, String type, String definition) {
            this.name = name;
            this.type = type;
            this.definition = definition;
        }

        public String getName() { return name; }
        public String getType() { return type; }
        public String getDefinition() { return definition; }
    }
}