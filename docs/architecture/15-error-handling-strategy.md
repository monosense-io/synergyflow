# 15. Error Handling Strategy

## 15.1 Backend Error Handling (RFC 7807 Problem Details)

```java
// shared/exceptions/GlobalExceptionHandler.java
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(EntityNotFoundException.class)
    public ResponseEntity<ProblemDetail> handleEntityNotFound(EntityNotFoundException ex, HttpServletRequest request) {
        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(
                HttpStatus.NOT_FOUND,
                ex.getMessage()
        );
        problemDetail.setType(URI.create("https://synergyflow.example.com/errors/not-found"));
        problemDetail.setTitle("Entity Not Found");
        problemDetail.setInstance(URI.create(request.getRequestURI()));
        problemDetail.setProperty("correlationId", MDC.get("correlationId"));
        problemDetail.setProperty("timestamp", Instant.now().toString());

        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(problemDetail);
    }

    @ExceptionHandler(OptimisticLockException.class)
    public ResponseEntity<ProblemDetail> handleOptimisticLock(OptimisticLockException ex, HttpServletRequest request) {
        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(
                HttpStatus.CONFLICT,
                "The resource has been modified by another user. Please refresh and try again."
        );
        problemDetail.setType(URI.create("https://synergyflow.example.com/errors/optimistic-lock"));
        problemDetail.setTitle("Optimistic Locking Conflict");
        problemDetail.setProperty("correlationId", MDC.get("correlationId"));

        return ResponseEntity.status(HttpStatus.CONFLICT).body(problemDetail);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ProblemDetail> handleValidationErrors(MethodArgumentNotValidException ex, HttpServletRequest request) {
        Map<String, String> errors = ex.getBindingResult()
                .getFieldErrors()
                .stream()
                .collect(Collectors.toMap(
                        FieldError::getField,
                        error -> error.getDefaultMessage() != null ? error.getDefaultMessage() : "Invalid value"
                ));

        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(
                HttpStatus.BAD_REQUEST,
                "Validation failed"
        );
        problemDetail.setType(URI.create("https://synergyflow.example.com/errors/validation-error"));
        problemDetail.setTitle("Validation Error");
        problemDetail.setProperty("errors", errors);
        problemDetail.setProperty("correlationId", MDC.get("correlationId"));

        return ResponseEntity.badRequest().body(problemDetail);
    }
}
```

## 15.2 Frontend Error Handling

```typescript
// lib/api/error-handler.ts
export class ApiError extends Error {
  constructor(
    public status: number,
    public detail: string,
    public correlationId: string,
    public errors?: Record<string, string>
  ) {
    super(detail);
    this.name = 'ApiError';
  }

  static fromResponse(response: Response, body: any, correlationId: string): ApiError {
    return new ApiError(
      response.status,
      body.detail || 'An error occurred',
      correlationId,
      body.errors
    );
  }

  get userMessage(): string {
    switch (this.status) {
      case 401:
        return 'Your session has expired. Please log in again.';
      case 403:
        return 'You do not have permission to perform this action.';
      case 404:
        return 'The requested resource was not found.';
      case 409:
        return 'This resource has been modified. Please refresh and try again.';
      case 500:
        return 'A server error occurred. Please try again later.';
      default:
        return this.detail;
    }
  }
}

// Global error boundary
// components/error-boundary.tsx
'use client';

import { useEffect } from 'react';

export function ErrorBoundary({
  error,
  reset,
}: {
  error: Error & { digest?: string };
  reset: () => void;
}) {
  useEffect(() => {
    // Log error to monitoring service
    console.error('Error caught by boundary:', error);
  }, [error]);

  return (
    <div className="flex flex-col items-center justify-center min-h-screen">
      <h2 className="text-2xl font-bold mb-4">Something went wrong!</h2>
      <p className="text-gray-600 mb-4">{error.message}</p>
      {error.digest && (
        <p className="text-sm text-gray-500 mb-4">Error ID: {error.digest}</p>
      )}
      <button
        onClick={reset}
        className="px-4 py-2 bg-blue-500 text-white rounded hover:bg-blue-600"
      >
        Try again
      </button>
    </div>
  );
}
```

---
