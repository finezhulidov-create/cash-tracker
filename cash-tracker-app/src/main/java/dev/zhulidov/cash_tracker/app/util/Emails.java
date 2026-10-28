package dev.zhulidov.cash_tracker.app.util;

import java.util.Locale;

public final class Emails {

    private Emails() {
    }

    /** Email хранится и сравнивается в нижнем регистре и без пробелов по краям, чтобы A@x.ru и a@x.ru были одним аккаунтом. */
    public static String normalize(String email) {
        return email == null ? null : email.trim().toLowerCase(Locale.ROOT);
    }
}
