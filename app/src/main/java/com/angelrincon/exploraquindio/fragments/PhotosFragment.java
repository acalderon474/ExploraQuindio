package com.angelrincon.exploraquindio.fragments;

import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.angelrincon.exploraquindio.R;
import com.google.android.material.card.MaterialCardView;

public class PhotosFragment extends Fragment {

    private MaterialCardView cardSalento;
    private MaterialCardView cardValleCocora;
    private MaterialCardView cardFilandia;
    private MaterialCardView cardCircasia;
    private MaterialCardView cardBuenavista;
    private MaterialCardView cardPijao;

    public PhotosFragment() {
        super(R.layout.fragment_photos);
    }

    @Override
    public void onViewCreated(
            @NonNull View view,
            @Nullable Bundle savedInstanceState
    ) {
        super.onViewCreated(view, savedInstanceState);

        cardSalento = view.findViewById(R.id.cardSalento);
        cardValleCocora = view.findViewById(R.id.cardValleCocora);
        cardFilandia = view.findViewById(R.id.cardFilandia);
        cardCircasia = view.findViewById(R.id.cardCircasia);
        cardBuenavista = view.findViewById(R.id.cardBuenavista);
        cardPijao = view.findViewById(R.id.cardPijao);

        configurarEventosFotos();
    }

    private void configurarEventosFotos() {

        cardSalento.setOnClickListener(v ->
                mostrarDestinoSeleccionado(
                        getString(R.string.destino_salento)
                )
        );

        cardValleCocora.setOnClickListener(v ->
                mostrarDestinoSeleccionado(
                        getString(R.string.destino_valle_cocora)
                )
        );

        cardFilandia.setOnClickListener(v ->
                mostrarDestinoSeleccionado(
                        getString(R.string.destino_filandia)
                )
        );

        cardCircasia.setOnClickListener(v ->
                mostrarDestinoSeleccionado(
                        getString(R.string.destino_circasia)
                )
        );

        cardBuenavista.setOnClickListener(v ->
                mostrarDestinoSeleccionado(
                        getString(R.string.destino_buenavista)
                )
        );

        cardPijao.setOnClickListener(v ->
                mostrarDestinoSeleccionado(
                        getString(R.string.destino_pijao)
                )
        );
    }

    private void mostrarDestinoSeleccionado(String destino) {
        String mensaje = getString(
                R.string.photos_destino_seleccionado,
                destino
        );

        Toast.makeText(
                requireContext(),
                mensaje,
                Toast.LENGTH_SHORT
        ).show();
    }
}