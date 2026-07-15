package dev.zhulidov.cash_tracker.common.util;

import dev.zhulidov.cash_tracker.common.exception.InaccessibleResourceException;

public class SecurityUtils {
    public static void assertOwner(Long resourceOwnerId, Long requesterId){
        if (!requesterId.equals(resourceOwnerId)){
            throw new InaccessibleResourceException("Resource not Found");
        }
    }
}
