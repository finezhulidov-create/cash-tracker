package dev.zhulidov.cash_tracker.transactions.service;

import dev.zhulidov.cash_tracker.common.exception.ResourceNotFoundException;
import dev.zhulidov.cash_tracker.common.util.SecurityUtils;
import dev.zhulidov.cash_tracker.common.util.SplitValidationUtils;
import dev.zhulidov.cash_tracker.transactions.dto.*;
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
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class TransactionService {
    private final TransactionRepository transactionRepository;
    private final CategoryRepository categoryRepository;
    private final TransactionMapper transactionMapper;
    private final CacheManager cacheManager;

    @Transactional(rollbackFor = RuntimeException.class)
    public TransactionDto createTransaction(TransactionCreateRequest request, Long userId){
              var trans = Transaction.builder()
                .amount(request.amount())
                .description(request.description())
                .dateTime(LocalDateTime.now())
                .userId(userId)
                .build();

        List<TransactionSplit> splits = request.splits().
                stream().map(split->{
                    var category = categoryRepository.findById(split.categoryDto().id())
                            .orElseThrow(()-> new ResourceNotFoundException("Category not found"));
                    SecurityUtils.assertOwner(category.getUserId(),userId);
                    return TransactionSplit.builder()
                                .category(category)
                                .amount(split.amount())
                                .transaction(trans)
                                .build();}
                ).toList();
        trans.setSplits(splits);
        SplitValidationUtils.assertSplit(trans.getAmount(),
                trans.getSplits().stream().map(TransactionSplit::getAmount).toList());
        transactionRepository.save(trans);
        evictSplitsCache(userId,splits);
        return transactionMapper.toDto(trans);
    }

    @Transactional(rollbackFor = RuntimeException.class)
    public void deleteTransactionById(Long id, Long userId){
        var trans = transactionRepository.findById(id)
                .orElseThrow(()-> new ResourceNotFoundException("Transaction not found"));
        SecurityUtils.assertOwner(trans.getUserId(),userId);
        transactionRepository.deleteById(id);
        evictSplitsCache(userId,trans.getSplits());
    }


    @Transactional(rollbackFor = RuntimeException.class)
    public TransactionDto updateTransaction(TransactionUpdateRequest request, Long id, Long userId){
        var trans = transactionRepository.findById(id)
                .orElseThrow(()-> new ResourceNotFoundException("Transaction not fouund"));
        SecurityUtils.assertOwner(trans.getUserId(),userId);
        List<TransactionSplit> oldSplit = new ArrayList<>(trans.getSplits());
        List<TransactionSplit> splits = request.splits().
                stream().map(split-> {
                    var category = categoryRepository.findById(split.categoryDto().id())
                            .orElseThrow(()-> new ResourceNotFoundException("Category not found"));
                    SecurityUtils.assertOwner(category.getUserId(),userId);
                    return TransactionSplit.builder()
                        .category(category)
                        .amount(split.amount())
                        .transaction(trans)
                        .build();}
                ).toList();

       trans.setDescription(request.description());
       trans.setAmount(request.amount());
       trans.setDateTime(request.dateTime());
       trans.getSplits().clear();
       trans.getSplits().addAll(splits);
       SplitValidationUtils.assertSplit(trans.getAmount(),splits.stream()
               .map(TransactionSplit::getAmount).toList());
       transactionRepository.save(trans);
       evictSplitsCache(userId, oldSplit);
       evictSplitsCache(userId, splits);
        return transactionMapper.toDto(trans);
    }

    public TransactionDto getById(Long id, Long userId){
        var trans = transactionRepository.findById(id)
                .orElseThrow(()-> new ResourceNotFoundException("Transaction not found"));
        SecurityUtils.assertOwner(trans.getUserId(),userId);
        return transactionMapper.toDto(trans);
    }

    public Page<TransactionDto> getAllTransactionsByUser(Long userId, Pageable pageable){
            var transByUser = transactionRepository.findAllByUserId(userId, pageable);
            return transactionMapper.toDtoPage(transByUser);
    }

    public BigDecimal getTotalAmountByDateRange(Long userId, LocalDateTime from, LocalDateTime to){
       return transactionRepository.sumAmountByUserIdAndDateTimeBetween(userId, from, to);
    }


    public Page<TransactionDto> getTransactions(Long userId, TransactionSearchCriteria criteria, Pageable pageable){
        Specification<Transaction> spec =
                TransactionSpecifications.hasUserId(userId)
                .and(TransactionSpecifications.hasCategoryId(criteria.getCategoryId()))
                .and(TransactionSpecifications.amountBetween(criteria.getMinAmount(),criteria.getMaxAmount()))
                .and(TransactionSpecifications.dateBetween(criteria.getFrom(),criteria.getTo()));
        var page = transactionRepository.findAll(spec,pageable);
        return transactionMapper.toDtoPage(page);
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
