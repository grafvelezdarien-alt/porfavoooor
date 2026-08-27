package com.appmint.miapp;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.graphics.Bitmap;
import android.net.Uri;
import android.net.http.SslError;
import android.os.Build;
import android.os.Bundle;
import android.os.Message;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.view.View;
import android.webkit.CookieManager;
import android.webkit.JavascriptInterface;
import android.webkit.SslErrorHandler;
import android.webkit.ValueCallback;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceError;
import android.webkit.WebResourceRequest;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.appcompat.widget.Toolbar;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

public class MainActivity extends AppCompatActivity {

    // ================== CONFIGURACION GENERADA POR APPMINT STUDIO ==================
    private static final String APP_NAME = "Mi App";
    private static final String APP_URL = "";
    private static final boolean EXTERNAL_LINKS = true;
    private static final boolean CONFIRM_EXIT = true;
    private static final boolean POPUP_BLOCK = true;
    private static final boolean PULL_TO_REFRESH = true;
    private static final boolean GESTURE_ZOOM = true;
    private static final boolean DESKTOP_MODE = false;
    private static final boolean HIDE_USER_AGENT = false;
    private static final boolean PERSISTENT_COOKIES = true;
    private static final boolean PREVENT_SLEEP = false;
    private static final boolean BLOCK_SCREENSHOTS = false;
    private static final int DARK_MODE = -1;
    private static final String STATUS_BAR_COLOR = "#6d28d9";
    private static final boolean LIGHT_STATUS_ICONS = true;
    // ==============================================================================

    private WebView webView;
    private SwipeRefreshLayout swipeRefresh;
    private Toolbar toolbar;
    private FrameLayout splashView;
    private ProgressBar progressBar;
    private ValueCallback<Uri[]> filePathCallback;

    private final ActivityResultLauncher<Intent> fileChooserLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(), result -> {
                if (filePathCallback == null) return;
                Intent data = result.getData();
                Uri[] results = null;
                if (result.getResultCode() == RESULT_OK && data != null) {
                    if (data.getClipData() != null) {
                        int n = data.getClipData().getItemCount();
                        results = new Uri[n];
                        for (int i = 0; i < n; i++) {
                            results[i] = data.getClipData().getItemAt(i).getUri();
                        }
                    } else if (data.getData() != null) {
                        results = new Uri[]{ data.getData() };
                    }
                }
                filePathCallback.onReceiveValue(results);
                filePathCallback = null;
            });

    @SuppressLint("SetJavaScriptEnabled", "AddJavascriptInterface")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        if (DARK_MODE == 1) AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
        else if (DARK_MODE == 0) AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
        else AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM);

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        if (PREVENT_SLEEP) getWindow().addFlags(android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        if (BLOCK_SCREENSHOTS) getWindow().addFlags(android.view.WindowManager.LayoutParams.FLAG_SECURE);

        toolbar = findViewById(R.id.toolbar);
        toolbar.setTitle(APP_NAME);
        setSupportActionBar(toolbar);

        getWindow().setStatusBarColor(android.graphics.Color.parseColor(STATUS_BAR_COLOR));
        if (Build.VERSION.SDK_INT >= 23) {
            int uiFlags = getWindow().getDecorView().getSystemUiVisibility();
            if (!LIGHT_STATUS_ICONS) uiFlags |= View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR;
            else uiFlags &= ~View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR;
            getWindow().getDecorView().setSystemUiVisibility(uiFlags);
        }

        webView = findViewById(R.id.webView);
        swipeRefresh = findViewById(R.id.swipeRefresh);
        splashView = findViewById(R.id.splashView);
        progressBar = findViewById(R.id.progressBar);

        if (!PULL_TO_REFRESH) swipeRefresh.setEnabled(false);
        swipeRefresh.setColorSchemeResources(R.color.primary, R.color.accent);
        swipeRefresh.setOnRefreshListener(() -> webView.reload());

        setupWebView();

        if (APP_URL != null && !APP_URL.isEmpty()) {
            webView.loadUrl(APP_URL);
        } else {
            webView.loadUrl("file:///android_asset/www/index.html");
        }
    }

    @SuppressLint("SetJavaScriptEnabled")
    private void setupWebView() {
        WebSettings s = webView.getSettings();
        s.setJavaScriptEnabled(true);
        s.setDomStorageEnabled(true);
        s.setAllowFileAccess(true);
        s.setAllowContentAccess(true);
        s.setJavaScriptCanOpenWindowsAutomatically(true);
        s.setSupportMultipleWindows(!POPUP_BLOCK);
        s.setBuiltInZoomControls(GESTURE_ZOOM);
        s.setDisplayZoomControls(false);
        s.setSupportZoom(GESTURE_ZOOM);
        if (DESKTOP_MODE) {
            s.setUseWideViewPort(true);
            s.setLoadWithOverviewMode(true);
        }
        if (HIDE_USER_AGENT) {
            s.setUserAgentString("Mozilla/5.0 (Linux; Android 14) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/125.0.0.0 Mobile Safari/537.36 AppMint");
        }
        if (PERSISTENT_COOKIES) {
            CookieManager cm = CookieManager.getInstance();
            cm.setAcceptCookie(true);
            cm.setAcceptThirdPartyCookies(webView, true);
        }

        webView.setWebViewClient(new WebViewClient() {
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
                return handleUrl(request.getUrl().toString());
            }

            @Override
            public void onPageStarted(WebView view, String url, Bitmap favicon) {
                progressBar.setVisibility(View.VISIBLE);
            }

            @Override
            public void onPageFinished(WebView view, String url) {
                progressBar.setVisibility(View.GONE);
                swipeRefresh.setRefreshing(false);
                hideSplash();
                String t = view.getTitle();
                if (t != null && !t.isEmpty()) toolbar.setTitle(t);
            }

            @Override
            public void onReceivedError(WebView view, WebResourceRequest request, WebResourceError error) {
                if (request != null && request.isForMainFrame()) {
                    showErrorPage();
                }
            }

            @Override
            public void onReceivedSslError(WebView view, SslErrorHandler handler, SslError error) {
                handler.proceed();
            }
        });

        webView.setWebChromeClient(new WebChromeClient() {
            @Override
            public boolean onCreateWindow(WebView view, boolean isDialog, boolean isUserGesture, Message resultMsg) {
                if (POPUP_BLOCK) {
                    String url = null;
                    WebView.HitTestResult hr = view.getHitTestResult();
                    if (hr != null) url = hr.getExtra();
                    if (url != null && url.startsWith("http")) {
                        openInBrowser(url);
                        return false;
                    }
                    return false;
                }
                WebView newWv = new WebView(MainActivity.this);
                newWv.setWebViewClient(new WebViewClient());
                WebView.WebViewTransport transport = (WebView.WebViewTransport) resultMsg.obj;
                transport.setWebView(newWv);
                resultMsg.sendToTarget();
                return true;
            }

            @Override
            public void onProgressChanged(WebView view, int newProgress) {
                if (newProgress >= 100) progressBar.setVisibility(View.GONE);
                else progressBar.setVisibility(View.VISIBLE);
                progressBar.setProgress(newProgress);
            }

            @Override
            public boolean onShowFileChooser(WebView wv, ValueCallback<Uri[]> cb, FileChooserParams params) {
                filePathCallback = cb;
                Intent intent = params.createIntent();
                intent.addCategory(Intent.CATEGORY_OPENABLE);
                try {
                    fileChooserLauncher.launch(intent);
                } catch (ActivityNotFoundException e) {
                    filePathCallback = null;
                    return false;
                }
                return true;
            }
        });

        webView.addJavascriptInterface(new MintBridge(), "MintApp");
        webView.addJavascriptInterface(new MintBridge(), "Android");
    }

    private boolean handleUrl(String url) {
        if (url == null) return false;
        Uri uri = Uri.parse(url);
        String scheme = uri.getScheme();
        if (scheme == null) return false;
        if ("tel".equals(scheme) || "mailto".equals(scheme) || "sms".equals(scheme)) {
            openInBrowser(url);
            return true;
        }
        if ("http".equals(scheme) || "https".equals(scheme)) {
            if (EXTERNAL_LINKS && APP_URL != null && !APP_URL.isEmpty()) {
                String host = uri.getHost();
                String appHost = Uri.parse(APP_URL).getHost();
                if (host != null && appHost != null && !host.equals(appHost)) {
                    openInBrowser(url);
                    return true;
                }
            }
            return false;
        }
        return false;
    }

    private void openInBrowser(String url) {
        try {
            startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(url)));
        } catch (ActivityNotFoundException e) {
            Toast.makeText(this, "No se encontró una app para abrir este enlace", Toast.LENGTH_SHORT).show();
        }
    }

    private void showErrorPage() {
        String html = "<html><body style='background:#f3f4f6;color:#374151;font-family:sans-serif;text-align:center;padding:64px 24px'>"
                + "<h2>No se pudo cargar el contenido</h2>"
                + "<p style='color:#6b7280'>Comprueba tu conexión a internet e inténtalo de nuevo.</p>"
                + "<button onclick='location.reload()' style='background:#7c3aed;color:#fff;border:0;padding:12px 22px;border-radius:10px;font-size:15px;cursor:pointer'>Reintentar</button>"
                + "</body></html>";
        webView.loadDataWithBaseURL(null, html, "text/html", "utf-8", null);
    }

    private void hideSplash() {
        if (splashView != null && splashView.getVisibility() == View.VISIBLE) {
            splashView.animate().alpha(0f).setDuration(300)
                    .withEndAction(() -> splashView.setVisibility(View.GONE));
        }
    }

    @Override
    public void onBackPressed() {
        if (webView.canGoBack()) {
            webView.goBack();
        } else if (CONFIRM_EXIT) {
            new AlertDialog.Builder(this)
                    .setTitle(APP_NAME)
                    .setMessage("¿Quieres salir de la aplicación?")
                    .setPositiveButton("Salir", (d, w) -> finish())
                    .setNegativeButton("Cancelar", null)
                    .show();
        } else {
            finish();
        }
    }

    private class MintBridge {
        @JavascriptInterface
        public void toast(String message) {
            runOnUiThread(() -> Toast.makeText(MainActivity.this, message, Toast.LENGTH_SHORT).show());
        }

        @JavascriptInterface
        public void showDialog(String title, String message) {
            runOnUiThread(() -> new AlertDialog.Builder(MainActivity.this)
                    .setTitle(title).setMessage(message).setPositiveButton("OK", null).show());
        }

        @JavascriptInterface
        public void vibrate(long millis) {
            Vibrator v = (Vibrator) getSystemService(VIBRATOR_SERVICE);
            if (v != null && v.hasVibrator()) {
                if (Build.VERSION.SDK_INT >= 26) {
                    v.vibrate(VibrationEffect.createOneShot(millis, VibrationEffect.DEFAULT_AMPLITUDE));
                } else {
                    v.vibrate(millis);
                }
            }
        }

        @JavascriptInterface
        public void setKeepScreenOn(boolean on) {
            runOnUiThread(() -> {
                if (on) getWindow().addFlags(android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
                else getWindow().clearFlags(android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
            });
        }

        @JavascriptInterface
        public void setScreenshotsEnabled(boolean on) {
            runOnUiThread(() -> {
                if (on) getWindow().clearFlags(android.view.WindowManager.LayoutParams.FLAG_SECURE);
                else getWindow().addFlags(android.view.WindowManager.LayoutParams.FLAG_SECURE);
            });
        }

        @JavascriptInterface
        public void openUrl(String url) {
            runOnUiThread(() -> openInBrowser(url));
        }

        @JavascriptInterface
        public void share(String text) {
            Intent i = new Intent(Intent.ACTION_SEND);
            i.setType("text/plain");
            i.putExtra(Intent.EXTRA_TEXT, text);
            startActivity(Intent.createChooser(i, "Compartir"));
        }

        @JavascriptInterface
        public void exit() {
            runOnUiThread(MainActivity.this::finishAffinity);
        }

        @JavascriptInterface
        public void setTitle(String title) {
            runOnUiThread(() -> toolbar.setTitle(title));
        }

        @JavascriptInterface
        public void setOrientation(String o) {
            runOnUiThread(() -> {
                if ("portrait".equals(o)) setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
                else if ("landscape".equals(o)) setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
                else setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED);
            });
        }

        @JavascriptInterface
        public void setFullscreen(boolean on) {
            runOnUiThread(() -> {
                View decor = getWindow().getDecorView();
                if (on) {
                    decor.setSystemUiVisibility(View.SYSTEM_UI_FLAG_FULLSCREEN
                            | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                            | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                            | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                            | View.SYSTEM_UI_FLAG_LAYOUT_STABLE);
                } else {
                    decor.setSystemUiVisibility(View.SYSTEM_UI_FLAG_VISIBLE);
                }
            });
        }

        @JavascriptInterface
        public void hideSplash() {
            runOnUiThread(() -> {
                if (splashView != null) splashView.setVisibility(View.GONE);
            });
        }

        @JavascriptInterface
        public String getAppName() {
            return APP_NAME;
        }

        @JavascriptInterface
        public String getAppVersion() {
            try {
                return getPackageManager().getPackageInfo(getPackageName(), 0).versionName;
            } catch (Exception e) {
                return "1.0.0";
            }
        }
    }
}
