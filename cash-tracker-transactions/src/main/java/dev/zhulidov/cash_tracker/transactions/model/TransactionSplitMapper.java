package dev.zhulidov.cash_tracker.transactions.model;

import dev.zhulidov.cash_tracker.transactions.dto.TransactionSplitDto;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface TransactionSplitMapper {

    // Имена полей не совпадают дословно (category -> categoryDto), поэтому маппинг явный.
    // transactionDto игнорируем: TransactionSplit и так лежит внутри Transaction/TransactionDto,
    // если пытаться замапить обратную ссылку на родителя — Transaction -> splits -> split.transactionDto
    // -> Transaction -> splits -> ... уйдёт в бесконечную рекурсию.
    @Mapping(source = "category", target = "categoryDto")
    @Mapping(target = "transactionDto", ignore = true)
    TransactionSplitDto toDto(TransactionSplit split);
}
