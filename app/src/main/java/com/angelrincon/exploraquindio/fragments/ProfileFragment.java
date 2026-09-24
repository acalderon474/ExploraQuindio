package com.angelrincon.exploraquindio.fragments;

import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.angelrincon.exploraquindio.R;

public class ProfileFragment extends Fragment {

    private ImageView imgProfile;
    private TextView txtProfileName;
    private TextView txtProfileRole;
    private TextView txtFormacionContenido;
    private TextView txtExperienciaContenido;
    private TextView txtEspecialidadesContenido;

    public ProfileFragment() {
        super(R.layout.fragment_profile);
    }

    @Override
    public void onViewCreated(
            @NonNull View view,
            @Nullable Bundle savedInstanceState
    ) {
        super.onViewCreated(view, savedInstanceState);

        imgProfile = view.findViewById(R.id.imgProfile);
        txtProfileName = view.findViewById(R.id.txtProfileName);
        txtProfileRole = view.findViewById(R.id.txtProfileRole);
        txtFormacionContenido = view.findViewById(R.id.txtFormacionContenido);
        txtExperienciaContenido = view.findViewById(R.id.txtExperienciaContenido);
        txtEspecialidadesContenido =
                view.findViewById(R.id.txtEspecialidadesContenido);

        configurarDatosPerfil();
    }

    private void configurarDatosPerfil() {
        imgProfile.setImageResource(R.mipmap.ic_launcher);

        txtProfileName.setText(R.string.profile_nombre);
        txtProfileRole.setText(R.string.profile_rol);
        txtFormacionContenido.setText(R.string.profile_formacion_contenido);
        txtExperienciaContenido.setText(R.string.profile_experiencia_contenido);
        txtEspecialidadesContenido.setText(
                R.string.profile_especialidades_contenido
        );
    }
}