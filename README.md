# OurVault

**OurVault** is an AI-powered knowledge base and Retrieval-Augmented Generation (RAG) platform developed as a master's project.

The application allows users to organize documents inside collaborative workspaces, process and index their contents, perform semantic search, generate AI-powered document insights, and ask questions grounded in uploaded documents.

OurVault combines traditional document management with local artificial intelligence using embeddings, vector search, OCR, and large language models.

---

## Main Features

### Authentication and User Management

- User registration and login
- JWT-based authentication
- Access and refresh tokens
- Refresh-token revocation
- Forgot password functionality
- Secure password-reset tokens
- Password reset through email
- User profile management
- English and Bulgarian interface

### Workspace Management

Users organize their knowledge inside workspaces.

Each workspace supports the following roles:

- `OWNER`
- `ADMIN`
- `MEMBER`

Workspace functionality includes:

- Create workspaces
- Update workspace information
- Delete workspaces
- Add members
- Remove members
- Change member roles
- Workspace-level authorization
- Member listing

Workspace access is isolated so that users can only access resources belonging to workspaces of which they are members.

### Document Management

Users can upload and manage documents inside a workspace.

Supported document types include:

- PDF
- DOCX
- TXT
- Markdown

The document processing pipeline performs:

1. File storage
2. Text extraction
3. OCR when required
4. Text normalization
5. Text chunking
6. Embedding generation
7. Vector persistence
8. Document status management

Documents use the following processing states:

- `PENDING`
- `PROCESSING`
- `READY`
- `FAILED`

### OCR

OurVault supports scanned PDF processing through OCR.

PDF processing first attempts native text extraction using Apache PDFBox.

If insufficient textual content is detected, the document is processed using Tesseract OCR.

Configured OCR languages:

```text
Bulgarian
English
```

This allows both digitally generated and scanned documents to participate in the RAG pipeline.

### Text Chunking

Extracted document text is normalized and split into overlapping chunks before embedding.

Default configuration:

```text
Chunk size: 1500 characters
Overlap:    200 characters
```

The chunking algorithm attempts to split text at natural boundaries in the following order:

1. Paragraph boundary
2. Sentence boundary
3. Whitespace boundary
4. Hard character boundary

This reduces unnecessary semantic fragmentation.

### Embeddings

OurVault uses a local Ollama embedding model:

```text
embeddinggemma
```

Embedding dimension:

```text
768
```

Embeddings are stored directly inside PostgreSQL using the `pgvector` extension.

### Semantic Search

Users can perform semantic search across documents in a workspace.

Search flow:

```text
Query
  ↓
Embedding generation
  ↓
pgvector similarity search
  ↓
Similarity filtering
  ↓
Relevant document chunks
```

Default retrieval configuration:

```text
Normal minimum similarity:   0.35
Fallback minimum similarity: 0.20
```

The fallback threshold is used only for explicitly selected documents where broader contextual retrieval may be useful.

### Retrieval-Augmented Generation

OurVault supports AI-powered question answering based on uploaded documents.

The RAG pipeline is:

```text
User Question
      ↓
Query Embedding
      ↓
Semantic Retrieval
      ↓
Relevant Document Chunks
      ↓
Prompt Context Construction
      ↓
Local LLM
      ↓
Grounded Response
```

The application uses:

```text
gemma3:4b
```

through Ollama for local language-model inference.

### AI Chat

Users can create AI chat sessions inside individual workspaces.

AI chats:

- Belong to a workspace
- Belong to the current authenticated user
- Can use workspace documents as context
- Support attached-document retrieval
- Store user and assistant messages
- Preserve generated answers

AI Chat is separate from the human Team Chat functionality.

### AI Document Insights

For processed documents, OurVault can generate:

- Summary
- Key points
- Important facts

Insights are generated on demand using the local language model.

The implementation also supports larger documents by reducing the document into manageable contextual summaries before producing the final structured response.

### Email AI Insights

Generated document insights can be sent to the authenticated user's email address.

The recipient is determined exclusively from the currently authenticated user:

```text
currentUser.getEmail()
```

Users cannot supply arbitrary recipient addresses.

Email output supports:

- English
- Bulgarian

### Team Chat

Each workspace contains a human-to-human Team Chat.

Workspace members can:

- Read recent workspace messages
- Send messages
- See message authors
- Distinguish their own messages
- Communicate directly inside the workspace

Team Chat is separate from AI Chat.

### Global Search

OurVault includes global search across accessible:

- Workspaces
- Documents
- AI chat sessions

Search results respect workspace access permissions.

---

# Technology Stack

## Backend

- Java 21
- Spring Boot 3
- Spring Web
- Spring Security
- Spring Data JPA
- Hibernate
- PostgreSQL
- pgvector
- JWT
- Spring AI
- Ollama
- Apache Tika
- Apache PDFBox
- Tesseract OCR
- Jakarta Validation
- Spring Mail
- Lombok
- Maven

## Frontend

- React
- TypeScript
- Vite
- React Router
- Axios
- CSS

## AI

- Ollama
- `embeddinggemma`
- `gemma3:4b`

## Database

- PostgreSQL
- pgvector

---

# System Architecture

A simplified architecture of OurVault is shown below.

```text
┌─────────────────────┐
│ React Frontend      │
│ TypeScript + Vite   │
└──────────┬──────────┘
           │ REST
           ▼
┌─────────────────────┐
│ Spring Boot API     │
│                     │
│ Auth / JWT          │
│ Workspaces          │
│ Documents           │
│ Team Chat           │
│ AI Chat             │
│ AI Insights         │
│ Semantic Search     │
└──────┬───────┬──────┘
       │       │
       │       └──────────────────┐
       ▼                          ▼
┌───────────────┐          ┌───────────────┐
│ PostgreSQL    │          │ Ollama        │
│ + pgvector    │          │               │
│               │          │ embeddinggemma│
│ Metadata      │          │ gemma3:4b     │
│ Documents     │          └───────────────┘
│ Chunks        │
│ Embeddings    │
│ Chats         │
└───────────────┘
       ▲
       │
┌──────┴────────┐
│ Text/OCR      │
│               │
│ Tika          │
│ PDFBox        │
│ Tesseract     │
└───────────────┘
```

---

# Document Processing Pipeline

```text
Document Upload
      ↓
File Storage
      ↓
Text Extraction
      ↓
Native PDF text available?
     / \
   Yes  No
    │    │
    │    └──→ Tesseract OCR
    │
    ↓
Text Normalization
      ↓
Text Chunking
      ↓
Embedding Generation
      ↓
pgvector Storage
      ↓
READY
```

If any processing stage fails:

```text
PROCESSING
    ↓
FAILED
```

The processing error is stored for diagnostic purposes.

---

# Prerequisites

Before running OurVault, install:

### Java

Java 21 is required.

Verify:

```powershell
java -version
```

### Maven

The project includes the Maven Wrapper when available.

Verify Maven with:

```powershell
mvn -version
```

### Node.js

Install a modern Node.js LTS version.

Verify:

```powershell
node --version
npm --version
```

### PostgreSQL

Install PostgreSQL.

Verify that the PostgreSQL server is running before starting the backend.

### pgvector

The PostgreSQL database must support the `vector` extension.

Enable it inside the OurVault database:

```sql
CREATE EXTENSION IF NOT EXISTS vector;
```

Verify:

```sql
SELECT extname
FROM pg_extension
WHERE extname = 'vector';
```

### Ollama

Install Ollama from the official Ollama distribution.

Verify:

```powershell
ollama --version
```

Pull the required models:

```powershell
ollama pull embeddinggemma
ollama pull gemma3:4b
```

Verify:

```powershell
ollama list
```

Both models should be present.

### Tesseract OCR

Install Tesseract OCR.

Required languages:

```text
eng
bul
```

Verify installation:

```powershell
tesseract --version
```

Verify languages:

```powershell
tesseract --list-langs
```

The output should contain:

```text
eng
bul
```

---

# PostgreSQL Setup

Create a PostgreSQL database for OurVault.

Example:

```sql
CREATE DATABASE ourvault;
```

Connect to the database and enable pgvector:

```sql
CREATE EXTENSION IF NOT EXISTS vector;
```

Configure the database connection inside:

```text
src/main/resources/application.properties
```

Do not commit real database credentials to source control.

---

# Backend Configuration

Copy:

```text
src/main/resources/application.properties.example
```

to:

```text
src/main/resources/application.properties
```

Then provide your local configuration.

Important configuration categories include:

- PostgreSQL connection
- JWT configuration
- Ollama
- RAG thresholds
- SMTP
- Frontend URL
- Password reset expiration
- File storage

Never commit:

- Database passwords
- Gmail App Passwords
- JWT secrets
- Other private credentials

---

# Email Configuration

OurVault uses SMTP email for:

- Password reset emails
- Document AI Insights emails

For Gmail development environments, an App Password can be used.

Example properties:

```properties
spring.mail.host=smtp.gmail.com
spring.mail.port=587
spring.mail.username=YOUR_EMAIL
spring.mail.password=YOUR_APP_PASSWORD
spring.mail.properties.mail.smtp.auth=true
spring.mail.properties.mail.smtp.starttls.enable=true
spring.mail.properties.mail.smtp.starttls.required=true
```

Do not store a real Gmail password in source control.

---

# Starting Ollama

Make sure Ollama is running before using AI functionality.

Verify the installed models:

```powershell
ollama list
```

Required:

```text
embeddinggemma
gemma3:4b
```

---

# Running the Backend

Open a terminal in the backend project:

```powershell
cd C:\Master_2026\rag-knowledge-platform\OurVault
```

Using Maven Wrapper:

```powershell
.\mvnw spring-boot:run
```

or Maven:

```powershell
mvn spring-boot:run
```

The backend will start using the configured Spring Boot server port.

---

# Running the Frontend

Open another terminal:

```powershell
cd C:\Master_2026\rag-knowledge-platform\ourvault-frontend
```

Install dependencies:

```powershell
npm install
```

Start the development server:

```powershell
npm run dev
```

The Vite development server normally runs at:

```text
http://localhost:5173
```

---

# Production Frontend Build

To verify that the frontend compiles successfully:

```powershell
npm run build
```

The generated production files are placed inside:

```text
dist/
```

---

# Running Automated Tests

From the backend directory:

```powershell
cd C:\Master_2026\rag-knowledge-platform\OurVault
```

Run all tests:

```powershell
.\mvnw clean test
```

or:

```powershell
mvn clean test
```

The final result should contain:

```text
Failures: 0
Errors: 0
BUILD SUCCESS
```

The automated test suite covers important business logic including:

- Workspace permissions
- Workspace management
- Workspace Team Chat
- Password reset security
- Text chunking
- Document processing
- Semantic retrieval
- AI Insights email delivery

---

# Security Design

OurVault applies several security controls.

### Workspace Isolation

Resources are always associated with a workspace.

Users must be members of the relevant workspace before accessing workspace resources.

### Role-Based Authorization

Workspace roles:

```text
OWNER
ADMIN
MEMBER
```

Examples:

```text
OWNER
├── Full workspace control
├── Change roles
├── Remove administrators
└── Delete workspace

ADMIN
├── Manage members
├── Manage documents
└── Update workspace

MEMBER
├── View workspace
├── Upload/view documents
├── Semantic search
├── AI functionality
└── Team Chat
```

### Password Reset Security

Password-reset tokens use:

```text
32 random bytes
      ↓
Base64 URL-safe raw token
      ↓
SHA-256
      ↓
Stored token hash
```

The raw reset token is only sent through the email reset URL.

Additional protection includes:

- Expiration
- One-time use
- Previous reset-token invalidation
- Refresh-token revocation after password change
- Generic forgot-password response to prevent account enumeration

### File Storage

Uploaded files are stored using generated identifiers rather than trusting user-provided filenames.

Path normalization and confinement are used to reduce unsafe filesystem access.

---

# RAG Retrieval Strategy

OurVault supports several retrieval strategies depending on the user request.

### Workspace Search

Semantic similarity search across READY documents belonging to the workspace.

### Selected Document Search

Semantic search constrained to explicitly selected documents.

### Fallback Search

If selected documents do not produce results above the normal similarity threshold, a lower fallback threshold may be used.

### Full Document Context

For smaller selected documents, complete document context can be used instead of semantic retrieval.

This helps with broad questions such as:

```text
What topics are covered by this document?
```

or:

```text
What animals are mentioned?
```

---

# Known Limitations

OurVault is a master's-project implementation and has several known limitations.

### OCR Accuracy

OCR quality depends on:

- Scan resolution
- Font quality
- Document layout
- Language
- Image noise

Some symbols may be recognized incorrectly.

During evaluation, percentage symbols were occasionally incorrectly recognized by OCR.

### Local Model Limitations

The quality of generated responses depends on the local language model.

Small local models may:

- Miss details
- Produce incomplete structured responses
- Have weaker reasoning than larger cloud-hosted models

### Large Documents

Large documents require context reduction before AI processing because language models have limited context windows.

### Team Chat

Workspace Team Chat currently uses REST polling rather than WebSockets.

This is sufficient for the project scope but is not intended as a high-scale real-time messaging implementation.

### Local Infrastructure

The current project is designed primarily for local execution.

A production deployment would require additional infrastructure and operational configuration.

---

# Evaluation

OurVault was evaluated using documents designed to test:

- Native PDF extraction
- TXT processing
- DOCX processing
- Scanned Bulgarian PDF OCR
- Large-document handling
- Semantic retrieval
- AI question answering
- AI document insights

The evaluation identified and resolved several implementation issues, including the distinction between native PDF text extraction and OCR processing.

---

# Project Structure

Simplified backend structure:

```text
src/main/java/src
│
├── auth
│   ├── controller
│   ├── dto
│   ├── entity
│   ├── repository
│   └── service
│
├── common
│   └── exception
│
├── document
│   ├── controller
│   ├── dto
│   ├── repository
│   ├── service
│   └── util
│
├── embedding
│   └── service
│
├── mail
│   └── service
│
├── rag
│   ├── dto
│   └── service
│
├── workspace
│   ├── controller
│   ├── dto
│   ├── entity
│   ├── repository
│   ├── service
│   └── util
│
└── entity
```

Frontend:

```text
ourvault-frontend
│
├── src
│   ├── components
│   ├── pages
│   ├── services
│   ├── types
│   └── ...
│
├── package.json
└── vite.config.ts
```

---

# Master's Project

OurVault was developed as a master's project focused on the design and implementation of an:

> AI-Powered Knowledge Base and Retrieval-Augmented Generation System

The project explores the integration of:

- Document management
- OCR
- Natural language processing
- Embeddings
- Vector databases
- Semantic search
- Retrieval-Augmented Generation
- Local large language models
- Collaboration
- Authentication and authorization

---

# Author

**Svetlin Sofroniev**

Master's project — **OurVault**

AI-Powered Knowledge Base + RAG System