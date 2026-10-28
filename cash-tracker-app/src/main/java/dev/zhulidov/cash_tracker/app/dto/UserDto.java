package dev.zhulidov.cash_tracker.app.dto;

import java.math.BigDecimal;

public record UserDto(String name, String email, BigDecimal balance) {
}
