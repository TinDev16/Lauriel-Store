package com.lauriel.store.exception;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

class GlobalExceptionHandlerTest {
    private MockMvc mvc;

    @BeforeEach
    void setUp() {
        mvc = MockMvcBuilders.standaloneSetup(new TestController())
                .setControllerAdvice(new GlobalExceptionHandler()).build();
    }

    @Test
    void missingResourceHasConsistent404Body() throws Exception {
        mvc.perform(get("/test/missing")).andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.error").value("Not Found"))
                .andExpect(jsonPath("$.timestamp").exists());
    }

    @Test
    void invalidRequestHas400Body() throws Exception {
        mvc.perform(get("/test/bad")).andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));
        mvc.perform(post("/test/validation").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"\"}"))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.message").value("Yêu cầu không hợp lệ"));
        mvc.perform(post("/test/validation").contentType(MediaType.APPLICATION_JSON).content("{"))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.status").value(400));
    }

    @Test
    void databaseConstraintDoesNotExposeSql() throws Exception {
        mvc.perform(get("/test/constraint")).andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("Dữ liệu trùng lặp hoặc vi phạm ràng buộc"))
                .andExpect(jsonPath("$.trace").doesNotExist());
    }

    @Test
    void unexpectedFailureDoesNotExposeInternalMessage() throws Exception {
        mvc.perform(get("/test/unexpected")).andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.message").value("Có lỗi hệ thống, vui lòng thử lại sau"))
                .andExpect(jsonPath("$.trace").doesNotExist());
    }

    @Test
    void wrongHttpMethodKeeps405AndErrorFormat() throws Exception {
        mvc.perform(post("/test/missing")).andExpect(status().isMethodNotAllowed())
                .andExpect(jsonPath("$.status").value(405))
                .andExpect(jsonPath("$.error").value("Method Not Allowed"));
    }

    // Test-only endpoints; no application REST controllers are created in Phase 1.
    @RestController
    static class TestController {
        @GetMapping("/test/missing")
        String missing() {
            throw new ResourceNotFoundException("Không tìm thấy");
        }

        @GetMapping("/test/bad")
        String bad() {
            throw new BadRequestException("Dữ liệu không hợp lệ");
        }

        @GetMapping("/test/constraint")
        String constraint() {
            throw new DataIntegrityViolationException("Internal SQL details");
        }

        @GetMapping("/test/unexpected")
        String unexpected() {
            throw new IllegalStateException("Internal implementation details");
        }

        @PostMapping("/test/validation")
        String validation(@Valid @RequestBody TestRequest request) {
            return request.name();
        }
    }

    record TestRequest(@NotBlank String name) {
    }
}
