package com.lauriel.mobile.data.local;

import static org.junit.Assert.*;

import android.content.Context;
import java.util.Set;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;

@RunWith(RobolectricTestRunner.class)
public class VoucherStoreTest {

    private Context context;

    @Before
    public void reset() {
        context = RuntimeEnvironment.getApplication();
        context
            .getSharedPreferences(
                "lauriel_voucher_wallet",
                Context.MODE_PRIVATE
            )
            .edit()
            .clear()
            .commit();
    }

    @Test
    public void savedVouchersAreSeparatedByAccount() {
        VoucherStore first = new VoucherStore(context, 11);
        VoucherStore second = new VoucherStore(context, 22);
        first.toggle(100);
        assertTrue(first.contains(100));
        assertFalse(second.contains(100));
        second.toggle(200);
        assertEquals(Set.of(100L), first.savedIds());
        assertEquals(Set.of(200L), second.savedIds());
    }

    @Test
    public void legacyWalletMovesToCurrentAccountOnce() {
        context
            .getSharedPreferences(
                "lauriel_voucher_wallet",
                Context.MODE_PRIVATE
            )
            .edit()
            .putStringSet("saved_voucher_ids", Set.of("7", "bad"))
            .commit();
        VoucherStore current = new VoucherStore(context, 11);
        VoucherStore another = new VoucherStore(context, 22);
        assertEquals(Set.of(7L), current.savedIds());
        assertTrue(another.savedIds().isEmpty());
    }
}
