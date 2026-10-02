package mx.gob.tecoman.vialidad.secure;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.view.Window;
import android.webkit.CookieManager;
import android.webkit.GeolocationPermissions;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceRequest;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Toast;

import java.net.URI;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

public class MainActivity extends Activity {
    private static final String APP_URL = "https://script.google.com/macros/s/AKfycbw6kPg77-m8S2tSRsibwIJdPJ2vnZ5PWH7JunsiPScfwmBwzPBJ4mGCw0X0skrNPL4j/exec";
    private static final String DASHBOARD_URL = "file:///android_asset/dashboard.html";

    private static final Set<String> ALLOWED_HOSTS = new HashSet<>(Arrays.asList(
            "script.google.com",
            "script.googleusercontent.com",
            "accounts.google.com",
            "google.com",
            "www.google.com"
    ));

    private static final Set<String> ALLOWED_ROUTES = new HashSet<>(Arrays.asList(
            "infraccion",
            "amonestacion",
            "reporte",
            "busqueda",
            "mapa_calor",
            "hechos_transito",
            "mapa_hechos",
            "estadisticas",
            "borrar_boleta",
            "folios_agente",
            "cat_lugares"
    ));

    private WebView webView;
    private boolean switchingToDashboard = false;

    @SuppressLint("SetJavaScriptEnabled")
    @Override public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Se permiten capturas de pantalla y grabación de pantalla.
        Window w = getWindow();
        w.setStatusBarColor(Color.rgb(7, 24, 39));
        w.setNavigationBarColor(Color.rgb(7, 24, 39));

        webView = new WebView(this);
        setContentView(webView);

        WebSettings s = webView.getSettings();
        s.setJavaScriptEnabled(true);
        s.setDomStorageEnabled(true);
        s.setDatabaseEnabled(true);
        s.setGeolocationEnabled(true);
        s.setAllowFileAccess(true); // Solo para cargar el dashboard empacado dentro de la APK.
        s.setAllowContentAccess(false);
        s.setMixedContentMode(WebSettings.MIXED_CONTENT_NEVER_ALLOW);
        s.setSavePassword(false);
        s.setSupportZoom(false);
        s.setUserAgentString(s.getUserAgentString() + " VialidadTecoman/1.0.2");
        WebView.setWebContentsDebuggingEnabled(false);

        CookieManager.getInstance().setAcceptCookie(true);
        CookieManager.getInstance().setAcceptThirdPartyCookies(webView, true);

        webView.setWebViewClient(new WebViewClient() {
            @Override public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
                return handleNavigation(request.getUrl().toString());
            }

            @Override public void onPageFinished(WebView view, String url) {
                super.onPageFinished(view, url);

                if (url == null || !url.startsWith(APP_URL)) return;

                Uri uri = Uri.parse(url);
                String page = uri.getQueryParameter("page");

                // Cuando el login redirige al menú remoto, sustituimos ese menú
                // por el nuevo dashboard móvil. La sesión ya quedó validada en servidor.
                if ("menu".equalsIgnoreCase(page) && !switchingToDashboard) {
                    view.evaluateJavascript(
                            "(function(){return document.body ? document.body.innerText : '';})()",
                            value -> {
                                String body = value == null ? "" : value;
                                boolean looksLikeLogin = body.contains("Inicia sesi")
                                        || body.contains("PIN de 4")
                                        || body.contains("Captura correo");
                                if (!looksLikeLogin) showDashboard();
                            }
                    );
                }
            }
        });

        webView.setWebChromeClient(new WebChromeClient() {
            @Override public void onGeolocationPermissionsShowPrompt(
                    String origin,
                    GeolocationPermissions.Callback callback
            ) {
                if (checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION)
                        == PackageManager.PERMISSION_GRANTED) {
                    callback.invoke(origin, true, false);
                } else {
                    requestPermissions(
                            new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                            44
                    );
                    callback.invoke(origin, false, false);
                }
            }
        });

        // Se entra primero por el login/menú actual para conservar autenticación real.
        webView.loadUrl(APP_URL + "?page=menu&app=android");
    }

    private boolean handleNavigation(String raw) {
        if (raw == null) return true;

        if (raw.startsWith("vialidad://dashboard")) {
            showDashboard();
            return true;
        }

        if (raw.startsWith("vialidad://open")) {
            Uri uri = Uri.parse(raw);
            String page = uri.getQueryParameter("page");
            if (page != null && ALLOWED_ROUTES.contains(page)) {
                switchingToDashboard = false;
                webView.loadUrl(APP_URL + "?page=" + Uri.encode(page) + "&app=android");
            } else {
                Toast.makeText(this, "Módulo no permitido.", Toast.LENGTH_SHORT).show();
            }
            return true;
        }

        if (raw.startsWith(DASHBOARD_URL)) return false;

        if (!isAllowed(raw)) {
            Toast.makeText(this, "Enlace bloqueado por seguridad.", Toast.LENGTH_SHORT).show();
            return true;
        }

        return false;
    }

    private void showDashboard() {
        switchingToDashboard = true;
        webView.loadUrl(DASHBOARD_URL);
        webView.postDelayed(() -> switchingToDashboard = false, 350);
    }

    private boolean isAllowed(String raw) {
        try {
            URI u = new URI(raw);
            if (!"https".equalsIgnoreCase(u.getScheme())) return false;
            String h = u.getHost();
            if (h == null) return false;
            h = h.toLowerCase();
            if (ALLOWED_HOSTS.contains(h)) return true;
            return h.endsWith(".googleusercontent.com") || h.endsWith(".gstatic.com");
        } catch (Exception e) {
            return false;
        }
    }

    @Override public void onBackPressed() {
        if (webView != null && !DASHBOARD_URL.equals(webView.getUrl()) && webView.canGoBack()) {
            webView.goBack();
        } else {
            super.onBackPressed();
        }
    }
}
