package src.document.service;

import org.springframework.stereotype.Service;
import src.entity.Document;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Set;

@Service
public class DocumentTextExtractor {

    private static final Set<String> SUPPORTED_TYPES = Set.of(
            "text/plain",
            "text/markdown"
    );

    public String extract(Document document) {
        validateFileType(document);

        Path path = Path.of(document.getStoragePath())
                .toAbsolutePath()
                .normalize();

        if (!Files.exists(path)) {
            throw new RuntimeException(
                    "Stored document file does not exist"
            );
        }

        if (!Files.isRegularFile(path)) {
            throw new RuntimeException(
                    "Document storage path does not point to a file"
            );
        }

        try {
            String text = Files.readString(
                    path,
                    StandardCharsets.UTF_8
            );

            if (text.isBlank()) {
                throw new RuntimeException(
                        "Document contains no extractable text"
                );
            }

            return text;

        } catch (IOException exception) {
            throw new RuntimeException(
                    "Failed to read stored document",
                    exception
            );
        }
    }

    private void validateFileType(Document document) {
        String fileType = document.getFileType();

        if (fileType != null && SUPPORTED_TYPES.contains(fileType)) {
            return;
        }

        /*
         * Some clients send application/octet-stream for Markdown
         * or plain-text files, so also inspect the filename.
         */
        String filename = document.getOriginalFilename();

        if (filename != null) {
            String lowercaseName = filename.toLowerCase();

            if (lowercaseName.endsWith(".txt") ||
                    lowercaseName.endsWith(".md") ||
                    lowercaseName.endsWith(".markdown")) {
                return;
            }
        }

        throw new RuntimeException(
                "Unsupported document type: " + fileType
        );
    }
}