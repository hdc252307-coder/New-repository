package com.example.demo;

/**
 * カレンダー1週行内で、予定帯（開始日〜終了日）をどの列・レーンに置くか。
 */
public record ScheduleCalendarBandPlacement(
        Long scheduleId,
        String title,
        int colStart,
        int colEndInclusive,
        int lane
) {
}
