package dev.zhulidov.cash_tracker.transactions.model;

import jakarta.persistence.*;
import lombok.*;

import java.util.List;

@Entity
@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Table(name="categories")
public class Category {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    private Long id;
    @Column(name = "category_name", nullable = false)
    private String categoryName;
    @Column(name = "user_id", nullable = false)
    private Long userId;

    @OneToMany(mappedBy = "category")
    private List<Expense> expensies;
}
