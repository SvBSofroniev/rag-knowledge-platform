package src.document.service;

import lombok.extern.slf4j.Slf4j;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.tika.Tika;
import org.apache.tika.exception.TikaException;
import org.apache.tika.metadata.Metadata;
import org.apache.tika.metadata.TikaCoreProperties;
import org.springframework.stereotype.Service;
import src.common.exception.ApiErrorCodes;
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
     * Some PDFs may contain a very small amount of metadata,
     * page numbers, scanner text, or other insignificant text
     * even though the actual page content is image-based.
     *
     * Requiring a minimum amount of meaningful letters/digits
     * allows those PDFs to fall back to OCR.
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

        /*
         * PDFs are intentionally handled separately.
         *
         * We do NOT use Tika first for PDFs because Tika may
         * invoke its own OCR integration. That would bypass
         * the OCR configuration managed by OcrService.
         *
         * Instead:
         *
         * PDFBox -> native PDF text layer
         *
         * If no meaningful native text exists:
         *
         * OcrService -> configured Tesseract OCR
         */
        if ("pdf".equals(extension)) {
            return extractPdf(
                    document,
                    path
            );
        }

        /*
         * DOCX / TXT / MD / MARKDOWN
         *
         * These formats continue to use Tika because they
         * normally contain an actual machine-readable
         * text layer and do not need the PDF OCR fallback.
         */
        String tikaText =
                extractWithTika(
                        document,
                        path
                );

        if (!hasAnyText(
                tikaText
        )) {
            throw new DocumentProcessingException(
                    "Document contains no extractable text"
            );
        }

        return tikaText.trim();
    }

    private String extractPdf(
            Document document,
            Path path
    ) {
        /*
         * First inspect only the PDF's native text layer.
         */
        String nativeText =
                extractNativePdfText(
                        document,
                        path
                );

        /*
         * Normal text-based PDF.
         */
        if (hasMeaningfulPdfText(
                nativeText
        )) {
            log.info(
                    "PDF native text layer detected: file={}, characters={}",
                    document.getOriginalFilename(),
                    nativeText.length()
            );

            return nativeText.trim();
        }

        /*
         * No meaningful embedded text exists.
         *
         * This is where scanned PDFs should reach our
         * configured OCR pipeline.
         */
        if (!ocrService.isEnabled()) {
            throw new DocumentProcessingException(
                    "Document contains no meaningful text layer and OCR is disabled"
            );
        }

        log.info(
                "PDF contains no meaningful native text layer. Starting OCR fallback: file={}",
                document.getOriginalFilename()
        );

        String ocrText =
                ocrService.extractPdf(
                        path
                );

        if (!hasAnyText(
                ocrText
        )) {
            throw new DocumentProcessingException(
                    "OCR produced no usable text"
            );
        }

        log.info(
                "PDF OCR extraction completed: file={}, characters={}",
                document.getOriginalFilename(),
                ocrText.length()
        );

        return ocrText.trim();
    }

    private String extractNativePdfText(
            Document document,
            Path path
    ) {
        try (PDDocument pdfDocument =
                     Loader.loadPDF(
                             path.toFile()
                     )) {

            if (pdfDocument.getNumberOfPages() == 0) {
                throw new DocumentProcessingException(
                        "PDF contains no pages"
                );
            }

            PDFTextStripper textStripper =
                    new PDFTextStripper();

            String extractedText =
                    textStripper.getText(
                            pdfDocument
                    );

            if (extractedText == null) {
                return "";
            }

            String result =
                    extractedText.trim();

            log.debug(
                    "PDFBox native extraction completed: file={}, characters={}",
                    document.getOriginalFilename(),
                    result.length()
            );

            return result;

        } catch (DocumentProcessingException exception) {
            throw exception;

        } catch (IOException exception) {

            /*
             * If native PDF extraction fails but OCR is
             * enabled, allow the OCR pipeline to attempt
             * recovery from the rendered pages.
             */
            if (ocrService.isEnabled()) {
                log.warn(
                        "Native PDF text extraction failed. OCR fallback will be attempted: file={}",
                        document.getOriginalFilename(),
                        exception
                );

                return "";
            }

            throw new DocumentProcessingException(
                    "Could not extract text from PDF: " +
                            document.getOriginalFilename(),
                    exception
            );
        }
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
         * Ignore whitespace and punctuation.
         *
         * What matters here is whether the PDF contains a
         * meaningful number of actual letters or digits.
         */
        long meaningfulCharacters =
                text.chars()
                        .filter(
                                Character::isLetterOrDigit
                        )
                        .count();

        return meaningfulCharacters >=
                MIN_PDF_TEXT_CHARACTERS;
    }

    private void validateDocument(
            Document document
    ) {
        if (document == null) {
            throw new BadRequestException(
                    ApiErrorCodes.DOCUMENT_REQUIRED,
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