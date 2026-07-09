package dev.zhulidov.cash_tracker.transactions.util;

import dev.zhulidov.cash_tracker.transactions.model.Transaction;
import org.springframework.data.jpa.domain.Specification;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class TransactionSpecifications {
    public static Specification<Transaction> hasUserId(Long userId){
        return ((root, query, criteriaBuilder) ->
                criteriaBuilder.equal(root.get("userId"),userId));
    }
    public static Specification<Transaction> hasCategoryId(Long categoryId){
        if (categoryId == null) return null;
        return (root, query, cb)->{
            var splitJoin = root.join("splits");
            query.distinct(true);
            return cb.equal(splitJoin.get("category").get("id"), categoryId);
        };
    }
    public static Specification<Transaction> amountBetween(BigDecimal min, BigDecimal max){
        return (root, query, cb)->{
            if (min != null && max != null) return cb.between(root.get("amount"),min,max);
            if (min != null) return cb.greaterThanOrEqualTo(root.get("amount"), min);
            if (max != null) return cb.lessThanOrEqualTo(root.get("amount"), max);
            return null;
        };
    }

    public static Specification<Transaction> dateBetween(LocalDateTime from, LocalDateTime to) {
        if (from == null || to == null) return null;
        return (root, query, cb) -> cb.between(root.get("dateTime"), from, to);
    }
}
