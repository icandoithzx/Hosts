# SSE-Flux-Demo: Spring Boot WebFlux SSE
This project is a demonstration of Server-Sent Events (SSE) using Spring Boot and WebFlux. It allows a client to upload a text document and then streams the content of that document back sentence by sentence over an SSE connection.

## Features
- **Reactive API**: Built with Spring WebFlux for a non-blocking, reactive stack.
- **File Upload**: A REST endpoint to handle text file uploads.
- **Real-time Streaming**: An SSE endpoint that streams document content back to the client.
- **In-Memory Storage**: For simplicity, uploaded documents are temporarily stored in memory.
- **Clear Workflow**:
  1. The client uploads a file and gets a unique `taskId`.
  2. The client uses the `taskId` to subscribe to an SSE stream.
  3. The server first sends the full document, then sends each sentence with a short delay.
  4. Finally, the server sends a `DONE` message and cleans up the resources.

## Prerequisites
- Java 1.8 or higher
- Apache Maven

## How to Run
1.  **Clone the repository**
    ```bash
    git clone <repository-url>
    cd sse-flux-demo
    ```

2.  **Build and run the application using Maven**
    ```bash
    mvn spring-boot:run
    ```
    The application will start on `http://localhost:8080`.

## API Endpoints

### 1. Upload Document
Upload a text file to receive a `taskId` for streaming.

- **URL**: `/api/documents/upload`
- **Method**: `POST`
- **Content-Type**: `multipart/form-data`
- **Request Body**: Form data with a key `file` and the text file as the value.

**Example using `curl`:**
```bash
# Create a dummy text file
echo "This is the first sentence. This is the second! And this is the third?" > test.txt

# Send the request
curl -X POST -F "file=@test.txt" http://localhost:8080/api/documents/upload
```

**Successful Response:**
```json
{
  "taskId": "a1b2c3d4-e5f6-7890-1234-567890abcdef"
}
```

### 2. Stream Document
Subscribe to the SSE stream using the `taskId` from the upload step.

- **URL**: `/api/documents/stream/{taskId}`
- **Method**: `GET`
- **Produces**: `text/event-stream`

**Example using `curl`:**
Replace `{taskId}` with the ID you received from the upload.
```bash
curl -N http://localhost:8080/api/documents/stream/{taskId}
```

**Output Stream:**
You will see a stream of events like this:
```
id:a1b2c3d4-e5f6-7890-1234-567890abcdef-FULL_DOCUMENT
event:message
data:{"type":"FULL_DOCUMENT","taskId":"a1b2c3d4-e5f6-7890-1234-567890abcdef","content":"This is the first sentence. This is the second! And this is the third?","sequence":0}

id:a1b2c3d4-e5f6-7890-1234-567890abcdef-SENTENCE
event:message
data:{"type":"SENTENCE","taskId":"a1b2c3d4-e5f6-7890-1234-567890abcdef","content":"This is the first sentence.","sequence":1}

id:a1b2c3d4-e5f6-7890-1234-567890abcdef-SENTENCE
event:message
data:{"type":"SENTENCE","taskId":"a1b2c3d4-e5f6-7890-1234-567890abcdef","content":"This is the second!","sequence":2}

id:a1b2c3d4-e5f6-7890-1234-567890abcdef-SENTENCE
event:message
data:{"type":"SENTENCE","taskId":"a1b2c3d4-e5f6-7890-1234-567890abcdef","content":"And this is the third?","sequence":3}

id:a1b2c3d4-e5f6-7890-1234-567890abcdef-DONE
event:message
data:{"type":"DONE","taskId":"a1b2c3d4-e5f6-7890-1234-567890abcdef","content":"Processing complete.","sequence":0}
```
The connection will close automatically after the `DONE` event.
