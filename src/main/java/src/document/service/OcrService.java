package src.document.service;

import lombok.extern.slf4j.Slf4j;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.rendering.ImageType;
import org.apache.pdfbox.rendering.PDFRenderer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import src.common.exception.DocumentProcessingException;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
public class OcrService {

    private final boolean enabled;
    private final String tesseractPath;
    private final String languages;
    private final int dpi;
    private final long pageTimeoutSeconds;
    private final int maxPages;

    public OcrService(
            @Value("${ourvault.ocr.enabled:true}")
            boolean enabled,

            @Value("${ourvault.ocr.tesseract-path}")
            String tesseractPath,

            @Value("${ourvault.ocr.languages:bul+eng}")
            String languages,

            @Value("${ourvault.ocr.dpi:300}")
            int dpi,

            @Value("${ourvault.ocr.page-timeout-seconds:60}")
            long pageTimeoutSeconds,

            @Value("${ourvault.ocr.max-pages:50}")
            int maxPages
    ) {
        this.enabled = enabled;
        this.tesseractPath = tesseractPath;
        this.languages = languages;
        this.dpi = dpi;
        this.pageTimeoutSeconds = pageTimeoutSeconds;
        this.maxPages = maxPages;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public String extractPdf(Path pdfPath) {
        if (!enabled) {
            throw new DocumentProcessingException(
                    "OCR processing is disabled"
            );
        }

        validateConfiguration();

        Path temporaryDirectory = null;

        try {
            temporaryDirectory =
                    Files.createTempDirectory(
                            "ourvault-ocr-"
                    );

            try (PDDocument pdfDocument =
                         Loader.loadPDF(
                                 pdfPath.toFile()
                         )) {

                int pageCount =
                        pdfDocument.getNumberOfPages();

                if (pageCount == 0) {
                    throw new DocumentProcessingException(
                            "PDF contains no pages"
                    );
                }

                if (pageCount > maxPages) {
                    throw new DocumentProcessingException(
                            "PDF contains " +
                                    pageCount +
                                    " pages, exceeding the OCR limit of " +
                                    maxPages
                    );
                }

                log.info(
                        "Starting OCR: file={}, pages={}, dpi={}, languages={}",
                        pdfPath.getFileName(),
                        pageCount,
                        dpi,
                        languages
                );

                PDFRenderer renderer =
                        new PDFRenderer(
                                pdfDocument
                        );

                StringBuilder extractedText =
                        new StringBuilder();

                for (
                        int pageIndex = 0;
                        pageIndex < pageCount;
                        pageIndex++
                ) {
                    String pageText =
                            processPage(
                                    renderer,
                                    pageIndex,
                                    temporaryDirectory
                            );

                    if (pageText != null &&
                            !pageText.isBlank()) {

                        extractedText
                                .append("[Page ")
                                .append(pageIndex + 1)
                                .append("]\n")
                                .append(pageText.trim())
                                .append("\n\n");
                    }
                }

                String result =
                        extractedText
                                .toString()
                                .trim();

                if (result.isBlank()) {
                    throw new DocumentProcessingException(
                            "OCR could not extract text from the PDF"
                    );
                }

                log.info(
                        "OCR completed: file={}, extractedCharacters={}",
                        pdfPath.getFileName(),
                        result.length()
                );

                return result;
            }

        } catch (DocumentProcessingException exception) {
            throw exception;

        } catch (IOException exception) {
            throw new DocumentProcessingException(
                    "OCR failed while reading the PDF",
                    exception
            );

        } finally {
            deleteTemporaryDirectory(
                    temporaryDirectory
            );
        }
    }

    private String processPage(
            PDFRenderer renderer,
            int pageIndex,
            Path temporaryDirectory
    ) throws IOException {

        int humanPageNumber =
                pageIndex + 1;

        log.debug(
                "OCR rendering page {}",
                humanPageNumber
        );

        BufferedImage pageImage =
                renderer.renderImageWithDPI(
                        pageIndex,
                        dpi,
                        ImageType.RGB
                );

        Path imagePath =
                temporaryDirectory.resolve(
                        "page-" +
                                humanPageNumber +
                                ".png"
                );

        boolean written =
                ImageIO.write(
                        pageImage,
                        "png",
                        imagePath.toFile()
                );

        if (!written) {
            throw new DocumentProcessingException(
                    "Could not create temporary OCR image for page " +
                            humanPageNumber
            );
        }

        Path outputBase =
                temporaryDirectory.resolve(
                        "page-" +
                                humanPageNumber +
                                "-ocr"
                );

        Path outputText =
                Path.of(
                        outputBase.toString() +
                                ".txt"
                );

        Path processLog =
                temporaryDirectory.resolve(
                        "page-" +
                                humanPageNumber +
                                "-tesseract.log"
                );

        ProcessBuilder processBuilder =
                new ProcessBuilder(
                        tesseractPath,
                        imagePath.toString(),
                        outputBase.toString(),
                        "-l",
                        languages,
                        "--psm",
                        "6"
                );

        /*
         * Combine stderr/stdout and write it to a temporary
         * file so the process cannot block because a pipe fills.
         */
        processBuilder.redirectErrorStream(
                true
        );

        processBuilder.redirectOutput(
                processLog.toFile()
        );

        Process process;

        try {
            process =
                    processBuilder.start();

        } catch (IOException exception) {
            throw new DocumentProcessingException(
                    "Could not start Tesseract. Verify the configured executable path: " +
                            tesseractPath,
                    exception
            );
        }

        boolean finished;

        try {
            finished =
                    process.waitFor(
                            pageTimeoutSeconds,
                            TimeUnit.SECONDS
                    );

        } catch (InterruptedException exception) {
            Thread.currentThread()
                    .interrupt();

            process.destroyForcibly();

            throw new DocumentProcessingException(
                    "OCR processing was interrupted",
                    exception
            );
        }

        if (!finished) {
            process.destroyForcibly();

            throw new DocumentProcessingException(
                    "OCR timed out while processing page " +
                            humanPageNumber
            );
        }

        int exitCode =
                process.exitValue();

        if (exitCode != 0) {
            String logOutput =
                    readFileSafely(
                            processLog
                    );

            throw new DocumentProcessingException(
                    "Tesseract failed on page " +
                            humanPageNumber +
                            " with exit code " +
                            exitCode +
                            (
                                    logOutput.isBlank()
                                            ? ""
                                            : ": " + logOutput
                            )
            );
        }

        if (!Files.exists(outputText)) {
            return "";
        }

        String text =
                Files.readString(
                        outputText,
                        StandardCharsets.UTF_8
                );

        /*
         * We no longer need the rendered page once OCR has
         * finished.
         */
        Files.deleteIfExists(
                imagePath
        );

        Files.deleteIfExists(
                outputText
        );

        Files.deleteIfExists(
                processLog
        );

        log.debug(
                "OCR completed for page {}: characters={}",
                humanPageNumber,
                text.length()
        );

        return text;
    }

    private void validateConfiguration() {
        if (tesseractPath == null ||
                tesseractPath.isBlank()) {
            throw new DocumentProcessingException(
                    "Tesseract executable path is not configured"
            );
        }

        Path executable =
                Path.of(
                        tesseractPath
                );

        if (!Files.exists(executable) ||
                !Files.isRegularFile(executable)) {
            throw new DocumentProcessingException(
                    "Tesseract executable was not found at: " +
                            tesseractPath
            );
        }

        if (languages == null ||
                languages.isBlank()) {
            throw new DocumentProcessingException(
                    "OCR languages are not configured"
            );
        }

        if (dpi < 72 ||
                dpi > 600) {
            throw new DocumentProcessingException(
                    "OCR DPI must be between 72 and 600"
            );
        }

        if (pageTimeoutSeconds < 1) {
            throw new DocumentProcessingException(
                    "OCR page timeout must be greater than zero"
            );
        }

        if (maxPages < 1) {
            throw new DocumentProcessingException(
                    "OCR maximum page count must be greater than zero"
            );
        }
    }

    private String readFileSafely(
            Path path
    ) {
        try {
            if (path == null ||
                    !Files.exists(path)) {
                return "";
            }

            return Files.readString(
                    path,
                    StandardCharsets.UTF_8
            ).trim();

        } catch (IOException exception) {
            return "";
        }
    }

    private void deleteTemporaryDirectory(
            Path directory
    ) {
        if (directory == null ||
                !Files.exists(directory)) {
            return;
        }

        try (var paths =
                     Files.walk(directory)) {

            paths.sorted(
                            Comparator.reverseOrder()
                    )
                    .forEach(path -> {
                        try {
                            Files.deleteIfExists(
                                    path
                            );
                        } catch (IOException exception) {
                            log.warn(
                                    "Could not delete OCR temporary file: {}",
                                    path
                            );
                        }
                    });

        } catch (IOException exception) {
            log.warn(
                    "Could not clean OCR temporary directory: {}",
                    directory
            );
        }
    }
}