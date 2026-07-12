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
import android.webkit.WebResourceRequest;
import android.webkit.WebResourceResponse;
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

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.TimeUnit;

import okhttp3.OkHttpClient;
import okhttp3.ResponseBody;

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

    private final OkHttpClient injectionHttpClient = new OkHttpClient.Builder()
            .connectTimeout(20, TimeUnit.SECONDS)
            .readTimeout(20, TimeUnit.SECONDS)
            .build();

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
            public WebResourceResponse shouldInterceptRequest(WebView view, WebResourceRequest request) {
                // On n'intercepte que la requête de la page principale
                // demandée (pas les .js/.css/images/appels API), et une
                // seule fois : on récupère nous-mêmes le HTML et on y
                // colle notre script de session TOUT AU DÉBUT, avant que
                // le <script> de la page (qui vérifie le token) ne
                // s'exécute. Ça évite la redirection vers index.html
                // qui se produisait quand l'injection arrivait trop tard.
                if (!sessionInjected
                        && pendingInjectJs != null && !pendingInjectJs.isEmpty()
                        && request.isForMainFrame()
                        && request.getUrl().toString().equals(pendingUrl)) {
                    WebResourceResponse injected = fetchAndInjectSession(request.getUrl().toString());
                    if (injected != null) {
                        sessionInjected = true;
                        return injected;
                    }
                }
                return super.shouldInterceptRequest(view, request);
            }

            @Override
            public void onPageFinished(WebView view, String url) {
                super.onPageFinished(view, url);
                progressBar.setVisibility(View.GONE);
            }
        });
    }

    /**
     * Télécharge le HTML de la page cible et insère un &lt;script&gt; contenant
     * l'injection de session juste après &lt;head&gt; (ou tout au début si pas
     * de &lt;head&gt;). Retourne null en cas d'échec réseau, pour laisser la
     * WebView charger la page normalement (repli).
     */
    @Nullable
    private WebResourceResponse fetchAndInjectSession(String url) {
        try {
            okhttp3.Request request = new okhttp3.Request.Builder().url(url).build();
            okhttp3.Response response = injectionHttpClient.newCall(request).execute();
            ResponseBody body = response.body();
            if (!response.isSuccessful() || body == null) {
                return null;
            }
            String html = body.string();
            String scriptTag = "<script>" + pendingInjectJs + "</script>";
            String injectedHtml;
            if (html.contains("<head>")) {
                injectedHtml = html.replaceFirst("<head>", "<head>" + scriptTag);
            } else if (html.toLowerCase().contains("<html>")) {
                injectedHtml = html.replaceFirst("(?i)<html>", "<html>" + scriptTag);
            } else {
                injectedHtml = scriptTag + html;
            }
            return new WebResourceResponse("text/html", "UTF-8",
                    new ByteArrayInputStream(injectedHtml.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception e) {
            return null; // repli : la page se chargera normalement (sans session pré-injectée)
        }
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
        // IMPORTANT : ne PAS activer "overview mode" / "wide viewport" ici.
        // Ces réglages servent à zoomer une page web pour qu'elle rentre à
        // l'écran d'un téléphone — appliqués à notre rendu de ticket, ils
        // réduisaient tout le contenu (texte minuscule sur le papier).
        // On veut un rendu 1 pixel CSS = 1 pixel, à la largeur exacte de
        // l'imprimante.
        hidden.getSettings().setLoadWithOverviewMode(false);
        hidden.getSettings().setUseWideViewPort(false);
        hidden.getSettings().setTextZoom(100); // ignore la taille de police système du téléphone
        hidden.setInitialScale(100);
        // Indispensable : sans ça, une WebView positionnée hors-écran ne
        // dessine pas sa couche accélérée matériellement, et view.draw()
        // capture une image blanche. Le rendu logiciel force un vrai dessin
        // dans le canvas, même hors-écran.
        hidden.setLayerType(View.LAYER_TYPE_SOFTWARE, null);

        FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(PRINTER_WIDTH_PX, FrameLayout.LayoutParams.WRAP_CONTENT);
        hidden.setLayoutParams(params);
        hidden.setTranslationX(-10000f); // hors écran mais toujours mesuré/rendu
        rootLayout.addView(hidden);

        hidden.setWebViewClient(new WebViewClient() {
            @Override
            public void onPageFinished(WebView view, String url) {
                // Un premier délai pour laisser le CSS/les images se mettre en place,
                // puis on vérifie que le contenu a une hauteur avant de capturer ;
                // sinon on retente une fois après un délai plus long.
                view.postDelayed(() -> attemptCapture(view, 0), 400);
            }
        });

        hidden.loadDataWithBaseURL(LotatoConstants.BASE_URL, html, "text/html", "UTF-8", null);
    }

    private void attemptCapture(WebView view, int retryCount) {
        int contentHeightPx = (int) (view.getContentHeight() * view.getScale());
        if (contentHeightPx <= 0 && retryCount < 3) {
            view.postDelayed(() -> attemptCapture(view, retryCount + 1), 400);
            return;
        }
        captureAndPrint(view, contentHeightPx > 0 ? contentHeightPx : 1000);
    }

    private void captureAndPrint(WebView view, int contentHeightPx) {
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
