package dev.zhulidov.cash_tracker.app.ai;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.math.BigDecimal;
import java.util.List;

/**
 * То, что уходит в модель (после сериализации в JSON): только суммы, проценты и названия категорий.
 * Внутренних идентификаторов (пользователя, бюджетов, категорий) здесь нет. Проценты, остатки и статусы
 * посчитаны заранее: языковые модели ошибаются в арифметике, поэтому им остаётся пересказ и советы.
 * Названия полей описаны в системном промпте (resources/ai/advisor-prompt.md), менять их нужно вместе.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record AdvicePayload(
        /* Месяц «ГГГГ-ММ». */
        String month,
        /* Сегодняшняя дата (по UTC сервера). */
        String today,
        /* Идёт ли этот месяц сейчас; прошедший месяц анализируется как итог. */
        boolean monthInProgress,
        /* Дней до конца месяца, считая сегодняшний; 0 для прошедших месяцев. */
        int daysLeftInMonth,
        String currency,
        /* Все траты месяца, включая категории без бюджета. */
        BigDecimal totalSpent,
        /* Траты прошлого месяца; null, если их не было. */
        BigDecimal previousMonthTotalSpent,
        /* Изменение к прошлому месяцу в процентах; null, если сравнивать не с чем. */
        BigDecimal changeVsPreviousMonthPercent,
        /* Общий бюджет месяца; null, если не задан. */
        BudgetLine overallBudget,
        List<CategoryLine> categories,
        /* Траты в категориях, на которые бюджет не задан. */
        BigDecimal spentOutsideBudgets
) {

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record BudgetLine(
            BigDecimal limit,
            BigDecimal spent,
            /* Остаток; отрицательный при перерасходе. */
            BigDecimal remaining,
            BigDecimal usedPercent,
            /* ok — в норме, warn — израсходовано 85% и больше, over — перерасход. */
            String status
    ) {
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record CategoryLine(
            String name,
            BigDecimal spent,
            /* Доля категории во всех тратах месяца, %. */
            BigDecimal sharePercent,
            BigDecimal previousMonthSpent,
            /* Бюджет категории; null, если не задан. */
            BudgetLine budget
    ) {
    }
}
