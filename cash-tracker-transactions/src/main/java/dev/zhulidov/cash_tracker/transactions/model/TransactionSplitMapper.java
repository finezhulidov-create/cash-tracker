package dev.zhulidov.cash_tracker.transactions.model;

import dev.zhulidov.cash_tracker.transactions.dto.TransactionSplitDto;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.springframework.data.domain.Page;

@Mapper(componentModel = "spring", uses = CategoryMapper.class)
public interface TransactionSplitMapper {

    // Родительскую транзакцию целиком не маппим (иначе Transaction -> splits -> split -> Transaction ...
    // уйдёт в бесконечную рекурсию), отдаём только её id: читается из прокси без загрузки из БД.
    @Mapping(source = "transaction.id", target = "transactionId")
    TransactionSplitDto toDto(TransactionSplit split);

    default Page<TransactionSplitDto> toDtoPage(Page<TransactionSplit> pages) {
        return pages.map(this::toDto);
    }
}
