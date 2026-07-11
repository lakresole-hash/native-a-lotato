package com.smartxplorer.bestsystemlottery.lotato;

/**
 * Constantes pour l'intégration de l'application web LOTATO PRO
 * dans la coquille Android "Aigle Lotto".
 *
 * L'app web (player.html, agent1.html, owner.html, superadmin.html)
 * reste hébergée en ligne : l'app native se contente d'ouvrir la bonne
 * page dans une WebView après une connexion faite en natif.
 */
public class LotatoConstants {

    // Domaine où est hébergée l'app web LOTATO PRO (front + API sur le même serveur Node/Express).
    public static final String BASE_URL = "https://lotato1.onrender.com";

    // Pages web par rôle
    public static final String PAGE_AGENT = BASE_URL + "/agent1.html";
    public static final String PAGE_OWNER = BASE_URL + "/owner.html";
    public static final String PAGE_SUPERADMIN = BASE_URL + "/superadmin.html";
    public static final String PAGE_PLAYER = BASE_URL + "/player.html";

    // Endpoints d'authentification (voir server.js)
    public static final String ENDPOINT_LOGIN = BASE_URL + "/api/auth/login"; // agent / supervisor / owner
    public static final String ENDPOINT_SUPERADMIN_LOGIN = BASE_URL + "/api/auth/superadmin-login";
    public static final String ENDPOINT_PLAYER_LOGIN = BASE_URL + "/api/auth/player/login";

    // Valeurs de rôle envoyées au serveur (doivent correspondre à la colonne "role" en base)
    public static final String ROLE_AGENT = "agent";
    public static final String ROLE_SUPERVISOR = "supervisor";
    public static final String ROLE_OWNER = "owner";
    // Superadmin et joueur utilisent des routes dédiées, sans champ "role" dans la requête.

    public static final String EXTRA_URL = "extra_lotato_url";
    public static final String EXTRA_INJECT_JS = "extra_lotato_inject_js";
}
