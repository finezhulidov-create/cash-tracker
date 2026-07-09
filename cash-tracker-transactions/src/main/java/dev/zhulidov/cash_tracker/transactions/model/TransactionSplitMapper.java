package dev.zhulidov.cash_tracker.transactions.model;

import dev.zhulidov.cash_tracker.transactions.dto.TransactionSplitDto;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface TransactionSplitMapper {
    TransactionSplitDto toDto(TransactionSplit split);
}
