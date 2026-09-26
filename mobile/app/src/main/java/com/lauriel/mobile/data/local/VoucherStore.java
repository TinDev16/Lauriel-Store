package com.lauriel.mobile.data.local;

import android.content.Context;
import java.util.HashSet;
import java.util.Set;

public final class VoucherStore {
    private static final String PREFS = "lauriel_voucher_wallet";
    private static final String SAVED_IDS = "saved_voucher_ids";
    private final android.content.SharedPreferences preferences;

    public VoucherStore(Context context) {
        preferences = context.getApplicationContext().getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }

    public Set<Long> savedIds() {
        Set<Long> result = new HashSet<>();
        for (String value : preferences.getStringSet(SAVED_IDS, Set.of())) {
            try { result.add(Long.parseLong(value)); } catch (NumberFormatException ignored) { }
        }
        return result;
    }

    public boolean contains(long voucherId) {
        return savedIds().contains(voucherId);
    }

    public void toggle(long voucherId) {
        Set<String> values = new HashSet<>(preferences.getStringSet(SAVED_IDS, Set.of()));
        String id = Long.toString(voucherId);
        if (!values.add(id)) values.remove(id);
        preferences.edit().putStringSet(SAVED_IDS, values).apply();
    }
}