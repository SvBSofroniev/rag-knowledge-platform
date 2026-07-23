package src.document.service;

import org.apache.tika.Tika;
import org.apache.tika.exception.TikaException;
import org.apache.tika.metadata.Metadata;
import org.apache.tika.metadata.TikaCoreProperties;
import org.springframework.stereotype.Service;
import src.entity.Document;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Set;

@Service
public class DocumentTextExtractor {

    private static final Set<String> SUPPORTED_EXTENSIONS = Set.of(
            "pdf",
            "docx",
            "txt",
            "md",
            "markdown"
    );

    private final Tika tika = new Tika();

    public String extract(Document document) {
        validateDocument(document);

        Path path = Path.of(document.getStoragePath())
                .toAbsolutePath()
                .normalize();

        validateStoredFile(path);

        Metadata metadata = new Metadata();

        if (document.getOriginalFilename() != null) {
            metadata.set(
                    TikaCoreProperties.RESOURCE_NAME_KEY,
                    document.getOriginalFilename()
            );
        }

        try (InputStream inputStream = Files.newInputStream(path)) {
            String extractedText = tika.parseToString(
                    inputStream,
                    metadata
            );

            if (extractedText == null || extractedText.isBlank()) {
                throw new RuntimeException(
                        "Document contains no extractable text"
                );
            }

            return extractedText.trim();

        } catch (IOException exception) {
            throw new RuntimeException(
                    "Failed to read the stored document",
                    exception
            );

        } catch (TikaException exception) {
            throw new RuntimeException(
                    "Failed to extract text from the document",
                    exception
            );
        }
    }

    private void validateDocument(Document document) {
        if (document == null) {
            throw new IllegalArgumentException(
                    "Document cannot be null"
            );
        }

        if (document.getStoragePath() == null ||
                document.getStoragePath().isBlank()) {
            throw new RuntimeException(
                    "Document storage path is missing"
            );
        }

        String filename = document.getOriginalFilename();

        if (filename == null || filename.isBlank()) {
            throw new RuntimeException(
                    "Document filename is missing"
            );
        }

        String extension = getExtension(filename);

        if (!SUPPORTED_EXTENSIONS.contains(extension)) {
            throw new RuntimeException(
                    "Unsupported document extension: " + extension
            );
        }
    }

    private void validateStoredFile(Path path) {
        if (!Files.exists(path)) {
            throw new RuntimeException(
                    "Stored document file does not exist"
            );
        }

        if (!Files.isRegularFile(path)) {
            throw new RuntimeException(
                    "Document storage path is not a regular file"
            );
        }

        if (!Files.isReadable(path)) {
            throw new RuntimeException(
                    "Stored document file is not readable"
            );
        }
    }

    private String getExtension(String filename) {
        int lastDotIndex = filename.lastIndexOf('.');

        if (lastDotIndex < 0 ||
                lastDotIndex == filename.length() - 1) {
            return "";
        }

        return filename
                .substring(lastDotIndex + 1)
                .toLowerCase();
    }
}