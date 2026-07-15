package dev.zhulidov.cash_tracker.transactions.model;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Entity
@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Table(name = "transaction")
public class Transaction {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    private Long id;
    @Column(name = "user_id", nullable = false)
    private Long userId;
    @Column(name = "amount", nullable = false)
    private BigDecimal amount;
    @Column(name = "description", nullable = false)
    private String description;
    @Column(name = "date_time", nullable = false)
    private LocalDateTime dateTime;
    @OneToMany(mappedBy = "transaction", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<TransactionSplit> splits;

}
