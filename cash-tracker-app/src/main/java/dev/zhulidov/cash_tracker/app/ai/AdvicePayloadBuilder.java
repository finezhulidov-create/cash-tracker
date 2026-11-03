package dev.zhulidov.cash_tracker.app.ai;

import dev.zhulidov.cash_tracker.transactions.dto.BudgetStatusDto;
import dev.zhulidov.cash_tracker.transactions.dto.CategorySpendingDto;
import dev.zhulidov.cash_tracker.transactions.dto.MonthSummaryDto;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Собирает {@link AdvicePayload} из сводки месяца и трат по категориям (текущего и прошлого месяца). */
@Component
public class AdvicePayloadBuilder {

    /** С какого процента расхода бюджет считается «на исходе»; то же значение в интерфейсе (frontend/src/lib/budget.ts). */
    static final BigDecimal WARN_THRESHOLD_PERCENT = BigDecimal.valueOf(85);
    /** Предел размера запроса: платим за токены, а у модели есть лимит на вход. */
    static final int MAX_CATEGORIES = 30;
    private static final BigDecimal HUNDRED = BigDecimal.valueOf(100);
    private static final String CURRENCY = "RUB";

    public AdvicePayload build(YearMonth month, LocalDate today, MonthSummaryDto summary,
                               List<CategorySpendingDto> spending, List<CategorySpendingDto> previousSpending) {
        BigDecimal total = money(summary.totalSpent());
        BigDecimal previousTotal = money(previousSpending.stream()
                .map(CategorySpendingDto::spent)
                .reduce(BigDecimal.ZERO, BigDecimal::add));

        Map<Long, BudgetStatusDto> budgetsByCategory = new LinkedHashMap<>();
        for (BudgetStatusDto budget : summary.categories()) {
            if (budget.category() != null) {
                budgetsByCategory.putIfAbsent(budget.category().id(), budget);
            }
        }
        Map<Long, BigDecimal> previousById = new LinkedHashMap<>();
        for (CategorySpendingDto item : previousSpending) {
            previousById.merge(item.categoryId(), item.spent(), BigDecimal::add);
        }

        Map<Long, String> names = new LinkedHashMap<>();
        Map<Long, BigDecimal> spentById = new LinkedHashMap<>();
        for (CategorySpendingDto item : spending) {
            names.put(item.categoryId(), item.categoryName());
            spentById.put(item.categoryId(), item.spent());
        }
        for (BudgetStatusDto budget : budgetsByCategory.values()) {
            names.putIfAbsent(budget.category().id(), budget.category().categoryName());
            spentById.putIfAbsent(budget.category().id(), budget.spent());
        }

        List<AdvicePayload.CategoryLine> lines = spentById.entrySet().stream()
                .filter(entry -> entry.getValue().signum() > 0 || budgetsByCategory.containsKey(entry.getKey()))
                .map(entry -> new AdvicePayload.CategoryLine(
                        clean(names.get(entry.getKey())),
                        money(entry.getValue()),
                        share(entry.getValue(), total),
                        money(previousById.getOrDefault(entry.getKey(), BigDecimal.ZERO)),
                        budgetLine(budgetsByCategory.get(entry.getKey()))))
                .sorted(Comparator.comparing(AdvicePayload.CategoryLine::spent).reversed()
                        .thenComparing(AdvicePayload.CategoryLine::name))
                .limit(MAX_CATEGORIES)
                .toList();

        BigDecimal budgetedSpent = budgetsByCategory.values().stream()
                .map(BudgetStatusDto::spent)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal outsideBudgets = money(total.subtract(budgetedSpent).max(BigDecimal.ZERO));

        boolean inProgress = month.equals(YearMonth.from(today));
        int daysLeft = inProgress ? month.lengthOfMonth() - today.getDayOfMonth() + 1 : 0;

        return new AdvicePayload(
                month.toString(),
                today.toString(),
                inProgress,
                daysLeft,
                CURRENCY,
                total,
                previousTotal.signum() > 0 ? previousTotal : null,
                previousTotal.signum() > 0 ? change(total, previousTotal) : null,
                budgetLine(summary.overall()),
                lines,
                outsideBudgets);
    }

    private static AdvicePayload.BudgetLine budgetLine(BudgetStatusDto budget) {
        if (budget == null) {
            return null;
        }
        BigDecimal limit = money(budget.assigned());
        BigDecimal used = budget.assigned().signum() > 0
                ? percent(budget.spent(), budget.assigned())
                : BigDecimal.ZERO.setScale(1);
        return new AdvicePayload.BudgetLine(limit, money(budget.spent()), money(budget.available()), used,
                status(budget.available(), used));
    }

    static String status(BigDecimal remaining, BigDecimal usedPercent) {
        if (remaining.signum() < 0) {
            return "over";
        }
        return usedPercent.compareTo(WARN_THRESHOLD_PERCENT) >= 0 ? "warn" : "ok";
    }

    private static BigDecimal share(BigDecimal part, BigDecimal total) {
        return total.signum() > 0 ? percent(part, total) : BigDecimal.ZERO.setScale(1);
    }

    private static BigDecimal percent(BigDecimal part, BigDecimal whole) {
        return part.multiply(HUNDRED).divide(whole, 1, RoundingMode.HALF_UP);
    }

    private static BigDecimal change(BigDecimal current, BigDecimal previous) {
        return current.subtract(previous).multiply(HUNDRED).divide(previous, 1, RoundingMode.HALF_UP);
    }

    private static BigDecimal money(BigDecimal value) {
        return value.setScale(2, RoundingMode.HALF_UP);
    }

    /**
     * Названия категорий вводит пользователь, и они попадают в текст для модели: убираем переводы строк и
     * управляющие символы, чтобы название не могло притвориться отдельной инструкцией.
     */
    static String clean(String name) {
        if (name == null) {
            return "";
        }
        String cleaned = name.replaceAll("\\p{Cntrl}+", " ").replaceAll("\\s{2,}", " ").trim();
        return cleaned.length() > 50 ? cleaned.substring(0, 50) : cleaned;
    }
}
