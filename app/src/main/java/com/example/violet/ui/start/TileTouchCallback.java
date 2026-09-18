package com.example.violet.ui.start;

import android.graphics.Canvas;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.RecyclerView;

/**
 * ItemTouchHelper callback enabling drag-and-drop tile rearrangement.
 * Ensures the Start header view cannot be dragged or dropped on, and dismisses
 * any open contextual popup menus only when actual drag displacement begins.
 */
public class TileTouchCallback extends ItemTouchHelper.Callback {

    public interface ItemTouchHelperAdapter {
        boolean onItemMove(int fromPosition, int toPosition);
        void onDragStarted();
        void onDragFinished();
    }

    private final ItemTouchHelperAdapter adapter;
    private boolean hasMoved = false;

    public TileTouchCallback(ItemTouchHelperAdapter adapter) {
        this.adapter = adapter;
    }

    @Override
    public boolean isLongPressDragEnabled() {
        return true;
    }

    @Override
    public boolean isItemViewSwipeEnabled() {
        return false;
    }

    @Override
    public int getMovementFlags(@NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder) {
        if (viewHolder.getItemViewType() == TileAdapter.VIEW_TYPE_HEADER) {
            return makeMovementFlags(0, 0);
        }
        int dragFlags = ItemTouchHelper.UP | ItemTouchHelper.DOWN | ItemTouchHelper.LEFT | ItemTouchHelper.RIGHT;
        return makeMovementFlags(dragFlags, 0);
    }

    @Override
    public boolean canDropOver(@NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder current, @NonNull RecyclerView.ViewHolder target) {
        return target.getItemViewType() != TileAdapter.VIEW_TYPE_HEADER;
    }

    @Override
    public boolean onMove(@NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder source, @NonNull RecyclerView.ViewHolder target) {
        if (target.getItemViewType() == TileAdapter.VIEW_TYPE_HEADER) {
            return false;
        }
        hasMoved = true;
        adapter.onDragStarted();
        return adapter.onItemMove(source.getAdapterPosition(), target.getAdapterPosition());
    }

    @Override
    public void onSwiped(@NonNull RecyclerView.ViewHolder viewHolder, int direction) {
        // Swiping tiles away is disabled
    }

    @Override
    public void onSelectedChanged(@Nullable RecyclerView.ViewHolder viewHolder, int actionState) {
        super.onSelectedChanged(viewHolder, actionState);
        if (actionState == ItemTouchHelper.ACTION_STATE_DRAG) {
            hasMoved = false;
            // Do not dismiss context menu or lift tile here yet.
            // A long press could be intended for the context menu.
            // Drag lift & menu dismissal only activate once deliberate motion begins.
        }
    }

    @Override
    public void onChildDraw(@NonNull Canvas c, @NonNull RecyclerView recyclerView,
                            @NonNull RecyclerView.ViewHolder viewHolder,
                            float dX, float dY, int actionState, boolean isCurrentlyActive) {
        super.onChildDraw(c, recyclerView, viewHolder, dX, dY, actionState, isCurrentlyActive);
        if (actionState == ItemTouchHelper.ACTION_STATE_DRAG && isCurrentlyActive) {
            // Dismiss context menu and lift tile when deliberate drag motion begins (> 12px)
            if (Math.abs(dX) > 12f || Math.abs(dY) > 12f) {
                if (!hasMoved) {
                    hasMoved = true;
                    adapter.onDragStarted();
                    if (viewHolder.getItemViewType() != TileAdapter.VIEW_TYPE_HEADER) {
                        viewHolder.itemView.animate()
                                .scaleX(1.05f)
                                .scaleY(1.05f)
                                .alpha(0.85f)
                                .setDuration(120)
                                .start();
                    }
                }
            }
        }
    }

    @Override
    public void clearView(@NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder) {
        super.clearView(recyclerView, viewHolder);
        if (hasMoved) {
            if (viewHolder.getItemViewType() != TileAdapter.VIEW_TYPE_HEADER) {
                viewHolder.itemView.animate()
                        .scaleX(1.0f)
                        .scaleY(1.0f)
                        .alpha(1.0f)
                        .setDuration(120)
                        .start();
            }
            adapter.onDragFinished();
            hasMoved = false;
        }
    }
}
