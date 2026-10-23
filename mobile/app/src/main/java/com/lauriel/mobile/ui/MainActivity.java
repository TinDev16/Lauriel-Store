package com.lauriel.mobile.ui;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;
import com.google.android.material.badge.BadgeDrawable;
import com.lauriel.mobile.R;
import com.lauriel.mobile.data.api.ApiClient;
import com.lauriel.mobile.data.local.CartStore;
import com.lauriel.mobile.data.repository.CustomerActivityRepository;
import com.lauriel.mobile.databinding.ActivityMainBinding;
import com.lauriel.mobile.ui.cart.CartFragment;
import com.lauriel.mobile.ui.home.HomeFragment;
import com.lauriel.mobile.ui.order.OrdersFragment;
import com.lauriel.mobile.ui.product.ShopFragment;
import com.lauriel.mobile.ui.profile.ProfileFragment;

public class MainActivity extends BaseActivity {

    private static final long ACTIVITY_SYNC_INTERVAL_MS = 120_000L;
    private final Handler activityHandler = new Handler(Looper.getMainLooper());
    private ActivityMainBinding b;
    private final Runnable activityPulse = new Runnable() {
        @Override
        public void run() {
            syncEngagement();
            activityHandler.postDelayed(this, ACTIVITY_SYNC_INTERVAL_MS);
        }
    };

    @Override
    protected void onCreate(Bundle state) {
        super.onCreate(state);
        b = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(b.getRoot());
        SystemBars.apply(b.getRoot());
        b.bottomNav.setOnItemSelectedListener(item -> {
            show(item.getItemId());
            return true;
        });
        b.bottomNav.setOnItemReselectedListener(item -> {
            if (item.getItemId() != R.id.nav_home) return;
            Fragment current = getSupportFragmentManager().findFragmentByTag(
                tag(R.id.nav_home)
            );
            if (current instanceof HomeFragment) (
                (HomeFragment) current
            ).refresh();
            syncEngagement();
            updateCartBadge();
        });
        if (
            state == null ||
            getSupportFragmentManager().getFragments().isEmpty()
        ) {
            b.bottomNav.getMenu().findItem(R.id.nav_home).setChecked(true);
            show(R.id.nav_home);
        }
        updateCartBadge();
    }

    private void show(int id) {
        String tag = tag(id);
        Fragment target = getSupportFragmentManager().findFragmentByTag(tag);
        FragmentTransaction tx = getSupportFragmentManager()
            .beginTransaction()
            .setReorderingAllowed(true)
            .setCustomAnimations(R.anim.fade_in, R.anim.fade_out);
        for (Fragment current : getSupportFragmentManager().getFragments())
            if (current.isAdded()) tx.hide(current);
        if (target == null) tx.add(R.id.container, create(id), tag);
        else tx.show(target);
        tx.commit();
        updateCartBadge();
    }

    private Fragment create(int id) {
        if (id == R.id.nav_shop) return new ShopFragment();
        if (id == R.id.nav_cart) return new CartFragment();
        if (id == R.id.nav_orders) return new OrdersFragment();
        if (id == R.id.nav_profile) return new ProfileFragment();
        return new HomeFragment();
    }

    private String tag(int id) {
        return "tab:" + id;
    }

    public void showTab(int id) {
        b.bottomNav.setSelectedItemId(id);
    }

    public void updateCartBadge() {
        if (b == null) return;
        var session = ApiClient.get(this).session();
        if (!session.hasCustomer()) {
            b.bottomNav.removeBadge(R.id.nav_cart);
            return;
        }
        int count = 0;
        for (CartStore.CartItem item : new CartStore(
            this,
            session.userId()
        ).items())
            count += item.quantity;
        if (count == 0) b.bottomNav.removeBadge(R.id.nav_cart);
        else {
            BadgeDrawable badge = b.bottomNav.getOrCreateBadge(R.id.nav_cart);
            badge.setNumber(count);
            badge.setMaxCharacterCount(3);
            badge.setVisible(true);
        }
    }

    public void syncEngagement() {
        var client = ApiClient.get(this);
        var session = client.session();
        if (!session.hasCustomer()) return;
        CustomerActivityRepository activity = new CustomerActivityRepository(
            client.api()
        );
        activity.syncCart(new CartStore(this, session.userId()));
    }

    @Override
    protected void onResume() {
        super.onResume();
        updateCartBadge();
        activityHandler.removeCallbacks(activityPulse);
        activityPulse.run();
    }

    @Override
    protected void onPause() {
        activityHandler.removeCallbacks(activityPulse);
        super.onPause();
    }
}
