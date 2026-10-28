package dev.zhulidov.cash_tracker.common.util;

import dev.zhulidov.cash_tracker.common.exception.InaccessibleResourceException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

class SecurityUtilsTest {

    @Test
    void assertOwner_whenRequesterIsOwner_doesNotThrow() {
        assertDoesNotThrow(() -> SecurityUtils.assertOwner(1L, 1L));
    }

    @Test
    void assertOwner_whenRequesterIsAnotherUser_throwsInaccessibleResourceException() {
        assertThrows(InaccessibleResourceException.class, () -> SecurityUtils.assertOwner(1L, 2L));
    }

    @Test
    void assertOwner_whenOwnerIsNull_throwsInaccessibleResourceException() {
        assertThrows(InaccessibleResourceException.class, () -> SecurityUtils.assertOwner(null, 1L));
    }
}
