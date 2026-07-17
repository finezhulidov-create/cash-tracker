package dev.zhulidov.cash_tracker.notifications.util;

import java.util.Random;

public class VerificationCodeRandomiser {

    public static Integer randomeCode(){
        Random random = new Random();
        return random.nextInt(999999);
    }
}
