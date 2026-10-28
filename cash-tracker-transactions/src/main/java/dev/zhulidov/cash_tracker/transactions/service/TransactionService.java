package dev.zhulidov.cash_tracker.transactions.service;

import dev.zhulidov.cash_tracker.common.exception.InvalidRequestException;
import dev.zhulidov.cash_tracker.common.exception.ResourceNotFoundException;
import dev.zhulidov.cash_tracker.common.util.SecurityUtils;
import dev.zhulidov.cash_tracker.common.util.SplitValidationUtils;
import dev.zhulidov.cash_tracker.transactions.dto.SplitRequest;
import dev.zhulidov.cash_tracker.transactions.dto.TransactionCreateRequest;
import dev.zhulidov.cash_tracker.transactions.dto.TransactionDto;
import dev.zhulidov.cash_tracker.transactions.dto.TransactionSearchCriteria;
import dev.zhulidov.cash_tracker.transactions.dto.TransactionUpdateRequest;
import dev.zhulidov.cash_tracker.transactions.model.Category;
import dev.zhulidov.cash_tracker.transactions.model.Transaction;
import dev.zhulidov.cash_tracker.transactions.model.TransactionMapper;
import dev.zhulidov.cash_tracker.transactions.model.TransactionSplit;
import dev.zhulidov.cash_tracker.transactions.repository.CategoryRepository;
import dev.zhulidov.cash_tracker.transactions.repository.TransactionRepository;
import dev.zhulidov.cash_tracker.transactions.util.TransactionSpecifications;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.CacheManager;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class TransactionService {
    private final TransactionRepository transactionRepository;
    private final CategoryRepository categoryRepository;
    private final TransactionMapper transactionMapper;
    private final CacheManager cacheManager;
    private final Clock clock;

    @Transactional(rollbackFor = RuntimeException.class)
    public TransactionDto createTransaction(TransactionCreateRequest request, Long userId){
        assertSplitsMatchAmount(request.amount(), request.splits());
        var trans = Transaction.builder()
                .amount(request.amount())
                .description(request.description())
                // время «как ввёл пользователь»; если не передано, берём текущее серверное (UTC)
                .dateTime(request.dateTime() != null ? request.dateTime() : LocalDateTime.now(clock))
                .userId(userId)
                .build();

        List<TransactionSplit> splits = buildSplits(request.splits(), userId, trans);
        trans.setSplits(splits);
        transactionRepository.save(trans);
        evictSplitsCache(userId, splits);
        return transactionMapper.toDto(trans);
    }

    @Transactional(rollbackFor = RuntimeException.class)
    public void deleteTransactionById(Long id, Long userId){
        var trans = findOwnedTransaction(id, userId);
        List<TransactionSplit> splits = new ArrayList<>(trans.getSplits());
        transactionRepository.delete(trans);
        evictSplitsCache(userId, splits);
    }


    @Transactional(rollbackFor = RuntimeException.class)
    public TransactionDto updateTransaction(TransactionUpdateRequest request, Long id, Long userId){
        var trans = findOwnedTransaction(id, userId);
        assertSplitsMatchAmount(request.amount(), request.splits());
        List<TransactionSplit> oldSplits = new ArrayList<>(trans.getSplits());
        List<TransactionSplit> newSplits = buildSplits(request.splits(), userId, trans);

        trans.setDescription(request.description());
        trans.setAmount(request.amount());
        trans.setDateTime(request.dateTime());
        trans.getSplits().clear();
        trans.getSplits().addAll(newSplits);
        // flush нужен, чтобы у новых частей появились id до маппинга в ответ
        transactionRepository.saveAndFlush(trans);
        evictSplitsCache(userId, oldSplits);
        evictSplitsCache(userId, newSplits);
        return transactionMapper.toDto(trans);
    }

    @Transactional(readOnly = true)
    public TransactionDto getById(Long id, Long userId){
        return transactionMapper.toDto(findOwnedTransaction(id, userId));
    }

    public BigDecimal getTotalAmountByDateRange(Long userId, LocalDateTime from, LocalDateTime to){
        if (from.isAfter(to)) {
            throw new InvalidRequestException("'from' must not be after 'to'");
        }
        return transactionRepository.sumAmountByUserIdAndDateTimeBetween(userId, from, to);
    }

    @Transactional(readOnly = true)
    public Page<TransactionDto> getTransactions(Long userId, TransactionSearchCriteria criteria, Pageable pageable){
        if (criteria.getFrom() != null && criteria.getTo() != null && criteria.getFrom().isAfter(criteria.getTo())) {
            throw new InvalidRequestException("'from' must not be after 'to'");
        }
        if (criteria.getMinAmount() != null && criteria.getMaxAmount() != null
                && criteria.getMinAmount().compareTo(criteria.getMaxAmount()) > 0) {
            throw new InvalidRequestException("'minAmount' must not be greater than 'maxAmount'");
        }
        Specification<Transaction> spec =
                TransactionSpecifications.hasUserId(userId)
                .and(TransactionSpecifications.hasCategoryId(criteria.getCategoryId()))
                .and(TransactionSpecifications.amountBetween(criteria.getMinAmount(),criteria.getMaxAmount()))
                .and(TransactionSpecifications.dateBetween(criteria.getFrom(),criteria.getTo()));
        var page = transactionRepository.findAll(spec,pageable);
        return transactionMapper.toDtoPage(page);
    }

    private Transaction findOwnedTransaction(Long id, Long userId){
        var trans = transactionRepository.findById(id)
                .orElseThrow(()-> new ResourceNotFoundException("Transaction not found"));
        SecurityUtils.assertOwner(trans.getUserId(), userId);
        return trans;
    }

    private void assertSplitsMatchAmount(BigDecimal amount, List<SplitRequest> splits){
        SplitValidationUtils.assertSplit(amount, splits.stream().map(SplitRequest::amount).toList());
    }

    /** Каждая категория проверяется на существование и владельца один раз, даже если встречается в нескольких частях. */
    private List<TransactionSplit> buildSplits(List<SplitRequest> requests, Long userId, Transaction trans){
        Map<Long, Category> categories = new HashMap<>();
        return requests.stream().map(split -> {
            Category category = categories.computeIfAbsent(split.categoryId(), categoryId -> {
                var found = categoryRepository.findById(categoryId)
                        .orElseThrow(() -> new ResourceNotFoundException("Category not found"));
                SecurityUtils.assertOwner(found.getUserId(), userId);
                return found;
            });
            return TransactionSplit.builder()
                    .category(category)
                    .amount(split.amount())
                    .transaction(trans)
                    .build();
        }).toList();
    }

    private void evictSplitsCache(Long userId, List<TransactionSplit> splits){
        var cache = cacheManager.getCache("splits");
        if (cache != null){
            splits.stream()
                    .map(split -> split.getCategory().getId())
                    .distinct()
                    .forEach(catId -> cache.evict(userId + ":" + catId));
        }
    }
}
