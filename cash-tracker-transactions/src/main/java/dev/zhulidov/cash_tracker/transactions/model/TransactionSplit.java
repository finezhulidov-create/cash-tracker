package dev.zhulidov.cash_tracker.transactions.model;

public class TransactionSplit {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    private Long id;


}
