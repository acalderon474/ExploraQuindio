package com.angelrincon.exploraquindio.fragments;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.webkit.WebBackForwardList;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceError;
import android.webkit.WebResourceRequest;
import android.webkit.WebResourceResponse;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import com.angelrincon.exploraquindio.R;

import java.util.Locale;

public class WebFragment extends Fragment {

    private EditText etUrl;
    private Button btnCargarWeb;

    private Button btnAtrasWeb;
    private TextView tvUrlActual;
    private TextView tvPlaceholderTitulo;
    private TextView tvPlaceholderMensaje;
    private ProgressBar progressWeb;
    private LinearLayout layoutPlaceholderWeb;
    private WebView webView;

    /*
     * Permite distinguir un final de carga correcto de un final de carga
     * precedido por un error en el frame principal.
     */
    private boolean hayError = false;

    private OnBackPressedCallback backCallback;

    public WebFragment() {
        super(R.layout.fragment_web);
    }

    @Override
    public void onViewCreated(
            @NonNull View view,
            @Nullable Bundle savedInstanceState
    ) {
        super.onViewCreated(
                view,
                savedInstanceState
        );

        vincularVistas(view);
        configurarWebView();
        configurarEventos();
        configurarBotonAtrasDelSistema();

        /*
         * restoreState() está pensado principalmente para recreaciones del
         * Fragment, por ejemplo un cambio de orientación.
         */
        if (savedInstanceState != null) {

            WebBackForwardList estadoRestaurado =
                    webView.restoreState(
                            savedInstanceState
                    );

            if (estadoRestaurado != null
                    && webView.getUrl() != null) {

                String urlRestaurada =
                        webView.getUrl();

                etUrl.setText(
                        urlRestaurada
                );

                layoutPlaceholderWeb.setVisibility(
                        View.GONE
                );

                mostrarUrlActual(
                        urlRestaurada
                );

                actualizarBotonAtras();
            }
        }
    }

    private void vincularVistas(
            @NonNull View view
    ) {

        etUrl =
                view.findViewById(
                        R.id.etUrl
                );

        btnCargarWeb =
                view.findViewById(
                        R.id.btnCargarWeb
                );

        btnAtrasWeb =
                view.findViewById(
                        R.id.btnAtrasWeb
                );

        tvUrlActual =
                view.findViewById(
                        R.id.tvUrlActual
                );

        progressWeb =
                view.findViewById(
                        R.id.progressWeb
                );

        webView =
                view.findViewById(
                        R.id.webView
                );

        layoutPlaceholderWeb =
                view.findViewById(
                        R.id.layoutPlaceholderWeb
                );

        tvPlaceholderTitulo =
                view.findViewById(
                        R.id.tvPlaceholderTitulo
                );

        tvPlaceholderMensaje =
                view.findViewById(
                        R.id.tvPlaceholderMensaje
                );
    }

    @SuppressLint("SetJavaScriptEnabled")
    private void configurarWebView() {

        WebSettings settings =
                webView.getSettings();

        /*
         * Compatibilidad con sitios web modernos.
         *
         * No se agrega ninguna JavaScriptInterface, por lo que no se expone
         * código Java nativo a las páginas cargadas.
         */
        settings.setJavaScriptEnabled(true);
        settings.setDomStorageEnabled(true);

        settings.setSupportZoom(true);
        settings.setBuiltInZoomControls(true);
        settings.setDisplayZoomControls(false);

        settings.setLoadWithOverviewMode(true);
        settings.setUseWideViewPort(true);

        /*
         * Restricciones de seguridad.
         */
        settings.setAllowFileAccess(false);
        settings.setAllowContentAccess(false);

        settings.setMixedContentMode(
                WebSettings.MIXED_CONTENT_NEVER_ALLOW
        );

        settings.setJavaScriptCanOpenWindowsAutomatically(
                false
        );

        settings.setSupportMultipleWindows(
                false
        );

        /*
         * Safe Browsing está disponible desde Android 8.0 / API 26.
         * El proyecto tiene minSdk 24, por eso se aplica condicionalmente.
         */
        if (Build.VERSION.SDK_INT
                >= Build.VERSION_CODES.O) {

            settings.setSafeBrowsingEnabled(
                    true
            );
        }

        webView.setWebViewClient(
                new WebViewClient() {

                    /*
                     * Para minSdk 24 se utiliza el overload moderno basado
                     * en WebResourceRequest.
                     *
                     * HTTP/HTTPS continúan dentro del WebView.
                     * Los demás esquemas se bloquean.
                     */
                    @Override
                    public boolean shouldOverrideUrlLoading(
                            WebView view,
                            WebResourceRequest request
                    ) {

                        Uri destino =
                                request.getUrl();

                        if (esUrlHttpValida(destino)) {

                            return false;
                        }

                        mostrarEsquemaNoPermitido();

                        return true;
                    }

                    @Override
                    public void onPageStarted(
                            WebView view,
                            String url,
                            Bitmap favicon
                    ) {

                        hayError = false;

                        progressWeb.setVisibility(
                                View.VISIBLE
                        );

                        progressWeb.setProgress(
                                0
                        );

                        /*
                         * Durante una navegación válida se oculta el placeholder.
                         */
                        layoutPlaceholderWeb.setVisibility(
                                View.GONE
                        );

                        /*
                         * El campo también refleja las navegaciones internas
                         * del sitio, no únicamente lo que escribió el usuario.
                         */
                        etUrl.setText(
                                url
                        );

                        tvUrlActual.setTextColor(
                                ContextCompat.getColor(
                                        requireContext(),
                                        R.color.explora_texto_secundario
                                )
                        );

                        tvUrlActual.setText(
                                getString(
                                        R.string.web_estado_cargando,
                                        url
                                )
                        );
                    }

                    @Override
                    public void onPageFinished(
                            WebView view,
                            String url
                    ) {

                        progressWeb.setVisibility(
                                View.INVISIBLE
                        );

                        if (!hayError) {

                            layoutPlaceholderWeb.setVisibility(
                                    View.GONE
                            );

                            etUrl.setText(
                                    url
                            );

                            mostrarUrlActual(
                                    url
                            );
                        }

                        actualizarBotonAtras();
                    }

                    @Override
                    public void onReceivedError(
                            WebView view,
                            WebResourceRequest request,
                            WebResourceError error
                    ) {

                        /*
                         * Solo se muestra el error si falla la página principal.
                         * Una imagen secundaria que no cargue no debe sustituir
                         * toda la página por el placeholder.
                         */
                        if (request.isForMainFrame()) {

                            hayError = true;

                            mostrarErrorCarga(
                                    request
                                            .getUrl()
                                            .toString()
                            );
                        }
                    }

                    @Override
                    public void onReceivedHttpError(
                            WebView view,
                            WebResourceRequest request,
                            WebResourceResponse errorResponse
                    ) {

                        if (request.isForMainFrame()
                                && errorResponse.getStatusCode()
                                >= 400) {

                            hayError = true;

                            mostrarErrorCarga(
                                    request
                                            .getUrl()
                                            .toString()
                            );
                        }
                    }

                    /*
                     * No se sobrescribe onReceivedSslError().
                     *
                     * Esto es intencional: Android conserva su comportamiento
                     * seguro por defecto y no se aceptan certificados inválidos.
                     */
                }
        );

        webView.setWebChromeClient(
                new WebChromeClient() {

                    @Override
                    public void onProgressChanged(
                            WebView view,
                            int newProgress
                    ) {

                        progressWeb.setProgress(
                                newProgress
                        );
                    }
                }
        );
    }

    private void configurarEventos() {

        btnCargarWeb.setOnClickListener(
                v -> cargarUrlIngresada()
        );

        btnAtrasWeb.setOnClickListener(v -> {

            if (webView != null && webView.canGoBack()) {
                webView.goBack();
            }
        });

        etUrl.setOnEditorActionListener(
                (v, actionId, event) -> {

                    boolean enter =
                            event != null
                                    && event.getAction()
                                    == KeyEvent.ACTION_DOWN
                                    && event.getKeyCode()
                                    == KeyEvent.KEYCODE_ENTER;

                    if (actionId
                            == EditorInfo.IME_ACTION_GO
                            || enter) {

                        cargarUrlIngresada();

                        return true;
                    }

                    return false;
                }
        );
    }

    private void cargarUrlIngresada() {

        String entrada =
                etUrl
                        .getText()
                        .toString()
                        .trim();

        if (entrada.isEmpty()) {

            etUrl.setError(
                    getString(
                            R.string.web_error_url_vacia
                    )
            );

            mostrarPlaceholder(
                    R.string.web_placeholder_vacio_titulo,
                    R.string.web_placeholder_vacio_mensaje
            );

            return;
        }

        Uri uri =
                normalizarUrl(
                        entrada
                );

        if (uri == null) {

            rechazarUrl();

            return;
        }

        etUrl.setError(
                null
        );

        ocultarTeclado();

        String urlNormalizada =
                uri.toString();

        etUrl.setText(
                urlNormalizada
        );

        webView.loadUrl(
                urlNormalizada
        );
    }

    /*
     * Normalización:
     *
     *  www.quindio.gov.co
     *      ↓
     *  https://www.quindio.gov.co
     *
     * Si ya existe http:// o https:// se respeta.
     * Si se detecta otro esquema explícito se rechaza.
     */
    @Nullable
    private Uri normalizarUrl(
            @NonNull String entrada
    ) {

        String url =
                entrada.trim();

        if (TextUtils.isEmpty(url)
                || contieneEspacios(url)) {

            return null;
        }

        String minusculas =
                url.toLowerCase(
                        Locale.ROOT
                );

        boolean tieneHttp =
                minusculas.startsWith(
                        "http://"
                );

        boolean tieneHttps =
                minusculas.startsWith(
                        "https://"
                );

        if (!tieneHttp
                && !tieneHttps) {

            if (tieneEsquemaNoHttp(
                    url
            )) {

                return null;
            }

            url =
                    "https://" + url;
        }

        try {

            Uri uri =
                    Uri.parse(
                            url
                    );

            if (!esUrlHttpValida(
                    uri
            )) {

                return null;
            }

            return uri;

        } catch (Exception e) {

            return null;
        }
    }

    /*
     * Detecta esquemas explícitos distintos de HTTP/HTTPS.
     *
     * Ejemplos rechazados:
     * javascript:alert(1)
     * file:///...
     * intent://...
     * tel:...
     * mailto:...
     * spotify:...
     *
     * Se permite el caso habitual dominio:puerto, por ejemplo:
     * localhost:8080
     * ejemplo.com:8080
     */
    private boolean tieneEsquemaNoHttp(
            @NonNull String valor
    ) {

        int indiceDosPuntos =
                valor.indexOf(':');

        if (indiceDosPuntos <= 0) {

            return false;
        }

        String prefijo =
                valor.substring(
                        0,
                        indiceDosPuntos
                );

        if (!prefijo.matches(
                "[A-Za-z][A-Za-z0-9+.-]*"
        )) {

            return false;
        }

        String resto =
                valor.substring(
                        indiceDosPuntos + 1
                );

        /*
         * Si parece dominio/localhost + puerto se interpreta como una
         * dirección web sin esquema.
         */
        boolean parecePuerto =
                resto.matches(
                        "\\d+(?:/.*)?"
                )
                        && (
                        prefijo.contains(".")
                                || "localhost"
                                .equalsIgnoreCase(prefijo)
                );

        return !parecePuerto;
    }

    private boolean esUrlHttpValida(
            @Nullable Uri uri
    ) {

        if (uri == null) {

            return false;
        }

        String esquema =
                uri.getScheme();

        String host =
                uri.getHost();

        if (TextUtils.isEmpty(esquema)
                || TextUtils.isEmpty(host)) {

            return false;
        }

        boolean protocoloValido =
                "https".equalsIgnoreCase(
                        esquema
                )
                        || "http".equalsIgnoreCase(
                        esquema
                );

        return protocoloValido
                && !contieneEspacios(host);
    }

    private boolean contieneEspacios(
            @NonNull String valor
    ) {

        for (int i = 0;
             i < valor.length();
             i++) {

            if (Character.isWhitespace(
                    valor.charAt(i)
            )) {

                return true;
            }
        }

        return false;
    }

    private void rechazarUrl() {

        etUrl.setError(
                getString(
                        R.string.web_error_url_invalida
                )
        );

        mostrarPlaceholder(
                R.string.web_placeholder_url_invalida_titulo,
                R.string.web_placeholder_url_invalida_mensaje
        );
    }

    private void mostrarEsquemaNoPermitido() {

        if (!isAdded()) {

            return;
        }

        Toast.makeText(
                requireContext(),
                R.string.web_esquema_no_permitido,
                Toast.LENGTH_SHORT
        ).show();
    }

    private void mostrarErrorCarga(
            @NonNull String url
    ) {

        progressWeb.setVisibility(
                View.INVISIBLE
        );

        mostrarPlaceholder(
                R.string.web_error_conexion_titulo,
                R.string.web_error_conexion_mensaje
        );

        tvUrlActual.setTextColor(
                ContextCompat.getColor(
                        requireContext(),
                        R.color.explora_error
                )
        );

        tvUrlActual.setText(
                getString(
                        R.string.web_estado_error,
                        url
                )
        );

        actualizarBotonAtras();
    }

    private void mostrarUrlActual(
            @NonNull String url
    ) {

        tvUrlActual.setTextColor(
                ContextCompat.getColor(
                        requireContext(),
                        R.color.explora_texto_secundario
                )
        );

        tvUrlActual.setText(
                url
        );
    }

    private void mostrarPlaceholder(
            @StringRes int titulo,
            @StringRes int mensaje
    ) {

        tvPlaceholderTitulo.setText(
                titulo
        );

        tvPlaceholderMensaje.setText(
                mensaje
        );

        layoutPlaceholderWeb.setVisibility(
                View.VISIBLE
        );
    }

    private void ocultarTeclado() {

        etUrl.clearFocus();

        InputMethodManager imm =
                (InputMethodManager)
                        requireContext()
                                .getSystemService(
                                        Context.INPUT_METHOD_SERVICE
                                );

        if (imm != null) {

            imm.hideSoftInputFromWindow(
                    etUrl.getWindowToken(),
                    0
            );
        }
    }

    private void configurarBotonAtrasDelSistema() {

        backCallback =
                new OnBackPressedCallback(
                        false
                ) {

                    @Override
                    public void handleOnBackPressed() {

                        if (webView != null
                                && webView.canGoBack()) {

                            webView.goBack();

                            /*
                             * onPageFinished() volverá a evaluar el historial.
                             */
                        }
                    }
                };

        requireActivity()
                .getOnBackPressedDispatcher()
                .addCallback(
                        getViewLifecycleOwner(),
                        backCallback
                );
    }

    private void actualizarBotonAtras() {

        boolean puedeVolver =
                webView != null
                        && webView.canGoBack();

        if (backCallback != null) {
            backCallback.setEnabled(puedeVolver);
        }

        if (btnAtrasWeb != null) {
            btnAtrasWeb.setEnabled(puedeVolver);
        }
    }

    @Override
    public void onSaveInstanceState(
            @NonNull Bundle outState
    ) {

        if (webView != null) {

            webView.saveState(
                    outState
            );
        }

        super.onSaveInstanceState(
                outState
        );
    }

    @Override
    public void onResume() {

        super.onResume();

        if (webView != null) {

            webView.onResume();
        }
    }

    @Override
    public void onPause() {

        if (webView != null) {

            webView.onPause();
        }

        super.onPause();
    }

    @Override
    public void onDestroyView() {

        if (webView != null) {

            webView.stopLoading();

            ViewGroup parent =
                    (ViewGroup)
                            webView.getParent();

            if (parent != null) {

                parent.removeView(
                        webView
                );
            }

            webView.removeAllViews();
            webView.destroy();
            webView = null;
            btnAtrasWeb = null;
        }

        etUrl = null;
        btnCargarWeb = null;
        tvUrlActual = null;
        tvPlaceholderTitulo = null;
        tvPlaceholderMensaje = null;
        progressWeb = null;
        layoutPlaceholderWeb = null;

        backCallback = null;
        hayError = false;

        super.onDestroyView();
    }
}
