package mx.gob.tecoman.vialidad.secure;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.os.Bundle;
import android.view.Window;
import android.view.WindowManager;
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
    private static final Set<String> ALLOWED_HOSTS = new HashSet<>(Arrays.asList(
            "script.google.com",
            "script.googleusercontent.com",
            "accounts.google.com",
            "google.com",
            "www.google.com"
    ));
    private WebView webView;

    @SuppressLint("SetJavaScriptEnabled")
    @Override public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Screenshots and screen recording are intentionally allowed.\n        // Do not enable FLAG_SECURE here.
        Window w=getWindow();
        w.setStatusBarColor(Color.rgb(7,24,39));
        w.setNavigationBarColor(Color.rgb(7,24,39));

        webView=new WebView(this);
        setContentView(webView);
        WebSettings s=webView.getSettings();
        s.setJavaScriptEnabled(true);
        s.setDomStorageEnabled(true);
        s.setDatabaseEnabled(true);
        s.setGeolocationEnabled(true);
        s.setAllowFileAccess(false);
        s.setAllowContentAccess(false);
        s.setMixedContentMode(WebSettings.MIXED_CONTENT_NEVER_ALLOW);
        s.setSavePassword(false);
        s.setSupportZoom(false);
        s.setUserAgentString(s.getUserAgentString() + " VialidadTecoman/1.0.1");
        CookieManager.getInstance().setAcceptCookie(true);
        CookieManager.getInstance().setAcceptThirdPartyCookies(webView,true);
        WebView.setWebContentsDebuggingEnabled(false);

        webView.setWebViewClient(new WebViewClient(){
            @Override public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest req) {
                String url=req.getUrl().toString();
                return !isAllowed(url);
            }
        });
        webView.setWebChromeClient(new WebChromeClient(){
            @Override public void onGeolocationPermissionsShowPrompt(String origin, GeolocationPermissions.Callback callback) {
                if (checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION)==PackageManager.PERMISSION_GRANTED) {
                    callback.invoke(origin,true,false);
                } else {
                    requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION},44);
                    callback.invoke(origin,false,false);
                }
            }
        });
        webView.loadUrl(APP_URL + "?app=android&ui=mobile&role=admin_general");
    }

    private boolean isAllowed(String raw) {
        try {
            URI u=new URI(raw);
            if (!"https".equalsIgnoreCase(u.getScheme())) return false;
            String h=u.getHost();
            if (h==null) return false;
            h=h.toLowerCase();
            if (ALLOWED_HOSTS.contains(h)) return true;
            return h.endsWith(".googleusercontent.com") || h.endsWith(".gstatic.com");
        } catch(Exception e) { return false; }
    }

    @Override public void onBackPressed() {
        if (webView!=null && webView.canGoBack()) webView.goBack(); else super.onBackPressed();
    }
}
