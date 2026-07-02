package dev.zhulidov.cash_tracker.model;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Table(name = "expenses")
public class Expense {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    private Long id;
    @Column(name = "expense")
    private String expense;
    @Column(name = "amount", nullable = false)
    private BigDecimal amount;
    @Column(name = "date_time", nullable = false)
    private LocalDateTime dateTime;
    @ManyToOne
    @JoinColumn(name = "category_id", nullable = false)
    private Category category;
}
