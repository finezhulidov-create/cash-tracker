package dev.zhulidov.cash_tracker.app.controller;

import dev.zhulidov.cash_tracker.transactions.controller.CategoryController;
import dev.zhulidov.cash_tracker.transactions.service.CategoryService;
import dev.zhulidov.cash_tracker.app.service.CustomUserDetailsService;
import dev.zhulidov.cash_tracker.transactions.service.ExpenseService;
import dev.zhulidov.cash_tracker.common.service.JwtService;
import org.junit.jupiter.api.Test;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import org.springframework.beans.factory.annotation.Autowired;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(CategoryController.class)
public class CategoryControllerTest {
    @MockitoBean
  private   CategoryService service;
    @MockitoBean
    private JwtService jwtService;
    @MockitoBean
    private ExpenseService expenseService;
    @MockitoBean
    private CustomUserDetailsService userDetailsService;
    @Autowired
   private MockMvc mockMvc;

    @Test
    @WithMockUser
    void getCategory_withNegativeId_returns400() throws Exception {
        mockMvc.perform(get("/categories/{categoryId}",-5))
                .andExpect(status().isBadRequest());
    }

}
