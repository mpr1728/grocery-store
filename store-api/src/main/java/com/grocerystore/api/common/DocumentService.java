package com.grocerystore.api.common;

import java.io.IOException;
import java.time.Clock;
import java.time.Instant;
import java.util.Set;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Service
public class DocumentService {

    private static final Set<String> ALLOWED_TYPES = Set.of("application/pdf", "image/jpeg", "image/png");

    private final DocumentRepository documents;
    private final Clock clock;

    public DocumentService(DocumentRepository documents, Clock clock) {
        this.documents = documents;
        this.clock = clock;
    }

    /** Saves an upload and returns its id, or null when no file was sent. */
    @Transactional
    public Long store(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            return null;
        }
        if (file.getContentType() == null || !ALLOWED_TYPES.contains(file.getContentType())) {
            throw new BadRequestException("Upload a PDF, JPEG, or PNG file");
        }
        String name = file.getOriginalFilename() == null ? "document" : file.getOriginalFilename();
        // Keep the name only, without any folder a browser may have sent, and safe to put in a header.
        name = name.substring(Math.max(name.lastIndexOf('/'), name.lastIndexOf('\\')) + 1)
                .replaceAll("[^A-Za-z0-9._ -]", "_");
        if (name.length() > 200) {
            name = name.substring(name.length() - 200);
        }
        try {
            return documents.save(new Document(name, file.getContentType(), file.getBytes(), Instant.now(clock))).getId();
        } catch (IOException e) {
            throw new BadRequestException("The file could not be read");
        }
    }

    @Transactional(readOnly = true)
    public Document require(Long id) {
        return documents.findById(id).orElseThrow(() -> new NotFoundException("Document " + id + " was not found"));
    }
}
