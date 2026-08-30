package src.document.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import src.common.exception.ApiErrorCodes;
import src.common.exception.BadRequestException;
import src.common.exception.FileStorageException;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Locale;
import java.util.UUID;

@Service
public class FileStorageService {

    private final Path uploadDir;

    public FileStorageService(
            @Value("${app.upload-dir:uploads}") String uploadDir
    ) {
        try {
            this.uploadDir = Paths.get(uploadDir)
                    .toAbsolutePath()
                    .normalize();
        } catch (InvalidPathException exception) {
            throw new FileStorageException(
                    "The configured upload directory is invalid",
                    exception
            );
        }
    }

    public StoredFile store(MultipartFile file) {
        validateFile(file);

        try {
            Files.createDirectories(uploadDir);

            String originalFilename =
                    resolveSafeOriginalFilename(file);

            String extension =
                    extractExtension(originalFilename);

            /*
             * Do not include the user-supplied filename in the physical
             * stored filename. This avoids unsafe characters, collisions,
             * path traversal, and information leakage.
             */
            String storedFilename =
                    UUID.randomUUID() + extension;

            Path targetPath = uploadDir
                    .resolve(storedFilename)
                    .normalize();

            ensurePathIsInsideUploadDirectory(targetPath);

            Files.copy(
                    file.getInputStream(),
                    targetPath,
                    StandardCopyOption.REPLACE_EXISTING
            );

            return new StoredFile(
                    storedFilename,
                    targetPath.toString()
            );

        } catch (IOException exception) {
            throw new FileStorageException(
                    "Could not store the uploaded file",
                    exception
            );
        }
    }

    public void delete(String filePath) {
        if (filePath == null || filePath.isBlank()) {
            throw new BadRequestException(
                    ApiErrorCodes.STORED_FILE_PATH_REQUIRED,
                    "Stored file path cannot be empty"
            );
        }

        try {
            Path targetPath = Paths.get(filePath)
                    .toAbsolutePath()
                    .normalize();

            ensurePathIsInsideUploadDirectory(targetPath);

            Files.deleteIfExists(targetPath);

        } catch (InvalidPathException exception) {
            throw new BadRequestException(
                    ApiErrorCodes.STORED_FILE_PATH_INVALID,
                    "Stored file path is invalid"
            );
        } catch (IOException exception) {
            throw new FileStorageException(
                    "Could not delete the stored file",
                    exception
            );
        }
    }

    public Path resolveStoredFile(
            String filePath
    ) {
        if (filePath == null ||
                filePath.isBlank()) {

            throw new BadRequestException(
                    ApiErrorCodes.STORED_FILE_PATH_REQUIRED,
                    "Stored file path cannot be empty"
            );
        }

        try {
            Path targetPath =
                    Paths.get(filePath)
                            .toAbsolutePath()
                            .normalize();

            ensurePathIsInsideUploadDirectory(
                    targetPath
            );

            if (!Files.exists(targetPath)) {
                throw new FileStorageException(
                        "Stored file does not exist"
                );
            }

            if (!Files.isRegularFile(targetPath)) {
                throw new FileStorageException(
                        "Stored file path is not a regular file"
                );
            }

            if (!Files.isReadable(targetPath)) {
                throw new FileStorageException(
                        "Stored file is not readable"
                );
            }

            return targetPath;

        } catch (InvalidPathException exception) {
            throw new BadRequestException(
                    ApiErrorCodes.STORED_FILE_PATH_INVALID,
                    "Stored file path is invalid"
            );
        }
    }

    private void validateFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BadRequestException(
                    ApiErrorCodes.DOCUMENT_FILE_EMPTY,
                    "Uploaded file cannot be empty"
            );
        }
    }

    private String resolveSafeOriginalFilename(
            MultipartFile file
    ) {
        String suppliedFilename = file.getOriginalFilename();

        if (suppliedFilename == null ||
                suppliedFilename.isBlank()) {
            throw new BadRequestException(
                    ApiErrorCodes.DOCUMENT_FILENAME_MISSING,
                    "Uploaded file name is missing"
            );
        }

        try {
            String safeFilename = Paths
                    .get(suppliedFilename)
                    .getFileName()
                    .toString();

            if (safeFilename.isBlank()) {
                throw new BadRequestException(
                        ApiErrorCodes.DOCUMENT_FILENAME_INVALID,
                        "Uploaded file name is invalid"
                );
            }

            return safeFilename;

        } catch (InvalidPathException exception) {
            throw new BadRequestException(
                    ApiErrorCodes.DOCUMENT_FILENAME_INVALID,
                    "Uploaded file name is invalid"
            );
        }
    }

    private String extractExtension(String filename) {
        int dotIndex = filename.lastIndexOf('.');

        if (dotIndex < 0 ||
                dotIndex == filename.length() - 1) {
            return "";
        }

        String extension = filename
                .substring(dotIndex)
                .toLowerCase(Locale.ROOT);

        /*
         * Allows extensions such as .pdf, .docx and .markdown,
         * while rejecting unusual path or control characters.
         */
        if (!extension.matches("\\.[a-z0-9]{1,15}")) {
            throw new BadRequestException(
                    ApiErrorCodes.DOCUMENT_EXTENSION_INVALID,
                    "Uploaded file extension is invalid"
            );
        }

        return extension;
    }

    private void ensurePathIsInsideUploadDirectory(
            Path targetPath
    ) {
        if (!targetPath.startsWith(uploadDir)) {
            throw new BadRequestException(
                    ApiErrorCodes.INVALID_FILE_STORAGE_PATH,
                    "Invalid file storage path"
            );
        }
    }

    public record StoredFile(
            String storedFilename,
            String filePath
    ) {
    }
}