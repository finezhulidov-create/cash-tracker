package dev.zhulidov.cash_tracker.app.model;

import dev.zhulidov.cash_tracker.app.dto.UserDto;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface UserMapper {
    UserDto toDto(User user);
}
