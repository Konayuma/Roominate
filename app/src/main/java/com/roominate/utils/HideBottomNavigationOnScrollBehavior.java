package com.roominate.utils;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;
import androidx.annotation.NonNull;
import androidx.coordinatorlayout.widget.CoordinatorLayout;
import androidx.core.view.ViewCompat;
import com.google.android.material.bottomnavigation.BottomNavigationView;

public class HideBottomNavigationOnScrollBehavior extends CoordinatorLayout.Behavior<BottomNavigationView> {

    public HideBottomNavigationOnScrollBehavior() {
        super();
    }

    public HideBottomNavigationOnScrollBehavior(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    @Override
    public boolean onStartNestedScroll(@NonNull CoordinatorLayout coordinatorLayout,
                                       @NonNull BottomNavigationView child,
                                       @NonNull View directTargetChild,
                                       @NonNull View target,
                                       int axes,
                                       int type) {
        return axes == ViewCompat.SCROLL_AXIS_VERTICAL;
    }

    @Override
    public void onNestedPreScroll(@NonNull CoordinatorLayout coordinatorLayout,
                                  @NonNull BottomNavigationView child,
                                  @NonNull View target,
                                  int dx,
                                  int dy,
                                  @NonNull int[] consumed,
                                  int type) {
        if (dy > 0) {
            // Scrolling down - hide bottom nav
            hideBottomNavigationView(child);
        } else if (dy < 0) {
            // Scrolling up - show bottom nav
            showBottomNavigationView(child);
        }
    }

    private void hideBottomNavigationView(BottomNavigationView view) {
        view.animate()
                .translationY(view.getHeight() + ((CoordinatorLayout.LayoutParams) view.getLayoutParams()).bottomMargin)
                .setDuration(200)
                .start();
    }

    private void showBottomNavigationView(BottomNavigationView view) {
        view.animate()
                .translationY(0)
                .setDuration(200)
                .start();
    }
}
