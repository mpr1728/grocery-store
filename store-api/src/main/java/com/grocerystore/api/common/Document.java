package com.grocerystore.api.common;

import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/** An uploaded certificate or lab report. */
@Entity
@Table(name = "documents")
public class Document {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "file_name")
    private String fileName;
    @Column(name = "content_type")
    private String contentType;
    @Column(name = "size_bytes")
    private int sizeBytes;
    private byte[] content;
    @Column(name = "uploaded_at")
    private Instant uploadedAt;

    protected Document() {
    }

    public Document(String fileName, String contentType, byte[] content, Instant uploadedAt) {
        this.fileName = fileName;
        this.contentType = contentType;
        this.sizeBytes = content.length;
        this.content = content;
        this.uploadedAt = uploadedAt;
    }

    public Long getId() {
        return id;
    }

    public String getFileName() {
        return fileName;
    }

    public String getContentType() {
        return contentType;
    }

    public byte[] getContent() {
        return content;
    }
}
