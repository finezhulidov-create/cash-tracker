package dev.zhulidov.cash_tracker.transactions.model;

import dev.zhulidov.cash_tracker.transactions.dto.CategoryDto;
import dev.zhulidov.cash_tracker.transactions.dto.TransactionDto;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.springframework.data.domain.Page;

@Mapper(componentModel = "spring")
public interface CategoryMapper {

    @Mapping(target = "userId")
    CategoryDto toDto(Category category);

    default Page<CategoryDto> toDtoPage(Page<Category> pages) {
        return pages.map(this::toDto);
    }
}
