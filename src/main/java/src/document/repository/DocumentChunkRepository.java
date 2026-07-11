package src.document.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import src.entity.Document;
import src.entity.DocumentChunk;

import java.util.List;
import java.util.UUID;

@Repository
public interface DocumentChunkRepository
        extends JpaRepository<DocumentChunk, UUID> {

    List<DocumentChunk> findByDocumentOrderByChunkIndexAsc(Document document);

    void deleteByDocument(Document document);

    boolean existsByDocument(Document document);

    long countByDocument(Document document);
}