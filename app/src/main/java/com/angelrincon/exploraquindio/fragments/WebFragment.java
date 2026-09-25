package com.angelrincon.exploraquindio.fragments;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Bitmap;
import android.net.Uri;
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

/**
 * Fragmento de navegación web interna de Explora Quindío.
 *
 * Permite al usuario ingresar una dirección URL, cargarla en un
 * {@link WebView} integrado y navegar por el historial usando el botón
 * atrás del sistema. Incluye:
 *
 *   Normalización y validación de URLs antes de cargarlas.
 *   Restricciones de seguridad en el WebView (sin acceso a archivos
 *       locales, sin contenido mixto, esquemas no HTTP bloqueados).
 *   Barra de progreso de carga y placeholders informativos para
 *       cada estado posible (inicial, cargando, error, URL inválida).
 *   Persistencia del estado del WebView ante rotaciones de pantalla.
 *   Limpieza completa de referencias en {@code onDestroyView} para
 *       evitar memory leaks.
 *
 *
 * El layout asociado es {@code R.layout.fragment_web}.
 */
public class WebFragment extends Fragment {

    // ── Vistas ──────────────────────────────────────────────────────────────

    /** Campo de texto donde el usuario escribe la URL a cargar. */
    private EditText etUrl;

    /** Botón que dispara la carga de la URL ingresada. */
    private Button btnCargarWeb;

    /** Muestra la URL actualmente cargada o el estado de la carga. */
    private TextView tvUrlActual;

    /** Título del placeholder cuando no hay página cargada o hay un error. */
    private TextView tvPlaceholderTitulo;

    /** Mensaje descriptivo del placeholder. */
    private TextView tvPlaceholderMensaje;

    /** Barra de progreso horizontal que refleja el avance de carga de la página. */
    private ProgressBar progressWeb;

    /** Contenedor del placeholder; se muestra u oculta según el estado del WebView. */
    private LinearLayout layoutPlaceholderWeb;

    /** Vista principal que renderiza el contenido web. */
    private WebView webView;

    // ── Estado interno ───────────────────────────────────────────────────────

    /**
     * Indica si la última carga del WebView terminó con error.
     * Se usa para evitar ocultar el placeholder cuando {@code onPageFinished}
     * se llama tras un error de red o HTTP.
     */
    private boolean hayError = false;

    /**
     * Callback registrado en el dispatcher de back press de la Activity.
     * Se habilita cuando el WebView tiene historial hacia atrás y permite
     * navegar por él antes de salir del fragmento.
     */
    private OnBackPressedCallback backCallback;

    // ── Constructor ──────────────────────────────────────────────────────────

    /**
     * Constructor sin argumentos requerido por el sistema de fragmentos.
     * Asocia el layout {@code R.layout.fragment_web} al fragmento.
     */
    public WebFragment() {
        super(R.layout.fragment_web);
    }

    // ── Ciclo de vida ────────────────────────────────────────────────────────

    /**
     * Se ejecuta justo después de que la vista del fragmento es creada.
     *
     * <p>Realiza el enlace de vistas, configura el WebView y sus eventos,
     * registra el callback del botón atrás del sistema, y restaura el
     * estado del WebView si existe uno guardado (por ejemplo, tras una
     * rotación de pantalla).</p>
     *
     * @param view               Vista raíz inflada desde {@code R.layout.fragment_web}.
     * @param savedInstanceState Estado previo del fragmento, o {@code null}.
     */
    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        vincularVistas(view);
        configurarWebView();
        configurarEventos();
        configurarBotonAtrasDelSistema();

        // Restaura el WebView si hubo una rotación u otro cambio de configuración
        if (savedInstanceState != null) {
            WebBackForwardList estadoRestaurado =
                    webView.restoreState(savedInstanceState);

            if (estadoRestaurado != null && webView.getUrl() != null) {
                layoutPlaceholderWeb.setVisibility(View.GONE);
                mostrarUrlActual(webView.getUrl());
                actualizarBotonAtras();
            }
        }
    }

    // ── Inicialización ───────────────────────────────────────────────────────

    /**
     * Enlaza cada campo de la clase con su vista correspondiente en el layout.
     *
     * @param view Vista raíz del fragmento.
     */
    private void vincularVistas(@NonNull View view) {
        etUrl               = view.findViewById(R.id.etUrl);
        btnCargarWeb        = view.findViewById(R.id.btnCargarWeb);
        tvUrlActual         = view.findViewById(R.id.tvUrlActual);
        progressWeb         = view.findViewById(R.id.progressWeb);
        webView             = view.findViewById(R.id.webView);
        layoutPlaceholderWeb = view.findViewById(R.id.layoutPlaceholderWeb);
        tvPlaceholderTitulo = view.findViewById(R.id.tvPlaceholderTitulo);
        tvPlaceholderMensaje = view.findViewById(R.id.tvPlaceholderMensaje);
    }

    /**
     * Configura el {@link WebView} con ajustes de funcionalidad y seguridad,
     * y registra los clientes necesarios para manejar la navegación y el progreso.
     *
     * Funcionalidad habilitada: JavaScript, DOM Storage, zoom integrado,
     * vista general de página ancha.
     *
     * Seguridad aplicada: sin acceso a archivos locales ni contenido,
     * sin contenido mixto HTTP/HTTPS, sin apertura automática de ventanas,
     * sin soporte para múltiples ventanas.
     */
    @SuppressLint("SetJavaScriptEnabled")
    private void configurarWebView() {
        WebSettings settings = webView.getSettings();

        // Funcionalidad
        settings.setJavaScriptEnabled(true);
        settings.setDomStorageEnabled(true);
        settings.setBuiltInZoomControls(true);
        settings.setDisplayZoomControls(false);   // Oculta los botones +/- de zoom
        settings.setLoadWithOverviewMode(true);
        settings.setUseWideViewPort(true);

        // Seguridad
        settings.setAllowFileAccess(false);
        settings.setAllowContentAccess(false);
        settings.setMixedContentMode(WebSettings.MIXED_CONTENT_NEVER_ALLOW);
        settings.setJavaScriptCanOpenWindowsAutomatically(false);
        settings.setSupportMultipleWindows(false);

        webView.setWebViewClient(new WebViewClient() {

            /**
             * Intercepta cada navegación dentro del WebView.
             * Solo permite esquemas HTTP y HTTPS; bloquea cualquier otro
             * (tel:, mailto:, intent:, etc.) mostrando un Toast informativo.
             */
            @Override
            public boolean shouldOverrideUrlLoading(
                    WebView view,
                    WebResourceRequest request
            ) {
                Uri destino = request.getUrl();

                if (esUrlHttpSegura(destino)) {
                    return false; // Deja que el WebView maneje la navegación
                }

                Toast.makeText(
                        requireContext(),
                        R.string.web_esquema_no_permitido,
                        Toast.LENGTH_SHORT
                ).show();

                return true; // Bloquea la navegación
            }

            /**
             * Se llama cuando el WebView comienza a cargar una página.
             * Muestra la barra de progreso, oculta el placeholder y
             * actualiza el texto de estado con la URL en curso.
             */
            @Override
            public void onPageStarted(WebView view, String url, Bitmap favicon) {
                hayError = false;

                progressWeb.setVisibility(View.VISIBLE);
                progressWeb.setProgress(0);
                layoutPlaceholderWeb.setVisibility(View.GONE);

                tvUrlActual.setTextColor(
                        ContextCompat.getColor(
                                requireContext(),
                                R.color.explora_texto_secundario
                        )
                );

                tvUrlActual.setText(
                        getString(R.string.web_estado_cargando, url)
                );
            }

            /**
             * Se llama cuando el WebView termina de cargar una página.
             * Oculta la barra de progreso. Si no hubo error, oculta el
             * placeholder y muestra la URL final cargada.
             */
            @Override
            public void onPageFinished(WebView view, String url) {
                progressWeb.setVisibility(View.INVISIBLE);

                if (!hayError) {
                    layoutPlaceholderWeb.setVisibility(View.GONE);
                    mostrarUrlActual(url);
                }

                actualizarBotonAtras();
            }

            /**
             * Se llama cuando ocurre un error de red al cargar un recurso.
             * Solo se reacciona cuando el error afecta al frame principal
             * (la página completa), no a recursos secundarios como imágenes.
             */
            @Override
            public void onReceivedError(
                    WebView view,
                    WebResourceRequest request,
                    WebResourceError error
            ) {
                if (request.isForMainFrame()) {
                    hayError = true;
                    mostrarErrorCarga(request.getUrl().toString());
                }
            }

            /**
             * Se llama cuando el servidor responde con un código HTTP de error
             * (4xx o 5xx). Solo se reacciona cuando afecta al frame principal.
             */
            @Override
            public void onReceivedHttpError(
                    WebView view,
                    WebResourceRequest request,
                    WebResourceResponse errorResponse
            ) {
                if (request.isForMainFrame()
                        && errorResponse.getStatusCode() >= 400) {
                    hayError = true;
                    mostrarErrorCarga(request.getUrl().toString());
                }
            }
        });

        // Actualiza el progreso de la ProgressBar durante la carga
        webView.setWebChromeClient(new WebChromeClient() {
            @Override
            public void onProgressChanged(WebView view, int newProgress) {
                progressWeb.setProgress(newProgress);
            }
        });
    }

    /**
     * Registra los listeners de interacción del usuario:
     * el botón "Cargar página" y la tecla Enter/Go del teclado
     * en el campo de URL.
     */
    private void configurarEventos() {
        btnCargarWeb.setOnClickListener(v -> cargarUrlIngresada());

        etUrl.setOnEditorActionListener((v, actionId, event) -> {
            boolean enter =
                    event != null
                            && event.getAction() == KeyEvent.ACTION_DOWN
                            && event.getKeyCode() == KeyEvent.KEYCODE_ENTER;

            if (actionId == EditorInfo.IME_ACTION_GO || enter) {
                cargarUrlIngresada();
                return true;
            }

            return false;
        });
    }

    // ── Lógica de carga ──────────────────────────────────────────────────────

    /**
     * Lee el texto del campo URL, lo valida y, si es correcto, lo carga
     * en el WebView. En caso de error muestra el mensaje adecuado.
     *
     * Flujo:
     *
     * Verifica que el campo no esté vacío.
     * Normaliza la URL (agrega {@code https://} si falta esquema).
     * Verifica que el esquema resultante sea HTTP o HTTPS.
     * Oculta el teclado y lanza la carga en el WebView.
     *
     */
    private void cargarUrlIngresada() {
        String entrada = etUrl.getText().toString().trim();

        if (entrada.isEmpty()) {
            etUrl.setError(getString(R.string.web_error_url_vacia));
            mostrarPlaceholder(
                    R.string.web_placeholder_vacio_titulo,
                    R.string.web_placeholder_vacio_mensaje
            );
            return;
        }

        Uri uri = normalizarUrl(entrada);

        if (uri == null || !esUrlHttpSegura(uri)) {
            rechazarUrl();
            return;
        }

        etUrl.setError(null);
        ocultarTeclado();

        String urlNormalizada = uri.toString();
        etUrl.setText(urlNormalizada);

        webView.loadUrl(urlNormalizada);
    }

    /**
     * Intenta construir un {@link Uri} válido y seguro a partir de la
     * cadena ingresada por el usuario.
     *
     * Si la entrada no tiene esquema HTTP/HTTPS, se le antepone
     * {@code https://}. Cualquier otro esquema (javascript:, file:,
     * intent:, tel:, mailto:, etc.) devuelve {@code null}.
     *
     * @param entrada Texto ingresado por el usuario; no debe ser nulo.
     * @return Un {@link Uri} normalizado, o {@code null} si la entrada
     *         no es una URL válida o usa un esquema no permitido.
     */
    @Nullable
    private Uri normalizarUrl(@NonNull String entrada) {
        String url = entrada.trim();

        if (TextUtils.isEmpty(url)) {
            return null;
        }

        String minusculas = url.toLowerCase();

        if (!minusculas.startsWith("http://")
                && !minusculas.startsWith("https://")) {

            // Bloquea esquemas no HTTP explícitamente indicados
            if (minusculas.contains("://")
                    || minusculas.startsWith("javascript:")
                    || minusculas.startsWith("file:")
                    || minusculas.startsWith("intent:")
                    || minusculas.startsWith("tel:")
                    || minusculas.startsWith("mailto:")) {
                return null;
            }

            // Asume HTTPS si no hay esquema
            url = "https://" + url;
        }

        try {
            Uri uri = Uri.parse(url);

            // Rechaza URIs sin host (p. ej. "https://")
            if (TextUtils.isEmpty(uri.getHost())) {
                return null;
            }

            return uri;

        } catch (Exception e) {
            return null;
        }
    }

    /**
     * Verifica que el {@link Uri} dado use un esquema HTTP o HTTPS
     * y que tenga un host no vacío.
     *
     * @param uri URI a evaluar; puede ser {@code null}.
     * @return {@code true} si el esquema es {@code http} o {@code https}
     *         y el host es válido; {@code false} en cualquier otro caso.
     */
    private boolean esUrlHttpSegura(@Nullable Uri uri) {
        if (uri == null) return false;

        String esquema = uri.getScheme();
        String host    = uri.getHost();

        if (TextUtils.isEmpty(esquema) || TextUtils.isEmpty(host)) {
            return false;
        }

        return "https".equalsIgnoreCase(esquema)
                || "http".equalsIgnoreCase(esquema);
    }

    /**
     * Marca el campo URL con un error y muestra el placeholder
     * de URL inválida cuando la entrada no supera la validación.
     */
    private void rechazarUrl() {
        etUrl.setError(getString(R.string.web_error_url_invalida));
        mostrarPlaceholder(
                R.string.web_placeholder_url_invalida_titulo,
                R.string.web_placeholder_url_invalida_mensaje
        );
    }

    // ── Presentación de estados ──────────────────────────────────────────────

    /**
     * Muestra el placeholder de error de carga y actualiza el texto
     * de estado con la URL que falló, en color de error.
     *
     * @param url URL que no pudo cargarse.
     */
    private void mostrarErrorCarga(@NonNull String url) {
        progressWeb.setVisibility(View.INVISIBLE);

        mostrarPlaceholder(
                R.string.web_error_conexion_titulo,
                R.string.web_error_conexion_mensaje
        );

        tvUrlActual.setTextColor(
                ContextCompat.getColor(requireContext(), R.color.explora_error)
        );

        tvUrlActual.setText(getString(R.string.web_estado_error, url));
    }

    /**
     * Muestra la URL cargada exitosamente en el TextView de estado,
     * usando el color secundario estándar.
     *
     * @param url URL cargada correctamente.
     */
    private void mostrarUrlActual(@NonNull String url) {
        tvUrlActual.setTextColor(
                ContextCompat.getColor(
                        requireContext(),
                        R.color.explora_texto_secundario
                )
        );
        tvUrlActual.setText(url);
    }

    /**
     * Actualiza los textos del placeholder y lo hace visible.
     *
     * @param titulo  Recurso de string para el título del placeholder.
     * @param mensaje Recurso de string para el mensaje del placeholder.
     */
    private void mostrarPlaceholder(
            @StringRes int titulo,
            @StringRes int mensaje
    ) {
        tvPlaceholderTitulo.setText(titulo);
        tvPlaceholderMensaje.setText(mensaje);
        layoutPlaceholderWeb.setVisibility(View.VISIBLE);
    }

    // ── Utilidades ───────────────────────────────────────────────────────────

    /**
     * Oculta el teclado virtual, liberando el foco del campo URL.
     * Se llama justo antes de iniciar la carga de una página.
     */
    private void ocultarTeclado() {
        InputMethodManager imm =
                (InputMethodManager) requireContext()
                        .getSystemService(Context.INPUT_METHOD_SERVICE);

        if (imm != null) {
            imm.hideSoftInputFromWindow(etUrl.getWindowToken(), 0);
        }
    }

    // ── Navegación hacia atrás ───────────────────────────────────────────────

    /**
     * Registra un {@link OnBackPressedCallback} en el dispatcher de la Activity.
     * Cuando está habilitado, intercepta el botón atrás del sistema para
     * navegar al historial anterior del WebView en lugar de salir del fragmento.
     */
    private void configurarBotonAtrasDelSistema() {
        backCallback = new OnBackPressedCallback(false) {
            @Override
            public void handleOnBackPressed() {
                if (webView != null && webView.canGoBack()) {
                    webView.goBack();
                    actualizarBotonAtras();
                }
            }
        };

        requireActivity()
                .getOnBackPressedDispatcher()
                .addCallback(getViewLifecycleOwner(), backCallback);
    }

    /**
     * Habilita o deshabilita el callback del botón atrás según si el
     * WebView tiene páginas previas en su historial de navegación.
     */
    private void actualizarBotonAtras() {
        if (backCallback != null && webView != null) {
            backCallback.setEnabled(webView.canGoBack());
        }
    }

    // ── Persistencia y ciclo de vida ─────────────────────────────────────────

    /**
     * Guarda el estado completo del WebView (historial, posición de scroll,
     * URL actual) en el Bundle para restaurarlo tras una rotación u otro
     * cambio de configuración.
     *
     * @param outState Bundle donde se almacena el estado.
     */
    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        if (webView != null) {
            webView.saveState(outState);
        }
        super.onSaveInstanceState(outState);
    }

    /**
     * Reanuda la actividad del WebView (timers internos, animaciones, etc.)
     * cuando el fragmento vuelve a ser visible.
     */
    @Override
    public void onResume() {
        super.onResume();
        if (webView != null) webView.onResume();
    }

    /**
     * Pausa la actividad interna del WebView cuando el fragmento deja
     * de estar en primer plano, reduciendo el consumo de recursos.
     */
    @Override
    public void onPause() {
        if (webView != null) webView.onPause();
        super.onPause();
    }

    /**
     * Libera todos los recursos asociados a la vista del fragmento.
     *
     * <p>Detiene la carga del WebView, lo desvincula de su vista padre y
     * llama a {@link WebView#destroy()} para liberar la memoria nativa.
     * Todas las referencias a vistas se anulan para evitar memory leaks.</p>
     */
    @Override
    public void onDestroyView() {
        if (webView != null) {
            webView.stopLoading();

            // Desvincula el WebView de su padre antes de destruirlo
            ViewGroup parent = (ViewGroup) webView.getParent();
            if (parent != null) {
                parent.removeView(webView);
            }

            webView.destroy();
            webView = null;
        }

        etUrl                = null;
        btnCargarWeb         = null;
        tvUrlActual          = null;
        tvPlaceholderTitulo  = null;
        tvPlaceholderMensaje = null;
        progressWeb          = null;
        layoutPlaceholderWeb = null;

        super.onDestroyView();
    }
}