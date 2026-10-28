package dev.zhulidov.cash_tracker.common.exception;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.Valid;
import jakarta.validation.Validation;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.mapping.PropertyReferenceException;
import org.springframework.data.util.TypeInformation;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.InsufficientAuthenticationException;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Set;

import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class GlobalExceptionHandlerTest {

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(new ThrowingController())
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    // ==================== доменные исключения ====================

    @Test
    void resourceNotFound_returns404WithResourceNotFoundCode() throws Exception {
        mockMvc.perform(get("/test/not-found"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.message").value("Category not found"))
                .andExpect(jsonPath("$.errorCode").value("RESOURCE_NOT_FOUND"))
                .andExpect(jsonPath("$.timestamp").exists());
    }

    @Test
    void inaccessibleResource_returns404SoThatExistenceIsNotRevealed() throws Exception {
        mockMvc.perform(get("/test/inaccessible"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.errorCode").value("ACCESS_DENIED"));
    }

    @Test
    void resourceAlreadyExists_returns409WithDefaultCode() throws Exception {
        mockMvc.perform(get("/test/conflict"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.message").value("Budget exists"))
                .andExpect(jsonPath("$.errorCode").value("RESOURCE_ALREADY_EXISTS"));
    }

    @Test
    void resourceAlreadyExists_withCustomCode_returnsThatCode() throws Exception {
        mockMvc.perform(get("/test/conflict-email"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.errorCode").value("EMAIL_ALREADY_EXISTS"));
    }

    @Test
    void resourceConflict_returnsItsOwnCode() throws Exception {
        mockMvc.perform(get("/test/in-use"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.errorCode").value("CATEGORY_IN_USE"))
                .andExpect(jsonPath("$.message").value("Category has transactions"));
    }

    @Test
    void transactionSplitMismatch_returns400() throws Exception {
        mockMvc.perform(get("/test/split-mismatch"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.errorCode").value("NOT_VALID_TRANSACTION_SPLIT_SUM"));
    }

    @Test
    void invalidRequest_returns400() throws Exception {
        mockMvc.perform(get("/test/invalid-request"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("INVALID_REQUEST"))
                .andExpect(jsonPath("$.message").value("from must not be after to"));
    }

    @Test
    void unknownSortProperty_returns400() throws Exception {
        mockMvc.perform(get("/test/sort"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("INVALID_SORT"))
                .andExpect(jsonPath("$.message").value("Unknown sort property: foo"));
    }

    @Test
    void dataIntegrityViolation_returns409() throws Exception {
        mockMvc.perform(get("/test/integrity"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.errorCode").value("DATA_INTEGRITY_VIOLATION"))
                // детали БД клиенту не отдаём
                .andExpect(jsonPath("$.message").value("The request conflicts with existing data"));
    }

    // ==================== валидация ====================

    @Test
    void constraintViolation_returns400WithFieldErrors() throws Exception {
        mockMvc.perform(get("/test/constraint"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.errors.name").exists());
    }

    @Test
    void invalidRequestBody_returns400WithFieldErrorsAndValidationCode() throws Exception {
        mockMvc.perform(post("/test/body")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.errorCode").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.message").value("Validation failed"))
                .andExpect(jsonPath("$.errors.name").exists());
    }

    // ==================== стандартные исключения Spring MVC ====================

    @Test
    void malformedJson_returns400WithoutParserDetails() throws Exception {
        mockMvc.perform(post("/test/body")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{not json"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("MALFORMED_REQUEST"))
                .andExpect(jsonPath("$.message", not(containsString("Jackson"))));
    }

    @Test
    void missingRequestBody_returns400() throws Exception {
        mockMvc.perform(post("/test/body").contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("MALFORMED_REQUEST"));
    }

    @Test
    void missingRequiredParameter_returns400WithParameterName() throws Exception {
        mockMvc.perform(get("/test/param"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("MISSING_PARAMETER"))
                .andExpect(jsonPath("$.errors.number").exists());
    }

    @Test
    void parameterOfWrongType_returns400WithParameterName() throws Exception {
        mockMvc.perform(get("/test/param").param("number", "abc"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("INVALID_PARAMETER"))
                .andExpect(jsonPath("$.errors.number").exists());
    }

    @Test
    void builtInMethodValidation_returns400WithParameterName() throws Exception {
        // контроллер без @Validated: Spring 6.1+ сам валидирует параметры и бросает HandlerMethodValidationException
        var standalone = MockMvcBuilders.standaloneSetup(new UnvalidatedClassController())
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();

        standalone.perform(get("/plain/positive").param("value", "-5"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.errors.value").exists());
    }

    @Test
    void unsupportedHttpMethod_returns405InErrorFormat() throws Exception {
        mockMvc.perform(post("/test/not-found"))
                .andExpect(status().isMethodNotAllowed())
                .andExpect(jsonPath("$.status").value(405))
                .andExpect(jsonPath("$.errorCode").value("METHOD_NOT_ALLOWED"));
    }

    @Test
    void unsupportedMediaType_returns415InErrorFormat() throws Exception {
        mockMvc.perform(post("/test/body")
                        .contentType(MediaType.TEXT_PLAIN)
                        .content("name"))
                .andExpect(status().isUnsupportedMediaType())
                .andExpect(jsonPath("$.status").value(415))
                .andExpect(jsonPath("$.errorCode").value("UNSUPPORTED_MEDIA_TYPE"));
    }

    // ==================== безопасность ====================

    @Test
    void badCredentials_returns401WithInvalidCredentialsCode() throws Exception {
        mockMvc.perform(get("/test/bad-credentials"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.errorCode").value("INVALID_CREDENTIALS"))
                .andExpect(jsonPath("$.message").value("Invalid email or password"));
    }

    @Test
    void otherAuthenticationException_returns401() throws Exception {
        mockMvc.perform(get("/test/auth-required"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.errorCode").value("UNAUTHORIZED"));
    }

    @Test
    void accessDenied_returns403() throws Exception {
        mockMvc.perform(get("/test/denied"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.errorCode").value("FORBIDDEN"));
    }

    // ==================== непредвиденное ====================

    @Test
    void unexpectedException_returns500WithoutLeakingDetails() throws Exception {
        mockMvc.perform(get("/test/boom"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.status").value(500))
                .andExpect(jsonPath("$.errorCode").value("INTERNAL_ERROR"))
                .andExpect(jsonPath("$.message").value("Unexpected server error"))
                .andExpect(jsonPath("$.message", not(containsString("secret detail"))));
    }

    record Body(@NotBlank String name) {
    }

    @RestController
    @RequestMapping("/plain")
    public static class UnvalidatedClassController {

        @GetMapping("/positive")
        public void positive(@RequestParam("value") @Positive int value) {
        }
    }

    @RestController
    @RequestMapping("/test")
    public static class ThrowingController {

        @GetMapping("/not-found")
        public void notFound() {
            throw new ResourceNotFoundException("Category not found");
        }

        @GetMapping("/inaccessible")
        public void inaccessible() {
            throw new InaccessibleResourceException("Resource not Found");
        }

        @GetMapping("/conflict")
        public void conflict() {
            throw new ResourceAlreadyExistsException("Budget exists");
        }

        @GetMapping("/conflict-email")
        public void conflictEmail() {
            throw new ResourceAlreadyExistsException("EMAIL_ALREADY_EXISTS", "Email is already registered");
        }

        @GetMapping("/in-use")
        public void inUse() {
            throw new ResourceConflictException("CATEGORY_IN_USE", "Category has transactions");
        }

        @GetMapping("/split-mismatch")
        public void splitMismatch() {
            throw new TransactionSplitMismatchException("mismatch");
        }

        @GetMapping("/invalid-request")
        public void invalidRequest() {
            throw new InvalidRequestException("from must not be after to");
        }

        @GetMapping("/sort")
        public void sort() {
            throw new PropertyReferenceException("foo", TypeInformation.of(Object.class), List.of());
        }

        @GetMapping("/integrity")
        public void integrity() {
            throw new DataIntegrityViolationException("insert failed", new RuntimeException("duplicate key value violates unique constraint"));
        }

        @GetMapping("/constraint")
        public void constraint() {
            Set<ConstraintViolation<Body>> violations = Validation.buildDefaultValidatorFactory()
                    .getValidator().validate(new Body(""));
            throw new ConstraintViolationException(violations);
        }

        @PostMapping("/body")
        public void body(@RequestBody @Valid Body body) {
        }

        @GetMapping("/param")
        public void param(@RequestParam int number) {
        }

        @GetMapping("/bad-credentials")
        public void badCredentials() {
            throw new BadCredentialsException("bad");
        }

        @GetMapping("/auth-required")
        public void authRequired() {
            throw new InsufficientAuthenticationException("full authentication is required");
        }

        @GetMapping("/denied")
        public void denied() {
            throw new AccessDeniedException("denied");
        }

        @GetMapping("/boom")
        public void boom() {
            throw new IllegalStateException("secret detail: jdbc:postgresql://db/prod");
        }
    }
}
