package dev.zhulidov.cash_tracker.transactions.model;

import dev.zhulidov.cash_tracker.transactions.dto.TransactionDto;
import dev.zhulidov.cash_tracker.transactions.dto.TransactionSplitDto;
import org.mapstruct.Mapper;
import org.springframework.data.domain.Page;

import java.util.List;

@Mapper(componentModel = "spring")
public interface TransactionMapper {
    TransactionDto toDto(Transaction transaction);
    List<TransactionSplitDto> toDtoList(List<TransactionSplit> splits);
    // MapStruct не умеет сам мапить Page<X> -> Page<Y> (нет единственной реализации Page,
    // плюс нужно сохранить метаданные пагинации), поэтому пишем вручную через Page.map(...),
    // который как раз трансформирует контент, сохраняя номер страницы/размер/total.
    default Page<TransactionDto> toDtoPage(Page<Transaction> pages) {
        return pages.map(this::toDto);
    }
}
