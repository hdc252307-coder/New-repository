package com.example.demo;

/**
 * カレンダー1週行内で、タスク帯（作成日〜期日）をどの列・レーンに置くか。
 */
public record TaskCalendarBandPlacement(
        Long taskId,
        String title,
        String colorKey,
        int colStart,
        int colEndInclusive,
        int lane,
        /** その週行で帯の左端に対応する日付（ドラッグ移動の基準） */
        String segmentStartDate
) {
}
