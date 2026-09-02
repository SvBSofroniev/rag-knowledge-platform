package src.workspace.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import src.common.exception.ApiErrorCodes;
import src.common.exception.BadRequestException;
import src.common.exception.ResourceNotFoundException;
import src.entity.User;
import src.entity.Workspace;
import src.entity.WorkspaceMember;
import src.workspace.dto.SendWorkspaceMessageRequest;
import src.workspace.dto.WorkspaceMessageResponse;
import src.workspace.entity.WorkspaceMessage;
import src.workspace.repository.WorkspaceMessageRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class WorkspaceMessageServiceTest {

    @Mock
    private WorkspaceMessageRepository
            workspaceMessageRepository;

    @Mock
    private WorkspacePermissionService
            workspacePermissionService;

    @Mock
    private Workspace
            workspace;

    @Mock
    private WorkspaceMember
            workspaceMember;

    @Mock
    private User
            currentUser;

    private WorkspaceMessageService
            workspaceMessageService;

    private UUID workspaceId;
    private UUID currentUserId;

    @BeforeEach
    void setUp() {
        workspaceMessageService =
                new WorkspaceMessageService(
                        workspaceMessageRepository,
                        workspacePermissionService
                );

        workspaceId =
                UUID.randomUUID();

        currentUserId =
                UUID.randomUUID();
    }

    @Nested
    class GetMessages {

        @Test
        void shouldRequireWorkspaceMembership() {
            prepareMembership();

            when(
                    workspaceMessageRepository
                            .findTop100ByWorkspaceOrderByCreatedAtDesc(
                                    workspace
                            )
            ).thenReturn(
                    List.of()
            );

            workspaceMessageService
                    .getMessages(
                            workspaceId,
                            currentUser
                    );

            verify(
                    workspacePermissionService
            ).requireMember(
                    workspaceId,
                    currentUser
            );

            verify(
                    workspaceMessageRepository
            ).findTop100ByWorkspaceOrderByCreatedAtDesc(
                    workspace
            );
        }

        @Test
        void shouldReturnMessagesOldestFirst() {
            prepareMembership();
            prepareCurrentUser();

            LocalDateTime olderTime =
                    LocalDateTime.of(
                            2026,
                            9,
                            1,
                            10,
                            0
                    );

            LocalDateTime newerTime =
                    LocalDateTime.of(
                            2026,
                            9,
                            1,
                            11,
                            0
                    );

            WorkspaceMessage older =
                    createMessage(
                            UUID.randomUUID(),
                            currentUser,
                            "Older message",
                            olderTime
                    );

            WorkspaceMessage newer =
                    createMessage(
                            UUID.randomUUID(),
                            currentUser,
                            "Newer message",
                            newerTime
                    );

            /*
             * Repository returns newest first.
             */
            when(
                    workspaceMessageRepository
                            .findTop100ByWorkspaceOrderByCreatedAtDesc(
                                    workspace
                            )
            ).thenReturn(
                    List.of(
                            newer,
                            older
                    )
            );

            List<WorkspaceMessageResponse> result =
                    workspaceMessageService
                            .getMessages(
                                    workspaceId,
                                    currentUser
                            );

            assertEquals(
                    2,
                    result.size()
            );

            assertEquals(
                    "Older message",
                    result.get(0).content()
            );

            assertEquals(
                    "Newer message",
                    result.get(1).content()
            );

            assertEquals(
                    olderTime,
                    result.get(0).createdAt()
            );

            assertEquals(
                    newerTime,
                    result.get(1).createdAt()
            );
        }

        @Test
        void shouldMarkMessageAsOwnWhenSenderIsCurrentUser() {
            prepareMembership();
            prepareCurrentUser();

            WorkspaceMessage message =
                    createMessage(
                            UUID.randomUUID(),
                            currentUser,
                            "My message",
                            LocalDateTime.now()
                    );

            when(
                    workspaceMessageRepository
                            .findTop100ByWorkspaceOrderByCreatedAtDesc(
                                    workspace
                            )
            ).thenReturn(
                    List.of(
                            message
                    )
            );

            List<WorkspaceMessageResponse> result =
                    workspaceMessageService
                            .getMessages(
                                    workspaceId,
                                    currentUser
                            );

            assertEquals(
                    1,
                    result.size()
            );

            assertTrue(
                    result.get(0)
                            .ownMessage()
            );
        }

        @Test
        void shouldMarkMessageAsNotOwnWhenSenderIsAnotherUser() {
            prepareMembership();

            when(
                    currentUser.getId()
            ).thenReturn(
                    currentUserId
            );

            User anotherUser =
                    mock(
                            User.class
                    );

            UUID anotherUserId =
                    UUID.randomUUID();

            when(
                    anotherUser.getId()
            ).thenReturn(
                    anotherUserId
            );

            when(
                    anotherUser.getUsername()
            ).thenReturn(
                    "another.user"
            );

            WorkspaceMessage message =
                    createMessage(
                            UUID.randomUUID(),
                            anotherUser,
                            "Other user's message",
                            LocalDateTime.now()
                    );

            when(
                    workspaceMessageRepository
                            .findTop100ByWorkspaceOrderByCreatedAtDesc(
                                    workspace
                            )
            ).thenReturn(
                    List.of(
                            message
                    )
            );

            List<WorkspaceMessageResponse> result =
                    workspaceMessageService
                            .getMessages(
                                    workspaceId,
                                    currentUser
                            );

            assertFalse(
                    result.get(0)
                            .ownMessage()
            );

            assertEquals(
                    anotherUserId,
                    result.get(0)
                            .userId()
            );

            assertEquals(
                    "another.user",
                    result.get(0)
                            .username()
            );
        }

        @Test
        void shouldUseFirstAndLastNameAsDisplayName() {
            prepareMembership();

            when(
                    currentUser.getId()
            ).thenReturn(
                    currentUserId
            );

            when(
                    currentUser.getUsername()
            ).thenReturn(
                    "svetlin"
            );

            when(
                    currentUser.getFirstName()
            ).thenReturn(
                    "  Svetlin  "
            );

            when(
                    currentUser.getLastName()
            ).thenReturn(
                    "  Biserov  "
            );

            WorkspaceMessage message =
                    createMessage(
                            UUID.randomUUID(),
                            currentUser,
                            "Hello",
                            LocalDateTime.now()
                    );

            when(
                    workspaceMessageRepository
                            .findTop100ByWorkspaceOrderByCreatedAtDesc(
                                    workspace
                            )
            ).thenReturn(
                    List.of(
                            message
                    )
            );

            List<WorkspaceMessageResponse> result =
                    workspaceMessageService
                            .getMessages(
                                    workspaceId,
                                    currentUser
                            );

            assertEquals(
                    "Svetlin Biserov",
                    result.get(0)
                            .displayName()
            );
        }

        @Test
        void shouldFallbackToUsernameWhenDisplayNameIsMissing() {
            prepareMembership();

            when(
                    currentUser.getId()
            ).thenReturn(
                    currentUserId
            );

            when(
                    currentUser.getUsername()
            ).thenReturn(
                    "svetlin"
            );

            WorkspaceMessage message =
                    createMessage(
                            UUID.randomUUID(),
                            currentUser,
                            "Hello",
                            LocalDateTime.now()
                    );

            when(
                    workspaceMessageRepository
                            .findTop100ByWorkspaceOrderByCreatedAtDesc(
                                    workspace
                            )
            ).thenReturn(
                    List.of(
                            message
                    )
            );

            List<WorkspaceMessageResponse> result =
                    workspaceMessageService
                            .getMessages(
                                    workspaceId,
                                    currentUser
                            );

            assertEquals(
                    "svetlin",
                    result.get(0)
                            .displayName()
            );
        }

        @Test
        void shouldNotQueryMessagesWhenUserCannotAccessWorkspace() {
            ResourceNotFoundException exception =
                    new ResourceNotFoundException(
                            ApiErrorCodes.WORKSPACE_NOT_FOUND,
                            "Workspace not found"
                    );

            when(
                    workspacePermissionService
                            .requireMember(
                                    workspaceId,
                                    currentUser
                            )
            ).thenThrow(
                    exception
            );

            assertThrows(
                    ResourceNotFoundException.class,
                    () ->
                            workspaceMessageService
                                    .getMessages(
                                            workspaceId,
                                            currentUser
                                    )
            );

            verifyNoInteractions(
                    workspaceMessageRepository
            );
        }
    }

    @Nested
    class SendMessage {

        @Test
        void shouldSaveMessageForWorkspaceMember() {
            prepareMembership();
            prepareCurrentUser();

            SendWorkspaceMessageRequest request =
                    new SendWorkspaceMessageRequest(
                            "Hello team"
                    );

            UUID messageId =
                    UUID.randomUUID();

            LocalDateTime createdAt =
                    LocalDateTime.of(
                            2026,
                            9,
                            2,
                            18,
                            30
                    );

            when(
                    workspaceMessageRepository.save(
                            any(
                                    WorkspaceMessage.class
                            )
                    )
            ).thenAnswer(invocation -> {
                WorkspaceMessage message =
                        invocation.getArgument(
                                0
                        );

                message.setId(
                        messageId
                );

                message.setCreatedAt(
                        createdAt
                );

                return message;
            });

            WorkspaceMessageResponse response =
                    workspaceMessageService
                            .sendMessage(
                                    workspaceId,
                                    request,
                                    currentUser
                            );

            ArgumentCaptor<WorkspaceMessage> captor =
                    ArgumentCaptor.forClass(
                            WorkspaceMessage.class
                    );

            verify(
                    workspaceMessageRepository
            ).save(
                    captor.capture()
            );

            WorkspaceMessage saved =
                    captor.getValue();

            assertSame(
                    workspace,
                    saved.getWorkspace()
            );

            assertSame(
                    currentUser,
                    saved.getUser()
            );

            assertEquals(
                    "Hello team",
                    saved.getContent()
            );

            assertEquals(
                    messageId,
                    response.id()
            );

            assertEquals(
                    "Hello team",
                    response.content()
            );

            assertTrue(
                    response.ownMessage()
            );
        }

        @Test
        void shouldTrimMessageBeforeSaving() {
            prepareMembership();
            prepareCurrentUser();

            SendWorkspaceMessageRequest request =
                    new SendWorkspaceMessageRequest(
                            "   Hello team   "
                    );

            when(
                    workspaceMessageRepository.save(
                            any(
                                    WorkspaceMessage.class
                            )
                    )
            ).thenAnswer(
                    invocation ->
                            invocation.getArgument(
                                    0
                            )
            );

            workspaceMessageService
                    .sendMessage(
                            workspaceId,
                            request,
                            currentUser
                    );

            ArgumentCaptor<WorkspaceMessage> captor =
                    ArgumentCaptor.forClass(
                            WorkspaceMessage.class
                    );

            verify(
                    workspaceMessageRepository
            ).save(
                    captor.capture()
            );

            assertEquals(
                    "Hello team",
                    captor.getValue()
                            .getContent()
            );
        }

        @Test
        void shouldRejectNullRequest() {
            assertThrows(
                    BadRequestException.class,
                    () ->
                            workspaceMessageService
                                    .sendMessage(
                                            workspaceId,
                                            null,
                                            currentUser
                                    )
            );

            verifyNoInteractions(
                    workspacePermissionService
            );

            verifyNoInteractions(
                    workspaceMessageRepository
            );
        }

        @Test
        void shouldRejectNullContent() {
            SendWorkspaceMessageRequest request =
                    new SendWorkspaceMessageRequest(
                            null
                    );

            assertThrows(
                    BadRequestException.class,
                    () ->
                            workspaceMessageService
                                    .sendMessage(
                                            workspaceId,
                                            request,
                                            currentUser
                                    )
            );

            verifyNoInteractions(
                    workspacePermissionService
            );

            verifyNoInteractions(
                    workspaceMessageRepository
            );
        }

        @Test
        void shouldRejectBlankMessage() {
            SendWorkspaceMessageRequest request =
                    new SendWorkspaceMessageRequest(
                            "     "
                    );

            assertThrows(
                    BadRequestException.class,
                    () ->
                            workspaceMessageService
                                    .sendMessage(
                                            workspaceId,
                                            request,
                                            currentUser
                                    )
            );

            verifyNoInteractions(
                    workspacePermissionService
            );

            verifyNoInteractions(
                    workspaceMessageRepository
            );
        }

        @Test
        void shouldRejectMessageLongerThan2000Characters() {
            SendWorkspaceMessageRequest request =
                    new SendWorkspaceMessageRequest(
                            "a".repeat(
                                    2001
                            )
                    );

            assertThrows(
                    BadRequestException.class,
                    () ->
                            workspaceMessageService
                                    .sendMessage(
                                            workspaceId,
                                            request,
                                            currentUser
                                    )
            );

            verifyNoInteractions(
                    workspacePermissionService
            );

            verifyNoInteractions(
                    workspaceMessageRepository
            );
        }

        @Test
        void shouldAllowMessageWithExactly2000Characters() {
            prepareMembership();
            prepareCurrentUser();

            String content =
                    "a".repeat(
                            2000
                    );

            SendWorkspaceMessageRequest request =
                    new SendWorkspaceMessageRequest(
                            content
                    );

            when(
                    workspaceMessageRepository.save(
                            any(
                                    WorkspaceMessage.class
                            )
                    )
            ).thenAnswer(
                    invocation ->
                            invocation.getArgument(
                                    0
                            )
            );

            assertDoesNotThrow(
                    () ->
                            workspaceMessageService
                                    .sendMessage(
                                            workspaceId,
                                            request,
                                            currentUser
                                    )
            );

            verify(
                    workspaceMessageRepository
            ).save(
                    any(
                            WorkspaceMessage.class
                    )
            );
        }

        @Test
        void shouldNotSaveWhenUserCannotAccessWorkspace() {
            SendWorkspaceMessageRequest request =
                    new SendWorkspaceMessageRequest(
                            "Secret message"
                    );

            ResourceNotFoundException exception =
                    new ResourceNotFoundException(
                            ApiErrorCodes.WORKSPACE_NOT_FOUND,
                            "Workspace not found"
                    );

            when(
                    workspacePermissionService
                            .requireMember(
                                    workspaceId,
                                    currentUser
                            )
            ).thenThrow(
                    exception
            );

            assertThrows(
                    ResourceNotFoundException.class,
                    () ->
                            workspaceMessageService
                                    .sendMessage(
                                            workspaceId,
                                            request,
                                            currentUser
                                    )
            );

            verify(
                    workspaceMessageRepository,
                    never()
            ).save(
                    any(
                            WorkspaceMessage.class
                    )
            );
        }
    }

    private void prepareMembership() {
        when(
                workspacePermissionService
                        .requireMember(
                                workspaceId,
                                currentUser
                        )
        ).thenReturn(
                workspaceMember
        );

        when(
                workspaceMember.getWorkspace()
        ).thenReturn(
                workspace
        );
    }

    private void prepareCurrentUser() {
        when(
                currentUser.getId()
        ).thenReturn(
                currentUserId
        );

        when(
                currentUser.getUsername()
        ).thenReturn(
                "svetlin"
        );
    }

    private WorkspaceMessage createMessage(
            UUID id,
            User sender,
            String content,
            LocalDateTime createdAt
    ) {
        WorkspaceMessage message =
                new WorkspaceMessage();

        message.setId(
                id
        );

        message.setWorkspace(
                workspace
        );

        message.setUser(
                sender
        );

        message.setContent(
                content
        );

        message.setCreatedAt(
                createdAt
        );

        return message;
    }
}