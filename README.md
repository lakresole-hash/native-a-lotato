# Aigle Lotto — app native LOTATO PRO

## Organisation des dossiers (ce dépôt)

Ce dépôt correspond à **un seul projet Gradle multi-modules**. Il ne faut pas
le mélanger avec le code du site web LOTATO PRO (`player.html`, `server.js`,
etc.) : ce sont deux projets séparés, avec deux dépôts GitHub séparés.

```
Aigle Lotto/                  ← racine du dépôt Git
├── .github/
│   └── workflows/
│       └── android-build.yml ← build automatique de l'APK (GitHub Actions)
├── app/                       ← module principal de l'application
│   ├── src/main/java/com/smartxplorer/bestsystemlottery/
│   │   ├── LoginActivity.java        (connexion LOTATO PRO)
│   │   ├── LotatoWebActivity.java    (WebView + impression + scan)
│   │   ├── MainActivity.java         (ancien flux, conservé)
│   │   ├── SplashActivity.java
│   │   ├── ScanActivity.java
│   │   └── lotato/LotatoConstants.java
│   ├── src/main/res/          (layouts, strings, images...)
│   ├── google-services.json   (config Firebase)
│   └── build.gradle.kts
├── print/                      ← module de la librairie d'impression Bluetooth
├── gradlew / gradlew.bat        ← wrapper Gradle (ne pas modifier)
├── settings.gradle.kts          ← déclare les modules (:app, :print)
└── build.gradle.kts
```

Vous n'avez rien à réorganiser : ouvrez simplement ce dossier tel quel comme
racine du dépôt GitHub.

## Builder l'APK depuis GitHub Actions

Le fichier `.github/workflows/android-build.yml` est déjà configuré :

1. Poussez ce dossier sur GitHub (voir commandes ci-dessous).
2. Allez dans l'onglet **Actions** de votre dépôt GitHub.
3. Le workflow "Build Android APK" se lance automatiquement à chaque `push`
   sur `main` (ou manuellement via le bouton **Run workflow**).
4. À la fin du build (2-5 minutes), ouvrez le run terminé → section
   **Artifacts** en bas de page → téléchargez `aigle-lotto-debug` : c'est
   un zip contenant `app-debug.apk`, installable directement sur un
   téléphone Android (paramètres à activer : "Sources inconnues").

### Commandes pour pousser le projet

```bash
cd "Aigle Lotto"
git init
git add .
git commit -m "Projet Aigle Lotto - intégration LOTATO PRO"
git branch -M main
git remote add origin https://github.com/<votre-compte>/aigle-lotto.git
git push -u origin main
```

### Build release signé (optionnel, pour publier sur le Play Store)

Le workflow contient un bloc commenté pour un **build release signé**. Pour
l'activer :
1. Générez un keystore : `keytool -genkey -v -keystore release.keystore -alias aiglelotto -keyalg RSA -keysize 2048 -validity 10000`
2. Encodez-le en base64 : `base64 -w0 release.keystore` (copiez le résultat)
3. Dans GitHub : Settings → Secrets and variables → Actions → ajoutez
   `KEYSTORE_BASE64`, `KEYSTORE_PASSWORD`, `KEY_ALIAS`, `KEY_PASSWORD`.
4. Décommentez le bloc "release" dans `android-build.yml`.
5. Ajoutez la config de signature correspondante dans `app/build.gradle.kts`
   (`signingConfigs { release { ... } }`), qui n'existe pas encore dans ce
   projet — dites-le-moi si vous voulez que je l'ajoute.

## Le site web LOTATO PRO reste séparé

`player.html`, `agent1.html`, `owner.html`, `superadmin.html`, `server.js`
et les fichiers JS du moteur de jeu forment un **projet à part** (déployé
sur `lotato1.onrender.com`). Il mérite son propre dépôt GitHub (ex.
`lotato-pro-web`), indépendant de celui-ci. L'app Android ne fait
qu'appeler son API et afficher ses pages — elle n'a pas besoin de son code
source.
