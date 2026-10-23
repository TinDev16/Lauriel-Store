package com.lauriel.mobile.data.local;

import android.content.Context;
import java.util.HashSet;
import java.util.Set;

public final class VoucherStore {

    private static final String PREFS = "lauriel_voucher_wallet";
    private static final String LEGACY_SAVED_IDS = "saved_voucher_ids";
    private static final String SAVED_IDS_PREFIX = "saved_voucher_ids_";
    private final android.content.SharedPreferences preferences;
    private final String savedIdsKey;

    public VoucherStore(Context context, long userId) {
        if (userId <= 0) throw new IllegalArgumentException(
            "Yêu cầu tài khoản khách hàng hợp lệ"
        );
        preferences = context
            .getApplicationContext()
            .getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        savedIdsKey = SAVED_IDS_PREFIX + userId;
        migrateLegacyWallet();
    }

    public Set<Long> savedIds() {
        Set<Long> result = new HashSet<>();
        for (String value : preferences.getStringSet(savedIdsKey, Set.of())) {
            try {
                result.add(Long.parseLong(value));
            } catch (NumberFormatException ignored) {
                // Ignore obsolete local data instead of breaking the voucher wallet.
            }
        }
        return result;
    }

    public boolean contains(long voucherId) {
        return savedIds().contains(voucherId);
    }

    public void toggle(long voucherId) {
        if (voucherId <= 0) return;
        Set<String> values = new HashSet<>(
            preferences.getStringSet(savedIdsKey, Set.of())
        );
        String id = Long.toString(voucherId);
        if (!values.add(id)) values.remove(id);
        preferences.edit().putStringSet(savedIdsKey, values).apply();
    }

    private void migrateLegacyWallet() {
        if (
            preferences.contains(savedIdsKey) ||
            !preferences.contains(LEGACY_SAVED_IDS)
        ) return;
        Set<String> legacy = new HashSet<>(
            preferences.getStringSet(LEGACY_SAVED_IDS, Set.of())
        );
        preferences
            .edit()
            .putStringSet(savedIdsKey, legacy)
            .remove(LEGACY_SAVED_IDS)
            .apply();
    }
}
