package com.cinemora.movieorder;

// Bundle: 用來接收畫面狀態資料
import android.os.Bundle;

// 這些是畫面建立時需要用到的 class
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

// AndroidX annotation，幫助程式更清楚知道哪些參數不能是 null
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

// Fragment: 因為這個類別是 HomeFragment，所以要繼承 Fragment
import androidx.fragment.app.Fragment;

// RecyclerView 和 LinearLayoutManager
// RecyclerView = 顯示清單
// LinearLayoutManager = 決定清單方向（水平或垂直）
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

// HomeFragment 是 MainActivity 裡面的其中一個頁面
public class HomeFragment extends Fragment {

    // 宣告兩個 RecyclerView 變數
    // 一個給 Bestselling 用
    // 一個給 Movies 用
    private RecyclerView rvFeaturedMovies, rvMovies;

    // 空的 constructor
    // Fragment 通常都需要保留這個空建構子
    public HomeFragment() {
    }

    // onCreateView:
    // 這是 Fragment 最重要的方法之一
    // 它的工作是：
    // 1. 把 fragment_home.xml 變成畫面
    // 2. 抓到畫面裡的元件
    // 3. 做初始設定
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {

        // inflater.inflate(...) 的意思：
        // 把 XML 畫面檔 fragment_home 轉成真正的 View 物件
        // 這個 view 就是整個 HomeFragment 的畫面
        View view = inflater.inflate(R.layout.fragment_home, container, false);

        // 用 view.findViewById(...) 去抓 fragment_home.xml 裡的 RecyclerView
        // 注意：
        // Fragment 裡面通常不是直接 findViewById()
        // 而是先拿到根 view，再從 view 裡面找元件
        rvFeaturedMovies = view.findViewById(R.id.rvFeaturedMovies);
        rvMovies = view.findViewById(R.id.rvMovies);

        // 設定 Bestselling RecyclerView 的排列方向
        // LinearLayoutManager.HORIZONTAL = 水平排列
        // false = 不反向排列
        rvFeaturedMovies.setLayoutManager(
                new LinearLayoutManager(getContext(), LinearLayoutManager.HORIZONTAL, false)
        );

        // 設定 Movies RecyclerView 的排列方向
        // LinearLayoutManager.VERTICAL = 垂直排列
        rvMovies.setLayoutManager(
                new LinearLayoutManager(getContext(), LinearLayoutManager.VERTICAL, false)
        );

        // 最後一定要 return 這個 view
        // 讓系統知道：這就是 HomeFragment 要顯示的畫面
        return view;
    }
}