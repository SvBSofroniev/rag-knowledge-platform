package src.document.dto;

import java.util.UUID;

public interface DocumentChunkSearchResult {

    UUID getChunkId();

    UUID getDocumentId();

    String getDocumentTitle();

    Integer getChunkIndex();

    String getContent();

    Double getDistance();
}