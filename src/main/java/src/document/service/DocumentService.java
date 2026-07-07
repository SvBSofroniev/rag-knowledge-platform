package src.document.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import src.document.dto.DocumentResponse;
import src.document.repository.DocumentRepository;
import src.document.util.DocumentStatus;
import src.entity.Document;
import src.entity.User;
import src.entity.Workspace;
import src.entity.WorkspaceMember;
import src.workspace.service.WorkspacePermissionService;
import src.workspace.util.WorkspaceRole;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class DocumentService {

    private final DocumentRepository documentRepository;
    private final FileStorageService fileStorageService;
    private final WorkspacePermissionService permissionService;

    @Transactional
    public DocumentResponse uploadDocument(
            UUID workspaceId,
            MultipartFile file,
            User currentUser
    ) {
        WorkspaceMember member = permissionService.requireMember(workspaceId, currentUser);
        Workspace workspace = member.getWorkspace();

        if (file.isEmpty()) {
            throw new RuntimeException("File is empty");
        }

        FileStorageService.StoredFile storedFile = fileStorageService.store(file);

        Document document = new Document();
        document.setWorkspace(workspace);
        document.setUploadedBy(currentUser);

        document.setTitle(file.getOriginalFilename());
        document.setOriginalFilename(file.getOriginalFilename());
        document.setStoragePath(storedFile.filePath());
        document.setFileType(file.getContentType());
        document.setFileSize(file.getSize());
        document.setStatus(DocumentStatus.UPLOADED);

        Document saved = documentRepository.save(document);

        return toResponse(saved);
    }

    public List<DocumentResponse> getWorkspaceDocuments(
            UUID workspaceId,
            User currentUser
    ) {
        WorkspaceMember member = permissionService.requireMember(workspaceId, currentUser);
        Workspace workspace = member.getWorkspace();

        return documentRepository.findByWorkspaceOrderByCreatedAtDesc(workspace)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    public DocumentResponse getDocument(
            UUID documentId,
            User currentUser
    ) {
        Document document = getDocumentOrThrow(documentId);

        permissionService.requireMember(
                document.getWorkspace().getId(),
                currentUser
        );

        return toResponse(document);
    }

    @Transactional
    public void deleteDocument(
            UUID documentId,
            User currentUser
    ) {
        Document document = getDocumentOrThrow(documentId);

        WorkspaceMember member = permissionService.requireMember(
                document.getWorkspace().getId(),
                currentUser
        );

        boolean isUploader = document.getUploadedBy().getId().equals(currentUser.getId());
        boolean isAdminOrOwner =
                member.getRole() == WorkspaceRole.ADMIN ||
                        member.getRole() == WorkspaceRole.OWNER;

        if (!isUploader && !isAdminOrOwner) {
            throw new RuntimeException("You do not have permission to delete this document");
        }

        fileStorageService.delete(document.getStoragePath());
        documentRepository.delete(document);
    }

    private Document getDocumentOrThrow(UUID documentId) {
        return documentRepository.findById(documentId)
                .orElseThrow(() -> new RuntimeException("Document not found"));
    }

    private DocumentResponse toResponse(Document document) {
        return new DocumentResponse(
                document.getId(),
                document.getOriginalFilename(),
                document.getFileType(),
                document.getFileSize(),
                document.getStatus(),
                document.getUploadedBy().getId(),
                document.getUploadedBy().getUsername(),
                document.getCreatedAt()
        );
    }
}