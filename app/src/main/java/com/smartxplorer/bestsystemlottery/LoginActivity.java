package com.smartxplorer.bestsystemlottery;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.smartxplorer.bestsystemlottery.lotato.LotatoConstants;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

/**
 * Écran de connexion natif pour LOTATO PRO.
 *
 * L'utilisateur choisit son rôle, s'authentifie via l'API existante
 * (server.js : /api/auth/login, /api/auth/superadmin-login, /api/auth/player/login),
 * puis l'app ouvre directement la bonne page web (agent1.html / owner.html /
 * superadmin.html / player.html) avec la session déjà stockée dans le
 * localStorage de cette page — l'utilisateur ne revoit donc pas d'écran de
 * connexion web.
 */
public class LoginActivity extends AppCompatActivity {

    private static final MediaType JSON = MediaType.parse("application/json; charset=utf-8");

    private static final String[] ROLE_LABELS_KEYS = {
            "lotato_role_agent",
            "lotato_role_supervisor",
            "lotato_role_owner",
            "lotato_role_superadmin",
            "lotato_role_player"
    };

    private Spinner spinnerRole;
    private EditText edtIdentifiant;
    private EditText edtMotDePasse;
    private Button btnLogin;
    private ProgressBar progressLogin;
    private TextView txtErrorLogin;

    private final OkHttpClient httpClient = new OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .build();

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        spinnerRole = findViewById(R.id.spinnerRole);
        edtIdentifiant = findViewById(R.id.edtIdentifiant);
        edtMotDePasse = findViewById(R.id.edtMotDePasse);
        btnLogin = findViewById(R.id.btnLogin);
        progressLogin = findViewById(R.id.progressLogin);
        txtErrorLogin = findViewById(R.id.txtErrorLogin);

        String[] roleLabels = new String[]{
                getString(R.string.lotato_role_agent),
                getString(R.string.lotato_role_supervisor),
                getString(R.string.lotato_role_owner),
                getString(R.string.lotato_role_superadmin),
                getString(R.string.lotato_role_player)
        };
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, roleLabels);
        spinnerRole.setAdapter(adapter);

        updateIdentifiantHint();
        spinnerRole.setOnItemSelectedListener(new android.widget.AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(android.widget.AdapterView<?> parent, View view, int position, long id) {
                updateIdentifiantHint();
            }

            @Override
            public void onNothingSelected(android.widget.AdapterView<?> parent) {
            }
        });

        btnLogin.setOnClickListener(v -> attemptLogin());
    }

    private void updateIdentifiantHint() {
        int position = spinnerRole.getSelectedItemPosition();
        // Le rôle "Joueur" (index 4) se connecte par numéro de téléphone.
        if (position == 4) {
            edtIdentifiant.setHint(getString(R.string.lotato_hint_identifiant) + " (téléphone)");
        } else {
            edtIdentifiant.setHint(R.string.lotato_hint_identifiant);
        }
    }

    private void attemptLogin() {
        String identifiant = edtIdentifiant.getText().toString().trim();
        String motDePasse = edtMotDePasse.getText().toString();

        if (TextUtils.isEmpty(identifiant) || TextUtils.isEmpty(motDePasse)) {
            showError(getString(R.string.lotato_error_fields));
            return;
        }

        int position = spinnerRole.getSelectedItemPosition();
        setLoading(true);

        try {
            switch (position) {
                case 0: // Agent
                    doLogin(identifiant, motDePasse, LotatoConstants.ROLE_AGENT);
                    break;
                case 1: // Superviseur
                    doLogin(identifiant, motDePasse, LotatoConstants.ROLE_SUPERVISOR);
                    break;
                case 2: // Propriétaire
                    doLogin(identifiant, motDePasse, LotatoConstants.ROLE_OWNER);
                    break;
                case 3: // Super admin
                    doSuperadminLogin(identifiant, motDePasse);
                    break;
                case 4: // Joueur
                    doPlayerLogin(identifiant, motDePasse);
                    break;
                default:
                    setLoading(false);
                    showError(getString(R.string.lotato_error_generic));
            }
        } catch (JSONException e) {
            setLoading(false);
            showError(getString(R.string.lotato_error_generic));
        }
    }

    // ---- Agent / Superviseur / Propriétaire : POST /api/auth/login ----
    private void doLogin(String username, String password, String role) throws JSONException {
        JSONObject body = new JSONObject();
        body.put("username", username);
        body.put("password", password);
        body.put("role", role);

        postJson(LotatoConstants.ENDPOINT_LOGIN, body, new SimpleJsonCallback() {
            @Override
            void onSuccess(JSONObject json) {
                String token = json.optString("token", null);
                String returnedRole = json.optString("role", role);
                String name = json.optString("name", "");
                String ownerId = json.has("ownerId") && !json.isNull("ownerId") ? String.valueOf(json.opt("ownerId")) : "";
                double commission = json.optDouble("commissionPercentage", 0);

                String userIdForRole;
                if ("agent".equals(returnedRole)) {
                    userIdForRole = json.has("agentId") ? String.valueOf(json.opt("agentId")) : "";
                } else if ("supervisor".equals(returnedRole)) {
                    userIdForRole = json.has("supervisorId") ? String.valueOf(json.opt("supervisorId")) : "";
                } else {
                    userIdForRole = ownerId; // owner : son propre id = ownerId
                }

                if ("owner".equals(returnedRole)) {
                    String js = "localStorage.setItem('auth_token'," + jsQuote(token) + ");"
                            + "localStorage.setItem('user_role'," + jsQuote(returnedRole) + ");"
                            + "localStorage.setItem('user_name'," + jsQuote(name) + ");"
                            + "localStorage.setItem('owner_id'," + jsQuote(ownerId) + ");";
                    openWeb(LotatoConstants.PAGE_OWNER, js);
                } else {
                    // agent ou supervisor
                    String js = "localStorage.setItem('auth_token'," + jsQuote(token) + ");"
                            + "localStorage.setItem('user_role'," + jsQuote(returnedRole) + ");"
                            + "localStorage.setItem('agent_id'," + jsQuote(userIdForRole) + ");"
                            + "localStorage.setItem('agent_name'," + jsQuote(name) + ");"
                            + "localStorage.setItem('owner_id'," + jsQuote(ownerId) + ");"
                            + "localStorage.setItem('agent_commission'," + jsQuote(String.valueOf(commission)) + ");";
                    openWeb(LotatoConstants.PAGE_AGENT, js);
                }
            }
        });
    }

    // ---- Super admin : POST /api/auth/superadmin-login ----
    private void doSuperadminLogin(String username, String password) throws JSONException {
        JSONObject body = new JSONObject();
        body.put("username", username);
        body.put("password", password);

        postJson(LotatoConstants.ENDPOINT_SUPERADMIN_LOGIN, body, new SimpleJsonCallback() {
            @Override
            void onSuccess(JSONObject json) {
                String token = json.optString("token", null);
                String name = json.optString("name", "");
                String js = "localStorage.setItem('superadmin_token'," + jsQuote(token) + ");"
                        + "localStorage.setItem('superadmin_name'," + jsQuote(name) + ");";
                openWeb(LotatoConstants.PAGE_SUPERADMIN, js);
            }
        });
    }

    // ---- Joueur : POST /api/auth/player/login ----
    private void doPlayerLogin(String phone, String password) throws JSONException {
        JSONObject body = new JSONObject();
        body.put("phone", phone);
        body.put("password", password);

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
        });
    }

    private void openWeb(String url, String injectJs) {
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
                runOnUiThread(() -> {
                    setLoading(false);
                    showError(getString(R.string.lotato_error_generic));
                });
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                String bodyString = response.body() != null ? response.body().string() : "";
                runOnUiThread(() -> {
                    setLoading(false);
                    try {
                        JSONObject json = new JSONObject(bodyString);
                        if (response.isSuccessful() && json.optBoolean("success", false)) {
                            callback.onSuccess(json);
                        } else {
                            String error = json.optString("error", getString(R.string.lotato_error_generic));
                            showError(error);
                        }
                    } catch (JSONException e) {
                        showError(getString(R.string.lotato_error_generic));
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
    }
}
