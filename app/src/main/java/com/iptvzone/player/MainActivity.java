package com.iptvzone.player;

import android.app.Activity;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.webkit.ValueCallback;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.FrameLayout;

public class MainActivity extends Activity {
    private WebView web;
    private FrameLayout kok;
    private View ozelGorunum;
    private WebChromeClient.CustomViewCallback ozelCb;

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        kok = new FrameLayout(this);
        web = new WebView(this);
        kok.addView(web, new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        setContentView(kok);

        WebSettings s = web.getSettings();
        s.setJavaScriptEnabled(true);
        s.setDomStorageEnabled(true);
        s.setMediaPlaybackRequiresUserGesture(false);
        s.setMixedContentMode(WebSettings.MIXED_CONTENT_ALWAYS_ALLOW);
        s.setAllowFileAccess(true);
        s.setAllowContentAccess(true);
        s.setAllowFileAccessFromFileURLs(true);
        s.setAllowUniversalAccessFromFileURLs(true); // CORS kısıtını kaldırır
        s.setUseWideViewPort(true);
        s.setLoadWithOverviewMode(true);
        s.setDomStorageEnabled(true);

        web.setWebViewClient(new WebViewClient());
        web.setWebChromeClient(new WebChromeClient() {
            @Override
            public void onShowCustomView(View view, CustomViewCallback cb) {
                if (ozelGorunum != null) { onHideCustomView(); return; }
                ozelGorunum = view;
                ozelCb = cb;
                kok.addView(ozelGorunum, new FrameLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
                web.setVisibility(View.GONE);
            }
            @Override
            public void onHideCustomView() {
                if (ozelGorunum == null) return;
                kok.removeView(ozelGorunum);
                ozelGorunum = null;
                web.setVisibility(View.VISIBLE);
                if (ozelCb != null) { ozelCb.onCustomViewHidden(); ozelCb = null; }
            }
        });

        web.loadUrl("file:///android_asset/index.html");
    }

    @Override
    public void onBackPressed() {
        if (ozelGorunum != null) {
            web.getWebChromeClient().onHideCustomView();
            return;
        }
        web.evaluateJavascript(
            "(function(){ try { return androidGeri(); } catch(e){ return false; } })();",
            new ValueCallback<String>() {
                @Override
                public void onReceiveValue(String val) {
                    if (!"true".equals(val)) { finish(); }
                }
            });
    }

    @Override
    protected void onDestroy() {
        if (web != null) { web.destroy(); web = null; }
        super.onDestroy();
    }
}
