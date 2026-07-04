package dev.zhulidov.cash_tracker.transactions.dto;

import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.time.LocalDateTime;
@Builder
@Setter
@Getter
@AllArgsConstructor
@NoArgsConstructor
public class ExpensesByDateRangeRequestDto {
    @NotNull
   private LocalDateTime from;
    @NotNull
   private LocalDateTime to;
}



