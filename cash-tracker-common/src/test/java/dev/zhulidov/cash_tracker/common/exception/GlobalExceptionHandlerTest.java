package dev.zhulidov.cash_tracker.common.exception;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.Validation;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Set;

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
    void resourceAlreadyExists_returns409() throws Exception {
        mockMvc.perform(get("/test/conflict"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.message").value("Budget exists"))
                .andExpect(jsonPath("$.errorCode").value("RESOURCE_ALREADY_EXISTS"));
    }

    @Test
    void transactionSplitMismatch_returns400() throws Exception {
        mockMvc.perform(get("/test/split-mismatch"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.errorCode").value("NOT_VALID_TRANSACTION_SPLIT_SUM"));
    }

    @Test
    void constraintViolation_returns400WithFieldErrors() throws Exception {
        mockMvc.perform(get("/test/constraint"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.errors.name").exists());
    }

    @Test
    void invalidRequestBody_returns400() throws Exception {
        mockMvc.perform(post("/test/body")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.errorCode").exists());
    }

    record Body(@NotBlank String name) {
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

        @GetMapping("/split-mismatch")
        public void splitMismatch() {
            throw new TransactionSplitMismatchException("mismatch");
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
    }
}
