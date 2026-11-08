package dev.zhulidov.cash_tracker.transactions.importpdf;

import dev.zhulidov.cash_tracker.transactions.dto.ImportKey;
import dev.zhulidov.cash_tracker.transactions.dto.ImportPreviewItem;
import dev.zhulidov.cash_tracker.transactions.dto.TransactionCreateRequest;
import dev.zhulidov.cash_tracker.transactions.model.ExistingTx;
import dev.zhulidov.cash_tracker.transactions.repository.TransactionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;

@Service
@RequiredArgsConstructor
public class DuplicateDetector {

    private final TransactionRepository repository;

    public List<ImportPreviewItem> markDupl(List<TransactionCreateRequest> operations, Long userId){
        if (operations.isEmpty()) return List.of();
        Map<ImportKey, Integer> counter = new HashMap<>();
        List<ImportPreviewItem> preview = new ArrayList<>();
        LocalDateTime min = operations.stream().map(TransactionCreateRequest::dateTime)
                .min(Comparator.naturalOrder()).orElseThrow();
        LocalDateTime max = operations.stream().map(TransactionCreateRequest::dateTime)
                .max(Comparator.naturalOrder()).orElseThrow();
        var from = min.truncatedTo(ChronoUnit.MINUTES);
        var to = max.truncatedTo(ChronoUnit.MINUTES).plusMinutes(1);
        List<ExistingTx> baseKey = repository.findForDedup(userId,from, to);
        for (ExistingTx tx: baseKey){
            var key = ImportKey.of(tx.getDateTime(),tx.getAmount());
            counter.merge(key,1,Integer::sum);
        }
        for (TransactionCreateRequest op: operations){
            ImportKey key = ImportKey.of(op.dateTime(),op.amount());
            int left = counter.getOrDefault(key,0);
            boolean duplicate = left > 0;
            if (duplicate){
                counter.put(key, left - 1);
            }
            preview.add(new ImportPreviewItem(op.description(), op.amount(),
            op.dateTime(),op.splits(),duplicate));
        }
        return preview;


    }
}
