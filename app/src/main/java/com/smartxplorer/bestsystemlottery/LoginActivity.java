package com.smartxplorer.bestsystemlottery;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.smartxplorer.bestsystemlottery.lotato.LotatoConstants;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

/**
 * Écran de connexion natif pour LOTATO.
 *
 * L'utilisateur tape juste son nom d'utilisateur (ou téléphone pour un
 * joueur) et son mot de passe — il ne choisit pas de rôle. L'app essaie
 * les différents types de compte les uns après les autres auprès du
 * serveur (agent, superviseur, propriétaire, super admin, joueur) et
 * ouvre automatiquement la bonne page dès qu'une connexion réussit, avec
 * la session déjà stockée dans le localStorage de cette page.
 */
public class LoginActivity extends AppCompatActivity {

    private static final MediaType JSON = MediaType.parse("application/json; charset=utf-8");

    private EditText edtIdentifiant;
    private EditText edtMotDePasse;
    private Button btnLogin;
    private ProgressBar progressLogin;
    private TextView txtErrorLogin;

    private final OkHttpClient httpClient = new OkHttpClient.Builder()
            .connectTimeout(45, TimeUnit.SECONDS)
            .readTimeout(45, TimeUnit.SECONDS)
            .retryOnConnectionFailure(true)
            .build();

    // File d'essais de connexion (un par type de compte), tentés dans l'ordre
    // jusqu'à ce que l'un d'eux réussisse.
    private List<Runnable> loginAttempts;
    private int attemptIndex;
    private String currentIdentifiant;
    private String currentMotDePasse;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        edtIdentifiant = findViewById(R.id.edtIdentifiant);
        edtMotDePasse = findViewById(R.id.edtMotDePasse);
        btnLogin = findViewById(R.id.btnLogin);
        progressLogin = findViewById(R.id.progressLogin);
        txtErrorLogin = findViewById(R.id.txtErrorLogin);

        btnLogin.setOnClickListener(v -> attemptLogin());
    }

    private void attemptLogin() {
        String identifiant = edtIdentifiant.getText().toString().trim();
        String motDePasse = edtMotDePasse.getText().toString();

        if (TextUtils.isEmpty(identifiant) || TextUtils.isEmpty(motDePasse)) {
            showError(getString(R.string.lotato_error_fields));
            return;
        }

        currentIdentifiant = identifiant;
        currentMotDePasse = motDePasse;
        setLoading(true);

        // Ordre des essais : agent, superviseur, propriétaire (même
        // endpoint, rôle différent), puis super admin, puis joueur
        // (identifiant traité comme un numéro de téléphone).
        loginAttempts = new ArrayList<>();
        loginAttempts.add(() -> doLogin(currentIdentifiant, currentMotDePasse, LotatoConstants.ROLE_AGENT));
        loginAttempts.add(() -> doLogin(currentIdentifiant, currentMotDePasse, LotatoConstants.ROLE_SUPERVISOR));
        loginAttempts.add(() -> doLogin(currentIdentifiant, currentMotDePasse, LotatoConstants.ROLE_OWNER));
        loginAttempts.add(() -> doSuperadminLogin(currentIdentifiant, currentMotDePasse));
        loginAttempts.add(() -> doPlayerLogin(currentIdentifiant, currentMotDePasse));

        attemptIndex = 0;
        runNextAttempt();
    }

    private void runNextAttempt() {
        if (attemptIndex >= loginAttempts.size()) {
            setLoading(false);
            showError(getString(R.string.lotato_error_generic));
            return;
        }
        Runnable attempt = loginAttempts.get(attemptIndex);
        attemptIndex++;
        try {
            attempt.run();
        } catch (Exception e) {
            runNextAttempt();
        }
    }

    // ---- Agent / Superviseur / Propriétaire : POST /api/auth/login ----
    private void doLogin(String username, String password, String role) {
        JSONObject body = new JSONObject();
        try {
            body.put("username", username);
            body.put("password", password);
            body.put("role", role);
        } catch (JSONException e) {
            runNextAttempt();
            return;
        }

        postJson(LotatoConstants.ENDPOINT_LOGIN, body, new SimpleJsonCallback() {
            @Override
            void onSuccess(JSONObject json) {
                String token = json.optString("token", null);
                String returnedRole = json.optString("role", role);
                String name = json.optString("name", "");
                String ownerId = json.has("ownerId") && !json.isNull("ownerId") ? String.valueOf(json.opt("ownerId")) : "";
                double commission = json.optDouble("commissionPercentage", 0);

                if ("owner".equals(returnedRole)) {
                    String js = "localStorage.setItem('auth_token'," + jsQuote(token) + ");"
                            + "localStorage.setItem('user_role'," + jsQuote(returnedRole) + ");"
                            + "localStorage.setItem('user_name'," + jsQuote(name) + ");"
                            + "localStorage.setItem('owner_id'," + jsQuote(ownerId) + ");";
                    openWeb(LotatoConstants.PAGE_OWNER, js);
                } else if ("supervisor".equals(returnedRole)) {
                    String js = "localStorage.setItem('auth_token'," + jsQuote(token) + ");"
                            + "localStorage.setItem('user_role'," + jsQuote(returnedRole) + ");"
                            + "localStorage.setItem('user_name'," + jsQuote(name) + ");";
                    openWeb(LotatoConstants.PAGE_SUPERVISOR, js);
                } else {
                    // agent
                    String agentId = json.has("agentId") ? String.valueOf(json.opt("agentId")) : "";
                    String js = "localStorage.setItem('auth_token'," + jsQuote(token) + ");"
                            + "localStorage.setItem('user_role'," + jsQuote(returnedRole) + ");"
                            + "localStorage.setItem('agent_id'," + jsQuote(agentId) + ");"
                            + "localStorage.setItem('agent_name'," + jsQuote(name) + ");"
                            + "localStorage.setItem('owner_id'," + jsQuote(ownerId) + ");"
                            + "localStorage.setItem('agent_commission'," + jsQuote(String.valueOf(commission)) + ");";
                    openWeb(LotatoConstants.PAGE_AGENT, js);
                }
            }

            @Override
            void onFailure() {
                runNextAttempt();
            }
        });
    }

    // ---- Super admin : POST /api/auth/superadmin-login ----
    private void doSuperadminLogin(String username, String password) {
        JSONObject body = new JSONObject();
        try {
            body.put("username", username);
            body.put("password", password);
        } catch (JSONException e) {
            runNextAttempt();
            return;
        }

        postJson(LotatoConstants.ENDPOINT_SUPERADMIN_LOGIN, body, new SimpleJsonCallback() {
            @Override
            void onSuccess(JSONObject json) {
                String token = json.optString("token", null);
                String name = json.optString("name", "");
                String js = "localStorage.setItem('superadmin_token'," + jsQuote(token) + ");"
                        + "localStorage.setItem('superadmin_name'," + jsQuote(name) + ");";
                openWeb(LotatoConstants.PAGE_SUPERADMIN, js);
            }

            @Override
            void onFailure() {
                runNextAttempt();
            }
        });
    }

    // ---- Joueur : POST /api/auth/player/login (identifiant = téléphone) ----
    private void doPlayerLogin(String phone, String password) {
        JSONObject body = new JSONObject();
        try {
            body.put("phone", phone);
            body.put("password", password);
        } catch (JSONException e) {
            runNextAttempt();
            return;
        }

        postJson(LotatoConstants.ENDPOINT_PLAYER_LOGIN, body, new SimpleJsonCallback() {
            @Override
            void onSuccess(JSONObject json) {
                String token = json.optString("token", null);
                String name = json.optString("name", "");
                String playerId = json.has("playerId") ? String.valueOf(json.opt("playerId")) : "";
                String ownerId = json.has("ownerId") && !json.isNull("ownerId") ? String.valueOf(json.opt("ownerId")) : "";
                String balance = String.valueOf(json.optDouble("balance", 0));

                String js = "localStorage.setItem('player_token'," + jsQuote(token) + ");"
                        + "localStorage.setItem('player_id'," + jsQuote(playerId) + ");"
                        + "localStorage.setItem('player_name'," + jsQuote(name) + ");"
                        + "localStorage.setItem('player_phone'," + jsQuote(phone) + ");"
                        + "localStorage.setItem('player_owner_id'," + jsQuote(ownerId) + ");"
                        + "localStorage.setItem('player_balance'," + jsQuote(balance) + ");";
                openWeb(LotatoConstants.PAGE_PLAYER, js);
            }

            @Override
            void onFailure() {
                runNextAttempt();
            }
        });
    }

    private void openWeb(String url, String injectJs) {
        setLoading(false);
        Intent intent = new Intent(LoginActivity.this, LotatoWebActivity.class);
        intent.putExtra(LotatoConstants.EXTRA_URL, url);
        intent.putExtra(LotatoConstants.EXTRA_INJECT_JS, injectJs);
        startActivity(intent);
        finish();
    }

    private void postJson(String url, JSONObject body, SimpleJsonCallback callback) {
        RequestBody requestBody = RequestBody.create(JSON, body.toString());
        Request request = new Request.Builder()
                .url(url)
                .post(requestBody)
                .build();

        httpClient.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                runOnUiThread(callback::onFailure);
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                String bodyString = response.body() != null ? response.body().string() : "";
                runOnUiThread(() -> {
                    try {
                        JSONObject json = new JSONObject(bodyString);
                        if (response.isSuccessful() && json.optBoolean("success", false)) {
                            callback.onSuccess(json);
                        } else {
                            callback.onFailure();
                        }
                    } catch (JSONException e) {
                        callback.onFailure();
                    }
                });
            }
        });
    }

    private void setLoading(boolean loading) {
        progressLogin.setVisibility(loading ? View.VISIBLE : View.GONE);
        btnLogin.setEnabled(!loading);
        if (loading) {
            txtErrorLogin.setVisibility(View.GONE);
        }
    }

    private void showError(String message) {
        txtErrorLogin.setText(message);
        txtErrorLogin.setVisibility(View.VISIBLE);
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }

    /**
     * Échappe une valeur pour l'insérer en toute sécurité dans du JS généré
     * côté natif (utilisé pour préremplir le localStorage de la page web).
     */
    private static String jsQuote(String value) {
        if (value == null) value = "";
        return JSONObject.quote(value);
    }

    private abstract class SimpleJsonCallback {
        abstract void onSuccess(JSONObject json);
        void onFailure() {
            runNextAttempt();
        }
    }
}
