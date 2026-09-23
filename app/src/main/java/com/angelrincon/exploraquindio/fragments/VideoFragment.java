package com.angelrincon.exploraquindio.fragments;

import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.VideoView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import java.util.Locale;

import com.angelrincon.exploraquindio.R;

/**
 * Sección "Video" (RF-06): reproduce un recorrido audiovisual con controles
 * de reproducir, pausar, avanzar y retroceder.
 *
 * El video se toma de un recurso LOCAL (RNF-04): app/src/main/res/raw/video_quindio.mp4
 * Si ese archivo aún no existe, se usa un video de prueba en línea (requiere INTERNET)
 * para que el fragment se pueda probar mientras se agrega el video definitivo.
 */
public class VideoFragment extends Fragment {

    private static final String TAG = "VideoFragment";
    private static final String VIDEO_RAW_NAME = "video_quindio";
    private static final String VIDEO_URL_RESPALDO =
            "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/BigBuckBunny.mp4";
    private static final int SALTO_MS = 10_000; // avanzar / retroceder 10 s
    private static final String KEY_POSICION = "posicion_video";

    private VideoView videoView;
    private ProgressBar progressVideo;
    private SeekBar seekVideo;
    private TextView tvTiempoVideo, tvEstadoVideo;
    private Button btnRetroceder, btnPlay, btnPausa, btnAvanzar;

    private final Handler handler = new Handler(Looper.getMainLooper());
    private final Runnable actualizador = new Runnable() {
        @Override
        public void run() {
            actualizarProgreso();
            handler.postDelayed(this, 500);
        }
    };

    private int posicionGuardada = 0;
    private boolean preparado = false;

    public VideoFragment() {
        // Constructor vacío requerido
    }

    public static VideoFragment newInstance() {
        return new VideoFragment();
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_video, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Vincular vistas XML con variables Java
        videoView = view.findViewById(R.id.videoView);
        progressVideo = view.findViewById(R.id.progressVideo);
        seekVideo = view.findViewById(R.id.seekVideo);
        tvTiempoVideo = view.findViewById(R.id.tvTiempoVideo);
        tvEstadoVideo = view.findViewById(R.id.tvEstadoVideo);
        btnRetroceder = view.findViewById(R.id.btnRetroceder);
        btnPlay = view.findViewById(R.id.btnPlay);
        btnPausa = view.findViewById(R.id.btnPausa);
        btnAvanzar = view.findViewById(R.id.btnAvanzar);

        if (savedInstanceState != null) {
            posicionGuardada = savedInstanceState.getInt(KEY_POSICION, 0);
        }

        configurarVideo();
        configurarControles();
        configurarSeekBar();
    }

    private void configurarVideo() {
        videoView.setOnPreparedListener(mp -> {
            preparado = true;
            progressVideo.setVisibility(View.GONE);
            seekVideo.setMax(videoView.getDuration());
            // seekTo(>0) hace que se vea el primer fotograma en lugar de pantalla negra
            videoView.seekTo(posicionGuardada > 0 ? posicionGuardada : 1);
            tvEstadoVideo.setText("Video listo para reproducir");
            actualizarProgreso();
        });

        videoView.setOnCompletionListener(mp -> {
            tvEstadoVideo.setText("Reproducción finalizada");
            actualizarProgreso();
        });

        videoView.setOnErrorListener((mp, what, extra) -> {
            preparado = false;
            progressVideo.setVisibility(View.GONE);
            tvEstadoVideo.setText("No se pudo reproducir el video");
            if (isAdded()) {
                Toast.makeText(requireContext(),
                        "Error al cargar el video. Verifica que exista res/raw/" + VIDEO_RAW_NAME + ".mp4",
                        Toast.LENGTH_LONG).show();
            }
            return true;
        });

        cargarVideo();
    }

    private void cargarVideo() {
        preparado = false;
        progressVideo.setVisibility(View.VISIBLE);
        tvEstadoVideo.setText("Cargando video...");

        String paquete = requireContext().getPackageName();
        int rawId = getResources().getIdentifier(VIDEO_RAW_NAME, "raw", paquete);

        Uri uri;
        if (rawId != 0) {
            uri = Uri.parse("android.resource://" + paquete + "/" + rawId);
        } else {
            Log.w(TAG, "No existe res/raw/" + VIDEO_RAW_NAME + ".mp4; se usa el video de respaldo en línea.");
            uri = Uri.parse(VIDEO_URL_RESPALDO);
        }
        videoView.setVideoURI(uri);
    }

    private void configurarControles() {
        btnPlay.setOnClickListener(v -> {
            if (!preparado) return;
            if (!videoView.isPlaying()) {
                videoView.start();
                tvEstadoVideo.setText("Reproduciendo");
            }
        });

        btnPausa.setOnClickListener(v -> {
            if (preparado && videoView.isPlaying()) {
                videoView.pause();
                tvEstadoVideo.setText("En pausa");
            }
        });

        btnRetroceder.setOnClickListener(v -> {
            if (!preparado) return;
            int nueva = Math.max(0, videoView.getCurrentPosition() - SALTO_MS);
            videoView.seekTo(nueva);
            tvEstadoVideo.setText("Retrocediendo 10 segundos");
            actualizarProgreso();
        });

        btnAvanzar.setOnClickListener(v -> {
            if (!preparado) return;
            int nueva = Math.min(videoView.getDuration(), videoView.getCurrentPosition() + SALTO_MS);
            videoView.seekTo(nueva);
            tvEstadoVideo.setText("Avanzando 10 segundos");
            actualizarProgreso();
        });
    }

    private void configurarSeekBar() {
        seekVideo.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (fromUser && preparado) {
                    videoView.seekTo(progress);
                    tvTiempoVideo.setText(formatear(progress) + " / " + formatear(videoView.getDuration()));
                }
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) { }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) { }
        });
    }

    private void actualizarProgreso() {
        if (videoView == null || !preparado) return;
        int actual = videoView.getCurrentPosition();
        int total = videoView.getDuration();
        seekVideo.setProgress(actual);
        tvTiempoVideo.setText(formatear(actual) + " / " + formatear(total));
    }

    private String formatear(int ms) {
        int totalSeg = Math.max(0, ms) / 1000;
        return String.format(Locale.getDefault(), "%02d:%02d", totalSeg / 60, totalSeg % 60);
    }

    @Override
    public void onResume() {
        super.onResume();
        handler.removeCallbacks(actualizador);
        handler.post(actualizador);
    }

    @Override
    public void onPause() {
        super.onPause();
        handler.removeCallbacks(actualizador);
        if (videoView != null && preparado) {
            posicionGuardada = videoView.getCurrentPosition();
            if (videoView.isPlaying()) {
                videoView.pause();
                tvEstadoVideo.setText("En pausa");
            }
        }
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        if (videoView != null && preparado) {
            posicionGuardada = videoView.getCurrentPosition();
        }
        outState.putInt(KEY_POSICION, posicionGuardada);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        handler.removeCallbacks(actualizador);
        if (videoView != null) {
            videoView.stopPlayback();
        }
        videoView = null;
    }
}
