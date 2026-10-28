package dev.zhulidov.cash_tracker.transactions.model;

import dev.zhulidov.cash_tracker.transactions.dto.TransactionDto;
import org.mapstruct.Mapper;
import org.springframework.data.domain.Page;

@Mapper(componentModel = "spring", uses = TransactionSplitMapper.class)
public interface TransactionMapper {
    // splits маппятся через TransactionSplitMapper
    TransactionDto toDto(Transaction transaction);

    // MapStruct не умеет сам мапить Page<X> -> Page<Y> (нет единственной реализации Page,
    // плюс нужно сохранить метаданные пагинации), поэтому пишем вручную через Page.map(...),
    // который как раз трансформирует контент, сохраняя номер страницы/размер/total.
    default Page<TransactionDto> toDtoPage(Page<Transaction> pages) {
        return pages.map(this::toDto);
    }
}
