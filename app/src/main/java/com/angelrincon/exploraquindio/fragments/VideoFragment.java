package com.angelrincon.exploraquindio.fragments;

import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.VideoView;

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.view.WindowInsetsControllerCompat;
import androidx.fragment.app.Fragment;

import com.angelrincon.exploraquindio.R;

import java.util.Locale;

/**
 * Fragmento del reproductor de video de Explora Quindío.
 *
 * Reproduce un video local almacenado en {@code res/raw/video_quindio.mp4}
 * y ofrece controles completos de reproducción:
 *
 *   Play, pausa, avance y retroceso de 10 segundos.
 *   Barra de progreso ({@link SeekBar}) con tiempo actual y total.
 *   Modo pantalla completa que oculta el encabezado, el menú lateral
 *       y las barras del sistema.
 *   Restauración del estado (posición, modo reproducción, pantalla
 *       completa) ante rotaciones de pantalla.
 *   Limpieza completa de recursos en {@code onDestroyView}.
 *
 *
 * El layout asociado es {@code R.layout.fragment_video}.
 */
public class VideoFragment extends Fragment {

    // ── Constantes ───────────────────────────────────────────────────────────

    /** Nombre del archivo de video en {@code res/raw/} (sin extensión). */
    private static final String VIDEO_RAW_NAME = "video_quindio";

    /** Milisegundos que se avanza o retrocede con los botones de salto. */
    private static final int SALTO_MS = 10_000;

    /** Clave para guardar la posición de reproducción en el Bundle. */
    private static final String KEY_POSICION      = "posicion_video";

    /** Clave para guardar si el video estaba reproduciéndose en el Bundle. */
    private static final String KEY_REPRODUCIENDO = "video_reproduciendo";

    /** Clave para guardar el estado de pantalla completa en el Bundle. */
    private static final String KEY_FULLSCREEN    = "video_fullscreen";

    // ── Vistas ───────────────────────────────────────────────────────────────

    /** Vista nativa de Android que renderiza el video. */
    private VideoView videoView;

    /** Indicador de carga circular mientras el video se prepara. */
    private ProgressBar progressVideo;

    /** Barra deslizable que refleja y controla la posición de reproducción. */
    private SeekBar seekVideo;

    /** Muestra el tiempo actual y la duración total en formato {@code mm:ss / mm:ss}. */
    private TextView tvTiempoVideo;

    /** Muestra el estado actual del reproductor (cargando, reproduciendo, pausa, etc.). */
    private TextView tvEstadoVideo;

    /** Botón para retroceder {@value #SALTO_MS} ms en la reproducción. */
    private Button btnRetroceder;

    /** Botón para iniciar o reanudar la reproducción. */
    private Button btnPlay;

    /** Botón para pausar la reproducción. */
    private Button btnPausa;

    /** Botón para avanzar {@value #SALTO_MS} ms en la reproducción. */
    private Button btnAvanzar;

    /** Botón para activar o desactivar el modo pantalla completa. */
    private Button btnPantallaCompleta;

    // ── Estado interno ───────────────────────────────────────────────────────

    /**
     * Última posición conocida del video en milisegundos.
     * Se usa para restaurar la reproducción tras una pausa o rotación.
     */
    private int posicionGuardada = 0;

    /**
     * Indica si el {@link VideoView} terminó de prepararse y está listo
     * para recibir comandos de reproducción.
     */
    private boolean preparado = false;

    /**
     * Indica si se debe iniciar la reproducción automáticamente en cuanto
     * el video termine de prepararse. Se usa para restaurar el estado
     * tras una rotación de pantalla.
     */
    private boolean reproducirTrasPreparar = false;

    /** Indica si el fragmento está actualmente en modo pantalla completa. */
    private boolean pantallaCompleta = false;

    /**
     * Callback registrado en el dispatcher de back press de la Activity.
     * Cuando está habilitado (pantalla completa activa), intercepta el
     * botón atrás para salir del modo pantalla completa.
     */
    private OnBackPressedCallback backCallback;

    // ── Actualización periódica de progreso ──────────────────────────────────

    /** Handler anclado al hilo principal para publicar actualizaciones de la UI. */
    private final Handler handler = new Handler(Looper.getMainLooper());

    /**
     * Tarea periódica que actualiza la {@link SeekBar} y el tiempo
     * mostrado cada 500 ms mientras el fragmento está en primer plano.
     */
    private final Runnable actualizador = new Runnable() {
        @Override
        public void run() {
            actualizarProgreso();
            handler.postDelayed(this, 500);
        }
    };

    // ── Constructor ──────────────────────────────────────────────────────────

    /**
     * Constructor sin argumentos requerido por el sistema de fragmentos.
     * Asocia el layout {@code R.layout.fragment_video} al fragmento.
     */
    public VideoFragment() {
        super(R.layout.fragment_video);
    }

    // ── Ciclo de vida ────────────────────────────────────────────────────────

    /**
     * Se ejecuta justo después de que la vista del fragmento es creada.
     *
     * Restaura el estado guardado si existe (posición, modo reproducción,
     * pantalla completa), luego inicializa el video, los controles, la
     * SeekBar y el callback del botón atrás. Si corresponde, aplica el
     * estado de pantalla completa tras el primer layout.
     *
     * @param view               Vista raíz inflada desde {@code R.layout.fragment_video}.
     * @param savedInstanceState Estado previo del fragmento, o {@code null}.
     */
    @Override
    public void onViewCreated(
            @NonNull View view,
            @Nullable Bundle savedInstanceState
    ) {
        super.onViewCreated(view, savedInstanceState);

        vincularVistas(view);

        // Restaura el estado previo si hubo rotación u otro cambio de configuración
        if (savedInstanceState != null) {
            posicionGuardada      = savedInstanceState.getInt(KEY_POSICION, 0);
            reproducirTrasPreparar = savedInstanceState.getBoolean(KEY_REPRODUCIENDO, false);
            pantallaCompleta      = savedInstanceState.getBoolean(KEY_FULLSCREEN, false);
        }

        configurarVideo();
        configurarControles();
        configurarSeekBar();
        configurarBotonAtras();

        // Aplica pantalla completa tras el primer layout si estaba activa
        if (pantallaCompleta) {
            view.post(this::aplicarEstadoPantallaCompleta);
        }
    }

    // ── Inicialización ───────────────────────────────────────────────────────

    /**
     * Enlaza cada campo de la clase con su vista correspondiente en el layout.
     *
     * @param view Vista raíz del fragmento.
     */
    private void vincularVistas(@NonNull View view) {
        videoView          = view.findViewById(R.id.videoView);
        progressVideo      = view.findViewById(R.id.progressVideo);
        seekVideo          = view.findViewById(R.id.seekVideo);
        tvTiempoVideo      = view.findViewById(R.id.tvTiempoVideo);
        tvEstadoVideo      = view.findViewById(R.id.tvEstadoVideo);

        btnRetroceder      = view.findViewById(R.id.btnRetroceder);
        btnPlay            = view.findViewById(R.id.btnPlay);
        btnPausa           = view.findViewById(R.id.btnPausa);
        btnAvanzar         = view.findViewById(R.id.btnAvanzar);
        btnPantallaCompleta = view.findViewById(R.id.btnPantallaCompleta);
    }

    /**
     * Configura los listeners del {@link VideoView} para los eventos de
     * preparación, finalización y error, y lanza la carga del video local.
     *
     * Los controles se deshabilitan hasta que el video esté listo para
     * reproducirse, momento en el que también se restaura la posición guardada.
     */
    private void configurarVideo() {
        establecerControlesHabilitados(false);

        // Se llama cuando el video termina de prepararse y está listo
        videoView.setOnPreparedListener(mediaPlayer -> {
            preparado = true;
            progressVideo.setVisibility(View.GONE);
            seekVideo.setMax(videoView.getDuration());

            // Busca la posición guardada (o el inicio si es la primera vez)
            videoView.seekTo(posicionGuardada > 0 ? posicionGuardada : 1);

            establecerControlesHabilitados(true);

            if (reproducirTrasPreparar) {
                // Reanuda automáticamente si estaba reproduciendo antes de la rotación
                videoView.start();
                tvEstadoVideo.setText(R.string.video_estado_reproduciendo);
                reproducirTrasPreparar = false;
            } else {
                tvEstadoVideo.setText(R.string.video_estado_listo);
            }

            actualizarProgreso();
        });

        // Se llama cuando el video llega al final
        videoView.setOnCompletionListener(mediaPlayer -> {
            tvEstadoVideo.setText(R.string.video_estado_finalizado);
            actualizarProgreso();
        });

        // Se llama cuando ocurre un error de reproducción
        videoView.setOnErrorListener((mediaPlayer, what, extra) -> {
            preparado = false;
            progressVideo.setVisibility(View.GONE);
            establecerControlesHabilitados(false);
            tvEstadoVideo.setText(R.string.video_estado_error);

            if (isAdded()) {
                Toast.makeText(
                        requireContext(),
                        getString(R.string.video_error_archivo, VIDEO_RAW_NAME),
                        Toast.LENGTH_LONG
                ).show();
            }

            return true; // Error manejado; evita el diálogo de error nativo
        });

        cargarVideoLocal();
    }

    /**
     * Busca el recurso de video en {@code res/raw/} por nombre y lo asigna
     * al {@link VideoView}. Si el archivo no existe, muestra un mensaje de
     * error y deshabilita los controles.
     */
    private void cargarVideoLocal() {
        preparado = false;
        progressVideo.setVisibility(View.VISIBLE);
        tvEstadoVideo.setText(R.string.video_estado_cargando);

        String paquete = requireContext().getPackageName();

        // Busca el ID del recurso raw por nombre en tiempo de ejecución
        int rawId = getResources().getIdentifier(
                VIDEO_RAW_NAME, "raw", paquete
        );

        if (rawId == 0) {
            // El archivo no existe en res/raw/
            progressVideo.setVisibility(View.GONE);
            tvEstadoVideo.setText(R.string.video_estado_archivo_faltante);
            establecerControlesHabilitados(false);

            Toast.makeText(
                    requireContext(),
                    getString(R.string.video_error_archivo, VIDEO_RAW_NAME),
                    Toast.LENGTH_LONG
            ).show();

            return;
        }

        // Construye la URI del recurso local y la asigna al VideoView
        Uri uri = Uri.parse("android.resource://" + paquete + "/" + rawId);
        videoView.setVideoURI(uri);
    }

    /**
     * Asigna los listeners a los botones de control del reproductor:
     * play, pausa, retroceder, avanzar y pantalla completa.
     */
    private void configurarControles() {

        btnPlay.setOnClickListener(v -> {
            if (!preparado) return;
            if (!videoView.isPlaying()) {
                videoView.start();
                tvEstadoVideo.setText(R.string.video_estado_reproduciendo);
            }
        });

        btnPausa.setOnClickListener(v -> {
            if (preparado && videoView.isPlaying()) {
                videoView.pause();
                tvEstadoVideo.setText(R.string.video_estado_pausa);
            }
        });

        btnRetroceder.setOnClickListener(v -> {
            if (!preparado) return;
            // Retrocede SALTO_MS ms sin pasar del inicio
            int nuevaPosicion = Math.max(
                    0,
                    videoView.getCurrentPosition() - SALTO_MS
            );
            videoView.seekTo(nuevaPosicion);
            tvEstadoVideo.setText(R.string.video_estado_retrocediendo);
            actualizarProgreso();
        });

        btnAvanzar.setOnClickListener(v -> {
            if (!preparado) return;
            // Avanza SALTO_MS ms sin pasar del final
            int nuevaPosicion = Math.min(
                    videoView.getDuration(),
                    videoView.getCurrentPosition() + SALTO_MS
            );
            videoView.seekTo(nuevaPosicion);
            tvEstadoVideo.setText(R.string.video_estado_avanzando);
            actualizarProgreso();
        });

        btnPantallaCompleta.setOnClickListener(v -> {
            pantallaCompleta = !pantallaCompleta;
            aplicarEstadoPantallaCompleta();
        });
    }

    /**
     * Configura el listener de la {@link SeekBar} para que el usuario
     * pueda arrastrarla y posicionarse en cualquier punto del video.
     * Solo responde a cambios iniciados por el usuario ({@code fromUser = true}).
     */
    private void configurarSeekBar() {
        seekVideo.setOnSeekBarChangeListener(
                new SeekBar.OnSeekBarChangeListener() {

                    @Override
                    public void onProgressChanged(
                            SeekBar seekBar,
                            int progress,
                            boolean fromUser
                    ) {
                        // Ignora actualizaciones automáticas del actualizador periódico
                        if (fromUser && preparado) {
                            videoView.seekTo(progress);
                            tvTiempoVideo.setText(
                                    getString(
                                            R.string.video_tiempo_formato,
                                            formatear(progress),
                                            formatear(videoView.getDuration())
                                    )
                            );
                        }
                    }

                    @Override
                    public void onStartTrackingTouch(SeekBar seekBar) { }

                    @Override
                    public void onStopTrackingTouch(SeekBar seekBar) { }
                }
        );
    }

    /**
     * Registra un {@link OnBackPressedCallback} en el dispatcher de la Activity.
     * Solo está habilitado cuando el fragmento está en modo pantalla completa;
     * en ese caso, el botón atrás sale del modo pantalla completa en lugar
     * de navegar hacia atrás en la app.
     */
    private void configurarBotonAtras() {
        backCallback = new OnBackPressedCallback(pantallaCompleta) {
            @Override
            public void handleOnBackPressed() {
                if (pantallaCompleta) {
                    pantallaCompleta = false;
                    aplicarEstadoPantallaCompleta();
                }
            }
        };

        requireActivity()
                .getOnBackPressedDispatcher()
                .addCallback(getViewLifecycleOwner(), backCallback);
    }

    // ── Pantalla completa ────────────────────────────────────────────────────

    /**
     * Aplica o revierte el modo pantalla completa según el valor actual
     * de {@link #pantallaCompleta}.
     *
     * Al entrar en pantalla completa:
     *
     * Oculta el encabezado ({@code headerContainer}) y el menú
     *       lateral ({@code menuContainer}) de la Activity.
     * Oculta las barras del sistema con comportamiento transient
     *       (reaparecen al deslizar desde el borde).
     * Actualiza el texto y la descripción del botón.
     *
     *
     * Al salir, revierte todos los cambios anteriores.
     */
    private void aplicarEstadoPantallaCompleta() {
        if (!isAdded()) return;

        View header = requireActivity().findViewById(R.id.headerContainer);
        View menu   = requireActivity().findViewById(R.id.menuContainer);

        if (header != null) {
            header.setVisibility(pantallaCompleta ? View.GONE : View.VISIBLE);
        }

        if (menu != null) {
            menu.setVisibility(pantallaCompleta ? View.GONE : View.VISIBLE);
        }

        WindowInsetsControllerCompat controller =
                WindowCompat.getInsetsController(
                        requireActivity().getWindow(),
                        requireActivity().getWindow().getDecorView()
                );

        if (pantallaCompleta) {
            controller.hide(WindowInsetsCompat.Type.systemBars());
            controller.setSystemBarsBehavior(
                    WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            );
            btnPantallaCompleta.setText(R.string.video_btn_salir_pantalla_completa);
            btnPantallaCompleta.setContentDescription(
                    getString(R.string.video_descripcion_salir_pantalla_completa)
            );
        } else {
            controller.show(WindowInsetsCompat.Type.systemBars());
            btnPantallaCompleta.setText(R.string.video_btn_pantalla_completa);
            btnPantallaCompleta.setContentDescription(
                    getString(R.string.video_descripcion_pantalla_completa)
            );
        }

        if (backCallback != null) {
            backCallback.setEnabled(pantallaCompleta);
        }
    }

    // ── Utilidades ───────────────────────────────────────────────────────────

    /**
     * Habilita o deshabilita todos los controles del reproductor excepto
     * el botón de pantalla completa, que permanece siempre habilitado.
     *
     * @param habilitados {@code true} para habilitar los controles;
     *                    {@code false} para deshabilitarlos.
     */
    private void establecerControlesHabilitados(boolean habilitados) {
        btnPlay.setEnabled(habilitados);
        btnPausa.setEnabled(habilitados);
        btnRetroceder.setEnabled(habilitados);
        btnAvanzar.setEnabled(habilitados);
        seekVideo.setEnabled(habilitados);
        btnPantallaCompleta.setEnabled(true); // Siempre habilitado
    }

    /**
     * Sincroniza la {@link SeekBar} y el {@link TextView} de tiempo con
     * la posición actual del video. No hace nada si el video no está preparado.
     */
    private void actualizarProgreso() {
        if (videoView == null || !preparado) return;

        int actual = videoView.getCurrentPosition();
        int total  = videoView.getDuration();

        seekVideo.setProgress(actual);
        tvTiempoVideo.setText(
                getString(
                        R.string.video_tiempo_formato,
                        formatear(actual),
                        formatear(total)
                )
        );
    }

    /**
     * Convierte una cantidad de milisegundos al formato {@code mm:ss}.
     *
     * @param milisegundos Tiempo en milisegundos; valores negativos se
     *                     tratan como cero.
     * @return Cadena con el tiempo formateado, por ejemplo {@code "01:35"}.
     */
    private String formatear(int milisegundos) {
        int totalSegundos = Math.max(0, milisegundos) / 1000;

        return String.format(
                Locale.getDefault(),
                "%02d:%02d",
                totalSegundos / 60,
                totalSegundos % 60
        );
    }

    // ── Ciclo de vida del fragmento ──────────────────────────────────────────

    /**
     * Reanuda el actualizador periódico de progreso cuando el fragmento
     * vuelve a primer plano. También reanuda los timers internos del WebView
     * si el video estaba activo.
     */
    @Override
    public void onResume() {
        super.onResume();
        handler.removeCallbacks(actualizador);
        handler.post(actualizador);
    }

    /**
     * Detiene el actualizador periódico y pausa el video cuando el
     * fragmento pasa a segundo plano, preservando la posición actual.
     */
    @Override
    public void onPause() {
        handler.removeCallbacks(actualizador);

        if (videoView != null && preparado) {
            posicionGuardada = videoView.getCurrentPosition();

            if (videoView.isPlaying()) {
                videoView.pause();
                if (tvEstadoVideo != null) {
                    tvEstadoVideo.setText(R.string.video_estado_pausa);
                }
            }
        }

        super.onPause();
    }

    /**
     * Guarda la posición actual, el estado de reproducción y el modo
     * pantalla completa en el Bundle para restaurarlos tras una rotación
     * u otro cambio de configuración.
     *
     * @param outState Bundle donde se almacena el estado.
     */
    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        if (videoView != null && preparado) {
            posicionGuardada = videoView.getCurrentPosition();
            outState.putBoolean(KEY_REPRODUCIENDO, videoView.isPlaying());
        } else {
            // Preserva la intención de reproducir si el video aún no estaba listo
            outState.putBoolean(KEY_REPRODUCIENDO, reproducirTrasPreparar);
        }

        outState.putInt(KEY_POSICION, posicionGuardada);
        outState.putBoolean(KEY_FULLSCREEN, pantallaCompleta);

        super.onSaveInstanceState(outState);
    }

    /**
     * Libera todos los recursos asociados a la vista del fragmento.
     *
     * Detiene el actualizador periódico, restaura la visibilidad del
     * encabezado y el menú de la Activity (por si se destruye en pantalla
     * completa), muestra las barras del sistema, detiene el video y anula
     * todas las referencias a vistas para evitar memory leaks.
     */
    @Override
    public void onDestroyView() {
        handler.removeCallbacks(actualizador);

        // Restaura la UI de la Activity si el fragmento se destruye en
        // modo pantalla completa (p. ej. al navegar a otra sección)
        if (isAdded()) {
            View header = requireActivity().findViewById(R.id.headerContainer);
            View menu   = requireActivity().findViewById(R.id.menuContainer);

            if (header != null) header.setVisibility(View.VISIBLE);
            if (menu != null)   menu.setVisibility(View.VISIBLE);

            WindowInsetsControllerCompat controller =
                    WindowCompat.getInsetsController(
                            requireActivity().getWindow(),
                            requireActivity().getWindow().getDecorView()
                    );
            controller.show(WindowInsetsCompat.Type.systemBars());
        }

        if (videoView != null) {
            videoView.stopPlayback();
        }

        // Anula referencias para evitar memory leaks
        videoView           = null;
        progressVideo       = null;
        seekVideo           = null;
        tvTiempoVideo       = null;
        tvEstadoVideo       = null;
        btnRetroceder       = null;
        btnPlay             = null;
        btnPausa            = null;
        btnAvanzar          = null;
        btnPantallaCompleta = null;

        super.onDestroyView();
    }
}