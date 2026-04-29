package com.example.demo;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

/**
 * 終日スケジュールの開始・終了を DB 上の {@link LocalDateTime} に正規化する。
 * 終了日はユーザー入力で「その日を含む」(inclusive)。
 */
public final class ScheduleAllDaySupport {

    private ScheduleAllDaySupport() {
    }

    public static LocalDateTime startOfDay(LocalDate date) {
        return date.atStartOfDay();
    }

    /** inclusive 終了日の 23:59:59.999999999 */
    public static LocalDateTime endOfInclusiveDay(LocalDate endDate) {
        return endDate.atTime(LocalTime.MAX);
    }

    public static LocalDateTime[] normalizeAllDayRange(LocalDate startDate, LocalDate endDate) {
        if (startDate == null || endDate == null) {
            throw new IllegalArgumentException("終日の開始日・終了日は必須です。");
        }
        if (endDate.isBefore(startDate)) {
            throw new IllegalArgumentException("終了日は開始日以降にしてください。");
        }
        return new LocalDateTime[]{startOfDay(startDate), endOfInclusiveDay(endDate)};
    }

    /**
     * DB値から「ユーザー入力向けの終了日（inclusive）」を復元する。
     * 旧データで終日終了が翌日 00:00（exclusive）保存されていても同日扱いに戻す。
     */
    public static LocalDate toInclusiveEndDate(LocalDateTime start, LocalDateTime end, Boolean allDay) {
        if (end == null) {
            return null;
        }
        if (!Boolean.TRUE.equals(allDay) || start == null) {
            return end.toLocalDate();
        }
        // 互換: [start, end) で end=翌日00:00 の旧終日データを inclusive 日付に補正
        if (end.toLocalTime().equals(LocalTime.MIDNIGHT) && end.toLocalDate().isAfter(start.toLocalDate())) {
            return end.toLocalDate().minusDays(1);
        }
        return end.toLocalDate();
    }
}
