CREATE EXTENSION IF NOT EXISTS pgcrypto;
CREATE EXTENSION IF NOT EXISTS vector;


-- =========================================================
-- USERS
-- =========================================================

CREATE TABLE users (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    username VARCHAR(50) NOT NULL UNIQUE,
    email VARCHAR(255) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    role VARCHAR(30) NOT NULL DEFAULT 'USER',
    is_enabled BOOLEAN NOT NULL DEFAULT TRUE,
    is_account_non_locked BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);


-- =========================================================
-- WORKSPACES
-- =========================================================

CREATE TABLE workspaces (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name VARCHAR(100) NOT NULL,
    description TEXT,
    created_by_id UUID NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_workspace_creator
        FOREIGN KEY (created_by_id)
        REFERENCES users(id)
        ON DELETE CASCADE
);

CREATE INDEX idx_workspaces_created_by
    ON workspaces(created_by_id);


-- =========================================================
-- WORKSPACE MEMBERS
-- =========================================================

CREATE TABLE workspace_members (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    workspace_id UUID NOT NULL,
    user_id UUID NOT NULL,
    role VARCHAR(20) NOT NULL DEFAULT 'MEMBER',
    joined_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_workspace_member_workspace
        FOREIGN KEY (workspace_id)
        REFERENCES workspaces(id)
        ON DELETE CASCADE,

    CONSTRAINT fk_workspace_member_user
        FOREIGN KEY (user_id)
        REFERENCES users(id)
        ON DELETE CASCADE,

    CONSTRAINT uk_workspace_member
        UNIQUE (workspace_id, user_id),

    CONSTRAINT chk_workspace_member_role
        CHECK (role IN ('OWNER', 'ADMIN', 'MEMBER'))
);

CREATE INDEX idx_workspace_members_user
    ON workspace_members(user_id);


-- =========================================================
-- DOCUMENTS
-- =========================================================

CREATE TABLE documents (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    workspace_id UUID NOT NULL,
    uploaded_by UUID NOT NULL,
    title VARCHAR(255) NOT NULL,
    original_filename VARCHAR(255) NOT NULL,
    file_type VARCHAR(100) NOT NULL,
    file_size BIGINT NOT NULL,
    storage_path TEXT NOT NULL,
    upload_status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    processing_error TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_document_workspace
        FOREIGN KEY (workspace_id)
        REFERENCES workspaces(id)
        ON DELETE CASCADE,

    CONSTRAINT fk_document_uploader
        FOREIGN KEY (uploaded_by)
        REFERENCES users(id)
        ON DELETE RESTRICT,

    CONSTRAINT chk_document_status
        CHECK (
            upload_status IN (
                'PENDING',
                'PROCESSING',
                'READY',
                'FAILED'
            )
        ),

    CONSTRAINT chk_document_file_size
        CHECK (file_size >= 0)
);

CREATE INDEX idx_documents_workspace
    ON documents(workspace_id);

CREATE INDEX idx_documents_uploaded_by
    ON documents(uploaded_by);

CREATE INDEX idx_documents_status
    ON documents(upload_status);


-- =========================================================
-- DOCUMENT CHUNKS + EMBEDDINGS
-- =========================================================

CREATE TABLE document_chunks (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    document_id UUID NOT NULL,
    chunk_index INTEGER NOT NULL,
    content TEXT NOT NULL,
    token_count INTEGER,
    embedding vector(768) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_document_chunk_document
        FOREIGN KEY (document_id)
        REFERENCES documents(id)
        ON DELETE CASCADE,

    CONSTRAINT uk_document_chunk
        UNIQUE (document_id, chunk_index),

    CONSTRAINT chk_document_chunk_index
        CHECK (chunk_index >= 0),

    CONSTRAINT chk_document_chunk_token_count
        CHECK (token_count IS NULL OR token_count >= 0)
);

CREATE INDEX idx_document_chunks_embedding_hnsw
    ON document_chunks
    USING hnsw (embedding vector_cosine_ops);


-- =========================================================
-- CHAT SESSIONS
-- =========================================================

CREATE TABLE chat_sessions (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    workspace_id UUID NOT NULL,
    user_id UUID NOT NULL,
    title VARCHAR(255),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_chat_session_workspace
        FOREIGN KEY (workspace_id)
        REFERENCES workspaces(id)
        ON DELETE CASCADE,

    CONSTRAINT fk_chat_session_user
        FOREIGN KEY (user_id)
        REFERENCES users(id)
        ON DELETE CASCADE
);

CREATE INDEX idx_chat_sessions_workspace
    ON chat_sessions(workspace_id);

CREATE INDEX idx_chat_sessions_user
    ON chat_sessions(user_id);


-- =========================================================
-- CHAT MESSAGES
-- =========================================================

CREATE TABLE chat_messages (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    session_id UUID NOT NULL,
    sender_type VARCHAR(20) NOT NULL,
    content TEXT NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_chat_message_session
        FOREIGN KEY (session_id)
        REFERENCES chat_sessions(id)
        ON DELETE CASCADE,

    CONSTRAINT chk_chat_message_sender
        CHECK (sender_type IN ('USER', 'ASSISTANT', 'SYSTEM'))
);

CREATE INDEX idx_chat_messages_session_created
    ON chat_messages(session_id, created_at);


-- =========================================================
-- DOCUMENTS ATTACHED TO CHAT SESSIONS
-- =========================================================

CREATE TABLE document_chat_context (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    chat_session_id UUID NOT NULL,
    document_id UUID NOT NULL,
    attached_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_document_chat_context_session
        FOREIGN KEY (chat_session_id)
        REFERENCES chat_sessions(id)
        ON DELETE CASCADE,

    CONSTRAINT fk_document_chat_context_document
        FOREIGN KEY (document_id)
        REFERENCES documents(id)
        ON DELETE CASCADE,

    CONSTRAINT uk_chat_session_document
        UNIQUE (chat_session_id, document_id)
);

CREATE INDEX idx_document_chat_context_document
    ON document_chat_context(document_id);


-- =========================================================
-- AI QUERY AUDIT
-- =========================================================

CREATE TABLE ai_queries (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL,
    chat_session_id UUID,
    query_text TEXT NOT NULL,
    response_text TEXT,
    model_name VARCHAR(100),
    response_time_ms INTEGER,
    prompt_tokens INTEGER,
    completion_tokens INTEGER,
    total_tokens INTEGER,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_ai_query_user
        FOREIGN KEY (user_id)
        REFERENCES users(id)
        ON DELETE CASCADE,

    CONSTRAINT fk_ai_query_session
        FOREIGN KEY (chat_session_id)
        REFERENCES chat_sessions(id)
        ON DELETE SET NULL,

    CONSTRAINT chk_ai_query_response_time
        CHECK (response_time_ms IS NULL OR response_time_ms >= 0),

    CONSTRAINT chk_ai_query_token_counts
        CHECK (
            (prompt_tokens IS NULL OR prompt_tokens >= 0)
            AND
            (completion_tokens IS NULL OR completion_tokens >= 0)
            AND
            (total_tokens IS NULL OR total_tokens >= 0)
        )
);

CREATE INDEX idx_ai_queries_user
    ON ai_queries(user_id);

CREATE INDEX idx_ai_queries_session
    ON ai_queries(chat_session_id);


-- =========================================================
-- REFRESH TOKENS
-- =========================================================

CREATE TABLE refresh_tokens (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL,
    token_hash VARCHAR(255) NOT NULL UNIQUE,
    expires_at TIMESTAMP NOT NULL,
    revoked BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_refresh_token_user
        FOREIGN KEY (user_id)
        REFERENCES users(id)
        ON DELETE CASCADE
);

CREATE INDEX idx_refresh_tokens_user
    ON refresh_tokens(user_id);

CREATE INDEX idx_refresh_tokens_active
    ON refresh_tokens(user_id, revoked, expires_at);

    -- =========================================================
    -- CHAT MESSAGE SOURCES
    -- =========================================================

    CREATE TABLE chat_message_sources (
        id UUID PRIMARY KEY DEFAULT gen_random_uuid(),

        message_id UUID NOT NULL,

        source_rank INTEGER NOT NULL,

        chunk_id UUID NOT NULL,
        document_id UUID NOT NULL,

        document_title VARCHAR(255) NOT NULL,
        chunk_index INTEGER NOT NULL,

        content TEXT NOT NULL,

        distance DOUBLE PRECISION,
        similarity DOUBLE PRECISION,

        created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

        CONSTRAINT fk_chat_message_sources_message
            FOREIGN KEY (message_id)
            REFERENCES chat_messages(id)
            ON DELETE CASCADE,

        CONSTRAINT uk_chat_message_source_rank
            UNIQUE (message_id, source_rank)
    );

    CREATE INDEX idx_chat_message_sources_message
        ON chat_message_sources(message_id);