package src.entity.embeddable;

import jakarta.persistence.Embeddable;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;

import java.io.Serializable;
import java.util.UUID;

@Getter
@Setter
@RequiredArgsConstructor
@Embeddable
public class DocumentChatContextId implements Serializable {

    private UUID chatSessionId;
    private UUID documentId;
}
