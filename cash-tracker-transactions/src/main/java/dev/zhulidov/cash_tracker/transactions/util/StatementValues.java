package dev.zhulidov.cash_tracker.transactions.util;

import dev.zhulidov.cash_tracker.transactions.dto.ParsedAmount;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.format.ResolverStyle;
import java.util.List;
import java.util.Optional;

public final class StatementValues {
    public StatementValues() {
    }

    public static Optional<ParsedAmount> parseAmount(String raw){
        if (raw == null || raw.isBlank()){
            return Optional.empty();
        }
        String s = raw
                .replace('\u2212','-')
                .replace('\u2013','-')
                .replaceAll("[\\s\u00A0\u202F]", "")
                .replaceAll("(?i)(₽|руб\\.?|rub|р\\.)", "")
                .replace(',','.');
        boolean explicitPlus = s.startsWith("+");
        if (explicitPlus){
            s = s.substring(1);
        }
        if (!s.matches("-?\\d+(\\.\\d{1,2})?")){
            return Optional.empty();
        }
        BigDecimal value = new BigDecimal(s);
        if (value.signum()==0){
            return Optional.empty();
        }
        boolean income =explicitPlus;
        return Optional.of(new ParsedAmount(income ? value : value.abs().negate(),income));

    }

    private static final List<DateTimeFormatter> DATE_TIME_FORMATS =
            List.of(pattern("dd.MM.uuuu HH:mm:ss"),
                    pattern("dd.MM.uuuu HH:mm"),
                    pattern("dd.MM.uu HH:mm:ss"),
                    pattern("dd.MM.uu HH:mm"),
                    pattern("uuuu-MM-dd'T'HH:mm[:ss]"));


    private static final List<DateTimeFormatter> DATE_FORMATS = List.of(
            pattern("dd.MM.uuuu"),
            pattern("dd.MM.uu"),
            pattern("uuuu-MM-dd")
    );

    public static Optional<LocalDateTime> parseDateTime(String raw){
        if (raw == null || raw.isBlank()){
            return Optional.empty();
        }
        String s = raw.trim()
                .replace(',',' ')
                .replaceAll("[\\s\u00A0]+", " ")
                .replaceAll(" (\\d):", " 0$1:");

        for (DateTimeFormatter f: DATE_TIME_FORMATS){
            try {
                return Optional.of(LocalDateTime.parse(s,f));

            } catch (DateTimeParseException ignored){

            }
        }
        for (DateTimeFormatter f: DATE_FORMATS){
            try {
                return Optional.of(LocalDate.parse(s,f).atStartOfDay());
            } catch (DateTimeParseException ignored){}
        }
        return Optional.empty();
    }


   private static DateTimeFormatter pattern(String p) {
        // STRICT не пропустит 31.02 или 25:61; в STRICT год пишется через 'u', а не 'y'
        return DateTimeFormatter.ofPattern(p).withResolverStyle(ResolverStyle.STRICT);}

}
