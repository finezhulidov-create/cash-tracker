package dev.zhulidov.cash_tracker.transactions.model;

import dev.zhulidov.cash_tracker.transactions.dto.BudgetDto;
import dev.zhulidov.cash_tracker.transactions.dto.BudgetUpdateRequest;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;

@Mapper(componentModel = "spring", nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
public interface BudgetMapper {
    BudgetDto toDto(Budget budget);

    // Меняется только лимит. period сервис нормализует до первого числа месяца сам
    // (иначе маппер затёр бы нормализованное значение сырой датой из запроса).
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "userId", ignore = true)
    @Mapping(target = "category", ignore = true)
    @Mapping(target = "period", ignore = true)
    void updateEntityFromDto(BudgetUpdateRequest request, @MappingTarget Budget budget);
}
