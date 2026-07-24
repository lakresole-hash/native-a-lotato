package com.smartxplorer.bestsystemlottery;

import static com.smartxplorer.bestsystemlottery.connectionInternet.UtilNetwork.isConnectedFast;
import static com.smartxplorer.bestsystemlottery.util.Constant.KEY_CONNECTED_PRINTER;
import static com.smartxplorer.bestsystemlottery.util.Constant.KEY_LOGO;
import static com.smartxplorer.bestsystemlottery.util.Constant.KEY_URL_LOGO;
import static com.smartxplorer.bestsystemlottery.util.Constant.SHARED_PREFS;
import static com.smartxplorer.bestsystemlottery.util.Constant.offline;
import static com.smartxplorer.bestsystemlottery.util.Utils.extractTicketDate;


import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.RequiresPermission;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.ConnectivityManager;
import android.net.http.SslError;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.util.Base64;
import android.util.Log;
import android.view.View;
import android.webkit.CookieManager;
import android.webkit.JavascriptInterface;
import android.webkit.SslErrorHandler;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceError;
import android.webkit.WebResourceRequest;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.ImageView;
import android.widget.Toast;

import com.google.firebase.remoteconfig.FirebaseRemoteConfig;
import com.google.firebase.remoteconfig.FirebaseRemoteConfigSettings;
import com.smartxplorer.bestsystemlottery.servicesManage.ServicesManager;
import com.smart.xplorer.SmartPrinter;
import com.smartxplorer.bestsystemlottery.connectionInternet.NetworkChangeListener;
import com.smartxplorer.bestsystemlottery.model.PrintFooter;
import com.smartxplorer.bestsystemlottery.model.PrintHeader;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.smartxplorer.bestsystemlottery.server.Request;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import okhttp3.ResponseBody;

public class MainActivity extends AppCompatActivity implements
        View.OnClickListener, SwipeRefreshLayout.OnRefreshListener {
    private WebView webView;
    private String userName;
    private String type, option_lot, numbers, amount;
    private boolean findUser = true;
    private FloatingActionButton btPrint, print_sale_report;
    // variable for shared preferences.
    private SharedPreferences sharedpreferences;
    private ImageView imgLogo;
    private boolean connectPrinter;
    private ProgressDialog progressDialog;
    private NetworkChangeListener networkChangeListener;
    private boolean isConnectionFast;
    private String imgLogoPref, Logo;
    private Bitmap bitmap;
    private static final int REQUEST_BLUETOOTH = 200;
    private static final int REQUEST_PERMISSION = 1;
    private static final int REQUEST_ENABLE_BT = 2;

    private static final int REQUEST_CAMERA = 100;
    BluetoothAdapter bluetoothAdapter;
    private SwipeRefreshLayout swipeRefreshLayout;
    private FirebaseRemoteConfig remoteConfig;
    public static String url;
    public static String BASE_URL;
    public static String API_KEY;
    public static String API_SECRET;
    private ActivityResultLauncher<Intent> scanLauncher;

    @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        init();
        scanLaucher();
    }

    private void checkCameraPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {

            if (checkSelfPermission(Manifest.permission.CAMERA)
                    != PackageManager.PERMISSION_GRANTED) {

                requestPermissions(
                        new String[]{
                                Manifest.permission.CAMERA},
                        REQUEST_CAMERA
                );

            } else {
                scanLauncher.launch(new Intent(this, ScanActivity.class));
            }
        } else {
            scanLauncher.launch(new Intent(this, ScanActivity.class));
        }
    }

    private void supportedBluetooth() {
        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

        if (bluetoothAdapter == null) {
            Toast.makeText(this, "Bluetooth non supporté", Toast.LENGTH_LONG).show();
            return;
        }

        checkPermissions();
    }

    private void checkPermissions() {

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {

            if (checkSelfPermission(Manifest.permission.BLUETOOTH_CONNECT)
                    != PackageManager.PERMISSION_GRANTED) {

                requestPermissions(
                        new String[]{
                                Manifest.permission.BLUETOOTH_CONNECT,
                                Manifest.permission.BLUETOOTH_SCAN
                        },
                        REQUEST_PERMISSION
                );

                return;
            }
        } else {
            if (checkSelfPermission(Manifest.permission.BLUETOOTH_CONNECT)
                    != PackageManager.PERMISSION_GRANTED) {

                requestPermissions(
                        new String[]{
                                Manifest.permission.BLUETOOTH_CONNECT,
                                Manifest.permission.BLUETOOTH_SCAN
                        },
                        REQUEST_PERMISSION
                );

                return;
            }
        }

        startBluetooth();
    }

    @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
    private void startBluetooth() {

        if (!bluetoothAdapter.isEnabled()) {

            Intent enableBtIntent =
                    new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);

            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                // TODO: Consider calling
                //    ActivityCompat#requestPermissions
                // here to request the missing permissions, and then overriding
                //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
                //                                          int[] grantResults)
                // to handle the case where the user grants the permission. See the documentation
                // for ActivityCompat#requestPermissions for more details.
                return;
            }
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                // TODO: Consider calling
                //    ActivityCompat#requestPermissions
                // here to request the missing permissions, and then overriding
                //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
                //                                          int[] grantResults)
                // to handle the case where the user grants the permission. See the documentation
                // for ActivityCompat#requestPermissions for more details.
                return;
            }
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
                // TODO: Consider calling
                //    ActivityCompat#requestPermissions
                // here to request the missing permissions, and then overriding
                //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
                //                                          int[] grantResults)
                // to handle the case where the user grants the permission. See the documentation
                // for ActivityCompat#requestPermissions for more details.
                return;
            }
            startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);

        } else {
            showPrinterList();
//              showPairedDevices();
        }
    }

    private void showPairedDevices() {

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (checkSelfPermission(Manifest.permission.BLUETOOTH_CONNECT)
                    != PackageManager.PERMISSION_GRANTED) {
                return;
            }
        }

        Set<BluetoothDevice> devices =
                bluetoothAdapter.getBondedDevices();

        for (BluetoothDevice device : devices) {

            Toast.makeText(
                    this,
                    device.getName() + " - " + device.getAddress(),
                    Toast.LENGTH_LONG
            ).show();
        }
    }

    @Override
    public void onRequestPermissionsResult(
            int requestCode,
            @NonNull String[] permissions,
            @NonNull int[] grantResults) {

        super.onRequestPermissionsResult(
                requestCode,
                permissions,
                grantResults);

        if (requestCode == REQUEST_PERMISSION || requestCode == REQUEST_CAMERA) {

            if (grantResults.length > 0
                    && grantResults[0] == PackageManager.PERMISSION_GRANTED) {

                if (ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                    // TODO: Consider calling
                    //    ActivityCompat#requestPermissions
                    // here to request the missing permissions, and then overriding
                    //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
                    //                                          int[] grantResults)
                    // to handle the case where the user grants the permission. See the documentation
                    // for ActivityCompat#requestPermissions for more details.
                    return;
                }
                startBluetooth();

            } else {

                Toast.makeText(
                        this,
                        "Permission Bluetooth refusée",
                        Toast.LENGTH_LONG
                ).show();
            }

            if (grantResults.length > 0
                    && grantResults[0] == PackageManager.PERMISSION_GRANTED) {

                if (ActivityCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
                    // TODO: Consider calling
                    //    ActivityCompat#requestPermissions
                    // here to request the missing permissions, and then overriding
                    //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
                    //                                          int[] grantResults)
                    // to handle the case where the user grants the permission. See the documentation
                    // for ActivityCompat#requestPermissions for more details.
                    return;
                }
                scanLauncher.launch(new Intent(this, ScanActivity.class));

            } else {

                Toast.makeText(
                        this,
                        "Permission Caméra refusée",
                        Toast.LENGTH_LONG
                ).show();
            }
        }
    }

    @SuppressLint("SetJavaScriptEnabled")
    private void init() {
        btPrint = (FloatingActionButton) findViewById(R.id.print);
        print_sale_report = (FloatingActionButton) findViewById(R.id.print_sale_report);
        btPrint.setOnClickListener(this);
        print_sale_report.setOnClickListener(this);

        webView = (WebView) findViewById(R.id.webview);
        imgLogo = (ImageView) findViewById(R.id.imgLogo);
        swipeRefreshLayout = (SwipeRefreshLayout) findViewById(R.id.refreshLayout);

        networkChangeListener = new NetworkChangeListener();

        // getting the data which is stored in shared preferences.
        sharedpreferences = getSharedPreferences(SHARED_PREFS, Context.MODE_PRIVATE);

        progressDialog = new ProgressDialog(this);

        webView.setWebChromeClient(new WebChromeClient());

        WebSettings webSettings = webView.getSettings();
        webSettings.setJavaScriptEnabled(true);
        webSettings.setDomStorageEnabled(true);
        webSettings.setAllowFileAccess(false);
        webSettings.setCacheMode(WebSettings.LOAD_DEFAULT);
        webSettings.setUseWideViewPort(true);
        webSettings.setLoadWithOverviewMode(true);
        webSettings.setSupportZoom(false);
        webSettings.setBuiltInZoomControls(false);

        // Refresh  the layout
        swipeRefreshLayout.setOnRefreshListener(this);

        isConnectionFast = isConnectedFast(getApplicationContext());

        if (isConnectionFast) {
            getUrlFirebase();
        } else {
            alerDialog(getString(R.string.no_connecton));
        }
    }

    private void enalbleNotification() {
        boolean enabled = NotificationManagerCompat
                .from(this)
                .areNotificationsEnabled();

        if (!enabled) {
            // Notifications bloquées
            Intent intent = new Intent();
            intent.setAction(android.provider.Settings.ACTION_APP_NOTIFICATION_SETTINGS);
            intent.putExtra(android.provider.Settings.EXTRA_APP_PACKAGE, getPackageName());
            startActivity(intent);
        }
    }

    private void getUrlFirebase() {

        if (isConnectionFast) {
            webView.setEnabled(false);

            remoteConfig = FirebaseRemoteConfig.getInstance();

            FirebaseRemoteConfigSettings configSettings =
                    new FirebaseRemoteConfigSettings.Builder()
                            .setMinimumFetchIntervalInSeconds(0) // dev mode
                            .build();

            remoteConfig.setConfigSettingsAsync(configSettings);

// Valeur par défaut
            remoteConfig.setDefaultsAsync(Map.of("webview_url", ""));

            remoteConfig.fetchAndActivate()
                    .addOnCompleteListener(task -> {

                        if (task.isSuccessful()) {

                            url = remoteConfig.getString("webview_url");

                            BASE_URL = remoteConfig.getString("BASE_URL");

                            API_KEY = remoteConfig.getString("api_key");

                            API_SECRET = remoteConfig.getString("api_secret");

                            Log.e("REMOTE", "URL: " + url);

                            Log.e("REMOTE", "BASE_URL: " + BASE_URL);

//                            loadWebView(url);
                            loadWebview(url);

                        } else {
                            if(!url.isEmpty()) {
                                loadOfflineHtml(url);
                            }
                        }
                    });
        } else {
            alerDialog(getString(R.string.no_connecton));
        }
    }

    private void loadOfflineHtml(String link) {

        try {
            InputStream is = getAssets().open("offline.html");
            int size = is.available();
            byte[] buffer = new byte[size];

            is.read(buffer);
            is.close();

            String html = new String(buffer, StandardCharsets.UTF_8);

            //
//            Remplacer la variable
            html = html.replace("{{LINK}}", link);

            webView.loadDataWithBaseURL(

                    offline,
                    html,
                    "text/html",
                    "UTF-8",

                    null
            );

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void requestBluetoothPermissions() {

        List<String> permissions = new ArrayList<>();

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            // Android 12+
            if (checkSelfPermission(Manifest.permission.BLUETOOTH_SCAN)
                    != PackageManager.PERMISSION_GRANTED) {
                permissions.add(Manifest.permission.BLUETOOTH_SCAN);
            }

            if (checkSelfPermission(Manifest.permission.BLUETOOTH_CONNECT)
                    != PackageManager.PERMISSION_GRANTED) {
                permissions.add(Manifest.permission.BLUETOOTH_CONNECT);
            }

        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            requestPermissions(
                    new String[]{Manifest.permission.POST_NOTIFICATIONS},
                    2001
            );

        } else {
            // Android 6 → 11
            if (checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION)
                    != PackageManager.PERMISSION_GRANTED) {
                permissions.add(Manifest.permission.ACCESS_FINE_LOCATION);
            }
        }

        if (!permissions.isEmpty()) {
            requestPermissions(
                    permissions.toArray(new String[0]),
                    REQUEST_BLUETOOTH
            );
        }
    }

    @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
    private void checkBluetoothEnabled() {

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (checkSelfPermission(Manifest.permission.BLUETOOTH_CONNECT)
                    != PackageManager.PERMISSION_GRANTED) {

                requestPermissions(
                        new String[]{Manifest.permission.BLUETOOTH_CONNECT},
                        1001
                );
                return;
            }

            BluetoothAdapter adapter = BluetoothAdapter.getDefaultAdapter();

            if (adapter == null) {
                Toast.makeText(this, "Bluetooth non supporté", Toast.LENGTH_LONG).show();
                return;
            }

            if (!adapter.isEnabled()) {
                Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                startActivity(enableBtIntent);
            }
        } else {
            BluetoothAdapter adapter = BluetoothAdapter.getDefaultAdapter();

            if (adapter == null) {
                Toast.makeText(this, "Bluetooth non supporté", Toast.LENGTH_LONG).show();
                return;
            }

            if (!adapter.isEnabled()) {
                Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                startActivity(enableBtIntent);
            }
        }

    }

    private void alerDialog(String alert) {
        AlertDialog alertDialog = new AlertDialog.Builder(getApplicationContext()).create();

        alertDialog.setTitle("Info");
        alertDialog.setMessage(alert);
        alertDialog.setIcon(android.R.drawable.ic_dialog_alert);
        alertDialog.setButton("OK", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int which) {
                finish();
            }
        });

        alertDialog.show();
    }

    private void sendDataScanToWeb(String ticket, String date) {
        webView.post(() -> {
            String js = "setTicketDate(" + JSONObject.quote(ticket) + "," + JSONObject.quote(date) + ")";

            webView.evaluateJavascript(js, null);
        });
    }

    @SuppressLint("JavascriptInterface")
    private void loadWebview(String url) {
        // Setting we View Client
        webView.setWebViewClient(new WebViewClient() {
            @Override
            public void onPageFinished(WebView view, String url) {
                super.onPageFinished(view, url);
                // initializing the printWeb Object
                if (progressDialog.isShowing()) {
                    progressDialog.dismiss();
                    webView.setEnabled(true);
                }

                // initializing our shared preferences.
                sharedpreferences = getSharedPreferences(SHARED_PREFS, Context.MODE_PRIVATE);
                // getting data from shared prefs and
                // storing it in our string variable.
                connectPrinter = sharedpreferences.getBoolean(KEY_CONNECTED_PRINTER, false);
                Log.e("KEY_CONNECTED_PRINTER", connectPrinter + "");

                if (isConnectionFast) {
                    if (connectPrinter) {
//                        enalbleNotification();
                        String cookies = CookieManager.getInstance().getCookie(url);
                        if (cookies != null) {
                            findUser = cookies.contains("username");

                            if (findUser) {
                                cookies = cookies.substring(cookies.lastIndexOf("username=") - 1);
                                userName = cookies.replace("username=", "");
//                    Toast.makeText(getApplicationContext(), "" + userName, Toast.LENGTH_SHORT).show();
                            }
                        }

//                        Log.e("COIOO", "UUU " + cookies.contains("username"));
                    } else {
                        supportedBluetooth();
//                        showPrinterList();
                    }
                }
            }

            @Override
            public void onReceivedSslError(WebView view, SslErrorHandler handler, SslError error) {
                String message = null;
                switch (error.getPrimaryError()) {
                    case SslError.SSL_UNTRUSTED:
                        message = "The certificate authority is not trusted.";
                        break;
                    case SslError.SSL_EXPIRED:
                        message = "The certificate has expired.";
                        break;
                    case SslError.SSL_IDMISMATCH:
                        message = "The certificate Hostname mismatch.";
                        break;
                    case SslError.SSL_INVALID:
                        message = "SSL connection is invalid.";
                        break;
                }

                Log.e("MESSAGE", message);
            }

            @SuppressLint("NewApi")
            @Override
            public void onReceivedError(WebView view, WebResourceRequest request,
                                        WebResourceError error) {
                if (request.isForMainFrame() && error != null) {
                    Log.e("MESSAGE error", error + "");
                    view.loadUrl(offline + "offline.html");
                }
            }

            @Override
            public void onReceivedError(WebView view, int errorCode, String description, String failingUrl) {
                if (errorCode != WebViewClient.ERROR_UNSUPPORTED_SCHEME && errorCode
                        != WebViewClient.ERROR_HOST_LOOKUP) {
                    Log.e("MESSAGE description", description + "");
                    view.loadUrl(offline + "offline.html");
                }
            }

            @Override
            public void onPageStarted(WebView view, String url, Bitmap favicon) {
                super.onPageStarted(view, url, favicon);
                Log.e("onPageStarted", "onPageStarted " + url);

                progressDialog.setMessage(getString(R.string.loading));
                progressDialog.show();
                webView.setEnabled(false);
            }
        });

        // loading the URL
        webView.loadUrl(url);

        webView.addJavascriptInterface(new Object() {

            @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
            @JavascriptInterface
            public void performClick(String msg) {

                Log.e("CLICK WEB", msg + "");

                String data[] = msg.replaceAll("\\s", "")
                        .split(";");

                String action = data[0];

                Log.e("CLICK action", action + "");

                if (action.equals(getString(R.string.sell_report))) {

                    String dateReport = data[1];
                    Log.e("dateReport", dateReport + "");
                    if (isConnectedFast(getApplicationContext())) {
                        connectPrinter = sharedpreferences.getBoolean(KEY_CONNECTED_PRINTER, false);
                        if (connectPrinter) {
                            printSaleReport(userName, dateReport);
                        } else {
                            showPrinterList();
                        }
                    } else {
                        alerDialog(getString(R.string.connetion_invalible));
                        Log.e("isConnected", "dateReport" + isConnectedFast(getApplicationContext()));
                    }
                }

                if (action.equals(getString(R.string.create_ticket))) {

                    boolean result = Boolean.parseBoolean(data[1]);
                    if (result) {
                        Log.e("result", result + "");
                        if (isConnectedFast(getApplicationContext())) {
                            connectPrinter = sharedpreferences.getBoolean(KEY_CONNECTED_PRINTER, false);
                            if (connectPrinter) {
                                printTicket(userName);
                            } else {
                                showPrinterList();
                            }
                        } else {
                            alerDialog(getString(R.string.connetion_invalible));
                        }
                    }
                }

                if (action.equals(getString(R.string.winning_tickets))) {

                    String date_time_start = data[1];
                    String date_time_end = data[2];
                    Log.e("date_time_start", date_time_start + "");
                    Log.e("date_time_end", date_time_end + "");
                    if (isConnectedFast(getApplicationContext())) {
                        connectPrinter = sharedpreferences.getBoolean(KEY_CONNECTED_PRINTER, false);
                        if (connectPrinter) {
                            printWinningTicket(userName, date_time_start, date_time_end);
                        } else {
                            showPrinterList();
                        }
                    } else {
                        alerDialog(getString(R.string.connetion_invalible));
                    }

                }

                if (action.equals(getString(R.string.historic_sale))) {

                    String date_time_start = data[1];
                    String date_time_end = data[2];
                    Log.e("date_time_start", date_time_start + "");
                    Log.e("date_time_end", date_time_end + "");
                    if (isConnectedFast(getApplicationContext())) {
                        connectPrinter = sharedpreferences.getBoolean(KEY_CONNECTED_PRINTER, false);
                        if (connectPrinter) {
                            printHistoricalSales(userName, date_time_start, date_time_end);
                        } else {
                            showPrinterList();
                        }
                    } else {
                        alerDialog(getString(R.string.connetion_invalible));
                    }

                }

                if (action.equals(getString(R.string.print_by_number_ticket))) {

                    String numberTicket = data[1];
                    String date = data[2];
                    Log.e("numberTicket", numberTicket + " date " + date);
                    if (isConnectedFast(getApplicationContext())) {
                        connectPrinter = sharedpreferences.getBoolean(KEY_CONNECTED_PRINTER, false);
                        if (connectPrinter) {
                            printByNumberTicket(numberTicket, date);
                        } else {
                            showPrinterList();
                        }
                    } else {
                        alerDialog(getString(R.string.connetion_invalible));
                    }

                }

                if (action.equals(getString(R.string.connection_bluetooth))) {
                    supportedBluetooth();
                }

                if (action.equals(getString(R.string.scan))) {
                    checkCameraPermission();
                }

                if (action.equals(getString(R.string.request_notif))) {
                    enalbleNotification();
                }

            }
        }, "message");

    }

    private void showPrinterList() {
        SmartPrinter.showPrinterList(this, R.color.colorBlue, printerName -> {
            Toast.makeText(this, printerName, Toast.LENGTH_SHORT).show();
//            TextView connectedTo = findViewById(R.id.tv_printer_info);
            String text = "Connected to : " + printerName;
//            connectedTo.setText(text);
            if (!printerName.contains("failed")) {
//                findViewById(R.id.btn_printer_test).setVisibility(View.VISIBLE);
                SharedPreferences.Editor editor = sharedpreferences.edit();
                // Put values for
                editor.putBoolean(KEY_CONNECTED_PRINTER, true);
                editor.commit();
                findViewById(R.id.btn_printer_test).setOnClickListener(v -> testPrinter());
            }
        });
    }

    private void testPrinter() {
        SmartPrinter.with(this, 58).printTest(getString(R.string.app_name_)
                + " " + getString(R.string.printTest));
    }

    @Override
    public void onRefresh() {
        isConnectionFast = isConnectedFast(getApplicationContext());
        if (isConnectionFast) {
//            Toast.makeText(MainActivity.this, "Load1", Toast.LENGTH_SHORT);
            progressDialog.setMessage(getString(R.string.loading));
            progressDialog.show();
            loadWebview(url);
        } else {
            loadOfflineHtml(url);
        }
        swipeRefreshLayout.setRefreshing(false);
    }

    private class LoadImage extends AsyncTask<String, Void, Bitmap> {
        @Override
        protected Bitmap doInBackground(String... strings) {
            Bitmap bitmap = null;
            try {
                InputStream inputStream = new URL(strings[0]).openStream();
                bitmap = BitmapFactory.decodeStream(inputStream);
            } catch (IOException e) {
                e.printStackTrace();
            }
            return bitmap;
        }

        @Override
        protected void onPostExecute(Bitmap bitmap) {
            imgLogo.setImageBitmap(bitmap);
            savebitmap(bitmap);
        }
    }

    private String getEncodingImg() {
        String encodingImg = "";
        sharedpreferences = getSharedPreferences(SHARED_PREFS, Context.MODE_PRIVATE);
        // getting data from shared prefs and
        // storing it in our string variable.
        imgLogoPref = sharedpreferences.getString(KEY_LOGO, null);

        if (imgLogoPref != null) {

            encodingImg = imgLogoPref;
        }

        return encodingImg;
    }

    private String getLogo() {
        String logo = "";
        sharedpreferences = getSharedPreferences(SHARED_PREFS, Context.MODE_PRIVATE);
        // getting data from shared prefs and
        // storing it in our string variable.
        Logo = sharedpreferences.getString(KEY_URL_LOGO, null);

        if (Logo != null) {

            logo = Logo;
        }

        return logo;
    }

    private Bitmap decodeImg(String encodingImg) {
        byte[] b = Base64.decode(encodingImg, Base64.DEFAULT);
        Bitmap bitmap = BitmapFactory.decodeByteArray(b, 0, b.length);

        return bitmap;
    }

    private void savebitmap(Bitmap bmp) {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        bmp.compress(Bitmap.CompressFormat.JPEG, 100, baos);
        byte[] b = baos.toByteArray();
        String encodedImage = Base64.encodeToString(b, Base64.DEFAULT);
        SharedPreferences.Editor editor = sharedpreferences.edit();
        // Put values for
        editor.putString(KEY_LOGO, encodedImage);
        editor.commit();
        bitmap = bmp;
    }

    private void saveLogo(String url) {
        SharedPreferences.Editor editor = sharedpreferences.edit();
        // Put values for
        editor.putString(KEY_URL_LOGO, url);
        editor.commit();
    }

    private void printSaleReport(String username, String date) {
        progressDialog.setMessage(getString(R.string.loading_print));
        progressDialog.show();
        Log.e("userName", "userName " + username);
        ServicesManager.getInstance(getApplicationContext()).
                getServicesManager().GetSalesReport(username, date,
                        new Request.RequestListener<ResponseBody>() {
                            @SuppressLint("UnsafeIntentLaunch")
                            @Override
                            public void onSuccess(ResponseBody response) throws IOException, JSONException {

                                String body = response.string();

                                Log.e("BODY RAPPORT", "RAPPORT " + body);

                                JSONObject object = new JSONObject(body);

                                boolean error;

                                error = object.getBoolean(getString(R.string.error));

                                if (!error) {

                                    JSONObject rapport = object.optJSONObject("rapport");
                                    JSONObject total = object.optJSONObject("total");
                                    JSONArray jsonArrayresultats_titre = object.getJSONArray("resultats");

                                    assert rapport != null;
                                    String saleReport = rapport.getString("titre");
                                    String businessName = rapport.getString("entreprise");
                                    String logoName = rapport.getString(getString(R.string.logo));
                                    String date = rapport.getString("date_heure_impression");
                                    String balance = rapport.getString("balance_totale");
                                    String ticket_pending = rapport.getString("montant_tickets_attente");
                                    String agent = rapport.getString("agent");
                                    String pendingTicket = rapport.getString("tickets_en_attente");
                                    String losingTicket = rapport.getString("tickets_perdants");
                                    String winningTicket = rapport.getString("tickets_gagnants");
                                    String totalTickets = rapport.getString("total_tickets");
                                    String sales = rapport.getString("ventes");
                                    String fees = rapport.getString("commissions");
                                    String prizeWins = rapport.getString("prix_gagnes");
                                    String net = rapport.getString("nette");
                                    String balanceDay = rapport.getString("balance_jour");
                                    String totalSold = total.getString("total_vendu");
                                    String fees1 = total.getString("commissions");
                                    String prizeWins1 = total.getString("prix_gagnes");
                                    String netRevenue = total.getString("revenu_net");

                                    PrintHeader header = new PrintHeader();

                                    //set print header
                                    header.setSalesReport(saleReport);
                                    header.setMerchantName(businessName);
                                    header.setDates(getString(R.string.date_hours_print) + "\n" + date);
                                    header.setTicketPending(ticket_pending);

                                    String logo = getLogo();

                                    if (!logo.equals(logoName)) {
                                        saveLogo(logoName);
                                        new LoadImage().execute(BASE_URL + "/uploads/" + logoName);
                                        String encodingImg = getEncodingImg();
                                        bitmap = decodeImg(encodingImg);
                                        Log.e("urlLogo 1", logo);
                                    } else {
                                        String encodingImg = getEncodingImg();
                                        bitmap = decodeImg(encodingImg);
                                        Log.e("urlLogo 2", logo);
                                    }

                                    SmartPrinter.with(getApplicationContext(), 58).connect(smartPrinter -> {

                                        // header
                                        smartPrinter.printImage(bitmap, 140);

                                        smartPrinter.printTextlnBold(header.getMerchantName().toUpperCase(), SmartPrinter.CENTER);

                                        String ticket = SmartPrinter.centerText(header.getSalesReport()) + "\n" +
                                                SmartPrinter.centerText(header.getDates()) + "\n" +
                                                smartPrinter.line() + "\n" +
                                                // body
                                                smartPrinter.twoCol(getString(R.string.balance_tot), balance) + "\n" +
                                                smartPrinter.twoCol(getString(R.string.agents), agent) + "\n" +
                                                smartPrinter.twoCol(getString(R.string.ticket_pending), pendingTicket) + "\n" +
                                                smartPrinter.twoCol(getString(R.string.lossing_ticket), losingTicket) + "\n" +
                                                smartPrinter.twoCol(getString(R.string.winning_ticket), winningTicket) + "\n" +
                                                smartPrinter.twoCol(getString(R.string.total_ticket), totalTickets) + "\n" +
                                                smartPrinter.twoCol(getString(R.string.sales), sales) + "\n" +
                                                smartPrinter.twoCol(getString(R.string.fess), fees) + "\n" +
                                                smartPrinter.twoCol(getString(R.string.prize_wins), prizeWins) + "\n" +
                                                smartPrinter.twoCol(getString(R.string.net), net) + "\n" +
                                                smartPrinter.twoCol(getString(R.string.balance_day), balanceDay) + "\n" +
                                                smartPrinter.twoCol(getString(R.string.sales), sales) + "\n";
//
                                        smartPrinter.printTextlnNormal(ticket);


                                        boolean resultats_statut;

                                        resultats_statut = object.getBoolean(getString(R.string.resultats_statut));

                                        if (resultats_statut) {

                                            String winningNumber = object.getString(getString(R.string.resultats_titre));

                                            header.setWinningNumber(winningNumber);

                                            smartPrinter.printTextlnBold(header.getWinningNumber(), SmartPrinter.CENTER);

                                            for (int i = 0; i < jsonArrayresultats_titre.length(); i++) {

                                                JSONObject objectresultats = jsonArrayresultats_titre.getJSONObject(i);

                                                String lottery = objectresultats.getString("loterie");
                                                String firstLot = objectresultats.getString("1erlot");
                                                String secondLot = objectresultats.getString("2elot");
                                                String thirdLot = objectresultats.getString("3elot");
                                                String loto3 = objectresultats.getString("loto3");
                                                String loto4_1 = objectresultats.getString("Loto4_1");
                                                String loto4_2 = objectresultats.getString("Loto4_2");
                                                String loto4_3 = objectresultats.getString("Loto4_3");
                                                String loto5_1 = objectresultats.getString("Loto5_1");
                                                String loto5_2 = objectresultats.getString("Loto5_2");
                                                String loto5_3 = objectresultats.getString("Loto5_3");
                                                String pick2FL = objectresultats.getString("pick2FL");

                                                String printLottery = smartPrinter.twoCol(getString(R.string.lottery), lottery) + "\n";

                                                String printwinningTicket = smartPrinter.twoCol(getString(R.string.first_lot), firstLot) + "\n" +
                                                        smartPrinter.twoCol(getString(R.string.second_lot), secondLot) + "\n" +
                                                        smartPrinter.twoCol(getString(R.string.third_lot), thirdLot) + "\n" +
                                                        smartPrinter.twoCol(getString(R.string.lot3), loto3) + "\n" +
                                                        smartPrinter.twoCol(getString(R.string.lot4_1), loto4_1) + "\n" +
                                                        smartPrinter.twoCol(getString(R.string.lot4_2), loto4_2) + "\n" +
                                                        smartPrinter.twoCol(getString(R.string.lot4_3), loto4_3) + "\n" +
                                                        smartPrinter.twoCol(getString(R.string.lot5_1), loto5_1) + "\n" +
                                                        smartPrinter.twoCol(getString(R.string.lot5_2), loto5_2) + "\n" +
                                                        smartPrinter.twoCol(getString(R.string.lot5_3), loto5_3);

                                                smartPrinter.printTextlnBold(printLottery);
                                                smartPrinter.printTextlnNormal(printwinningTicket);


                                                if (!pick2FL.isEmpty()) {

                                                    String printwinningTicket1 =
                                                            // body
                                                            smartPrinter.twoCol(getString(R.string.pick2_fl), pick2FL) + "\n";
//
                                                    smartPrinter.printTextlnNormal(printwinningTicket1);


                                                }
                                            }
                                        }

                                        smartPrinter.addNewLine(2);

                                        smartPrinter.close();
                                        startActivity(getIntent());
                                    }, this::showToast);


                                } else {
//                                    progressDialog.dismiss();
                                    alerDialog(getString(R.string.error_server));
                                }
                            }

                            private void showToast(String s) {
                                Toast.makeText(getApplicationContext(), s, Toast.LENGTH_SHORT).show();
                            }

                            @Override
                            public void onResponse() {

                            }

                            @Override
                            public void onError() {

                            }
                        });
    }

    private void printHistoricalSales(String username, String date_time_start,
                                      String date_time_end) {
        progressDialog.setMessage(getString(R.string.loading_print));
        progressDialog.show();

        Log.e("userName", "userName " + username);
        ServicesManager.getInstance(getApplicationContext()).
                getServicesManager().GethistoricalSales(username, date_time_start,
                        date_time_end,
                        new Request.RequestListener<ResponseBody>() {
                            @SuppressLint("UnsafeIntentLaunch")
                            @Override
                            public void onSuccess(ResponseBody response) throws IOException, JSONException {

                                String body = response.string();

                                Log.e("BODY GethistoricalSales", "" + body);

                                JSONObject object = new JSONObject(body);

                                boolean error;

                                error = object.getBoolean(getString(R.string.error));
                                String status = object.getString(getString(R.string.status));

                                if (!error) {
                                    if (status.equals(getString(R.string.success))) {
                                        JSONObject objectHistoric = object.
                                                getJSONObject(getString(R.string.historical));
                                        String logoName = objectHistoric.getString(getString(R.string.logo));
                                        String titre = objectHistoric.getString(getString(R.string.titre));
                                        String soustitre = objectHistoric.getString(getString(R.string.soustitre));
                                        String entreprise = objectHistoric.getString(getString(R.string.entreprise));
                                        String date_heure_impression = objectHistoric.getString(getString(R.string.date_heure_impression));
                                        String agent = objectHistoric.getString(getString(R.string.agent_name));
                                        String ref = objectHistoric.getString(getString(R.string.ref));
                                        String tickets = objectHistoric.getString(getString(R.string.tickets_));
//                                        String tickets = "09098098";
                                        String ventes = objectHistoric.getString(getString(R.string.ventes));
                                        String commissions = objectHistoric.getString(getString(R.string.commissions));
                                        String prix_gagnes = objectHistoric.getString(getString(R.string.prix_gagnes));
                                        String balance_totale = objectHistoric.getString(getString(R.string.balance_totale));

                                        PrintHeader printHeader = new PrintHeader();

                                        //set print header
                                        printHeader.setHistoricalSales(titre + "\n" + soustitre);
                                        printHeader.setMerchantName(entreprise);

                                        printHeader.setDates(getString(R.string.date_heure_impression_)
                                                + "\n" + date_heure_impression);

                                        String logo = getLogo();

                                        if (!logo.equals(logoName)) {
                                            saveLogo(logoName);
                                            new LoadImage().execute(BASE_URL + "/uploads/" + logoName);
                                            String encodingImg = getEncodingImg();
                                            bitmap = decodeImg(encodingImg);
                                            Log.e("urlLogo 1", logo);
                                        } else {
                                            String encodingImg = getEncodingImg();
                                            bitmap = decodeImg(encodingImg);
                                            Log.e("urlLogo 2", logo);
                                        }

                                        SmartPrinter.with(getApplicationContext(), 58).connect(smartPrinter -> {

                                            // header
                                            smartPrinter.printImage(bitmap, 140);

                                            smartPrinter.printTextlnBold(printHeader.getMerchantName().toUpperCase(), SmartPrinter.CENTER);

                                            String ticket = SmartPrinter.centerText(printHeader.getHistoricalSales()) + "\n" +
                                                    SmartPrinter.centerText(printHeader.getDates()) + "\n" +
                                                    smartPrinter.line() + "\n" +
                                                    // body
                                                    smartPrinter.twoCol(getString(R.string.agents), agent) + "\n" +
                                                    smartPrinter.twoCol(getString(R.string.ref_), ref) + "\n" +
                                                    smartPrinter.twoCol(getString(R.string.tickets), tickets) + "\n" +
                                                    smartPrinter.twoCol(getString(R.string.sales), ventes) + "\n" +
                                                    smartPrinter.twoCol(getString(R.string.fess), commissions) + "\n" +
                                                    smartPrinter.twoCol(getString(R.string.prize_wins), prix_gagnes) + "\n" +
                                                    smartPrinter.twoCol(getString(R.string.balance_totale_), balance_totale) + "\n" +
                                                    SmartPrinter.lineSpace(2);
//
                                            smartPrinter.printTextlnNormal(ticket);

                                            smartPrinter.close();
                                            startActivity(getIntent());
                                        }, this::showToast);
                                    }
                                } else {
//                                    progressDialog = new ProgressDialog(getApplicationContext());
//                                    progressDialog.dismiss();
                                    alerDialog(getString(R.string.error_server));

                                }
                            }

                            private void showToast(String s) {
                                Toast.makeText(getApplicationContext(), s, Toast.LENGTH_SHORT).show();
                                Log.e("TOASt", s + " TOATS");
                            }

                            @Override
                            public void onResponse() {

                            }

                            @Override
                            public void onError() {
//                                progressDialog1.dismiss();
                            }
                        });
    }

    private void printWinningTicket(String username, String date_time_start,
                                    String date_time_end) {
        progressDialog.setMessage(getString(R.string.loading_print));
        progressDialog.show();

        Log.e("userName", "userName " + username);
        ServicesManager.getInstance(getApplicationContext()).
                getServicesManager().GetwinningTicket(username, date_time_start,
                        date_time_end,
                        new Request.RequestListener<ResponseBody>() {
                            @SuppressLint("UnsafeIntentLaunch")
                            @Override
                            public void onSuccess(ResponseBody response) throws IOException, JSONException {

                                String body = response.string();

                                Log.e("BODY printWinningTicket", "" + body);

                                JSONObject object = new JSONObject(body);

                                boolean error = true;
                                boolean winning_statut = true;

                                error = object.getBoolean(getString(R.string.error));
                                String status = object.getString(getString(R.string.status));
                                winning_statut = object.getBoolean(getString(R.string.winning_statut));

                                if (!error) {
                                    if (status.equals(getString(R.string.success))) {
                                        if (winning_statut) {

                                            JSONObject objectwinning_header = object.
                                                    getJSONObject(getString(R.string.winning_header));
                                            JSONArray jsonArrayWinningBody = object.
                                                    getJSONArray(getString(R.string.winning_body));
                                            String logoName = objectwinning_header.getString(getString(R.string.logo));
                                            String titre = objectwinning_header.getString(getString(R.string.titre));
                                            String soustitre = objectwinning_header.getString(getString(R.string.soustitre));
                                            String entreprise = objectwinning_header.getString(getString(R.string.entreprise));
                                            String date_heure_impression = objectwinning_header.
                                                    getString(getString(R.string.date_heure_impression));
                                            String agent = objectwinning_header.getString(getString(R.string.agent_name));

                                            PrintHeader printHeader = new PrintHeader();

                                            //set print header
                                            printHeader.setWinningTicket(titre + "\n" + soustitre);
                                            printHeader.setMerchantName(entreprise);

                                            printHeader.setDates(getString(R.string.date_heure_impression_)
                                                    + "\n" + date_heure_impression);

                                            String logo = getLogo();

                                            if (!logo.equals(logoName)) {
                                                saveLogo(logoName);
                                                new LoadImage().execute(BASE_URL + "/uploads/" + logoName);
                                                String encodingImg = getEncodingImg();
                                                bitmap = decodeImg(encodingImg);
                                                Log.e("urlLogo 1", logo);
                                            } else {
                                                String encodingImg = getEncodingImg();
                                                bitmap = decodeImg(encodingImg);
                                                Log.e("urlLogo 2", logo);
                                            }

                                            SmartPrinter.with(getApplicationContext(), 58).connect(smartPrinter -> {

                                                // header
                                                smartPrinter.printImage(bitmap, 140);

                                                smartPrinter.printTextlnBold(printHeader.getMerchantName().toUpperCase(), SmartPrinter.CENTER);

                                                smartPrinter.printTextlnBold(getString(R.string.agents)
                                                        .toUpperCase() + ": " + agent.toUpperCase(), SmartPrinter.CENTER);

                                                String head = SmartPrinter.centerText(printHeader.getWinningTicket()) + "\n" +
                                                        SmartPrinter.centerText(printHeader.getDates()) + "\n" +
                                                        smartPrinter.line();

                                                String bodys = smartPrinter.twoCol(getString(R.string.ticket_num_),
                                                        getString(R.string.prize_wins_));

                                                smartPrinter.printTextlnNormal(head);
                                                smartPrinter.printTextlnNormal(bodys);

                                                for (int i = 0; i < jsonArrayWinningBody.length(); i++) {

                                                    JSONObject objectWinningBody = jsonArrayWinningBody.getJSONObject(i);

                                                    String num_tickets = objectWinningBody.getString(getString(R.string.num_tickets));
                                                    String prix_gagnants = objectWinningBody.getString(getString(R.string.prix_gagnants));

                                                    String numberTicketAmount = smartPrinter.twoCol(num_tickets, prix_gagnants);

                                                    smartPrinter.printTextlnNormal(numberTicketAmount);

                                                    Log.e("winning_body", num_tickets + " | " + prix_gagnants);

                                                }

                                                smartPrinter.addNewLine(2);

                                                smartPrinter.close();
                                                startActivity(getIntent());
                                            }, this::showToast);

                                        } else {
                                            Log.e("winning_body", "Pas de ticket gagnant");
                                        }
                                    }
                                } else {
//                                    progressDialog.dismiss();
                                    alerDialog(getString(R.string.error_server));
                                }
                            }

                            private void showToast(String s) {
                                Toast.makeText(getApplicationContext(), s, Toast.LENGTH_SHORT).show();
                            }

                            @Override
                            public void onResponse() {
                            }

                            @Override
                            public void onError() {
                            }
                        });
    }

    private void printTicket(String username) {
        Log.e("userName", "userName " + username);
        progressDialog.setMessage(getString(R.string.loading_print));
        progressDialog.show();
        ServicesManager.getInstance(getApplicationContext()).
                getServicesManager().GetTickets(username,
                        new Request.RequestListener<ResponseBody>() {
                            @SuppressLint("UnsafeIntentLaunch")
                            @Override
                            public void onSuccess(ResponseBody response) throws IOException, JSONException {

                                String body = response.string();

                                Log.e("BODY", "RESP " + body);

                                JSONObject object = new JSONObject(body);

                                boolean error = true;

                                error = object.getBoolean("error");


                                if (!error) {

                                    JSONObject user = object.optJSONObject("user");
                                    JSONObject ticket = object.optJSONObject("ticket");
                                    JSONArray jsonArrayBodyTicket = object.getJSONArray("body_ticket");

                                    assert user != null;
                                    String logoName = user.getString("logo");
                                    String businessName = user.getString("enterprise");
                                    String agentAddress = user.getString("address");
                                    String businessPhone = user.getString("phone");
                                    String agentName = user.getString("username");

                                    assert ticket != null;
                                    String original = ticket.getString("header");
                                    String serialNumber = ticket.getString("serial_number");
                                    String dateTime = ticket.getString("date_time");
                                    String total = ticket.getString("total");
                                    String footerText_1 = ticket.getString("footer_text_1");
                                    String footerText_2 = ticket.getString("footer_text_2");
                                    String footerText_3 = ticket.getString("footer_text_3");

                                    PrintHeader header = new PrintHeader();

                                    //set print header
                                    header.setMerchantName(businessName);
                                    header.setAgentAddress(agentAddress);
                                    header.setPhoneMerchant(getString(R.string.phone) + " " + businessPhone);
                                    header.setStatus(original);
                                    header.setAgentName(getString(R.string.agent) + " " + agentName);
                                    header.setTicketNo(getString(R.string.ticket_num) + " " + serialNumber);
                                    header.setDates(getString(R.string.date) + " " + dateTime);

                                    String logo = getLogo();

                                    if (!logo.equals(logoName)) {
                                        saveLogo(logoName);
                                        new LoadImage().execute(BASE_URL + "/uploads/" + logoName);
                                        String encodingImg = getEncodingImg();
                                        bitmap = decodeImg(encodingImg);
                                        Log.e("urlLogo 1", logo+"\n"+ BASE_URL +"/uploads/" + logoName);
                                    } else {
                                        String encodingImg = getEncodingImg();
                                        bitmap = decodeImg(encodingImg);
                                        Log.e("urlLogo 1", logo+"\n"+ BASE_URL +"/uploads/" + logoName);
                                    }

                                    SmartPrinter.with(getApplicationContext(), 58).connect(smartPrinter -> {

//                                        header
                                        smartPrinter.printImage(bitmap, 140);

                                        smartPrinter.printTextlnBold(header.getMerchantName()
                                                .toUpperCase(), SmartPrinter.CENTER);
                                        smartPrinter.printTextln(header.getAgentAddress(), SmartPrinter.CENTER);
                                        smartPrinter.printTextln(header.getPhoneMerchant(), SmartPrinter.CENTER);
                                        smartPrinter.printTextln(header.getStatus(), SmartPrinter.CENTER);
                                        smartPrinter.printTextlnBold(smartPrinter.line());
                                        smartPrinter.printTextlnBold(header.getAgentName(), SmartPrinter.LEFT);
                                        smartPrinter.printTextlnBold(header.getTicketNo(), SmartPrinter.LEFT);
                                        smartPrinter.printTextlnBold(header.getDates(), SmartPrinter.LEFT);

                                        String printTicket;
//                                         body
                                        for (int i = 0; i < jsonArrayBodyTicket.length(); i++) {

                                            JSONObject objectBodyTicket = jsonArrayBodyTicket.getJSONObject(i);

                                            String lotteryName = objectBodyTicket.getString("lot");

                                            if (!lotteryName.isEmpty()) {
                                                smartPrinter.printTextlnBold(smartPrinter.line());
                                                smartPrinter.printTextlnBold(lotteryName, SmartPrinter.LEFT);
                                                smartPrinter.printTextlnBold(smartPrinter.line());
                                                type = objectBodyTicket.getString("type");
                                                option_lot = objectBodyTicket.getString("option_lot");
                                                numbers = objectBodyTicket.getString("numbers");
                                                amount = objectBodyTicket.getString("amount");
                                            } else {
                                                type = objectBodyTicket.getString("type");
                                                option_lot = objectBodyTicket.getString("option_lot");
                                                numbers = objectBodyTicket.getString("numbers");
                                                amount = objectBodyTicket.getString("amount");
                                            }

                                            if (option_lot.equals("-")) {

                                                printTicket = smartPrinter.threeCol(type, numbers, amount);

                                            } else {

                                                printTicket = smartPrinter.threeCol(type + "-" + option_lot, numbers, amount);

                                            }

                                            smartPrinter.printTextln(printTicket);

                                        }

                                        String totalAmount = smartPrinter.twoCol(getString(R.string.total), total) + "\n";

                                        smartPrinter.printLine();

                                        smartPrinter.printTextlnBold(totalAmount);

                                        PrintFooter printFooter = new PrintFooter();

                                        printFooter.setFooter_text_1(footerText_1);
                                        printFooter.setFooter_text_2(footerText_2);
                                        printFooter.setFooter_text_3(footerText_3);

                                        smartPrinter.printTexCenter(printFooter.getFooter_text_1() +
                                                "\n" + printFooter.getFooter_text_2() +
                                                "\n" + printFooter.getFooter_text_3() +
                                                "\n");

                                        if (!header.getTicketNo().isEmpty() && !header.getDates().isEmpty()) {
                                            String text = header.getTicketNo() + header.getDates();

                                            Map<String, String> data = extractTicketDate(text);

                                            String code = data.get("ticket") +
                                                    Objects.requireNonNull(data.get("date")).
                                                            replaceAll("-", "");

                                            smartPrinter.printQrCode(code);
                                        }
                                        smartPrinter.printTexCenter(smartPrinter.lines() + "\n");

                                        smartPrinter.close();
                                        startActivity(getIntent());
                                    }, this::showToast);

                                } else {
//                                    progressDialog.dismiss();
                                    alerDialog(getString(R.string.error_server_print_ticket));

                                }
                            }

                            private void showToast(String s) {
                                Toast.makeText(getApplicationContext(), s, Toast.LENGTH_SHORT).show();
                            }

                            @Override
                            public void onResponse() {

                            }

                            @Override
                            public void onError() {

                            }
                        });
    }

    private void printByNumberTicket(String ticket_number, String date) {
        progressDialog.setMessage(getString(R.string.loading_print));
        progressDialog.show();
        ServicesManager.getInstance(getApplicationContext()).
                getServicesManager().GetByTicketNumber(ticket_number, date,
                        new Request.RequestListener<ResponseBody>() {
                            @SuppressLint("UnsafeIntentLaunch")
                            @Override
                            public void onSuccess(ResponseBody response) throws IOException, JSONException {

                                String body = response.string();

                                Log.e("BODY", "RESP " + body);

                                JSONObject object = new JSONObject(body);

                                boolean error = true;

                                error = object.getBoolean("error");


                                if (!error) {

                                    JSONObject user = object.optJSONObject("user");
                                    JSONObject ticket = object.optJSONObject("ticket");
                                    JSONArray jsonArrayBodyTicket = object.getJSONArray("body_ticket");

                                    assert user != null;
                                    String logoName = user.getString("logo");
                                    String businessName = user.getString("enterprise");
                                    String agentAddress = user.getString("address");
                                    String businessPhone = user.getString("phone");
                                    String agentName = user.getString("username");

                                    assert ticket != null;
                                    String original = ticket.getString("header");
                                    String serialNumber = ticket.getString("serial_number");
                                    String dateTime = ticket.getString("date_time");
                                    String total = ticket.getString("total");
                                    String footerText_1 = ticket.getString("footer_text_1");
                                    String footerText_2 = ticket.getString("footer_text_2");
                                    String footerText_3 = ticket.getString("footer_text_3");

                                    PrintHeader header = new PrintHeader();

                                    //set print header
                                    header.setMerchantName(businessName);
                                    header.setAgentAddress(agentAddress);
                                    header.setPhoneMerchant(getString(R.string.phone) + " " + businessPhone);
                                    header.setStatus(original);
                                    header.setAgentName(getString(R.string.agent) + " " + agentName);
                                    header.setTicketNo(getString(R.string.ticket_num) + " " + serialNumber);
                                    header.setDates(getString(R.string.date) + " " + dateTime);

                                    String logo = getLogo();

                                    if (!logo.equals(logoName)) {
                                        saveLogo(logoName);
                                        new LoadImage().execute(BASE_URL + "/uploads/" + logoName);
                                        String encodingImg = getEncodingImg();
                                        bitmap = decodeImg(encodingImg);
                                        Log.e("urlLogo 1", logo);
                                    } else {
                                        String encodingImg = getEncodingImg();
                                        bitmap = decodeImg(encodingImg);
                                        Log.e("urlLogo 2", logo);
                                    }

                                    SmartPrinter.with(getApplicationContext(), 58).connect(smartPrinter -> {

//                                        header
                                        smartPrinter.printImage(bitmap, 140);

                                        smartPrinter.printTextlnBold(header.getMerchantName()
                                                .toUpperCase(), SmartPrinter.CENTER);
                                        smartPrinter.printTextln(header.getAgentAddress(), SmartPrinter.CENTER);
                                        smartPrinter.printTextln(header.getPhoneMerchant(), SmartPrinter.CENTER);
                                        smartPrinter.printTextln(header.getStatus(), SmartPrinter.CENTER);
                                        smartPrinter.printTextlnBold(smartPrinter.line());
                                        smartPrinter.printTextlnBold(header.getAgentName(), SmartPrinter.LEFT);
                                        smartPrinter.printTextlnBold(header.getTicketNo(), SmartPrinter.LEFT);
                                        smartPrinter.printTextlnBold(header.getDates(), SmartPrinter.LEFT);

                                        String printTicket;
//                                         body
                                        for (int i = 0; i < jsonArrayBodyTicket.length(); i++) {

                                            JSONObject objectBodyTicket = jsonArrayBodyTicket.getJSONObject(i);

                                            String lotteryName = objectBodyTicket.getString("lot");

                                            if (!lotteryName.isEmpty()) {
                                                smartPrinter.printTextlnBold(smartPrinter.line());
                                                smartPrinter.printTextlnBold(lotteryName, SmartPrinter.LEFT);
                                                smartPrinter.printTextlnBold(smartPrinter.line());
                                                type = objectBodyTicket.getString("type");
                                                option_lot = objectBodyTicket.getString("option_lot");
                                                numbers = objectBodyTicket.getString("numbers");
                                                amount = objectBodyTicket.getString("amount");
                                            } else {
                                                type = objectBodyTicket.getString("type");
                                                option_lot = objectBodyTicket.getString("option_lot");
                                                numbers = objectBodyTicket.getString("numbers");
                                                amount = objectBodyTicket.getString("amount");
                                            }

                                            if (option_lot.equals("-")) {

                                                printTicket = smartPrinter.threeCol(type, numbers, amount);

                                            } else {

                                                printTicket = smartPrinter.threeCol(type + "-" + option_lot, numbers, amount);

                                            }

                                            smartPrinter.printTextln(printTicket);

                                        }

                                        String totalAmount = smartPrinter.twoCol(getString(R.string.total), total) + "\n";

                                        smartPrinter.printLine();

                                        smartPrinter.printTextlnBold(totalAmount);

                                        PrintFooter printFooter = new PrintFooter();

                                        printFooter.setFooter_text_1(footerText_1);
                                        printFooter.setFooter_text_2(footerText_2);
                                        printFooter.setFooter_text_3(footerText_3);

                                        smartPrinter.printTexCenter(printFooter.getFooter_text_1() +
                                                "\n" + printFooter.getFooter_text_2() +
                                                "\n" + printFooter.getFooter_text_3() +
                                                "\n");

                                        if (!header.getTicketNo().isEmpty() && !header.getDates().isEmpty()) {
                                            String text = header.getTicketNo() + header.getDates();

                                            Map<String, String> data = extractTicketDate(text);

                                            String code = data.get("ticket") +
                                                    Objects.requireNonNull(data.get("date")).
                                                            replaceAll("-", "");

                                            smartPrinter.printQrCode(code);
                                        }
                                        smartPrinter.printTexCenter(smartPrinter.lines() + "\n");

                                        smartPrinter.close();
                                        startActivity(getIntent());
                                    }, this::showToast);

                                } else {
//                                    progressDialog.dismiss();
                                    alerDialog(getString(R.string.error_server_print_ticket));

                                }
                            }

                            private void showToast(String s) {
                                Toast.makeText(getApplicationContext(), s, Toast.LENGTH_SHORT).show();
                            }

                            @Override
                            public void onResponse() {

                            }

                            @Override
                            public void onError() {

                            }
                        });
    }

    @Override
    public void onClick(View view) {

        if (view.getId() == R.id.print) {

            scanLauncher.launch(new Intent(this, ScanActivity.class));
        }
    }

    private void scanLaucher() {
        scanLauncher = registerForActivityResult(new
                        ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == RESULT_OK) {
                        assert result.getData() != null;

                        String ticket = result.getData().getStringExtra("ticket");

                        String date = result.getData().getStringExtra("date");

                        sendDataScanToWeb(ticket, date);
                    }
                });
    }

    @Override
    protected void onStart() {
        IntentFilter intentFilter = new IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION);
        registerReceiver(networkChangeListener, intentFilter);
        super.onStart();
    }

    @Override
    protected void onStop() {
        unregisterReceiver(networkChangeListener);
        super.onStop();
    }
}