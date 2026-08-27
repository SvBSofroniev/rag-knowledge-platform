package src.document.service;

import lombok.extern.slf4j.Slf4j;
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

@Slf4j
@Service
public class DocumentTextExtractor {

    /*
     * Some scanned PDFs contain a tiny amount of metadata or
     * scanner-generated text even though the actual page contents
     * are images.
     *
     * Requiring a small amount of meaningful text allows those
     * documents to fall back to OCR.
     */
    private static final int MIN_PDF_TEXT_CHARACTERS = 40;

    private static final Set<String>
            SUPPORTED_EXTENSIONS = Set.of(
            "pdf",
            "docx",
            "txt",
            "md",
            "markdown"
    );

    private final Tika tika =
            new Tika();

    private final OcrService ocrService;

    public DocumentTextExtractor(
            OcrService ocrService
    ) {
        this.ocrService =
                ocrService;
    }

    public String extract(
            Document document
    ) {
        validateDocument(
                document
        );

        Path path =
                resolveStoragePath(
                        document.getStoragePath()
                );

        validateStoredFile(
                path
        );

        String extension =
                getExtension(
                        document.getOriginalFilename()
                );

        String tikaText =
                extractWithTika(
                        document,
                        path
                );

        /*
         * Non-PDF files don't currently have an OCR fallback.
         */
        if (!"pdf".equals(extension)) {
            if (!hasAnyText(
                    tikaText
            )) {
                throw new DocumentProcessingException(
                        "Document contains no extractable text"
                );
            }

            return tikaText.trim();
        }

        /*
         * Normal PDF with a real text layer.
         */
        if (hasMeaningfulPdfText(
                tikaText
        )) {
            log.debug(
                    "PDF text extracted normally using Tika: file={}, characters={}",
                    document.getOriginalFilename(),
                    tikaText.length()
            );

            return tikaText.trim();
        }

        /*
         * PDF has no useful text layer.
         *
         * Fall back to OCR.
         */
        if (ocrService.isEnabled()) {
            log.info(
                    "PDF contains no meaningful text layer. Starting OCR fallback: file={}",
                    document.getOriginalFilename()
            );

            String ocrText =
                    ocrService.extractPdf(
                            path
                    );

            if (hasAnyText(
                    ocrText
            )) {
                return ocrText.trim();
            }
        }

        throw new DocumentProcessingException(
                "Document contains no extractable text"
        );
    }

    private String extractWithTika(
            Document document,
            Path path
    ) {
        Metadata metadata =
                new Metadata();

        metadata.set(
                TikaCoreProperties.RESOURCE_NAME_KEY,
                document.getOriginalFilename()
        );

        try (InputStream inputStream =
                     Files.newInputStream(
                             path
                     )) {

            String extractedText =
                    tika.parseToString(
                            inputStream,
                            metadata
                    );

            return extractedText == null
                    ? ""
                    : extractedText.trim();

        } catch (TikaException exception) {
            /*
             * If Tika cannot parse a PDF at all, OCR may still
             * be able to recover its visible page content.
             */
            if (isPdf(
                    document.getOriginalFilename()
            ) &&
                    ocrService.isEnabled()) {

                log.warn(
                        "Tika extraction failed for PDF. OCR fallback will be attempted: file={}",
                        document.getOriginalFilename(),
                        exception
                );

                return "";
            }

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

    private boolean hasAnyText(
            String text
    ) {
        return text != null &&
                !text.isBlank();
    }

    private boolean hasMeaningfulPdfText(
            String text
    ) {
        if (text == null ||
                text.isBlank()) {
            return false;
        }

        /*
         * Count actual letters/digits instead of spaces and
         * punctuation.
         */
        long meaningfulCharacters =
                text.chars()
                        .filter(Character::isLetterOrDigit
                        )
                        .count();

        return meaningfulCharacters >=
                MIN_PDF_TEXT_CHARACTERS;
    }

    private boolean isPdf(
            String filename
    ) {
        if (filename == null) {
            return false;
        }

        return filename
                .toLowerCase(Locale.ROOT)
                .endsWith(".pdf");
    }

    private void validateDocument(
            Document document
    ) {
        if (document == null) {
            throw new BadRequestException(
                    "Document cannot be null"
            );
        }

        if (document.getStoragePath() == null ||
                document.getStoragePath()
                        .isBlank()) {

            throw new DocumentProcessingException(
                    "Document storage path is missing"
            );
        }

        String filename =
                document.getOriginalFilename();

        if (filename == null ||
                filename.isBlank()) {

            throw new DocumentProcessingException(
                    "Document filename is missing"
            );
        }

        String extension =
                getExtension(
                        filename
                );

        if (!SUPPORTED_EXTENSIONS.contains(
                extension
        )) {
            throw new DocumentProcessingException(
                    "Unsupported document extension: " +
                            extension
            );
        }
    }

    private Path resolveStoragePath(
            String storagePath
    ) {
        try {
            return Path.of(
                            storagePath
                    )
                    .toAbsolutePath()
                    .normalize();

        } catch (InvalidPathException exception) {
            throw new DocumentProcessingException(
                    "Document storage path is invalid",
                    exception
            );
        }
    }

    private void validateStoredFile(
            Path path
    ) {
        if (!Files.exists(
                path
        )) {
            throw new DocumentProcessingException(
                    "Stored document file does not exist"
            );
        }

        if (!Files.isRegularFile(
                path
        )) {
            throw new DocumentProcessingException(
                    "Document storage path is not a regular file"
            );
        }

        if (!Files.isReadable(
                path
        )) {
            throw new DocumentProcessingException(
                    "Stored document file is not readable"
            );
        }
    }

    private String getExtension(
            String filename
    ) {
        int dotIndex =
                filename.lastIndexOf(
                        '.'
                );

        if (dotIndex < 0 ||
                dotIndex ==
                        filename.length() - 1) {

            throw new DocumentProcessingException(
                    "Document has no supported file extension"
            );
        }

        return filename
                .substring(
                        dotIndex + 1
                )
                .toLowerCase(
                        Locale.ROOT
                );
    }
}