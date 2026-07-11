package com.smartxplorer.bestsystemlottery;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.webkit.JavascriptInterface;
import android.webkit.WebChromeClient;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.FrameLayout;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import com.smart.xplorer.SmartPrinter;
import com.smartxplorer.bestsystemlottery.lotato.LotatoConstants;

import static com.smartxplorer.bestsystemlottery.util.Constant.KEY_CONNECTED_PRINTER;
import static com.smartxplorer.bestsystemlottery.util.Constant.SHARED_PREFS;

/**
 * Héberge l'app web LOTATO PRO (hébergée en ligne) dans une WebView, en
 * gardant la présentation et la logique de jeu identiques au site.
 *
 * Fonctionnalités natives ajoutées :
 *  - Injection de session (localStorage) après une connexion faite en natif
 *    (voir LoginActivity), pour ouvrir directement le bon écran (player,
 *    agent, owner, superadmin) sans repasser par un login web.
 *  - Impression Bluetooth des tickets : le JS du site appelle déjà
 *    window.AndroidPrint.printHTML(html) (voir cartManager.js / uiManager.js),
 *    on fournit ici cette interface.
 *  - Scan de code-barres via la caméra (AndroidScan.scan()) — à brancher
 *    côté web si besoin, le site actuel ne l'appelle pas encore.
 */
public class LotatoWebActivity extends AppCompatActivity {

    private static final int PRINTER_WIDTH_PX = 384; // ~ 58mm à 203dpi, à ajuster selon l'imprimante
    private static final int REQUEST_BLUETOOTH_PERMS = 501;
    private static final int REQUEST_CAMERA_PERM = 502;

    private WebView webView;
    private FrameLayout rootLayout;
    private ProgressBar progressBar;
    private boolean sessionInjected = false;
    private String pendingUrl;
    private String pendingInjectJs;
    private String pendingHtmlToPrint;

    private ActivityResultLauncher<Intent> scanLauncher;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_lotato_web);

        rootLayout = findViewById(R.id.rootLotatoLayout);
        webView = findViewById(R.id.webViewLotato);
        progressBar = findViewById(R.id.progressLotatoWeb);

        pendingUrl = getIntent().getStringExtra(LotatoConstants.EXTRA_URL);
        pendingInjectJs = getIntent().getStringExtra(LotatoConstants.EXTRA_INJECT_JS);
        if (pendingUrl == null) {
            pendingUrl = LotatoConstants.PAGE_PLAYER;
        }

        setupScanLauncher();
        setupWebView();

        webView.loadUrl(pendingUrl);
    }

    @SuppressLint({"SetJavaScriptEnabled", "JavascriptInterface"})
    private void setupWebView() {
        webView.getSettings().setJavaScriptEnabled(true);
        webView.getSettings().setDomStorageEnabled(true); // indispensable pour localStorage
        webView.getSettings().setLoadWithOverviewMode(true);
        webView.getSettings().setUseWideViewPort(true);
        webView.getSettings().setMixedContentMode(android.webkit.WebSettings.MIXED_CONTENT_COMPATIBILITY_MODE);
        webView.getSettings().setCacheMode(android.webkit.WebSettings.LOAD_DEFAULT);

        webView.addJavascriptInterface(new AndroidPrintBridge(), "AndroidPrint");
        webView.addJavascriptInterface(new AndroidScanBridge(), "AndroidScan");

        webView.setWebChromeClient(new WebChromeClient());
        webView.setWebViewClient(new WebViewClient() {
            @Override
            public void onPageFinished(WebView view, String url) {
                super.onPageFinished(view, url);
                progressBar.setVisibility(View.GONE);

                if (!sessionInjected && pendingInjectJs != null && !pendingInjectJs.isEmpty()) {
                    sessionInjected = true;
                    // On applique la session déjà obtenue en natif puis on
                    // recharge une fois pour que le site se comporte comme
                    // si l'utilisateur venait de se connecter normalement.
                    view.evaluateJavascript(pendingInjectJs, value -> view.postDelayed(view::reload, 150));
                }
            }
        });
    }

    @Override
    public void onBackPressed() {
        if (webView.canGoBack()) {
            webView.goBack();
        } else {
            super.onBackPressed();
        }
    }

    // ==================== Impression (Bluetooth) ====================

    private class AndroidPrintBridge {
        @JavascriptInterface
        public void printHTML(String html) {
            runOnUiThread(() -> handlePrintRequest(html));
        }
    }

    private void handlePrintRequest(String html) {
        boolean connected = getSharedPreferences(SHARED_PREFS, MODE_PRIVATE)
                .getBoolean(KEY_CONNECTED_PRINTER, false);

        if (!connected) {
            pendingHtmlToPrint = html;
            requestBluetoothPermissionsThenShowPrinterList();
            return;
        }
        renderHtmlAndPrint(html);
    }

    private void requestBluetoothPermissionsThenShowPrinterList() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT)
                    != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.BLUETOOTH_CONNECT, Manifest.permission.BLUETOOTH_SCAN},
                        REQUEST_BLUETOOTH_PERMS);
                return;
            }
        }
        showPrinterListThenPrint();
    }

    private void showPrinterListThenPrint() {
        SmartPrinter.showPrinterList(this, R.color.colorBlue, printerName -> {
            if (printerName != null && !printerName.contains("failed")) {
                getSharedPreferences(SHARED_PREFS, MODE_PRIVATE).edit()
                        .putBoolean(KEY_CONNECTED_PRINTER, true).apply();
                if (pendingHtmlToPrint != null) {
                    renderHtmlAndPrint(pendingHtmlToPrint);
                    pendingHtmlToPrint = null;
                }
            } else {
                Toast.makeText(this, "Connexion imprimante échouée", Toast.LENGTH_SHORT).show();
            }
        });
    }

    /**
     * Rend le HTML du ticket (généré par cartManager.js) dans une WebView
     * cachée, le capture en image, puis l'envoie à l'imprimante Bluetooth
     * déjà connectée. La largeur/hauteur de rendu (PRINTER_WIDTH_PX) peut
     * nécessiter un ajustement selon le modèle d'imprimante utilisé.
     */
    private void renderHtmlAndPrint(String html) {
        WebView hidden = new WebView(this);
        hidden.getSettings().setJavaScriptEnabled(true);
        hidden.getSettings().setLoadWithOverviewMode(true);
        hidden.getSettings().setUseWideViewPort(true);

        FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(PRINTER_WIDTH_PX, FrameLayout.LayoutParams.WRAP_CONTENT);
        hidden.setLayoutParams(params);
        hidden.setTranslationX(-10000f); // hors écran mais toujours mesuré/rendu
        rootLayout.addView(hidden);

        hidden.setWebViewClient(new WebViewClient() {
            @Override
            public void onPageFinished(WebView view, String url) {
                view.postDelayed(() -> captureAndPrint(view), 350);
            }
        });

        hidden.loadDataWithBaseURL(LotatoConstants.BASE_URL, html, "text/html", "UTF-8", null);
    }

    private void captureAndPrint(WebView view) {
        int contentHeightPx = (int) (view.getContentHeight() * view.getScale());
        if (contentHeightPx <= 0) contentHeightPx = 1000;

        int widthSpec = View.MeasureSpec.makeMeasureSpec(PRINTER_WIDTH_PX, View.MeasureSpec.EXACTLY);
        int heightSpec = View.MeasureSpec.makeMeasureSpec(contentHeightPx, View.MeasureSpec.EXACTLY);
        view.measure(widthSpec, heightSpec);
        view.layout(0, 0, PRINTER_WIDTH_PX, contentHeightPx);

        Bitmap bitmap = Bitmap.createBitmap(PRINTER_WIDTH_PX, contentHeightPx, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        canvas.drawColor(Color.WHITE);
        view.draw(canvas);

        rootLayout.removeView(view);

        SmartPrinter.with(getApplicationContext(), 58).connect(smartPrinter -> {
            smartPrinter.printImage(bitmap, SmartPrinter.FULL_WIDTH, SmartPrinter.CENTER);
            smartPrinter.feedPaper();
            smartPrinter.close();
        });
    }

    // ==================== Scan code-barre ====================

    private class AndroidScanBridge {
        @JavascriptInterface
        public void scan() {
            runOnUiThread(() -> requestCameraThenScan());
        }
    }

    private void requestCameraThenScan() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CAMERA}, REQUEST_CAMERA_PERM);
            return;
        }
        scanLauncher.launch(new Intent(this, ScanActivity.class));
    }

    private void setupScanLauncher() {
        scanLauncher = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
            if (result.getData() == null) return;
            // ScanActivity renvoie les extras "ticket" et "date" (voir ScanActivity.java).
            String ticket = result.getData().getStringExtra("ticket");
            String date = result.getData().getStringExtra("date");
            if (ticket == null) return;
            // Le site web doit définir window.onAndroidScanResult(ticket, date) pour
            // récupérer la valeur scannée (non branché par défaut côté LOTATO PRO).
            String js = "if (window.onAndroidScanResult) { window.onAndroidScanResult("
                    + org.json.JSONObject.quote(ticket) + "," + org.json.JSONObject.quote(date == null ? "" : date) + "); }";
            webView.evaluateJavascript(js, null);
        });
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (grantResults.length == 0 || grantResults[0] != PackageManager.PERMISSION_GRANTED) {
            return;
        }
        if (requestCode == REQUEST_BLUETOOTH_PERMS) {
            showPrinterListThenPrint();
        } else if (requestCode == REQUEST_CAMERA_PERM) {
            scanLauncher.launch(new Intent(this, ScanActivity.class));
        }
    }
}
