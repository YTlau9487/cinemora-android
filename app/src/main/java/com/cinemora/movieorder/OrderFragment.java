package com.cinemora.movieorder;

// Bundle：用來保存或接收 Fragment 狀態資料
import android.os.Bundle;

// 建立畫面需要用到的 class
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

// AndroidX 註解
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

// Fragment 父類別
import androidx.fragment.app.Fragment;

// OrderFragment 代表訂單頁面
public class OrderFragment extends Fragment {

    // 空的建構子
    public OrderFragment() {
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {

        // 把 fragment_order.xml 轉成畫面並回傳
        // 目前先只做空狀態版面
        // 之後才會顯示真正的訂單資料
        return inflater.inflate(R.layout.fragment_order, container, false);
    }
}