package com.cinemora.movieorder;

// Bundle：用來保存或接收 Fragment 狀態資料
import android.os.Bundle;

// 建立畫面時需要的 class
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

// AndroidX 註解
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

// Fragment 父類別
import androidx.fragment.app.Fragment;

// ProfileFragment 代表個人資料頁面
public class ProfileFragment extends Fragment {

    // 空的建構子
    public ProfileFragment() {
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {

        // 把 fragment_profile.xml 轉成畫面並回傳
        // 目前先只做靜態版面，之後再接會員資料
        return inflater.inflate(R.layout.fragment_profile, container, false);
    }
}