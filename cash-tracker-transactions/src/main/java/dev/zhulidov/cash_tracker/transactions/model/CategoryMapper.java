package dev.zhulidov.cash_tracker.transactions.model;

import dev.zhulidov.cash_tracker.transactions.dto.CategoryDto;
import dev.zhulidov.cash_tracker.transactions.dto.TransactionDto;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.springframework.data.domain.Page;

@Mapper(componentModel = "spring")
public interface CategoryMapper {
    // "user" в CategoryDto — это имя владельца категории, а не прямое поле сущности
    // (у Category есть только userId). Оно заполняется отдельно в сервисе через
    // UserLookUpService, поэтому маппер его сюда не трогает.
    @Mapping(target = "user", ignore = true)
    CategoryDto toDto(Category category);

    default Page<CategoryDto> toDtoPage(Page<Category> pages) {
        return pages.map(this::toDto);
    }
}
