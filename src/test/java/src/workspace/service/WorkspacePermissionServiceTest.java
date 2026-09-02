package src.workspace.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import src.common.exception.ForbiddenOperationException;
import src.common.exception.ResourceNotFoundException;
import src.entity.User;
import src.entity.Workspace;
import src.entity.WorkspaceMember;
import src.workspace.repository.WorkspaceMemberRepository;
import src.workspace.repository.WorkspaceRepository;
import src.workspace.util.WorkspaceRole;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class WorkspacePermissionServiceTest {

    @Mock
    private WorkspaceRepository
            workspaceRepository;

    @Mock
    private WorkspaceMemberRepository
            memberRepository;

    @Mock
    private Workspace
            workspace;

    @Mock
    private User
            user;

    @Mock
    private WorkspaceMember
            workspaceMember;

    private WorkspacePermissionService
            permissionService;

    private UUID workspaceId;

    @BeforeEach
    void setUp() {
        permissionService =
                new WorkspacePermissionService(
                        workspaceRepository,
                        memberRepository
                );

        workspaceId =
                UUID.randomUUID();
    }

    @Nested
    class GetWorkspaceOrThrow {

        @Test
        void shouldReturnWorkspaceWhenWorkspaceExists() {
            when(
                    workspaceRepository.findById(
                            workspaceId
                    )
            ).thenReturn(
                    Optional.of(
                            workspace
                    )
            );

            Workspace result =
                    permissionService
                            .getWorkspaceOrThrow(
                                    workspaceId
                            );

            assertSame(
                    workspace,
                    result
            );

            verify(
                    workspaceRepository
            ).findById(
                    workspaceId
            );
        }

        @Test
        void shouldThrowWhenWorkspaceDoesNotExist() {
            when(
                    workspaceRepository.findById(
                            workspaceId
                    )
            ).thenReturn(
                    Optional.empty()
            );

            assertThrows(
                    ResourceNotFoundException.class,
                    () ->
                            permissionService
                                    .getWorkspaceOrThrow(
                                            workspaceId
                                    )
            );

            verify(
                    workspaceRepository
            ).findById(
                    workspaceId
            );

            verifyNoInteractions(
                    memberRepository
            );
        }
    }

    @Nested
    class GetMemberOrThrow {

        @Test
        void shouldReturnMemberWhenUserBelongsToWorkspace() {
            when(
                    memberRepository
                            .findByWorkspaceAndUser(
                                    workspace,
                                    user
                            )
            ).thenReturn(
                    Optional.of(
                            workspaceMember
                    )
            );

            WorkspaceMember result =
                    permissionService
                            .getMemberOrThrow(
                                    workspace,
                                    user
                            );

            assertSame(
                    workspaceMember,
                    result
            );

            verify(
                    memberRepository
            ).findByWorkspaceAndUser(
                    workspace,
                    user
            );
        }

        @Test
        void shouldThrowNotFoundWhenUserIsNotWorkspaceMember() {
            when(
                    memberRepository
                            .findByWorkspaceAndUser(
                                    workspace,
                                    user
                            )
            ).thenReturn(
                    Optional.empty()
            );

            assertThrows(
                    ResourceNotFoundException.class,
                    () ->
                            permissionService
                                    .getMemberOrThrow(
                                            workspace,
                                            user
                                    )
            );

            verify(
                    memberRepository
            ).findByWorkspaceAndUser(
                    workspace,
                    user
            );
        }
    }

    @Nested
    class RequireMember {

        @Test
        void shouldReturnMemberWhenUserBelongsToWorkspace() {
            when(
                    workspaceRepository.findById(
                            workspaceId
                    )
            ).thenReturn(
                    Optional.of(
                            workspace
                    )
            );

            when(
                    memberRepository
                            .findByWorkspaceAndUser(
                                    workspace,
                                    user
                            )
            ).thenReturn(
                    Optional.of(
                            workspaceMember
                    )
            );

            WorkspaceMember result =
                    permissionService
                            .requireMember(
                                    workspaceId,
                                    user
                            );

            assertSame(
                    workspaceMember,
                    result
            );

            verify(
                    workspaceRepository
            ).findById(
                    workspaceId
            );

            verify(
                    memberRepository
            ).findByWorkspaceAndUser(
                    workspace,
                    user
            );
        }

        @Test
        void shouldThrowNotFoundWhenWorkspaceDoesNotExist() {
            when(
                    workspaceRepository.findById(
                            workspaceId
                    )
            ).thenReturn(
                    Optional.empty()
            );

            assertThrows(
                    ResourceNotFoundException.class,
                    () ->
                            permissionService
                                    .requireMember(
                                            workspaceId,
                                            user
                                    )
            );

            verify(
                    memberRepository,
                    never()
            ).findByWorkspaceAndUser(
                    workspace,
                    user
            );
        }

        @Test
        void shouldThrowNotFoundWhenWorkspaceExistsButUserIsNotMember() {
            when(
                    workspaceRepository.findById(
                            workspaceId
                    )
            ).thenReturn(
                    Optional.of(
                            workspace
                    )
            );

            when(
                    memberRepository
                            .findByWorkspaceAndUser(
                                    workspace,
                                    user
                            )
            ).thenReturn(
                    Optional.empty()
            );

            assertThrows(
                    ResourceNotFoundException.class,
                    () ->
                            permissionService
                                    .requireMember(
                                            workspaceId,
                                            user
                                    )
            );
        }
    }

    @Nested
    class RequireAdminOrOwner {

        @Test
        void shouldAllowOwner() {
            prepareMember(
                    WorkspaceRole.OWNER
            );

            WorkspaceMember result =
                    permissionService
                            .requireAdminOrOwner(
                                    workspaceId,
                                    user
                            );

            assertSame(
                    workspaceMember,
                    result
            );
        }

        @Test
        void shouldAllowAdmin() {
            prepareMember(
                    WorkspaceRole.ADMIN
            );

            WorkspaceMember result =
                    permissionService
                            .requireAdminOrOwner(
                                    workspaceId,
                                    user
                            );

            assertSame(
                    workspaceMember,
                    result
            );
        }

        @Test
        void shouldRejectMember() {
            prepareMember(
                    WorkspaceRole.MEMBER
            );

            assertThrows(
                    ForbiddenOperationException.class,
                    () ->
                            permissionService
                                    .requireAdminOrOwner(
                                            workspaceId,
                                            user
                                    )
            );
        }
    }

    @Nested
    class RequireOwner {

        @Test
        void shouldAllowOwner() {
            prepareMember(
                    WorkspaceRole.OWNER
            );

            WorkspaceMember result =
                    permissionService
                            .requireOwner(
                                    workspaceId,
                                    user
                            );

            assertSame(
                    workspaceMember,
                    result
            );
        }

        @Test
        void shouldRejectAdmin() {
            prepareMember(
                    WorkspaceRole.ADMIN
            );

            assertThrows(
                    ForbiddenOperationException.class,
                    () ->
                            permissionService
                                    .requireOwner(
                                            workspaceId,
                                            user
                                    )
            );
        }

        @Test
        void shouldRejectMember() {
            prepareMember(
                    WorkspaceRole.MEMBER
            );

            assertThrows(
                    ForbiddenOperationException.class,
                    () ->
                            permissionService
                                    .requireOwner(
                                            workspaceId,
                                            user
                                    )
            );
        }
    }

    @Nested
    class IsMember {

        @Test
        void shouldReturnTrueWhenUserIsMember() {
            when(
                    workspaceRepository.findById(
                            workspaceId
                    )
            ).thenReturn(
                    Optional.of(
                            workspace
                    )
            );

            when(
                    memberRepository
                            .existsByWorkspaceAndUser(
                                    workspace,
                                    user
                            )
            ).thenReturn(
                    true
            );

            boolean result =
                    permissionService
                            .isMember(
                                    workspaceId,
                                    user
                            );

            assertTrue(
                    result
            );
        }

        @Test
        void shouldReturnFalseWhenUserIsNotMember() {
            when(
                    workspaceRepository.findById(
                            workspaceId
                    )
            ).thenReturn(
                    Optional.of(
                            workspace
                    )
            );

            when(
                    memberRepository
                            .existsByWorkspaceAndUser(
                                    workspace,
                                    user
                            )
            ).thenReturn(
                    false
            );

            boolean result =
                    permissionService
                            .isMember(
                                    workspaceId,
                                    user
                            );

            assertFalse(
                    result
            );
        }

        @Test
        void shouldThrowWhenWorkspaceDoesNotExist() {
            when(
                    workspaceRepository.findById(
                            workspaceId
                    )
            ).thenReturn(
                    Optional.empty()
            );

            assertThrows(
                    ResourceNotFoundException.class,
                    () ->
                            permissionService
                                    .isMember(
                                            workspaceId,
                                            user
                                    )
            );

            verify(
                    memberRepository,
                    never()
            ).existsByWorkspaceAndUser(
                    workspace,
                    user
            );
        }
    }

    private void prepareMember(
            WorkspaceRole role
    ) {
        when(
                workspaceRepository.findById(
                        workspaceId
                )
        ).thenReturn(
                Optional.of(
                        workspace
                )
        );

        when(
                memberRepository
                        .findByWorkspaceAndUser(
                                workspace,
                                user
                        )
        ).thenReturn(
                Optional.of(
                        workspaceMember
                )
        );

        when(
                workspaceMember.getRole()
        ).thenReturn(
                role
        );
    }
}