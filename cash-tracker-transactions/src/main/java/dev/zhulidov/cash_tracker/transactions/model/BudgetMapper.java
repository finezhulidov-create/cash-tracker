package dev.zhulidov.cash_tracker.transactions.model;

import dev.zhulidov.cash_tracker.transactions.dto.BudgetDto;
import dev.zhulidov.cash_tracker.transactions.dto.BudgetUpdateRequest;
import org.mapstruct.Mapper;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;

@Mapper(componentModel = "spring", nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
public interface BudgetMapper {
    BudgetDto toDto(Budget budget);
    void updateEntityFromDto(BudgetUpdateRequest request,@MappingTarget Budget budget);


}
