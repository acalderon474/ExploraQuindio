package com.angelrincon.exploraquindio.fragments;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.os.Bundle;
import android.util.Patterns;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceError;
import android.webkit.WebResourceRequest;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.angelrincon.exploraquindio.R;

/**
 * Sección "Web" (RF-07): el usuario escribe una URL, la carga con un botón
 * y se muestra en un navegador interno (WebView).
 * Según el UML: valida el formato de la URL y muestra un mensaje de error si falla la conexión.
 *
 * Requiere el permiso INTERNET en AndroidManifest.xml (lo agrega quien integra).
 */
public class WebFragment extends Fragment {

    private static final int COLOR_NORMAL = Color.parseColor("#6B6B6B");
    private static final int COLOR_ERROR = Color.parseColor("#C62828");

    private EditText etUrl;
    private Button btnCargarWeb;
    private TextView tvUrlActual, tvPlaceholderTitulo, tvPlaceholderMensaje;
    private ProgressBar progressWeb;
    private LinearLayout layoutPlaceholderWeb;
    private WebView webView;

    private boolean hayError = false;
    private OnBackPressedCallback backCallback;

    public WebFragment() {
        // Constructor vacío requerido
    }

    public static WebFragment newInstance() {
        return new WebFragment();
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_web, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Vincular vistas XML con variables Java
        etUrl = view.findViewById(R.id.etUrl);
        btnCargarWeb = view.findViewById(R.id.btnCargarWeb);
        tvUrlActual = view.findViewById(R.id.tvUrlActual);
        progressWeb = view.findViewById(R.id.progressWeb);
        webView = view.findViewById(R.id.webView);
        layoutPlaceholderWeb = view.findViewById(R.id.layoutPlaceholderWeb);
        tvPlaceholderTitulo = view.findViewById(R.id.tvPlaceholderTitulo);
        tvPlaceholderMensaje = view.findViewById(R.id.tvPlaceholderMensaje);

        configurarWebView();
        configurarEventos();
        configurarBotonAtrasDelSistema();

        if (savedInstanceState != null) {
            webView.restoreState(savedInstanceState);
            if (webView.getUrl() != null) {
                layoutPlaceholderWeb.setVisibility(View.GONE);
                tvUrlActual.setText(webView.getUrl());
            }
        }
    }

    @SuppressLint("SetJavaScriptEnabled")
    private void configurarWebView() {
        WebSettings settings = webView.getSettings();
        settings.setJavaScriptEnabled(true);   // muchos sitios turísticos lo necesitan
        settings.setDomStorageEnabled(true);
        settings.setBuiltInZoomControls(true);
        settings.setDisplayZoomControls(false);

        // Mantiene la navegación dentro de la app (no abre el navegador externo)
        webView.setWebViewClient(new WebViewClient() {
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
                return false;
            }

            @Override
            public void onPageStarted(WebView view, String url, Bitmap favicon) {
                hayError = false;
                progressWeb.setVisibility(View.VISIBLE);
                tvUrlActual.setTextColor(COLOR_NORMAL);
                tvUrlActual.setText("Cargando: " + url);
            }

            @Override
            public void onPageFinished(WebView view, String url) {
                progressWeb.setVisibility(View.INVISIBLE);
                if (!hayError) {
                    layoutPlaceholderWeb.setVisibility(View.GONE);
                    tvUrlActual.setText(url);
                }
                actualizarBotonAtras();
            }

            @Override
            public void onReceivedError(WebView view, WebResourceRequest request,
                                        WebResourceError error) {
                // Solo interesa el error de la página principal (no de imágenes/scripts)
                if (request.isForMainFrame()) {
                    hayError = true;
                    mostrarPlaceholder("Error de conexión",
                            "No se pudo cargar la página. Verifica tu conexión a Internet "
                                    + "y que la dirección sea correcta.");
                    tvUrlActual.setTextColor(COLOR_ERROR);
                    tvUrlActual.setText("No se pudo cargar: " + request.getUrl());
                }
            }
        });

        webView.setWebChromeClient(new WebChromeClient() {
            @Override
            public void onProgressChanged(WebView view, int newProgress) {
                progressWeb.setProgress(newProgress);
            }
        });
    }

    private void configurarEventos() {
        btnCargarWeb.setOnClickListener(v -> cargarUrlIngresada());

        etUrl.setOnEditorActionListener((v, actionId, event) -> {
            boolean enter = event != null && event.getAction() == KeyEvent.ACTION_DOWN
                    && event.getKeyCode() == KeyEvent.KEYCODE_ENTER;
            if (actionId == EditorInfo.IME_ACTION_GO || enter) {
                cargarUrlIngresada();
                return true;
            }
            return false;
        });
    }

    /** Valida el texto escrito y, si es correcto, lo carga en el WebView. */
    private void cargarUrlIngresada() {
        String entrada = etUrl.getText().toString().trim();

        if (entrada.isEmpty()) {
            etUrl.setError("Escribe una dirección URL");
            mostrarPlaceholder("Dirección vacía", "Escribe una dirección web y presiona «Cargar página».");
            return;
        }

        // Si el usuario no escribió el protocolo, se agrega https://
        String url = entrada;
        String minus = url.toLowerCase();
        if (!minus.startsWith("http://") && !minus.startsWith("https://")) {
            if (minus.contains("://")) { // ej. file://, javascript: -> no permitidos
                rechazarUrl();
                return;
            }
            url = "https://" + url;
        }

        if (!Patterns.WEB_URL.matcher(url).matches()) {
            rechazarUrl();
            return;
        }

        etUrl.setError(null);
        ocultarTeclado();
        etUrl.setText(url);
        webView.loadUrl(url);
    }

    private void rechazarUrl() {
        etUrl.setError("La dirección no tiene un formato válido");
        mostrarPlaceholder("URL no válida",
                "Escribe una dirección con formato válido, por ejemplo: https://www.quindio.gov.co");
    }

    private void mostrarPlaceholder(String titulo, String mensaje) {
        tvPlaceholderTitulo.setText(titulo);
        tvPlaceholderMensaje.setText(mensaje);
        layoutPlaceholderWeb.setVisibility(View.VISIBLE);
    }

    private void ocultarTeclado() {
        InputMethodManager imm = (InputMethodManager)
                requireContext().getSystemService(Context.INPUT_METHOD_SERVICE);
        if (imm != null) {
            imm.hideSoftInputFromWindow(etUrl.getWindowToken(), 0);
        }
    }

    /** Si el WebView puede retroceder, el botón "atrás" del sistema retrocede en el historial web. */
    private void configurarBotonAtrasDelSistema() {
        backCallback = new OnBackPressedCallback(false) {
            @Override
            public void handleOnBackPressed() {
                if (webView != null && webView.canGoBack()) {
                    webView.goBack();
                }
            }
        };
        requireActivity().getOnBackPressedDispatcher()
                .addCallback(getViewLifecycleOwner(), backCallback);
    }

    private void actualizarBotonAtras() {
        if (backCallback != null && webView != null) {
            backCallback.setEnabled(webView.canGoBack());
        }
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        if (webView != null) webView.saveState(outState);
    }

    @Override
    public void onResume() {
        super.onResume();
        if (webView != null) webView.onResume();
    }

    @Override
    public void onPause() {
        if (webView != null) webView.onPause();
        super.onPause();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (webView != null) {
            webView.stopLoading();
            webView.destroy();
            webView = null;
        }
    }
}
