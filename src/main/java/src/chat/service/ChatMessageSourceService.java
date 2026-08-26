package src.chat.service;

import lombok.RequiredArgsConstructor;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import src.chat.repository.ChatMessageSourceRepository;
import src.entity.ChatMessage;
import src.entity.ChatMessageSource;
import src.rag.dto.SemanticSearchResponse;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ChatMessageSourceService {

    private final ChatMessageSourceRepository
            chatMessageSourceRepository;

    public void saveSources(
            ChatMessage message,
            List<SemanticSearchResponse> sources
    ) {
        if (sources == null || sources.isEmpty()) {
            return;
        }

        List<ChatMessageSource> entities =
                new ArrayList<>();

        for (int i = 0; i < sources.size(); i++) {
            SemanticSearchResponse source =
                    sources.get(i);

            ChatMessageSource entity =
                    new ChatMessageSource();

            entity.setMessage(message);
            entity.setSourceRank(i);

            entity.setChunkId(
                    source.chunkId()
            );

            entity.setDocumentId(
                    source.documentId()
            );

            entity.setDocumentTitle(
                    source.documentTitle()
            );

            entity.setChunkIndex(
                    source.chunkIndex()
            );

            entity.setContent(
                    source.content()
            );

            entity.setDistance(
                    source.distance()
            );

            entity.setSimilarity(
                    source.similarity()
            );

            entities.add(entity);
        }

        chatMessageSourceRepository.saveAll(
                entities
        );
    }

    @Transactional(readOnly = true)
    public List<SemanticSearchResponse> getSources(
            UUID messageId
    ) {
        return chatMessageSourceRepository
                .findAllByMessageIdOrderBySourceRankAsc(
                        messageId
                )
                .stream()
                .map(this::toResponse)
                .toList();
    }

    private SemanticSearchResponse toResponse(
            ChatMessageSource source
    ) {
        return new SemanticSearchResponse(
                source.getChunkId(),
                source.getDocumentId(),
                source.getDocumentTitle(),
                source.getChunkIndex(),
                source.getContent(),
                source.getDistance(),
                source.getSimilarity()
        );
    }
}
