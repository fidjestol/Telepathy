package com.example.telepathy.view.activities;

import android.os.Bundle;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.annotation.SuppressLint;
import android.view.View;

import androidx.appcompat.app.AppCompatActivity;

import com.example.telepathy.R;

public class VideoActivity extends AppCompatActivity {
    private WebView webView;

    @SuppressLint("SetJavaScriptEnabled") // We explicitly acknowledge the security implications
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_video);

        webView = findViewById(R.id.videoWebView);

        // Configure WebView settings with security in mind
        WebSettings webSettings = webView.getSettings();
        webSettings.setJavaScriptEnabled(true); // Required for YouTube player
        webSettings.setAllowFileAccess(false); // Prevent access to local files
        webSettings.setAllowContentAccess(false); // Prevent access to content providers
        webSettings.setAllowFileAccessFromFileURLs(false);
        webSettings.setAllowUniversalAccessFromFileURLs(false);
        webSettings.setLoadWithOverviewMode(true); // Enables viewport meta tag
        webSettings.setUseWideViewPort(true); // Enables viewport meta tag
        webSettings.setDisplayZoomControls(false);
        webSettings.setBuiltInZoomControls(false);

        // Hide system UI for true fullscreen
        getWindow().getDecorView().setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                        | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                        | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                        | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                        | View.SYSTEM_UI_FLAG_FULLSCREEN
                        | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY);

        // Set WebView clients with restricted navigation
        webView.setWebViewClient(new WebViewClient() {
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, String url) {
                // Only allow youtube.com URLs
                return !url.startsWith("https://www.youtube.com/");
            }
        });
        webView.setWebChromeClient(new WebChromeClient());

        // Load the YouTube video using the embed URL
        String videoId = "V2KCAfHjySQ"; // Your specific video ID
        String embedUrl = "https://www.youtube.com/embed/" + videoId;
        String html = "<!DOCTYPE html><html><head><meta name='viewport' content='width=device-width, initial-scale=1.0, maximum-scale=1.0, user-scalable=no'>"
                +
                "<style>body { margin: 0; padding: 0; width: 100vw; height: 100vh; background-color: black; }" +
                "iframe { width: 100vw; height: 100vh; border: none; }</style></head>" +
                "<body><iframe src='" + embedUrl + "?autoplay=1&playsinline=1&fs=1' " +
                "allow='accelerometer; autoplay; clipboard-write; encrypted-media; gyroscope; picture-in-picture; fullscreen' "
                +
                "allowfullscreen></iframe></body></html>";

        webView.loadData(html, "text/html", "utf-8");
    }

    @Override
    protected void onPause() {
        super.onPause();
        webView.onPause();
    }

    @Override
    protected void onResume() {
        super.onResume();
        webView.onResume();
        // Reapply fullscreen flags when resuming
        getWindow().getDecorView().setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                        | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                        | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                        | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                        | View.SYSTEM_UI_FLAG_FULLSCREEN
                        | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY);
    }
}