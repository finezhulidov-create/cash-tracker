package dev.zhulidov.cash_tracker.common.util;

import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PageUtilsTest {

    private static final List<String> ITEMS = List.of("a", "b", "c");

    @Test
    void toPage_whenListIsEmpty_returnsEmptyPage() {
        Page<String> page = PageUtils.toPage(List.of(), PageRequest.of(0, 10));

        assertTrue(page.getContent().isEmpty());
        assertEquals(0, page.getTotalElements());
    }

    @Test
    void toPage_whenFirstPage_returnsFirstSlice() {
        Page<String> page = PageUtils.toPage(ITEMS, PageRequest.of(0, 2));

        assertEquals(List.of("a", "b"), page.getContent());
        assertEquals(3, page.getTotalElements());
        assertEquals(2, page.getTotalPages());
    }

    @Test
    void toPage_whenLastPageIsPartial_returnsRemainingItems() {
        Page<String> page = PageUtils.toPage(ITEMS, PageRequest.of(1, 2));

        assertEquals(List.of("c"), page.getContent());
        assertEquals(3, page.getTotalElements());
    }

    @Test
    void toPage_whenPageSizeCoversWholeList_returnsAllItems() {
        Page<String> page = PageUtils.toPage(ITEMS, PageRequest.of(0, 50));

        assertEquals(ITEMS, page.getContent());
        assertEquals(1, page.getTotalPages());
    }

    @Test
    void toPage_whenOffsetEqualsListSize_returnsEmptyContentButKeepsTotal() {
        Page<String> page = PageUtils.toPage(ITEMS, PageRequest.of(1, 3));

        assertTrue(page.getContent().isEmpty());
        assertEquals(3, page.getTotalElements());
    }

    @Test
    void toPage_whenPageIsFarBeyondBounds_returnsEmptyContentButKeepsTotal() {
        Page<String> page = PageUtils.toPage(ITEMS, PageRequest.of(100, 10));

        assertTrue(page.getContent().isEmpty());
        assertEquals(3, page.getTotalElements());
    }
}
