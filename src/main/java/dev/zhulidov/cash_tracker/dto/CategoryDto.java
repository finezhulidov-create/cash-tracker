package dev.zhulidov.cash_tracker.dto;

import dev.zhulidov.cash_tracker.model.Expense;

import java.util.List;

public record CategoryDto(Long id,String categoryName, String user) {
}
