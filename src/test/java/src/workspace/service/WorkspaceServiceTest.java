package src.workspace.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import src.auth.repository.UserRepository;
import src.common.exception.BadRequestException;
import src.common.exception.ConflictException;
import src.common.exception.ForbiddenOperationException;
import src.common.exception.ResourceNotFoundException;
import src.entity.User;
import src.entity.Workspace;
import src.entity.WorkspaceMember;
import src.workspace.dto.AddWorkspaceMemberRequest;
import src.workspace.dto.CreateWorkspaceRequest;
import src.workspace.dto.UpdateWorkspaceRequest;
import src.workspace.dto.UpdateWorkspaceRoleRequest;
import src.workspace.dto.WorkspaceMemberResponse;
import src.workspace.dto.WorkspaceResponse;
import src.workspace.repository.WorkspaceMemberRepository;
import src.workspace.repository.WorkspaceRepository;
import src.workspace.util.WorkspaceRole;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class WorkspaceServiceTest {

    @Mock
    private WorkspaceRepository workspaceRepository;

    @Mock
    private WorkspaceMemberRepository memberRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private WorkspacePermissionService permissionService;

    @Mock
    private User currentUser;

    @Mock
    private User targetUser;

    @Mock
    private Workspace workspace;

    @Mock
    private WorkspaceMember currentMember;

    @Mock
    private WorkspaceMember targetMember;

    private WorkspaceService workspaceService;

    private UUID workspaceId;
    private UUID memberId;
    private UUID targetUserId;

    @BeforeEach
    void setUp() {
        workspaceService =
                new WorkspaceService(
                        workspaceRepository,
                        memberRepository,
                        userRepository,
                        permissionService
                );

        workspaceId = UUID.randomUUID();
        memberId = UUID.randomUUID();
        targetUserId = UUID.randomUUID();
    }

    @Nested
    class CreateWorkspace {

        @Test
        void shouldCreateWorkspaceWithCurrentUserAsOwner() {
            CreateWorkspaceRequest request =
                    mock(CreateWorkspaceRequest.class);

            UUID savedWorkspaceId =
                    UUID.randomUUID();

            when(request.name())
                    .thenReturn("Master Project");

            when(request.description())
                    .thenReturn("OurVault workspace");

            when(workspaceRepository.save(
                    any(Workspace.class)
            )).thenAnswer(invocation -> {
                Workspace saved =
                        invocation.getArgument(0);

                saved.setId(
                        savedWorkspaceId
                );

                return saved;
            });

            WorkspaceResponse response =
                    workspaceService.createWorkspace(
                            request,
                            currentUser
                    );

            ArgumentCaptor<Workspace> captor =
                    ArgumentCaptor.forClass(
                            Workspace.class
                    );

            verify(workspaceRepository)
                    .save(
                            captor.capture()
                    );

            Workspace created =
                    captor.getValue();

            assertEquals(
                    "Master Project",
                    created.getName()
            );

            assertEquals(
                    "OurVault workspace",
                    created.getDescription()
            );

            assertSame(
                    currentUser,
                    created.getCreatedBy()
            );

            assertEquals(
                    1,
                    created.getMembers().size()
            );

            WorkspaceMember owner =
                    created.getMembers()
                            .iterator()
                            .next();

            assertSame(
                    created,
                    owner.getWorkspace()
            );

            assertSame(
                    currentUser,
                    owner.getUser()
            );

            assertEquals(
                    WorkspaceRole.OWNER,
                    owner.getRole()
            );

            assertEquals(
                    savedWorkspaceId,
                    response.id()
            );

            assertEquals(
                    "Master Project",
                    response.name()
            );

            assertEquals(
                    "OurVault workspace",
                    response.description()
            );

            assertEquals(
                    WorkspaceRole.OWNER,
                    response.myRole()
            );
        }
    }

    @Nested
    class GetMyWorkspaces {

        @Test
        void shouldReturnWorkspacesWhereUserIsMember() {
            Workspace workspaceOne =
                    mock(Workspace.class);

            Workspace workspaceTwo =
                    mock(Workspace.class);

            WorkspaceMember memberOne =
                    mock(WorkspaceMember.class);

            WorkspaceMember memberTwo =
                    mock(WorkspaceMember.class);

            UUID workspaceOneId =
                    UUID.randomUUID();

            UUID workspaceTwoId =
                    UUID.randomUUID();

            when(memberRepository.findByUser(
                    currentUser
            )).thenReturn(
                    List.of(
                            memberOne,
                            memberTwo
                    )
            );

            when(memberOne.getWorkspace())
                    .thenReturn(workspaceOne);

            when(memberOne.getRole())
                    .thenReturn(WorkspaceRole.OWNER);

            when(workspaceOne.getId())
                    .thenReturn(workspaceOneId);

            when(workspaceOne.getName())
                    .thenReturn("Workspace One");

            when(workspaceOne.getDescription())
                    .thenReturn("First");

            when(memberTwo.getWorkspace())
                    .thenReturn(workspaceTwo);

            when(memberTwo.getRole())
                    .thenReturn(WorkspaceRole.MEMBER);

            when(workspaceTwo.getId())
                    .thenReturn(workspaceTwoId);

            when(workspaceTwo.getName())
                    .thenReturn("Workspace Two");

            when(workspaceTwo.getDescription())
                    .thenReturn("Second");

            List<WorkspaceResponse> result =
                    workspaceService
                            .getMyWorkspaces(
                                    currentUser
                            );

            assertEquals(
                    2,
                    result.size()
            );

            assertEquals(
                    workspaceOneId,
                    result.get(0).id()
            );

            assertEquals(
                    WorkspaceRole.OWNER,
                    result.get(0).myRole()
            );

            assertEquals(
                    workspaceTwoId,
                    result.get(1).id()
            );

            assertEquals(
                    WorkspaceRole.MEMBER,
                    result.get(1).myRole()
            );

            verify(memberRepository)
                    .findByUser(
                            currentUser
                    );
        }

        @Test
        void shouldReturnEmptyListWhenUserHasNoWorkspaces() {
            when(memberRepository.findByUser(
                    currentUser
            )).thenReturn(
                    List.of()
            );

            List<WorkspaceResponse> result =
                    workspaceService
                            .getMyWorkspaces(
                                    currentUser
                            );

            assertTrue(
                    result.isEmpty()
            );
        }
    }

    @Nested
    class GetWorkspace {

        @Test
        void shouldReturnWorkspaceForMember() {
            when(permissionService.requireMember(
                    workspaceId,
                    currentUser
            )).thenReturn(
                    currentMember
            );

            when(currentMember.getWorkspace())
                    .thenReturn(workspace);

            when(currentMember.getRole())
                    .thenReturn(WorkspaceRole.ADMIN);

            when(workspace.getId())
                    .thenReturn(workspaceId);

            when(workspace.getName())
                    .thenReturn("OurVault");

            when(workspace.getDescription())
                    .thenReturn("Master project");

            WorkspaceResponse response =
                    workspaceService.getWorkspace(
                            workspaceId,
                            currentUser
                    );

            assertEquals(
                    workspaceId,
                    response.id()
            );

            assertEquals(
                    "OurVault",
                    response.name()
            );

            assertEquals(
                    WorkspaceRole.ADMIN,
                    response.myRole()
            );

            verify(permissionService)
                    .requireMember(
                            workspaceId,
                            currentUser
                    );
        }
    }

    @Nested
    class AddMember {

        @Test
        void shouldAddUserAsMember() {
            AddWorkspaceMemberRequest request =
                    mock(AddWorkspaceMemberRequest.class);

            when(request.userId())
                    .thenReturn(targetUserId);

            when(permissionService
                    .requireAdminOrOwner(
                            workspaceId,
                            currentUser
                    ))
                    .thenReturn(currentMember);

            when(currentMember.getWorkspace())
                    .thenReturn(workspace);

            when(userRepository.findById(
                    targetUserId
            )).thenReturn(
                    Optional.of(targetUser)
            );

            when(memberRepository
                    .existsByWorkspaceAndUser(
                            workspace,
                            targetUser
                    ))
                    .thenReturn(false);

            workspaceService.addMember(
                    workspaceId,
                    request,
                    currentUser
            );

            ArgumentCaptor<WorkspaceMember> captor =
                    ArgumentCaptor.forClass(
                            WorkspaceMember.class
                    );

            verify(memberRepository)
                    .save(
                            captor.capture()
                    );

            WorkspaceMember savedMember =
                    captor.getValue();

            assertSame(
                    workspace,
                    savedMember.getWorkspace()
            );

            assertSame(
                    targetUser,
                    savedMember.getUser()
            );

            assertEquals(
                    WorkspaceRole.MEMBER,
                    savedMember.getRole()
            );
        }

        @Test
        void shouldRejectUnknownUser() {
            AddWorkspaceMemberRequest request =
                    mock(AddWorkspaceMemberRequest.class);

            when(request.userId())
                    .thenReturn(targetUserId);

            when(permissionService
                    .requireAdminOrOwner(
                            workspaceId,
                            currentUser
                    ))
                    .thenReturn(currentMember);

            when(currentMember.getWorkspace())
                    .thenReturn(workspace);

            when(userRepository.findById(
                    targetUserId
            )).thenReturn(
                    Optional.empty()
            );

            assertThrows(
                    ResourceNotFoundException.class,
                    () ->
                            workspaceService.addMember(
                                    workspaceId,
                                    request,
                                    currentUser
                            )
            );

            verify(memberRepository, never())
                    .save(
                            any()
                    );
        }

        @Test
        void shouldRejectDuplicateWorkspaceMember() {
            AddWorkspaceMemberRequest request =
                    mock(AddWorkspaceMemberRequest.class);

            when(request.userId())
                    .thenReturn(targetUserId);

            when(permissionService
                    .requireAdminOrOwner(
                            workspaceId,
                            currentUser
                    ))
                    .thenReturn(currentMember);

            when(currentMember.getWorkspace())
                    .thenReturn(workspace);

            when(userRepository.findById(
                    targetUserId
            )).thenReturn(
                    Optional.of(targetUser)
            );

            when(memberRepository
                    .existsByWorkspaceAndUser(
                            workspace,
                            targetUser
                    ))
                    .thenReturn(true);

            assertThrows(
                    ConflictException.class,
                    () ->
                            workspaceService.addMember(
                                    workspaceId,
                                    request,
                                    currentUser
                            )
            );

            verify(memberRepository, never())
                    .save(
                            any()
                    );
        }
    }

    @Nested
    class UpdateMemberRole {

        @Test
        void shouldAllowOwnerToChangeMemberRole() {
            UpdateWorkspaceRoleRequest request =
                    mock(UpdateWorkspaceRoleRequest.class);

            when(request.role())
                    .thenReturn(WorkspaceRole.ADMIN);

            when(memberRepository.findById(
                    memberId
            )).thenReturn(
                    Optional.of(targetMember)
            );

            when(targetMember.getWorkspace())
                    .thenReturn(workspace);

            when(workspace.getId())
                    .thenReturn(workspaceId);

            when(targetMember.getRole())
                    .thenReturn(WorkspaceRole.MEMBER);

            workspaceService.updateMemberRole(
                    workspaceId,
                    memberId,
                    request,
                    currentUser
            );

            verify(permissionService)
                    .requireOwner(
                            workspaceId,
                            currentUser
                    );

            verify(targetMember)
                    .setRole(
                            WorkspaceRole.ADMIN
                    );

            verify(memberRepository)
                    .save(
                            targetMember
                    );
        }

        @Test
        void shouldRejectUnknownMember() {
            UpdateWorkspaceRoleRequest request =
                    mock(UpdateWorkspaceRoleRequest.class);

            when(memberRepository.findById(
                    memberId
            )).thenReturn(
                    Optional.empty()
            );

            assertThrows(
                    ResourceNotFoundException.class,
                    () ->
                            workspaceService
                                    .updateMemberRole(
                                            workspaceId,
                                            memberId,
                                            request,
                                            currentUser
                                    )
            );

            verify(permissionService)
                    .requireOwner(
                            workspaceId,
                            currentUser
                    );

            verify(memberRepository, never())
                    .save(
                            any()
                    );
        }

        @Test
        void shouldRejectMemberFromDifferentWorkspace() {
            UUID otherWorkspaceId =
                    UUID.randomUUID();

            Workspace otherWorkspace =
                    mock(Workspace.class);

            UpdateWorkspaceRoleRequest request =
                    mock(UpdateWorkspaceRoleRequest.class);

            when(memberRepository.findById(
                    memberId
            )).thenReturn(
                    Optional.of(targetMember)
            );

            when(targetMember.getWorkspace())
                    .thenReturn(otherWorkspace);

            when(otherWorkspace.getId())
                    .thenReturn(otherWorkspaceId);

            assertThrows(
                    ResourceNotFoundException.class,
                    () ->
                            workspaceService
                                    .updateMemberRole(
                                            workspaceId,
                                            memberId,
                                            request,
                                            currentUser
                                    )
            );

            verify(memberRepository, never())
                    .save(
                            any()
                    );
        }

        @Test
        void shouldNotAllowOwnerRoleToBeChanged() {
            UpdateWorkspaceRoleRequest request =
                    mock(UpdateWorkspaceRoleRequest.class);

            when(memberRepository.findById(
                    memberId
            )).thenReturn(
                    Optional.of(targetMember)
            );

            when(targetMember.getWorkspace())
                    .thenReturn(workspace);

            when(workspace.getId())
                    .thenReturn(workspaceId);

            when(targetMember.getRole())
                    .thenReturn(WorkspaceRole.OWNER);

            assertThrows(
                    ForbiddenOperationException.class,
                    () ->
                            workspaceService
                                    .updateMemberRole(
                                            workspaceId,
                                            memberId,
                                            request,
                                            currentUser
                                    )
            );

            verify(memberRepository, never())
                    .save(
                            any()
                    );
        }

        @Test
        void shouldRejectAssigningOwnerRole() {
            UpdateWorkspaceRoleRequest request =
                    mock(UpdateWorkspaceRoleRequest.class);

            when(request.role())
                    .thenReturn(WorkspaceRole.OWNER);

            when(memberRepository.findById(
                    memberId
            )).thenReturn(
                    Optional.of(targetMember)
            );

            when(targetMember.getWorkspace())
                    .thenReturn(workspace);

            when(workspace.getId())
                    .thenReturn(workspaceId);

            when(targetMember.getRole())
                    .thenReturn(WorkspaceRole.MEMBER);

            assertThrows(
                    BadRequestException.class,
                    () ->
                            workspaceService
                                    .updateMemberRole(
                                            workspaceId,
                                            memberId,
                                            request,
                                            currentUser
                                    )
            );

            verify(targetMember, never())
                    .setRole(
                            any()
                    );

            verify(memberRepository, never())
                    .save(
                            any()
                    );
        }

        @Test
        void shouldRejectUnchangedRole() {
            UpdateWorkspaceRoleRequest request =
                    mock(UpdateWorkspaceRoleRequest.class);

            when(request.role())
                    .thenReturn(WorkspaceRole.MEMBER);

            when(memberRepository.findById(
                    memberId
            )).thenReturn(
                    Optional.of(targetMember)
            );

            when(targetMember.getWorkspace())
                    .thenReturn(workspace);

            when(workspace.getId())
                    .thenReturn(workspaceId);

            when(targetMember.getRole())
                    .thenReturn(WorkspaceRole.MEMBER);

            assertThrows(
                    ConflictException.class,
                    () ->
                            workspaceService
                                    .updateMemberRole(
                                            workspaceId,
                                            memberId,
                                            request,
                                            currentUser
                                    )
            );

            verify(targetMember, never())
                    .setRole(
                            any()
                    );

            verify(memberRepository, never())
                    .save(
                            any()
                    );
        }
    }

    @Nested
    class RemoveMember {

        @Test
        void shouldAllowAdminToRemoveMember() {
            prepareRemoval(
                    WorkspaceRole.ADMIN,
                    WorkspaceRole.MEMBER
            );

            workspaceService.removeMember(
                    workspaceId,
                    memberId,
                    currentUser
            );

            verify(memberRepository)
                    .delete(
                            targetMember
                    );
        }

        @Test
        void shouldAllowOwnerToRemoveAdmin() {
            prepareRemoval(
                    WorkspaceRole.OWNER,
                    WorkspaceRole.ADMIN
            );

            workspaceService.removeMember(
                    workspaceId,
                    memberId,
                    currentUser
            );

            verify(memberRepository)
                    .delete(
                            targetMember
                    );
        }

        @Test
        void shouldRejectAdminRemovingAnotherAdmin() {
            prepareRemoval(
                    WorkspaceRole.ADMIN,
                    WorkspaceRole.ADMIN
            );

            assertThrows(
                    ForbiddenOperationException.class,
                    () ->
                            workspaceService.removeMember(
                                    workspaceId,
                                    memberId,
                                    currentUser
                            )
            );

            verify(memberRepository, never())
                    .delete(
                            any()
                    );
        }

        @Test
        void shouldRejectRemovingWorkspaceOwner() {
            when(
                    permissionService.requireAdminOrOwner(
                            workspaceId,
                            currentUser
                    )
            ).thenReturn(
                    currentMember
            );

            when(
                    memberRepository.findById(
                            memberId
                    )
            ).thenReturn(
                    Optional.of(
                            targetMember
                    )
            );

            when(
                    targetMember.getWorkspace()
            ).thenReturn(
                    workspace
            );

            when(
                    workspace.getId()
            ).thenReturn(
                    workspaceId
            );

            when(
                    targetMember.getRole()
            ).thenReturn(
                    WorkspaceRole.OWNER
            );

            assertThrows(
                    ForbiddenOperationException.class,
                    () ->
                            workspaceService.removeMember(
                                    workspaceId,
                                    memberId,
                                    currentUser
                            )
            );

            verify(
                    memberRepository,
                    never()
            ).delete(
                    any()
            );
        }

        @Test
        void shouldRejectMemberFromDifferentWorkspace() {
            Workspace otherWorkspace =
                    mock(Workspace.class);

            when(permissionService
                    .requireAdminOrOwner(
                            workspaceId,
                            currentUser
                    ))
                    .thenReturn(currentMember);

            when(memberRepository.findById(
                    memberId
            )).thenReturn(
                    Optional.of(targetMember)
            );

            when(targetMember.getWorkspace())
                    .thenReturn(otherWorkspace);

            when(otherWorkspace.getId())
                    .thenReturn(
                            UUID.randomUUID()
                    );

            assertThrows(
                    ResourceNotFoundException.class,
                    () ->
                            workspaceService.removeMember(
                                    workspaceId,
                                    memberId,
                                    currentUser
                            )
            );

            verify(memberRepository, never())
                    .delete(
                            any()
                    );
        }

        @Test
        void shouldRejectUnknownMember() {
            when(permissionService
                    .requireAdminOrOwner(
                            workspaceId,
                            currentUser
                    ))
                    .thenReturn(currentMember);

            when(memberRepository.findById(
                    memberId
            )).thenReturn(
                    Optional.empty()
            );

            assertThrows(
                    ResourceNotFoundException.class,
                    () ->
                            workspaceService.removeMember(
                                    workspaceId,
                                    memberId,
                                    currentUser
                            )
            );

            verify(memberRepository, never())
                    .delete(
                            any()
                    );
        }
    }

    @Nested
    class GetWorkspaceMembers {

        @Test
        void shouldReturnWorkspaceMembers() {
            WorkspaceMember listedMember =
                    mock(WorkspaceMember.class);

            User listedUser =
                    mock(User.class);

            UUID workspaceMemberId =
                    UUID.randomUUID();

            UUID userId =
                    UUID.randomUUID();

            LocalDateTime joinedAt =
                    LocalDateTime.of(
                            2026,
                            9,
                            1,
                            12,
                            0
                    );

            when(permissionService.requireMember(
                    workspaceId,
                    currentUser
            )).thenReturn(
                    currentMember
            );

            when(currentMember.getWorkspace())
                    .thenReturn(workspace);

            when(memberRepository.findByWorkspace(
                    workspace
            )).thenReturn(
                    List.of(listedMember)
            );

            when(listedMember.getId())
                    .thenReturn(workspaceMemberId);

            when(listedMember.getUser())
                    .thenReturn(listedUser);

            when(listedMember.getRole())
                    .thenReturn(WorkspaceRole.MEMBER);

            when(listedMember.getJoinedAt())
                    .thenReturn(joinedAt);

            when(listedUser.getId())
                    .thenReturn(userId);

            when(listedUser.getUsername())
                    .thenReturn("member");

            when(listedUser.getEmail())
                    .thenReturn("member@example.com");

            List<WorkspaceMemberResponse> result =
                    workspaceService
                            .getWorkspaceMembers(
                                    workspaceId,
                                    currentUser
                            );

            assertEquals(
                    1,
                    result.size()
            );

            WorkspaceMemberResponse response =
                    result.get(0);

            assertEquals(
                    workspaceMemberId,
                    response.memberId()
            );

            assertEquals(
                    userId,
                    response.userId()
            );

            assertEquals(
                    "member",
                    response.username()
            );

            assertEquals(
                    "member@example.com",
                    response.email()
            );

            assertEquals(
                    WorkspaceRole.MEMBER,
                    response.role()
            );

            assertEquals(
                    joinedAt,
                    response.joinedAt()
            );
        }
    }

    @Nested
    class UpdateWorkspace {

        @Test
        void shouldTrimAndUpdateWorkspace() {
            UpdateWorkspaceRequest request =
                    mock(UpdateWorkspaceRequest.class);

            when(permissionService
                    .requireAdminOrOwner(
                            workspaceId,
                            currentUser
                    ))
                    .thenReturn(currentMember);

            when(currentMember.getWorkspace())
                    .thenReturn(workspace);

            when(currentMember.getRole())
                    .thenReturn(WorkspaceRole.ADMIN);

            when(request.name())
                    .thenReturn(
                            "   Updated Workspace   "
                    );

            when(request.description())
                    .thenReturn(
                            "   Updated description   "
                    );

            when(workspaceRepository.save(
                    workspace
            )).thenReturn(
                    workspace
            );

            when(workspace.getId())
                    .thenReturn(workspaceId);

            when(workspace.getName())
                    .thenReturn("Updated Workspace");

            when(workspace.getDescription())
                    .thenReturn("Updated description");

            WorkspaceResponse response =
                    workspaceService
                            .updateWorkspace(
                                    workspaceId,
                                    request,
                                    currentUser
                            );

            verify(workspace)
                    .setName(
                            "Updated Workspace"
                    );

            verify(workspace)
                    .setDescription(
                            "Updated description"
                    );

            verify(workspaceRepository)
                    .save(
                            workspace
                    );

            assertEquals(
                    workspaceId,
                    response.id()
            );

            assertEquals(
                    WorkspaceRole.ADMIN,
                    response.myRole()
            );
        }

        @Test
        void shouldConvertBlankDescriptionToNull() {
            UpdateWorkspaceRequest request =
                    mock(UpdateWorkspaceRequest.class);

            when(permissionService
                    .requireAdminOrOwner(
                            workspaceId,
                            currentUser
                    ))
                    .thenReturn(currentMember);

            when(currentMember.getWorkspace())
                    .thenReturn(workspace);

            when(request.name())
                    .thenReturn("Workspace");

            when(request.description())
                    .thenReturn("     ");

            when(workspaceRepository.save(
                    workspace
            )).thenReturn(
                    workspace
            );

            workspaceService.updateWorkspace(
                    workspaceId,
                    request,
                    currentUser
            );

            verify(workspace)
                    .setDescription(
                            null
                    );
        }

        @Test
        void shouldAllowNullDescription() {
            UpdateWorkspaceRequest request =
                    mock(UpdateWorkspaceRequest.class);

            when(permissionService
                    .requireAdminOrOwner(
                            workspaceId,
                            currentUser
                    ))
                    .thenReturn(currentMember);

            when(currentMember.getWorkspace())
                    .thenReturn(workspace);

            when(request.name())
                    .thenReturn("Workspace");

            when(request.description())
                    .thenReturn(null);

            when(workspaceRepository.save(
                    workspace
            )).thenReturn(
                    workspace
            );

            assertDoesNotThrow(
                    () ->
                            workspaceService
                                    .updateWorkspace(
                                            workspaceId,
                                            request,
                                            currentUser
                                    )
            );

            verify(workspace)
                    .setDescription(
                            null
                    );
        }

        @Test
        void shouldRejectBlankWorkspaceName() {
            UpdateWorkspaceRequest request =
                    mock(UpdateWorkspaceRequest.class);

            when(permissionService
                    .requireAdminOrOwner(
                            workspaceId,
                            currentUser
                    ))
                    .thenReturn(currentMember);

            when(currentMember.getWorkspace())
                    .thenReturn(workspace);

            when(request.name())
                    .thenReturn("     ");

            assertThrows(
                    BadRequestException.class,
                    () ->
                            workspaceService
                                    .updateWorkspace(
                                            workspaceId,
                                            request,
                                            currentUser
                                    )
            );

            verify(workspaceRepository, never())
                    .save(
                            any()
                    );
        }
    }

    @Nested
    class DeleteWorkspace {

        @Test
        void shouldAllowOwnerToDeleteWorkspace() {
            when(permissionService.requireOwner(
                    workspaceId,
                    currentUser
            )).thenReturn(
                    currentMember
            );

            when(currentMember.getWorkspace())
                    .thenReturn(workspace);

            workspaceService.deleteWorkspace(
                    workspaceId,
                    currentUser
            );

            verify(permissionService)
                    .requireOwner(
                            workspaceId,
                            currentUser
                    );

            verify(workspaceRepository)
                    .delete(
                            workspace
                    );
        }
    }

    private void prepareRemoval(
            WorkspaceRole currentRole,
            WorkspaceRole targetRole
    ) {
        when(permissionService
                .requireAdminOrOwner(
                        workspaceId,
                        currentUser
                ))
                .thenReturn(currentMember);

        when(currentMember.getRole())
                .thenReturn(currentRole);

        when(memberRepository.findById(
                memberId
        )).thenReturn(
                Optional.of(targetMember)
        );

        when(targetMember.getWorkspace())
                .thenReturn(workspace);

        when(workspace.getId())
                .thenReturn(workspaceId);

        when(targetMember.getRole())
                .thenReturn(targetRole);
    }
}