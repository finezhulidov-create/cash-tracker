package dev.zhulidov.cash_tracker.dto;

import dev.zhulidov.cash_tracker.model.User;

public record CategoryCreateRequestDto(String categoryName, UserDto userDto, Long userId) {
}
