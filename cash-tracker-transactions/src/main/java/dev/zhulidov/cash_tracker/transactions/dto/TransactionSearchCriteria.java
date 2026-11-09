package dev.zhulidov.cash_tracker.transactions.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.format.annotation.DateTimeFormat;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Schema(description = "Необязательные фильтры списка трат; любой неуказанный фильтр игнорируется")
public class TransactionSearchCriteria {
    @Schema(description = "Только траты, где есть часть в этой категории", example = "10")
    private Long categoryId;
    @Schema(description = "Минимальная общая сумма траты (включительно)", example = "100.00")
    private BigDecimal minAmount;
    @Schema(description = "Максимальная общая сумма траты (включительно)", example = "5000.00")
    private BigDecimal maxAmount;
    @Schema(description = "С какого момента (включительно), ISO-8601", example = "2026-10-01T00:00:00")
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
    private LocalDateTime from;
    @Schema(description = "По какой момент (включительно), ISO-8601", example = "2026-10-31T23:59:59")
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
    private LocalDateTime to;
    @Schema(description = "Подстрока описания траты, без учёта регистра; до 255 символов", example = "супермаркет")
    private String q;
}
