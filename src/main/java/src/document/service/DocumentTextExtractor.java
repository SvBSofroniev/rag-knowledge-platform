package src.document.service;

import org.apache.tika.Tika;
import org.apache.tika.exception.TikaException;
import org.apache.tika.metadata.Metadata;
import org.apache.tika.metadata.TikaCoreProperties;
import org.springframework.stereotype.Service;
import src.common.exception.BadRequestException;
import src.common.exception.DocumentProcessingException;
import src.entity.Document;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.util.Locale;
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

        Path path = resolveStoragePath(
                document.getStoragePath()
        );

        validateStoredFile(path);

        Metadata metadata = new Metadata();

        metadata.set(
                TikaCoreProperties.RESOURCE_NAME_KEY,
                document.getOriginalFilename()
        );

        try (InputStream inputStream =
                     Files.newInputStream(path)) {

            String extractedText = tika.parseToString(
                    inputStream,
                    metadata
            );

            if (extractedText == null ||
                    extractedText.isBlank()) {
                throw new DocumentProcessingException(
                        "Document contains no extractable text"
                );
            }

            return extractedText.trim();

        } catch (TikaException exception) {
            throw new DocumentProcessingException(
                    "Text extraction failed for document: " +
                            document.getOriginalFilename(),
                    exception
            );

        } catch (IOException exception) {
            throw new DocumentProcessingException(
                    "Could not read the stored document file",
                    exception
            );
        }
    }

    private void validateDocument(Document document) {
        if (document == null) {
            throw new BadRequestException(
                    "Document cannot be null"
            );
        }

        if (document.getStoragePath() == null ||
                document.getStoragePath().isBlank()) {
            throw new DocumentProcessingException(
                    "Document storage path is missing"
            );
        }

        String filename = document.getOriginalFilename();

        if (filename == null || filename.isBlank()) {
            throw new DocumentProcessingException(
                    "Document filename is missing"
            );
        }

        String extension = getExtension(filename);

        if (!SUPPORTED_EXTENSIONS.contains(extension)) {
            throw new DocumentProcessingException(
                    "Unsupported document extension: " +
                            extension
            );
        }
    }

    private Path resolveStoragePath(String storagePath) {
        try {
            return Path.of(storagePath)
                    .toAbsolutePath()
                    .normalize();

        } catch (InvalidPathException exception) {
            throw new DocumentProcessingException(
                    "Document storage path is invalid",
                    exception
            );
        }
    }

    private void validateStoredFile(Path path) {
        if (!Files.exists(path)) {
            throw new DocumentProcessingException(
                    "Stored document file does not exist"
            );
        }

        if (!Files.isRegularFile(path)) {
            throw new DocumentProcessingException(
                    "Document storage path is not a regular file"
            );
        }

        if (!Files.isReadable(path)) {
            throw new DocumentProcessingException(
                    "Stored document file is not readable"
            );
        }
    }

    private String getExtension(String filename) {
        int dotIndex = filename.lastIndexOf('.');

        if (dotIndex < 0 ||
                dotIndex == filename.length() - 1) {
            throw new DocumentProcessingException(
                    "Document has no supported file extension"
            );
        }

        return filename
                .substring(dotIndex + 1)
                .toLowerCase(Locale.ROOT);
    }
}