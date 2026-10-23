package com.lauriel.mobile.ui.common;

import android.graphics.Rect;
import android.view.View;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

public final class GridSpacingDecoration extends RecyclerView.ItemDecoration {

    private final int spacing;
    private final int columns;

    public GridSpacingDecoration(int spacing, int columns) {
        this.spacing = spacing;
        this.columns = columns;
    }

    @Override
    public void getItemOffsets(
        @NonNull Rect out,
        @NonNull View view,
        @NonNull RecyclerView parent,
        @NonNull RecyclerView.State state
    ) {
        int position = parent.getChildAdapterPosition(view);
        if (position == RecyclerView.NO_POSITION) return;
        int column = position % columns;
        out.left = spacing - (column * spacing) / columns;
        out.right = ((column + 1) * spacing) / columns;
        out.top = position < columns ? spacing : 0;
        out.bottom = spacing;
    }
}
