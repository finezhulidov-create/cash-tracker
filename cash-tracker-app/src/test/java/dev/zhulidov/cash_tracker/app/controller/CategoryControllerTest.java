package dev.zhulidov.cash_tracker.app.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import dev.zhulidov.cash_tracker.app.service.CustomUserDetailsService;
import dev.zhulidov.cash_tracker.common.exception.InaccessibleResourceException;
import dev.zhulidov.cash_tracker.common.exception.ResourceNotFoundException;
import dev.zhulidov.cash_tracker.common.security.UserPrincipal;
import dev.zhulidov.cash_tracker.common.service.JwtService;
import dev.zhulidov.cash_tracker.transactions.controller.CategoryController;
import dev.zhulidov.cash_tracker.transactions.dto.CategoryCreateRequestDto;
import dev.zhulidov.cash_tracker.transactions.dto.CategoryDto;
import dev.zhulidov.cash_tracker.transactions.dto.CategoryUpdateRequestDto;
import dev.zhulidov.cash_tracker.transactions.dto.TransactionSplitDto;
import dev.zhulidov.cash_tracker.transactions.service.CategoryService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(CategoryController.class)
class CategoryControllerTest {

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private CategoryService categoryService;

    // JwtFilter — прямой @Component, реализующий Filter, поэтому @WebMvcTest подхватывает
    // его автоматически. Его зависимости нужно замокать, чтобы контекст вообще поднялся,
    // хотя в самих тестах мы аутентификацию подставляем напрямую, минуя JwtFilter.
    @MockitoBean
    private JwtService jwtService;
    @MockitoBean
    private CustomUserDetailsService customUserDetailsService;

    private static final Long OWNER_ID = 1L;
    private static final Long CATEGORY_ID = 10L;

    private UsernamePasswordAuthenticationToken authToken() {
        var principal = new UserPrincipal(OWNER_ID, "owner@test.com", "hash");
        return new UsernamePasswordAuthenticationToken(principal, null, List.of());
    }

    // ==================== POST /categories ====================

    @Test
    void createCategory_whenValidAndAuthenticated_returns200() throws Exception {
        var request = new CategoryCreateRequestDto("Food");
        var responseDto = new CategoryDto(CATEGORY_ID, "Food", null);
        when(categoryService.createCategory(eq(request), eq(OWNER_ID))).thenReturn(responseDto);

        mockMvc.perform(post("/categories")
                        .with(authentication(authToken()))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(CATEGORY_ID))
                .andExpect(jsonPath("$.categoryName").value("Food"));
    }

    @Test
    void createCategory_whenBlankName_returns400() throws Exception {
        var request = new CategoryCreateRequestDto("");

        mockMvc.perform(post("/categories")
                        .with(authentication(authToken()))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void createCategory_whenNameTooLong_returns400() throws Exception {
        var request = new CategoryCreateRequestDto("a".repeat(51));

        mockMvc.perform(post("/categories")
                        .with(authentication(authToken()))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void createCategory_whenUnauthenticated_returns401() throws Exception {
        var request = new CategoryCreateRequestDto("Food");

        mockMvc.perform(post("/categories")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized());
    }

    // ==================== GET /categories/{id} ====================

    @Test
    void getCategory_whenExists_returns200() throws Exception {
        var responseDto = new CategoryDto(CATEGORY_ID, "Food", null);
        when(categoryService.getCategoryById(CATEGORY_ID, OWNER_ID)).thenReturn(responseDto);

        mockMvc.perform(get("/categories/{categoryId}", CATEGORY_ID)
                        .with(authentication(authToken())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.categoryName").value("Food"));
    }

    @Test
    void getCategory_whenIdNotPositive_returns400() throws Exception {
        mockMvc.perform(get("/categories/{categoryId}", -5)
                        .with(authentication(authToken())))
                .andExpect(status().isBadRequest());
    }

    @Test
    void getCategory_whenNotFound_returns404() throws Exception {
        when(categoryService.getCategoryById(CATEGORY_ID, OWNER_ID))
                .thenThrow(new ResourceNotFoundException("Category not found"));

        mockMvc.perform(get("/categories/{categoryId}", CATEGORY_ID)
                        .with(authentication(authToken())))
                .andExpect(status().isNotFound());
    }

    @Test
    void getCategory_whenBelongsToAnotherUser_returns404() throws Exception {
        when(categoryService.getCategoryById(CATEGORY_ID, OWNER_ID))
                .thenThrow(new InaccessibleResourceException("Resource not Found"));

        mockMvc.perform(get("/categories/{categoryId}", CATEGORY_ID)
                        .with(authentication(authToken())))
                .andExpect(status().isNotFound());
    }

    // ==================== DELETE /categories/{id} ====================

    @Test
    void deleteCategory_whenExists_returns200() throws Exception {
        mockMvc.perform(delete("/categories/{categoryId}", CATEGORY_ID)
                        .with(authentication(authToken()))
                        .with(csrf()))
                .andExpect(status().isOk());
    }

    @Test
    void deleteCategory_whenIdNotPositive_returns400() throws Exception {
        mockMvc.perform(delete("/categories/{categoryId}", 0)
                        .with(authentication(authToken()))
                        .with(csrf()))
                .andExpect(status().isBadRequest());
    }

    @Test
    void deleteCategory_whenNotFound_returns404() throws Exception {
        org.mockito.Mockito.doThrow(new ResourceNotFoundException("Category not found"))
                .when(categoryService).deleteCategoryById(CATEGORY_ID, OWNER_ID);

        mockMvc.perform(delete("/categories/{categoryId}", CATEGORY_ID)
                        .with(authentication(authToken()))
                        .with(csrf()))
                .andExpect(status().isNotFound());
    }

    // ==================== PUT /categories/{id} ====================

    @Test
    void updateCategory_whenValid_returns200() throws Exception {
        var request = new CategoryUpdateRequestDto("Groceries");
        var responseDto = new CategoryDto(CATEGORY_ID, "Groceries", null);
        when(categoryService.updateCategory(CATEGORY_ID, "Groceries", OWNER_ID)).thenReturn(responseDto);

        mockMvc.perform(put("/categories/{categoryId}", CATEGORY_ID)
                        .with(authentication(authToken()))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.categoryName").value("Groceries"));
    }

    @Test
    void updateCategory_whenBlankName_returns400() throws Exception {
        var request = new CategoryUpdateRequestDto("");

        mockMvc.perform(put("/categories/{categoryId}", CATEGORY_ID)
                        .with(authentication(authToken()))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void updateCategory_whenBelongsToAnotherUser_returns404() throws Exception {
        var request = new CategoryUpdateRequestDto("Groceries");
        when(categoryService.updateCategory(CATEGORY_ID, "Groceries", OWNER_ID))
                .thenThrow(new InaccessibleResourceException("Resource not Found"));

        mockMvc.perform(put("/categories/{categoryId}", CATEGORY_ID)
                        .with(authentication(authToken()))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound());
    }

    // ==================== GET /categories/mine ====================

    @Test
    void getCategoriesByUser_returns200WithPage() throws Exception {
        var dto1 =  new CategoryDto(1L, "Food", null);
        var dto2 = new CategoryDto(2L, "Transport", null);
        var pageable = PageRequest.of(0,2);
        var pageDto = new PageImpl<CategoryDto>(List.of(dto1, dto2),pageable,2);
        when(categoryService.getCategoriesByUserId(OWNER_ID, pageable))
                .thenReturn(pageDto );

        mockMvc.perform(get("/categories/mine")
                        .with(authentication(authToken())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2));
    }

    @Test
    void getCategoriesByUser_whenEmpty_returns200WithEmptyList() throws Exception {
        when(categoryService.getCategoriesByUserId(OWNER_ID)).thenReturn(List.of());

        mockMvc.perform(get("/categories/mine")
                        .with(authentication(authToken())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }

    // ==================== GET /categories/{id}/splits ====================

    @Test
    void getSplits_whenExists_returns200() throws Exception {
        var dto = new TransactionSplitDto(new BigDecimal("100"), null, null);
        var page = new PageImpl<>(List.of(dto), PageRequest.of(0, 10), 1);
        when(categoryService.getSplitsByCategory(eq(OWNER_ID), eq(CATEGORY_ID), any())).thenReturn(page);

        mockMvc.perform(get("/categories/{categoryId}/splits", CATEGORY_ID)
                        .with(authentication(authToken())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(1));
    }

    @Test
    void getSplits_whenIdNotPositive_returns400() throws Exception {
        mockMvc.perform(get("/categories/{categoryId}/splits", -1)
                        .with(authentication(authToken())))
                .andExpect(status().isBadRequest());
    }

    @Test
    void getSplits_whenCategoryNotFound_returns404() throws Exception {
        when(categoryService.getSplitsByCategory(eq(OWNER_ID), eq(CATEGORY_ID), any()))
                .thenThrow(new ResourceNotFoundException("Category not found"));

        mockMvc.perform(get("/categories/{categoryId}/splits", CATEGORY_ID)
                        .with(authentication(authToken())))
                .andExpect(status().isNotFound());
    }
}