package com.iptvzone.player;

import android.app.Activity;
import android.content.pm.ActivityInfo;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.webkit.ValueCallback;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.FrameLayout;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Arrays;

public class MainActivity extends Activity {

    private WebView web;
    private FrameLayout kok;
    private View ozelGorunum;                              // tam ekran görünümü
    private WebChromeClient.CustomViewCallback ozelCb;
    private WebChromeClient krom;
    private int eskiYon = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED;

    // Oynatıcı her açılışta GitHub'dan güncellenir; internet yoksa son indirilen ya da APK içindeki kopya açılır.
    private static final String GUNCEL_URL = "https://raw.githubusercontent.com/Dogukan3437/iptv_zone/main/index.html";
    private File yerelKopya;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        kok = new FrameLayout(this);
        kok.setBackgroundColor(0xFF000000);
        web = new WebView(this);
        kok.addView(web, new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        setContentView(kok);

        WebSettings s = web.getSettings();
        s.setJavaScriptEnabled(true);
        s.setDomStorageEnabled(true);
        s.setAllowFileAccess(true);
        s.setAllowContentAccess(true);
        s.setAllowFileAccessFromFileURLs(true);
        s.setAllowUniversalAccessFromFileURLs(true);
        s.setMediaPlaybackRequiresUserGesture(false);
        s.setLoadWithOverviewMode(true);
        s.setUseWideViewPort(true);
        s.setMixedContentMode(WebSettings.MIXED_CONTENT_ALWAYS_ALLOW);

        web.setWebViewClient(new WebViewClient());
        krom = new WebChromeClient() {
            @Override
            public void onShowCustomView(View view, CustomViewCallback callback) {
                if (ozelGorunum != null) { callback.onCustomViewHidden(); return; }
                ozelGorunum = view;
                ozelCb = callback;
                kok.addView(view, new FrameLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
                web.setVisibility(View.GONE);
                tamEkranAc();
            }

            @Override
            public void onHideCustomView() {
                if (ozelGorunum == null) return;
                kok.removeView(ozelGorunum);
                ozelGorunum = null;
                web.setVisibility(View.VISIBLE);
                tamEkranKapat();
                if (ozelCb != null) { ozelCb.onCustomViewHidden(); ozelCb = null; }
            }
        };
        web.setWebChromeClient(krom);

        yerelKopya = new File(getFilesDir(), "index.html");
        web.loadUrl(yerelKopya.exists() ? "file://" + yerelKopya.getAbsolutePath() : "file:///android_asset/index.html");
        guncellemeyiIndir();
    }

    /** GitHub'daki güncel index.html'i indirir; farklıysa kaydedip sayfayı yeniler. */
    private void guncellemeyiIndir() {
        new Thread(new Runnable() {
            @Override public void run() {
                HttpURLConnection c = null;
                try {
                    c = (HttpURLConnection) new URL(GUNCEL_URL + "?t=" + System.currentTimeMillis()).openConnection();
                    c.setConnectTimeout(8000);
                    c.setReadTimeout(15000);
                    c.setUseCaches(false);
                    c.setRequestProperty("Cache-Control", "no-cache");
                    if (c.getResponseCode() != 200) return;
                    InputStream in = c.getInputStream();
                    ByteArrayOutputStream out = new ByteArrayOutputStream();
                    byte[] buf = new byte[16384];
                    int n;
                    while ((n = in.read(buf)) > 0) out.write(buf, 0, n);
                    in.close();
                    byte[] yeni = out.toByteArray();
                    String metin = new String(yeni, "UTF-8");
                    if (!metin.contains("IPTV ZONE") || !metin.contains("</html>")) return;   // bozuk indirme
                    byte[] eski = oku(yerelKopya);
                    if (eski != null && Arrays.equals(eski, yeni)) return;                  // zaten güncel
                    File gecici = new File(getFilesDir(), "index.tmp");
                    FileOutputStream fo = new FileOutputStream(gecici);
                    fo.write(yeni);
                    fo.close();
                    if (!gecici.renameTo(yerelKopya)) { yerelKopya.delete(); gecici.renameTo(yerelKopya); }
                    runOnUiThread(new Runnable() {
                        @Override public void run() {
                            if (ozelGorunum == null && web != null) web.loadUrl("file://" + yerelKopya.getAbsolutePath());
                        }
                    });
                } catch (Exception e) {
                    // internet yok: mevcut sürümle devam
                } finally {
                    if (c != null) c.disconnect();
                }
            }
        }).start();
    }

    private static byte[] oku(File f) {
        if (!f.exists()) return null;
        try {
            InputStream in = new java.io.FileInputStream(f);
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            byte[] buf = new byte[16384];
            int n;
            while ((n = in.read(buf)) > 0) out.write(buf, 0, n);
            in.close();
            return out.toByteArray();
        } catch (Exception e) {
            return null;
        }
    }

    /** Durum çubuğu + gezinme çubuğunu gizler, ekranı yataya çevirir. */
    @SuppressWarnings("deprecation")
    private void tamEkranAc() {
        Window w = getWindow();
        w.addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
        if (Build.VERSION.SDK_INT >= 28) {
            w.getAttributes().layoutInDisplayCutoutMode =
                    WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES;   // çentikli ekranlar
        }
        w.getDecorView().setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_FULLSCREEN
              | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
              | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
              | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
              | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
              | View.SYSTEM_UI_FLAG_LAYOUT_STABLE);
        eskiYon = getRequestedOrientation();
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE);
    }

    @SuppressWarnings("deprecation")
    private void tamEkranKapat() {
        Window w = getWindow();
        w.clearFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
        w.getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_VISIBLE);
        setRequestedOrientation(eskiYon);
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        if (hasFocus && ozelGorunum != null) tamEkranAc();   // bildirim çekilince çubuklar geri gizlensin
    }

    @Override
    public void onBackPressed() {
        if (ozelGorunum != null) {                             // önce tam ekrandan çık
            krom.onHideCustomView();
            return;
        }
        web.evaluateJavascript(
                "(function(){ try { return androidGeri(); } catch(e){ return false; } })();",
                new ValueCallback<String>() {
                    @Override public void onReceiveValue(String value) {
                        if (!"true".equals(value)) finish();
                    }
                });
    }

    @Override
    protected void onDestroy() {
        if (web != null) web.destroy();
        super.onDestroy();
    }
}
