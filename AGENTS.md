# Agent Guidelines for Digital Money House

This document provides coding guidelines and commands for agents working on the Digital Money House microservices project.

## Project Overview

**Tech Stack**: Java 21, Spring Boot 3.4.13, Spring Cloud 2024.0.3, Maven, PostgreSQL  
**Architecture**: Microservices (config-server, eureka-server, api-gateway, user-service, account-service)  
**Security**: JWT authentication, BCrypt password hashing  
**Tools**: Lombok, Jakarta Validation, OpenFeign, JUnit 5, Mockito

---

## Build, Test & Run Commands

### Build Commands
```bash
# Build all services (from root)
mvn clean install

# Build specific service
cd user-service && mvn clean install

# Build without tests
mvn clean install -DskipTests

# Package service as JAR
cd user-service && mvn package
```

### Test Commands
```bash
# Run all tests in all services
mvn test

# Run tests for specific service
cd user-service && mvn test

# Run single test class
cd user-service && mvn test -Dtest=UserServiceTest

# Run single test method
cd user-service && mvn test -Dtest=UserServiceTest#testRegisterUser_Success

# Run tests with coverage
mvn test jacoco:report
```

### Run Commands
```bash
# Run service via Maven
cd user-service && mvn spring-boot:run

# Run packaged JAR
java -jar user-service/target/user-service-0.0.1-SNAPSHOT.jar

# Run with specific profile
mvn spring-boot:run -Dspring-boot.run.profiles=dev
```

### Service Startup Order
1. config-server (port 8888)
2. eureka-server (port 8761)
3. user-service (port 8081)
4. account-service (port 8082)
5. api-gateway (port 8080)

---

## Code Organization

### Package Structure
Each microservice follows this structure:
```
com.dmh.<service-name>/
├── controller/       # REST endpoints
├── service/          # Business logic
├── repository/       # Data access layer
├── entity/           # JPA entities
├── dto/              # Data Transfer Objects (Request/Response)
├── exception/        # Custom exceptions
├── util/             # Utility classes
├── validator/        # Custom validators
└── client/           # Feign clients (if needed)
```

### Import Organization
Order imports as follows:
1. Jakarta/Java EE packages (jakarta.*)
2. Java standard library (java.*, javax.*)
3. Spring Framework (org.springframework.*)
4. Third-party libraries (org.slf4j.*, lombok.*, etc.)
5. Internal project packages (com.dmh.*)

Separate groups with blank lines.

---

## Naming Conventions

### Classes
- **Entities**: Singular nouns (e.g., `User`, `Account`, `Transaction`)
- **DTOs**: Purpose + noun (e.g., `RegisterUserRequest`, `LoginResponse`, `UserResponse`)
- **Controllers**: Noun + `Controller` (e.g., `UserController`, `AccountController`)
- **Services**: Noun + `Service` (e.g., `UserService`, `AccountService`)
- **Repositories**: Noun + `Repository` (e.g., `UserRepository`)
- **Exceptions**: Descriptive + `Exception` (e.g., `UserAlreadyExistsException`, `InvalidCredentialsException`)

### Methods
- **CRUD operations**: `createX`, `getXById`, `updateX`, `deleteX`
- **Boolean queries**: `isX`, `hasX`, `existsX` (e.g., `isTokenValid`, `existsByEmail`)
- **Business logic**: Verb + noun (e.g., `registerUser`, `login`, `logout`)

### Variables
- Use **camelCase** for all variables and method names
- Use meaningful, descriptive names (avoid single letters except loop counters)
- Constants: **UPPER_SNAKE_CASE** (e.g., `JWT_EXPIRATION`, `MAX_RETRY_ATTEMPTS`)

### Database Columns
- Use **snake_case** in `@Column(name = "...")` annotations
- Entity fields use camelCase (e.g., `firstName` maps to `first_name`)

---

## Code Style & Formatting

### General Rules
- **Indentation**: 4 spaces (no tabs)
- **Line length**: Aim for 120 characters max
- **Braces**: Use Java style (opening brace on same line)
- **Blank lines**: One blank line between methods, two between major sections

### Lombok Annotations
Use Lombok to reduce boilerplate:
```java
@Data                    // Entities and DTOs
@NoArgsConstructor       // JPA entities (required)
@AllArgsConstructor      // Entities and DTOs
@Builder                 // Response DTOs
```

**Order**: Place `@Data` first, then `@NoArgsConstructor`, then `@AllArgsConstructor`, then `@Builder`

### Entity Pattern
```java
@Entity
@Table(name = "users")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "first_name", nullable = false, length = 100)
    private String firstName;
    
    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}
```

### DTO Pattern
```java
@Data
@NoArgsConstructor
@AllArgsConstructor
public class RegisterUserRequest {
    @NotBlank(message = "First name is required")
    @Size(min = 2, max = 100, message = "First name must be between 2 and 100 characters")
    private String firstName;
}

@Data
@Builder
public class UserResponse {
    private Long id;
    private String firstName;
}
```

### Controller Pattern
```java
@RestController
@RequestMapping("/api/users")
public class UserController {
    private static final Logger logger = LoggerFactory.getLogger(UserController.class);
    
    private final UserService userService;
    
    // Constructor injection (preferred)
    public UserController(UserService userService) {
        this.userService = userService;
    }
    
    @PostMapping("/register")
    public ResponseEntity<UserResponse> registerUser(@Valid @RequestBody RegisterUserRequest request) {
        logger.info("POST /api/users/register - Registering user with email: {}", request.getEmail());
        UserResponse response = userService.registerUser(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
}
```

### Service Pattern
```java
@Service
public class UserService {
    private static final Logger logger = LoggerFactory.getLogger(UserService.class);
    
    private final UserRepository userRepository;
    
    public UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }
    
    @Transactional
    public UserResponse registerUser(RegisterUserRequest request) {
        logger.info("Registering user with email: {}", request.getEmail());
        // Implementation
    }
    
    @Transactional(readOnly = true)
    public UserResponse getUserById(Long userId) {
        // Read-only transactions for queries
    }
}
```

---

## Error Handling

### Custom Exceptions
Create specific exceptions for business logic errors:
```java
public class UserAlreadyExistsException extends RuntimeException {
    public UserAlreadyExistsException(String message) {
        super(message);
    }
}
```

### Global Exception Handler
Use `@RestControllerAdvice` with `@ExceptionHandler`:
```java
@RestControllerAdvice
public class GlobalExceptionHandler {
    
    @ExceptionHandler(UserAlreadyExistsException.class)
    public ResponseEntity<ErrorResponse> handleUserAlreadyExists(UserAlreadyExistsException ex) {
        ErrorResponse error = ErrorResponse.builder()
                .timestamp(LocalDateTime.now())
                .status(HttpStatus.CONFLICT.value())
                .error(HttpStatus.CONFLICT.getReasonPhrase())
                .message(ex.getMessage())
                .build();
        return ResponseEntity.status(HttpStatus.CONFLICT).body(error);
    }
}
```

### HTTP Status Code Mapping
- **201 CREATED**: Resource successfully created (`POST` for new resources)
- **200 OK**: Successful read/update
- **400 BAD_REQUEST**: Validation errors, invalid input
- **401 UNAUTHORIZED**: Authentication failures
- **404 NOT_FOUND**: Resource not found
- **409 CONFLICT**: Resource already exists
- **500 INTERNAL_SERVER_ERROR**: Unexpected errors

---

## Testing Conventions

### Test Structure
```java
@ExtendWith(MockitoExtension.class)
class UserServiceTest {
    
    @Mock
    private UserRepository userRepository;
    
    private UserService userService;
    
    @BeforeEach
    void setUp() {
        userService = new UserService(userRepository);
    }
    
    @Test
    void testRegisterUser_Success() {
        // Arrange
        RegisterUserRequest request = new RegisterUserRequest();
        when(userRepository.save(any())).thenReturn(savedUser);
        
        // Act
        UserResponse response = userService.registerUser(request);
        
        // Assert
        assertNotNull(response);
        verify(userRepository, times(1)).save(any(User.class));
    }
}
```

### Test Naming
- Use descriptive names: `test[MethodName]_[Scenario]`
- Examples: `testRegisterUser_Success`, `testLogin_InvalidCredentials`

---

## Logging Guidelines

- Use **SLF4J** with `LoggerFactory.getLogger(ClassName.class)`
- Log levels:
  - `logger.info()`: Important business events (user registration, login, etc.)
  - `logger.debug()`: Detailed debugging information
  - `logger.warn()`: Recoverable issues (invalid credentials, validation failures)
  - `logger.error()`: Critical errors requiring attention
- Include relevant context in log messages (IDs, emails, etc.)
- Log at method entry for important operations: `logger.info("Registering user with email: {}", email)`

---

## Additional Guidelines

1. **Dependency Injection**: Always use constructor injection (preferred over field injection)
2. **Validation**: Use Jakarta Validation annotations on DTOs (`@NotBlank`, `@Email`, `@Size`, etc.)
3. **Transactions**: Mark service methods with `@Transactional` for write operations
4. **Time Handling**: Always use `UTC` timezone (configured in JVM args: `-Duser.timezone=UTC`)
5. **Passwords**: Never log or expose passwords; always use BCrypt for hashing
6. **JWT**: Store secrets in configuration files, never hardcode
7. **API Endpoints**: Follow RESTful conventions (`/api/{resource}`, use proper HTTP methods)
8. **Comments**: Use JavaDoc for public APIs; inline comments for complex logic only

---

## Configuration Files

- **bootstrap.yml**: Service name and config server connection
- **application.yml** (in config-repo): Service-specific configuration (port, database, etc.)
- Configuration hierarchy: bootstrap.yml → config-server → application.yml → service-specific.yml
