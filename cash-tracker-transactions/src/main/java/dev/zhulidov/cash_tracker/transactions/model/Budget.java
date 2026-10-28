package dev.zhulidov.cash_tracker.transactions.model;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
@Builder
@Getter
@Setter
@Entity
@Table(name = "budget")
@NoArgsConstructor
@AllArgsConstructor
public class Budget {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "limit_amount", nullable = false)
    private BigDecimal limitAmount;

    /** Первое число месяца, к которому относится бюджет. */
    @Column(name = "period", nullable = false)
    private LocalDate period;

    /** Категория бюджета; null означает общий бюджет на весь месяц (учитываются все траты). */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id")
    private Category category;


}
