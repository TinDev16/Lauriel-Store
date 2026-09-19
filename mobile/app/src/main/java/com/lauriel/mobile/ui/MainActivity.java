package com.lauriel.mobile.ui;
import android.os.Bundle;
import androidx.fragment.app.Fragment;
import com.lauriel.mobile.R;
import com.lauriel.mobile.databinding.ActivityMainBinding;
import com.lauriel.mobile.ui.home.HomeFragment;
import com.lauriel.mobile.ui.product.ShopFragment;
import com.lauriel.mobile.ui.cart.CartFragment;
import com.lauriel.mobile.ui.order.OrdersFragment;
import com.lauriel.mobile.ui.profile.ProfileFragment;
public class MainActivity extends BaseActivity {
    private ActivityMainBinding b;
    @Override protected void onCreate(Bundle state){
        super.onCreate(state);b=ActivityMainBinding.inflate(getLayoutInflater());setContentView(b.getRoot());com.lauriel.mobile.ui.SystemBars.apply(b.getRoot());
        b.bottomNav.setOnItemSelectedListener(item -> {
            Fragment f;
            int id=item.getItemId();
            if(id==R.id.nav_home)f=new HomeFragment();
            else if(id==R.id.nav_shop)f=new ShopFragment();
            else if(id==R.id.nav_cart)f=new CartFragment();
            else if(id==R.id.nav_orders)f=new OrdersFragment();
            else f=new ProfileFragment();
            getSupportFragmentManager().beginTransaction().replace(R.id.container,f).commit();
            return true;
        });
        if(state==null)b.bottomNav.setSelectedItemId(R.id.nav_home);
    }
    public void showTab(int id){b.bottomNav.setSelectedItemId(id);}
}
