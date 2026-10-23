package com.lauriel.mobile.ui.common;

import android.content.res.ColorStateList;
import androidx.core.content.ContextCompat;
import com.google.android.material.chip.Chip;
import com.lauriel.mobile.R;
import com.lauriel.mobile.util.Formatters;

public final class StatusChip {

    private StatusChip() {}

    public static void apply(Chip chip, String status) {
        int background = R.color.surface_strong,
            foreground = R.color.muted;
        if ("CONFIRMED".equals(status)) {
            background = R.color.info_surface;
            foreground = R.color.info;
        } else if ("PROCESSING".equals(status)) {
            background = R.color.warning_surface;
            foreground = R.color.warning;
        } else if ("SHIPPING".equals(status)) {
            background = R.color.purple_surface;
            foreground = R.color.purple;
        } else if ("DELIVERED".equals(status)) {
            background = R.color.success_surface;
            foreground = R.color.success;
        } else if ("CANCELLED".equals(status) || "RETURNED".equals(status)) {
            background = R.color.danger_surface;
            foreground = R.color.danger;
        }
        chip.setText(Formatters.status(status));
        chip.setChipBackgroundColor(
            ColorStateList.valueOf(
                ContextCompat.getColor(chip.getContext(), background)
            )
        );
        chip.setTextColor(
            ContextCompat.getColor(chip.getContext(), foreground)
        );
        chip.setChipStrokeWidth(0f);
        chip.setEnsureMinTouchTargetSize(false);
    }
}
