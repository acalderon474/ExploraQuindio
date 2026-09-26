package com.angelrincon.exploraquindio.fragments;

import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ProgressBar;
import android.widget.ScrollView;
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

public class VideoFragment extends Fragment {

    private static final String VIDEO_RAW_NAME = "video_quindio";
    private static final int SALTO_MS = 10_000;

    private static final String KEY_POSICION = "posicion_video";
    private static final String KEY_REPRODUCIENDO = "video_reproduciendo";
    private static final String KEY_FULLSCREEN = "video_fullscreen";

    private FrameLayout videoFrame;
    private FrameLayout fullscreenVideoContainer;
    private ScrollView scrollVideoNormal;

    private VideoView videoView;
    private ProgressBar progressVideo;
    private SeekBar seekVideo;
    private TextView tvTiempoVideo;
    private TextView tvEstadoVideo;

    private Button btnRetroceder;
    private Button btnPlay;
    private Button btnPausa;
    private Button btnAvanzar;
    private Button btnPantallaCompleta;

    private int posicionGuardada = 0;
    private boolean preparado = false;

    /*
     * Representa el estado de reproducción que el usuario espera.
     *
     * true:
     * el video estaba reproduciéndose y debe continuar después de una
     * recreación o al volver a primer plano.
     *
     * false:
     * el usuario lo dejó pausado o aún no inició la reproducción.
     *
     * Es diferente de consultar únicamente videoView.isPlaying(), porque
     * onPause() pausa físicamente el VideoView antes de una posible recreación.
     */
    private boolean reproduccionDeseada = false;

    private boolean pantallaCompleta = false;

    private OnBackPressedCallback backCallback;

    /*
     * Datos necesarios para devolver videoFrame a su lugar exacto
     * después de salir de pantalla completa.
     */
    private ViewGroup padreOriginalVideoFrame;
    private ViewGroup.LayoutParams parametrosOriginalesVideoFrame;
    private int indiceOriginalVideoFrame = -1;

    private int paddingLeftOriginal;
    private int paddingTopOriginal;
    private int paddingRightOriginal;
    private int paddingBottomOriginal;

    private final Handler handler =
            new Handler(Looper.getMainLooper());

    private final Runnable actualizador = new Runnable() {
        @Override
        public void run() {
            actualizarProgreso();
            handler.postDelayed(this, 500);
        }
    };

    public VideoFragment() {
        super(R.layout.fragment_video);
    }

    @Override
    public void onViewCreated(
            @NonNull View view,
            @Nullable Bundle savedInstanceState
    ) {
        super.onViewCreated(view, savedInstanceState);

        vincularVistas(view);

        if (savedInstanceState != null) {
            posicionGuardada =
                    savedInstanceState.getInt(KEY_POSICION, 0);

            reproduccionDeseada =
                    savedInstanceState.getBoolean(
                            KEY_REPRODUCIENDO,
                            false
                    );

            pantallaCompleta =
                    savedInstanceState.getBoolean(
                            KEY_FULLSCREEN,
                            false
                    );
        }

        configurarVideo();
        configurarControles();
        configurarSeekBar();
        configurarBotonAtras();

        /*
         * Si el Fragment fue recreado mientras estaba en fullscreen,
         * primero Android infla nuevamente el XML en modo normal.
         * Luego se mueve videoFrame al contenedor fullscreen.
         */
        if (pantallaCompleta) {
            view.post(() -> establecerPantallaCompleta(true));
        }
    }

    private void vincularVistas(@NonNull View view) {

        scrollVideoNormal =
                view.findViewById(R.id.scrollVideoNormal);

        fullscreenVideoContainer =
                view.findViewById(R.id.fullscreenVideoContainer);

        videoFrame =
                view.findViewById(R.id.videoFrame);

        videoView =
                view.findViewById(R.id.videoView);

        progressVideo =
                view.findViewById(R.id.progressVideo);

        seekVideo =
                view.findViewById(R.id.seekVideo);

        tvTiempoVideo =
                view.findViewById(R.id.tvTiempoVideo);

        tvEstadoVideo =
                view.findViewById(R.id.tvEstadoVideo);

        btnRetroceder =
                view.findViewById(R.id.btnRetroceder);

        btnPlay =
                view.findViewById(R.id.btnPlay);

        btnPausa =
                view.findViewById(R.id.btnPausa);

        btnAvanzar =
                view.findViewById(R.id.btnAvanzar);

        btnPantallaCompleta =
                view.findViewById(R.id.btnPantallaCompleta);

        paddingLeftOriginal = videoFrame.getPaddingLeft();
        paddingTopOriginal = videoFrame.getPaddingTop();
        paddingRightOriginal = videoFrame.getPaddingRight();
        paddingBottomOriginal = videoFrame.getPaddingBottom();
    }

    private void configurarVideo() {

        establecerControlesHabilitados(false);

        videoView.setOnPreparedListener(mediaPlayer -> {

            preparado = true;
            progressVideo.setVisibility(View.GONE);

            int duracion = videoView.getDuration();
            seekVideo.setMax(Math.max(duracion, 0));

            videoView.seekTo(
                    posicionGuardada > 0
                            ? posicionGuardada
                            : 1
            );

            establecerControlesHabilitados(true);

            if (reproduccionDeseada) {

                videoView.start();
                tvEstadoVideo.setText(
                        R.string.video_estado_reproduciendo
                );

            } else if (posicionGuardada > 0) {

                tvEstadoVideo.setText(
                        R.string.video_estado_pausa
                );

            } else {

                tvEstadoVideo.setText(
                        R.string.video_estado_listo
                );
            }

            actualizarProgreso();
        });

        videoView.setOnCompletionListener(mediaPlayer -> {

            reproduccionDeseada = false;
            posicionGuardada = videoView.getDuration();

            tvEstadoVideo.setText(
                    R.string.video_estado_finalizado
            );

            actualizarProgreso();
        });

        videoView.setOnErrorListener(
                (mediaPlayer, what, extra) -> {

                    preparado = false;
                    reproduccionDeseada = false;

                    progressVideo.setVisibility(View.GONE);
                    establecerControlesHabilitados(false);

                    tvEstadoVideo.setText(
                            R.string.video_estado_error
                    );

                    if (isAdded()) {
                        Toast.makeText(
                                requireContext(),
                                getString(
                                        R.string.video_error_archivo,
                                        VIDEO_RAW_NAME
                                ),
                                Toast.LENGTH_LONG
                        ).show();
                    }

                    return true;
                }
        );

        cargarVideoLocal();
    }

    private void cargarVideoLocal() {

        preparado = false;
        progressVideo.setVisibility(View.VISIBLE);

        tvEstadoVideo.setText(
                R.string.video_estado_cargando
        );

        String paquete =
                requireContext().getPackageName();

        int rawId =
                getResources().getIdentifier(
                        VIDEO_RAW_NAME,
                        "raw",
                        paquete
                );

        if (rawId == 0) {

            progressVideo.setVisibility(View.GONE);

            tvEstadoVideo.setText(
                    R.string.video_estado_archivo_faltante
            );

            establecerControlesHabilitados(false);

            Toast.makeText(
                    requireContext(),
                    getString(
                            R.string.video_error_archivo,
                            VIDEO_RAW_NAME
                    ),
                    Toast.LENGTH_LONG
            ).show();

            return;
        }

        Uri uri =
                Uri.parse(
                        "android.resource://"
                                + paquete
                                + "/"
                                + rawId
                );

        videoView.setVideoURI(uri);
    }

    private void configurarControles() {

        btnPlay.setOnClickListener(v -> {

            if (!preparado) {
                return;
            }

            reproduccionDeseada = true;

            if (!videoView.isPlaying()) {
                videoView.start();

                tvEstadoVideo.setText(
                        R.string.video_estado_reproduciendo
                );
            }
        });

        btnPausa.setOnClickListener(v -> {

            if (!preparado) {
                return;
            }

            reproduccionDeseada = false;
            posicionGuardada = videoView.getCurrentPosition();

            if (videoView.isPlaying()) {
                videoView.pause();
            }

            tvEstadoVideo.setText(
                    R.string.video_estado_pausa
            );
        });

        btnRetroceder.setOnClickListener(v -> {

            if (!preparado) {
                return;
            }

            int nuevaPosicion =
                    Math.max(
                            0,
                            videoView.getCurrentPosition()
                                    - SALTO_MS
                    );

            videoView.seekTo(nuevaPosicion);
            posicionGuardada = nuevaPosicion;

            tvEstadoVideo.setText(
                    R.string.video_estado_retrocediendo
            );

            actualizarProgreso();
        });

        btnAvanzar.setOnClickListener(v -> {

            if (!preparado) {
                return;
            }

            int nuevaPosicion =
                    Math.min(
                            videoView.getDuration(),
                            videoView.getCurrentPosition()
                                    + SALTO_MS
                    );

            videoView.seekTo(nuevaPosicion);
            posicionGuardada = nuevaPosicion;

            tvEstadoVideo.setText(
                    R.string.video_estado_avanzando
            );

            actualizarProgreso();
        });

        btnPantallaCompleta.setOnClickListener(v ->
                establecerPantallaCompleta(
                        !pantallaCompleta
                )
        );
    }

    private void configurarSeekBar() {

        seekVideo.setOnSeekBarChangeListener(
                new SeekBar.OnSeekBarChangeListener() {

                    @Override
                    public void onProgressChanged(
                            SeekBar seekBar,
                            int progress,
                            boolean fromUser
                    ) {

                        if (fromUser && preparado) {

                            videoView.seekTo(progress);
                            posicionGuardada = progress;

                            tvTiempoVideo.setText(
                                    getString(
                                            R.string.video_tiempo_formato,
                                            formatear(progress),
                                            formatear(
                                                    videoView.getDuration()
                                            )
                                    )
                            );
                        }
                    }

                    @Override
                    public void onStartTrackingTouch(
                            SeekBar seekBar
                    ) { }

                    @Override
                    public void onStopTrackingTouch(
                            SeekBar seekBar
                    ) { }
                }
        );
    }

    private void configurarBotonAtras() {

        backCallback =
                new OnBackPressedCallback(
                        pantallaCompleta
                ) {

                    @Override
                    public void handleOnBackPressed() {

                        if (pantallaCompleta) {
                            establecerPantallaCompleta(false);
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

    private void establecerPantallaCompleta(
            boolean activar
    ) {

        if (!isAdded()
                || videoFrame == null
                || fullscreenVideoContainer == null
                || scrollVideoNormal == null) {
            return;
        }

        pantallaCompleta = activar;

        if (activar) {
            entrarPantallaCompleta();
        } else {
            salirPantallaCompleta();
        }

        if (backCallback != null) {
            backCallback.setEnabled(pantallaCompleta);
        }
    }

    private void entrarPantallaCompleta() {

        if (videoFrame.getParent()
                != fullscreenVideoContainer) {

            ViewGroup padreActual =
                    (ViewGroup) videoFrame.getParent();

            if (padreActual == null) {
                pantallaCompleta = false;
                return;
            }

            padreOriginalVideoFrame = padreActual;
            indiceOriginalVideoFrame =
                    padreActual.indexOfChild(videoFrame);
            parametrosOriginalesVideoFrame =
                    videoFrame.getLayoutParams();

            padreActual.removeView(videoFrame);

            FrameLayout.LayoutParams parametrosFullscreen =
                    new FrameLayout.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT,
                            ViewGroup.LayoutParams.MATCH_PARENT
                    );

            fullscreenVideoContainer.addView(
                    videoFrame,
                    parametrosFullscreen
            );
        }

        videoFrame.setPadding(0, 0, 0, 0);

        scrollVideoNormal.setVisibility(View.GONE);
        fullscreenVideoContainer.setVisibility(View.VISIBLE);

        aplicarChromeActividad(true);

        btnPantallaCompleta.setText(
                R.string.video_btn_salir_pantalla_completa
        );

        btnPantallaCompleta.setContentDescription(
                getString(
                        R.string.video_descripcion_salir_pantalla_completa
                )
        );
    }

    private void salirPantallaCompleta() {

        if (videoFrame.getParent()
                == fullscreenVideoContainer
                && padreOriginalVideoFrame != null) {

            fullscreenVideoContainer.removeView(videoFrame);

            videoFrame.setPadding(
                    paddingLeftOriginal,
                    paddingTopOriginal,
                    paddingRightOriginal,
                    paddingBottomOriginal
            );

            if (indiceOriginalVideoFrame >= 0
                    && indiceOriginalVideoFrame
                    <= padreOriginalVideoFrame.getChildCount()) {

                padreOriginalVideoFrame.addView(
                        videoFrame,
                        indiceOriginalVideoFrame,
                        parametrosOriginalesVideoFrame
                );

            } else {

                padreOriginalVideoFrame.addView(
                        videoFrame,
                        parametrosOriginalesVideoFrame
                );
            }
        }

        fullscreenVideoContainer.setVisibility(View.GONE);
        scrollVideoNormal.setVisibility(View.VISIBLE);

        aplicarChromeActividad(false);

        btnPantallaCompleta.setText(
                R.string.video_btn_pantalla_completa
        );

        btnPantallaCompleta.setContentDescription(
                getString(
                        R.string.video_descripcion_pantalla_completa
                )
        );
    }

    private void aplicarChromeActividad(
            boolean fullscreen
    ) {

        View header =
                requireActivity().findViewById(
                        R.id.headerContainer
                );

        View headerDivider =
                requireActivity().findViewById(
                        R.id.headerDivider
                );

        View menu =
                requireActivity().findViewById(
                        R.id.menuContainer
                );

        View menuDivider =
                requireActivity().findViewById(
                        R.id.menuContentDivider
                );

        int visibilidad =
                fullscreen
                        ? View.GONE
                        : View.VISIBLE;

        if (header != null) {
            header.setVisibility(visibilidad);
        }

        if (headerDivider != null) {
            headerDivider.setVisibility(visibilidad);
        }

        if (menu != null) {
            menu.setVisibility(visibilidad);
        }

        if (menuDivider != null) {
            menuDivider.setVisibility(visibilidad);
        }

        WindowInsetsControllerCompat controller =
                WindowCompat.getInsetsController(
                        requireActivity().getWindow(),
                        requireActivity()
                                .getWindow()
                                .getDecorView()
                );

        if (fullscreen) {

            controller.hide(
                    WindowInsetsCompat.Type.systemBars()
            );

            controller.setSystemBarsBehavior(
                    WindowInsetsControllerCompat
                            .BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            );

        } else {

            controller.show(
                    WindowInsetsCompat.Type.systemBars()
            );
        }
    }

    private void establecerControlesHabilitados(
            boolean habilitados
    ) {

        btnPlay.setEnabled(habilitados);
        btnPausa.setEnabled(habilitados);
        btnRetroceder.setEnabled(habilitados);
        btnAvanzar.setEnabled(habilitados);
        seekVideo.setEnabled(habilitados);
        btnPantallaCompleta.setEnabled(habilitados);
    }

    private void actualizarProgreso() {

        if (videoView == null
                || !preparado) {
            return;
        }

        int actual =
                videoView.getCurrentPosition();

        int total =
                Math.max(
                        videoView.getDuration(),
                        0
                );

        posicionGuardada = actual;

        seekVideo.setProgress(actual);

        tvTiempoVideo.setText(
                getString(
                        R.string.video_tiempo_formato,
                        formatear(actual),
                        formatear(total)
                )
        );
    }

    private String formatear(
            int milisegundos
    ) {

        int totalSegundos =
                Math.max(
                        0,
                        milisegundos
                ) / 1000;

        return String.format(
                Locale.getDefault(),
                "%02d:%02d",
                totalSegundos / 60,
                totalSegundos % 60
        );
    }

    @Override
    public void onResume() {

        super.onResume();

        handler.removeCallbacks(actualizador);
        handler.post(actualizador);

        if (videoView != null
                && preparado
                && reproduccionDeseada
                && !videoView.isPlaying()) {

            videoView.start();

            if (tvEstadoVideo != null) {
                tvEstadoVideo.setText(
                        R.string.video_estado_reproduciendo
                );
            }
        }
    }

    @Override
    public void onPause() {

        handler.removeCallbacks(actualizador);

        if (videoView != null
                && preparado) {

            posicionGuardada =
                    videoView.getCurrentPosition();

            if (videoView.isPlaying()) {
                videoView.pause();
            }
        }

        super.onPause();
    }

    @Override
    public void onSaveInstanceState(
            @NonNull Bundle outState
    ) {

        if (videoView != null
                && preparado) {
            posicionGuardada =
                    videoView.getCurrentPosition();
        }

        outState.putInt(
                KEY_POSICION,
                posicionGuardada
        );

        outState.putBoolean(
                KEY_REPRODUCIENDO,
                reproduccionDeseada
        );

        outState.putBoolean(
                KEY_FULLSCREEN,
                pantallaCompleta
        );

        super.onSaveInstanceState(outState);
    }

    @Override
    public void onDestroyView() {

        handler.removeCallbacks(actualizador);

        if (isAdded()) {
            aplicarChromeActividad(false);
        }

        pantallaCompleta = false;

        if (videoView != null) {
            videoView.stopPlayback();
        }

        videoFrame = null;
        fullscreenVideoContainer = null;
        scrollVideoNormal = null;

        videoView = null;
        progressVideo = null;
        seekVideo = null;
        tvTiempoVideo = null;
        tvEstadoVideo = null;

        btnRetroceder = null;
        btnPlay = null;
        btnPausa = null;
        btnAvanzar = null;
        btnPantallaCompleta = null;

        padreOriginalVideoFrame = null;
        parametrosOriginalesVideoFrame = null;

        super.onDestroyView();
    }
}
