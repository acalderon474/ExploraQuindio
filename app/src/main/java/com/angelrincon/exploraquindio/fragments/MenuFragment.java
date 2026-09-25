package com.angelrincon.exploraquindio.fragments;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.angelrincon.exploraquindio.R;

public class MenuFragment extends Fragment {

    private Button btnPerfil;
    private Button btnFotos;
    private Button btnVideo;
    private Button btnWeb;
    private Button btnBotones;

    public MenuFragment() {
        super(R.layout.fragment_menu);
    }

    @Override
    public void onViewCreated(
            @NonNull View view,
            @Nullable Bundle savedInstanceState
    ) {
        super.onViewCreated(view, savedInstanceState);

        btnPerfil = view.findViewById(R.id.btnPerfil);
        btnFotos = view.findViewById(R.id.btnFotos);
        btnVideo = view.findViewById(R.id.btnVideo);
        btnWeb = view.findViewById(R.id.btnWeb);
        btnBotones = view.findViewById(R.id.btnBotones);

        // Perfil aparece seleccionado al iniciar la aplicación.
        actualizarOpcionSeleccionada(btnPerfil);

        btnPerfil.setOnClickListener(v -> {
            actualizarOpcionSeleccionada(btnPerfil);
            cambiarFragment(new ProfileFragment());
        });

        btnFotos.setOnClickListener(v -> {
            actualizarOpcionSeleccionada(btnFotos);
            cambiarFragment(new PhotosFragment());
        });
    }

    private void actualizarOpcionSeleccionada(Button seleccionado) {

        Button[] opciones = {
                btnPerfil,
                btnFotos,
                btnVideo,
                btnWeb,
                btnBotones
        };

        for (Button opcion : opciones) {
            opcion.setSelected(opcion == seleccionado);
        }
    }

    private void cambiarFragment(Fragment fragment) {
        getParentFragmentManager()
                .beginTransaction()
                .replace(R.id.contentContainer, fragment)
                .commit();
    }
}

