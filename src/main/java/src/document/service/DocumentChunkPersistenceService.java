package src.document.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import src.document.repository.DocumentChunkRepository;
import src.document.repository.DocumentRepository;
import src.entity.Document;
import src.entity.DocumentChunk;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class DocumentChunkPersistenceService {

    private final DocumentRepository documentRepository;
    private final DocumentChunkRepository chunkRepository;
    private final TokenCountEstimator tokenCountEstimator;

    @Transactional
    public void replaceChunks(
            UUID documentId,
            List<String> chunkContents
    ) {
        Document document = documentRepository.findById(documentId)
                .orElseThrow(() ->
                        new RuntimeException("Document not found")
                );

        chunkRepository.deleteByDocument(document);
        chunkRepository.flush();

        List<DocumentChunk> chunks = new ArrayList<>();

        for (int index = 0; index < chunkContents.size(); index++) {
            String content = chunkContents.get(index);

            DocumentChunk chunk = new DocumentChunk();
            chunk.setDocument(document);
            chunk.setChunkIndex(index);
            chunk.setContent(content);
            chunk.setTokenCount(
                    tokenCountEstimator.estimate(content)
            );

            chunks.add(chunk);
        }

        chunkRepository.saveAll(chunks);
    }
}
