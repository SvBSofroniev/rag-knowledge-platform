package src.document.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import src.common.exception.BadRequestException;
import src.common.exception.ForbiddenOperationException;
import src.common.exception.ResourceNotFoundException;
import src.document.dto.DocumentResponse;
import src.document.dto.GlobalDocumentResponse;
import src.document.repository.DocumentRepository;
import src.document.util.DocumentStatus;
import src.entity.Document;
import src.entity.User;
import src.entity.Workspace;
import src.entity.WorkspaceMember;
import src.workspace.service.WorkspacePermissionService;
import src.workspace.util.WorkspaceRole;

import java.nio.file.Path;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import src.document.dto.DocumentContentResponse;
import src.document.dto.DocumentDetailsResponse;
import src.document.repository.DocumentChunkRepository;
import src.entity.DocumentChunk;

import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;

@Service
@RequiredArgsConstructor
public class DocumentService {

    private static final Set<String> SUPPORTED_EXTENSIONS = Set.of(
            "pdf",
            "docx",
            "txt",
            "md",
            "markdown"
    );

    private final FileStorageService fileStorageService;
    private final WorkspacePermissionService permissionService;
    private final DocumentRepository documentRepository;
    private final DocumentChunkRepository documentChunkRepository;

    @Transactional
    public DocumentResponse uploadDocument(
            UUID workspaceId,
            MultipartFile file,
            User currentUser
    ) {
        WorkspaceMember member =
                permissionService.requireMember(
                        workspaceId,
                        currentUser
                );

        Workspace workspace = member.getWorkspace();

        String originalFilename = validateAndResolveFilename(file);

        FileStorageService.StoredFile storedFile =
                fileStorageService.store(file);

        try {
            Document document = new Document();
            document.setWorkspace(workspace);
            document.setUploadedBy(currentUser);
            document.setTitle(originalFilename);
            document.setOriginalFilename(originalFilename);
            document.setFileType(resolveFileType(file));
            document.setFileSize(file.getSize());
            document.setStoragePath(storedFile.filePath());
            document.setStatus(DocumentStatus.PENDING);
            document.setProcessingError(null);

            Document saved =
                    documentRepository.saveAndFlush(document);

            return toResponse(saved);

        } catch (RuntimeException exception) {
            fileStorageService.delete(storedFile.filePath());
            throw exception;
        }
    }

    @Transactional(readOnly = true)
    public List<DocumentResponse> getWorkspaceDocuments(
            UUID workspaceId,
            User currentUser
    ) {
        WorkspaceMember member =
                permissionService.requireMember(
                        workspaceId,
                        currentUser
                );

        return documentRepository
                .findByWorkspaceOrderByCreatedAtDesc(
                        member.getWorkspace()
                )
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public DocumentResponse getDocument(
            UUID documentId,
            User currentUser
    ) {
        Document document = getDocumentOrThrow(documentId);

        requireDocumentAccess(document, currentUser);

        return toResponse(document);
    }

    @Transactional
    public void deleteDocument(
            UUID documentId,
            User currentUser
    ) {
        Document document = getDocumentOrThrow(documentId);

        WorkspaceMember membership =
                permissionService.requireMember(
                        document.getWorkspace().getId(),
                        currentUser
                );

        boolean isUploader =
                document.getUploadedBy()
                        .getId()
                        .equals(currentUser.getId());

        boolean isAdminOrOwner =
                membership.getRole() == WorkspaceRole.ADMIN ||
                        membership.getRole() == WorkspaceRole.OWNER;

        if (!isUploader && !isAdminOrOwner) {
            throw new ForbiddenOperationException(
                    "You do not have permission to delete this document"
            );
        }

        fileStorageService.delete(document.getStoragePath());
        documentRepository.delete(document);
    }

    public void requireDocumentAccess(
            Document document,
            User currentUser
    ) {
        permissionService.requireMember(
                document.getWorkspace().getId(),
                currentUser
        );
    }

    public Document getDocumentOrThrow(UUID documentId) {
        return documentRepository.findById(documentId)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Document not found"
                        )
                );
    }

    public DocumentResponse toResponse(Document document) {
        return new DocumentResponse(
                document.getId(),
                document.getTitle(),
                document.getOriginalFilename(),
                document.getFileType(),
                document.getFileSize(),
                document.getStatus(),
                document.getProcessingError(),
                document.getUploadedBy().getId(),
                document.getUploadedBy().getUsername(),
                document.getCreatedAt()
        );
    }

    @Transactional(readOnly = true)
    public DocumentDetailsResponse getDocumentDetails(
            UUID documentId,
            User currentUser
    ) {
        Document document =
                getDocumentOrThrow(
                        documentId
                );

        requireDocumentAccess(
                document,
                currentUser
        );

        long chunkCount =
                documentChunkRepository
                        .countByDocument(
                                document
                        );

        return new DocumentDetailsResponse(
                document.getId(),
                document.getWorkspace().getId(),
                document.getWorkspace().getName(),
                document.getTitle(),
                document.getOriginalFilename(),
                document.getFileType(),
                document.getFileSize(),
                document.getStatus(),
                document.getProcessingError(),
                document.getUploadedBy().getId(),
                document.getUploadedBy().getUsername(),
                chunkCount,
                document.getCreatedAt(),
                document.getUpdatedAt()
        );
    }

    @Transactional(readOnly = true)
    public DocumentFile getDocumentFile(
            UUID documentId,
            User currentUser
    ) {
        Document document =
                getDocumentOrThrow(
                        documentId
                );

        requireDocumentAccess(
                document,
                currentUser
        );

        Path path =
                fileStorageService
                        .resolveStoredFile(
                                document.getStoragePath()
                        );

        Resource resource =
                new FileSystemResource(
                        path
                );

        String contentType =
                resolveContentType(
                        document,
                        path
                );

        return new DocumentFile(
                resource,
                document.getOriginalFilename(),
                contentType,
                document.getFileSize()
        );
    }

    @Transactional(readOnly = true)
    public DocumentContentResponse getDocumentContent(
            UUID documentId,
            User currentUser
    ) {
        Document document =
                getDocumentOrThrow(
                        documentId
                );

        requireDocumentAccess(
                document,
                currentUser
        );

        if (document.getStatus() !=
                DocumentStatus.READY) {

            throw new BadRequestException(
                    "Document content is available only when processing is complete"
            );
        }

        List<DocumentChunk> chunks =
                documentChunkRepository
                        .findByDocumentOrderByChunkIndexAsc(
                                document
                        );

        String content =
                reconstructDocumentContent(
                        chunks
                );

        return new DocumentContentResponse(
                document.getId(),
                document.getTitle(),
                chunks.size(),
                content
        );
    }

    private String reconstructDocumentContent(
            List<DocumentChunk> chunks
    ) {
        if (chunks == null ||
                chunks.isEmpty()) {

            return "";
        }

        StringBuilder result =
                new StringBuilder();

        String previousContent = null;

        for (DocumentChunk chunk : chunks) {
            if (chunk == null ||
                    chunk.getContent() == null ||
                    chunk.getContent().isBlank()) {

                continue;
            }

            String currentContent =
                    chunk.getContent();

            if (previousContent != null) {
                currentContent =
                        removeChunkOverlap(
                                previousContent,
                                currentContent
                        );
            }

            if (!currentContent.isBlank()) {
                if (!result.isEmpty()) {
                    result.append("\n\n");
                }

                result.append(
                        currentContent
                );
            }

            /*
             * Keep the original content here because the next
             * persisted chunk overlaps with the original chunk.
             */
            previousContent =
                    chunk.getContent();
        }

        return result
                .toString()
                .trim();
    }
    private String removeChunkOverlap(
            String previousContent,
            String currentContent
    ) {
        if (previousContent == null ||
                previousContent.isBlank() ||
                currentContent == null ||
                currentContent.isBlank()) {

            return currentContent;
        }

        int maximumOverlap =
                Math.min(
                        1000,
                        Math.min(
                                previousContent.length(),
                                currentContent.length()
                        )
                );

        final int minimumOverlap = 30;

        for (
                int overlapLength =
                maximumOverlap;
                overlapLength >=
                        minimumOverlap;
                overlapLength--
        ) {
            int previousStart =
                    previousContent.length()
                            - overlapLength;

            if (previousContent.regionMatches(
                    previousStart,
                    currentContent,
                    0,
                    overlapLength
            )) {
                return currentContent
                        .substring(
                                overlapLength
                        )
                        .stripLeading();
            }
        }

        return currentContent;
    }

    public List<GlobalDocumentResponse> getAccessibleDocuments(
            User currentUser
    ) {
        return documentRepository
                .findAllAccessibleByUser(currentUser)
                .stream()
                .map(this::toGlobalDocumentResponse)
                .toList();
    }

    private GlobalDocumentResponse toGlobalDocumentResponse(
            Document document
    ) {
        User uploadedBy =
                document.getUploadedBy();

        return new GlobalDocumentResponse(
                document.getId(),
                document.getWorkspace().getId(),
                document.getWorkspace().getName(),
                document.getTitle(),
                document.getOriginalFilename(),
                document.getFileType(),
                document.getFileSize(),
                document.getStatus(),
                uploadedBy == null
                        ? null
                        : uploadedBy.getId(),
                uploadedBy == null
                        ? null
                        : uploadedBy.getUsername(),
                document.getCreatedAt(),
                document.getUpdatedAt()
        );
    }
    private String resolveContentType(
            Document document,
            Path path
    ) {
        String storedContentType =
                document.getFileType();

        if (storedContentType != null &&
                !storedContentType.isBlank() &&
                !"application/octet-stream"
                        .equalsIgnoreCase(
                                storedContentType
                        )) {

            return storedContentType;
        }

        try {
            String detected =
                    Files.probeContentType(
                            path
                    );

            if (detected != null &&
                    !detected.isBlank()) {

                return detected;
            }

        } catch (IOException ignored) {
            // Fall through to the default.
        }

        return "application/octet-stream";
    }

    private String validateAndResolveFilename(
            MultipartFile file
    ) {
        if (file == null || file.isEmpty()) {
            throw new BadRequestException(
                    "Uploaded file cannot be empty"
            );
        }

        String suppliedFilename = file.getOriginalFilename();

        if (suppliedFilename == null ||
                suppliedFilename.isBlank()) {
            throw new BadRequestException(
                    "Uploaded file name is missing"
            );
        }

        String safeFilename = Path.of(suppliedFilename)
                .getFileName()
                .toString();

        String extension = getExtension(safeFilename);

        if (!SUPPORTED_EXTENSIONS.contains(extension)) {
            throw new BadRequestException(
                    "Unsupported document extension: " + extension
            );
        }

        return safeFilename;
    }

    private String getExtension(String filename) {
        int dotIndex = filename.lastIndexOf('.');

        if (dotIndex < 0 ||
                dotIndex == filename.length() - 1) {
            throw new BadRequestException(
                    "Document must have a supported file extension"
            );
        }

        return filename
                .substring(dotIndex + 1)
                .toLowerCase();
    }

    private String resolveFileType(MultipartFile file) {
        String contentType = file.getContentType();

        if (contentType == null || contentType.isBlank()) {
            return "application/octet-stream";
        }

        return contentType;
    }

    public record DocumentFile(
            Resource resource,
            String originalFilename,
            String contentType,
            Long fileSize
    ) {
    }
}