package dev.zhulidov.cash_tracker.common.util;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;


import java.util.List;

public class PageUtils {
    public static <T> Page<T> toPage(List<T> all, Pageable pageable) {
        int start = (int) pageable.getOffset();
        if (start >= all.size()) {
            return new PageImpl<>(List.of(), pageable, all.size());
        }
        int end = Math.min(start + pageable.getPageSize(), all.size());
        return new PageImpl<>(all.subList(start, end), pageable, all.size());
    }
}
