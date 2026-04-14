package com.cinemora.movieorder;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import com.google.firebase.auth.FirebaseAuth;

public class MainActivity extends AppCompatActivity {

    private LinearLayout navHome, navCart, navOrder, navProfile;
    private ImageView ivHome, ivCart, ivOrder, ivProfile;
    private TextView tvHome, tvCart, tvOrder, tvProfile;
    
    // Fragment instances
    private HomeFragment homeFragment;
    private CartFragment cartFragment;
    private OrderFragment orderFragment;
    private ProfileFragment profileFragment;
    private Fragment activeFragment;

    // To keep track of the currently active tab ID
    private int currentSelectedId = -1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // 初始化元件
        initViews();
        initFragments();

        // 設定點擊事件
        navHome.setOnClickListener(v -> selectTab(R.id.nav_home));
        navCart.setOnClickListener(v -> selectTab(R.id.nav_cart));
        navOrder.setOnClickListener(v -> selectTab(R.id.nav_order));
        
        // Profile tab with login protection
        navProfile.setOnClickListener(v -> {
            if (FirebaseAuth.getInstance().getCurrentUser() != null) {
                selectTab(R.id.nav_profile);
            } else {
                showLoginRequiredDialog();
            }
        });

        // Initial Selection
        if (savedInstanceState == null) {
            selectTab(R.id.nav_home);
        }
    }

    private void initFragments() {
        homeFragment = new HomeFragment();
        cartFragment = new CartFragment();
        orderFragment = new OrderFragment();
        profileFragment = new ProfileFragment();

        // Add all fragments once and hide them
        FragmentManager fm = getSupportFragmentManager();
        fm.beginTransaction().add(R.id.fragment_container, profileFragment, "4").hide(profileFragment).commit();
        fm.beginTransaction().add(R.id.fragment_container, orderFragment, "3").hide(orderFragment).commit();
        fm.beginTransaction().add(R.id.fragment_container, cartFragment, "2").hide(cartFragment).commit();
        fm.beginTransaction().add(R.id.fragment_container, homeFragment, "1").commit(); // Home visible by default
        
        activeFragment = homeFragment;
    }

    private void showLoginRequiredDialog() {
        DialogHelper.showConfirmationDialog(
                this,
                "Sign In Required",
                "You need to sign in to view your profile.",
                "Sign In",
                "Cancel",
                () -> {
                    Intent intent = new Intent(MainActivity.this, LoginActivity.class);
                    startActivity(intent);
                },
                null
        );
    }

    private void initViews() {
        navHome = findViewById(R.id.nav_home);
        navCart = findViewById(R.id.nav_cart);
        navOrder = findViewById(R.id.nav_order);
        navProfile = findViewById(R.id.nav_profile);

        ivHome = findViewById(R.id.iv_home);
        ivCart = findViewById(R.id.iv_cart);
        ivOrder = findViewById(R.id.iv_order);
        ivProfile = findViewById(R.id.iv_profile);

        tvHome = findViewById(R.id.tv_home);
        tvCart = findViewById(R.id.tv_cart);
        tvOrder = findViewById(R.id.tv_order);
        tvProfile = findViewById(R.id.tv_profile);
    }

    public void selectTab(int id) {
        // 1. Ignore if same tab selected
        if (id == currentSelectedId) return;

        Fragment targetFragment;
        if (id == R.id.nav_home) targetFragment = homeFragment;
        else if (id == R.id.nav_cart) targetFragment = cartFragment;
        else if (id == R.id.nav_order) targetFragment = orderFragment;
        else if (id == R.id.nav_profile) targetFragment = profileFragment;
        else return;

        // 2. Show/Hide fragments
        FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
        if (activeFragment != null) {
            ft.hide(activeFragment);
        }
        ft.show(targetFragment).commit();
        activeFragment = targetFragment;

        // 3. Trigger data load if needed (using the flag we added to fragments)
        triggerLoadData(targetFragment);

        // 4. Update UI visuals
        currentSelectedId = id;
        updateTabUI(id);
    }

    private void triggerLoadData(Fragment fragment) {
        if (fragment instanceof HomeFragment) ((HomeFragment) fragment).loadDataIfNeeded();
        else if (fragment instanceof CartFragment) ((CartFragment) fragment).loadDataIfNeeded();
        else if (fragment instanceof OrderFragment) ((OrderFragment) fragment).loadDataIfNeeded();
        else if (fragment instanceof ProfileFragment) ((ProfileFragment) fragment).loadDataIfNeeded();
    }

    private void updateTabUI(int id) {
        resetTabs();
        int activeColor = ContextCompat.getColor(this, R.color.bottom_nav_color);
        
        if (id == R.id.nav_home) {
            ivHome.setColorFilter(activeColor);
            tvHome.setTextColor(activeColor);
        } else if (id == R.id.nav_cart) {
            ivCart.setColorFilter(activeColor);
            tvCart.setTextColor(activeColor);
        } else if (id == R.id.nav_order) {
            ivOrder.setColorFilter(activeColor);
            tvOrder.setTextColor(activeColor);
        } else if (id == R.id.nav_profile) {
            ivProfile.setColorFilter(activeColor);
            tvProfile.setTextColor(activeColor);
        }
    }

    private void resetTabs() {
        int inactiveColor = 0xFF222222; // 深灰色
        
        ivHome.setColorFilter(inactiveColor);
        tvHome.setTextColor(inactiveColor);
        ivCart.setColorFilter(inactiveColor);
        tvCart.setTextColor(inactiveColor);
        ivOrder.setColorFilter(inactiveColor);
        tvOrder.setTextColor(inactiveColor);
        ivProfile.setColorFilter(inactiveColor);
        tvProfile.setTextColor(inactiveColor);
    }
}
