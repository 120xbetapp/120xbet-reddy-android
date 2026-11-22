# 120xBet Reddy Android
Android WebView project (not TWA), application ID `com.xbet120.reddy`.

Build: GitHub Actions → Build Reddy APK → Run workflow → download `Reddy-debug-apk` artifact.

**Important:** This is a debug-signed APK for private installation and testing. The included Reddy 3 HTML retains its original chat backend and includes the earlier 120xBet agent integration. Login and live backend transactions have NOT been verified. The proposed per-Reddy balance ledger, pairing, and chat notifications require additional secured backend implementation; they are NOT implemented by this APK project. Firebase authentication may reject `file:///android_asset` origins. Production should use a properly configured HTTPS origin or WebViewAssetLoader with corresponding Firebase authorized-domain and backend CORS settings. Do not use for financial transactions until server-side authorization, idempotency and audit logging are validated.


## Notification icon update (1.0.1)
- `@drawable/ic_stat_reddy`: transparent white R-only silhouette in five densities; Android tints it dark on a light status bar and light on a dark bar.
- Notification channel `reddy_alerts` created on Android 8+; Android 13+ notification permission requested.
- Firebase default notification icon/channel metadata prepared. **FCM SDK, device-token registration, server push dispatch and tap routing are NOT implemented in this project.** They require a configured Firebase Android app and backend integration.
- The existing Reddy 3 HTML, authentication and chat code is unchanged.
- GitHub Actions builds a debug APK. This project has not been built or tested on an Android device in this environment.
