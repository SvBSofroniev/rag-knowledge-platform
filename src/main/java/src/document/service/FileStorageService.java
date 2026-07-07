package src.document.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.*;
import java.util.UUID;

@Service
public class FileStorageService {

    private final Path uploadDir;

    public FileStorageService(
            @Value("${app.upload-dir:uploads}") String uploadDir
    ) {
        this.uploadDir = Paths.get(uploadDir).toAbsolutePath().normalize();
    }

    public StoredFile store(MultipartFile file) {
        try {
            Files.createDirectories(uploadDir);

            String originalFilename = file.getOriginalFilename();

            if (originalFilename == null || originalFilename.isBlank()) {
                throw new RuntimeException("Invalid file name");
            }

            String storedFilename = UUID.randomUUID() + "_" + originalFilename;
            Path targetPath = uploadDir.resolve(storedFilename).normalize();

            if (!targetPath.startsWith(uploadDir)) {
                throw new RuntimeException("Invalid file path");
            }

            Files.copy(file.getInputStream(), targetPath, StandardCopyOption.REPLACE_EXISTING);

            return new StoredFile(
                    storedFilename,
                    targetPath.toString()
            );

        } catch (IOException e) {
            throw new RuntimeException("Could not store file", e);
        }
    }

    public void delete(String filePath) {
        try {
            Files.deleteIfExists(Paths.get(filePath));
        } catch (IOException e) {
            throw new RuntimeException("Could not delete file", e);
        }
    }

    public record StoredFile(
            String storedFilename,
            String filePath
    ) {}
}
