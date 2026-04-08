package com.cinemora.movieorder;

import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

public class MainActivity extends AppCompatActivity {

    private LinearLayout navHome, navCart, navOrder, navProfile;
    private ImageView ivHome, ivCart, ivOrder, ivProfile;
    private TextView tvHome, tvCart, tvOrder, tvProfile;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // 初始化元件
        initViews();

        // 預設顯示 Home
        if (savedInstanceState == null) {
            selectTab(R.id.nav_home, new HomeFragment());
        }

        // 設定點擊事件
        navHome.setOnClickListener(v -> selectTab(R.id.nav_home, new HomeFragment()));
        navCart.setOnClickListener(v -> selectTab(R.id.nav_cart, new CartFragment()));
        navOrder.setOnClickListener(v -> selectTab(R.id.nav_order, new OrderFragment()));
        navProfile.setOnClickListener(v -> selectTab(R.id.nav_profile, new ProfileFragment()));
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

    private void selectTab(int id, Fragment fragment) {
        // 先重設所有顏色為灰色 (未選取)
        resetTabs();

        // 根據點擊的 ID 設定顏色 (已選取)
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

        // 切換 Fragment
        loadFragment(fragment);
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

    private void loadFragment(Fragment fragment) {
        getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.fragment_container, fragment)
                .commit();
    }
}