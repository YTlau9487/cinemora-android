package com.cinemora.movieorder;

// Bundle：用來保存或接收 Fragment 狀態資料
import android.os.Bundle;

// 建立畫面時會用到的 class
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

// AndroidX 註解
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

// Fragment 父類別
import androidx.fragment.app.Fragment;

// CartFragment 代表購物車頁面
public class CartFragment extends Fragment {

    // 空的建構子
    public CartFragment() {
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {

        // 把 fragment_cart.xml 轉換成畫面並回傳
        // 目前這個 Fragment 只有負責顯示 layout
        // 還沒有加入購物車資料邏輯
        return inflater.inflate(R.layout.fragment_cart, container, false);
    }
}