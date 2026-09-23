package com.angelrincon.exploraquindio.fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import com.angelrincon.exploraquindio.R;

/**
 * Sección "Botones" (RF-08 y RF-09): recomendador turístico.
 * El usuario marca preferencias (naturaleza, cultura, café, aventura, descanso),
 * presiona "Recomendar destino" y recibe un destino. Puede limpiar la selección
 * o pedir otra alternativa. Los datos son ficticios/demostrativos (RNF-07).
 */
public class ButtonsFragment extends Fragment {

    private static final String KEY_INDICE = "indice_recomendacion";

    /** Destino demostrativo con las categorías con las que se relaciona. */
    private static class Destino {
        final String nombre;
        final String descripcion;
        final List<String> categorias;

        Destino(String nombre, String descripcion, String... categorias) {
            this.nombre = nombre;
            this.descripcion = descripcion;
            this.categorias = Arrays.asList(categorias);
        }

        int puntaje(List<String> seleccionadas) {
            int p = 0;
            for (String s : seleccionadas) {
                if (categorias.contains(s)) p++;
            }
            return p;
        }
    }

    private static final String NATURALEZA = "naturaleza";
    private static final String CULTURA = "cultura";
    private static final String CAFE = "cafe";
    private static final String AVENTURA = "aventura";
    private static final String DESCANSO = "descanso";

    private static final List<Destino> DESTINOS = Arrays.asList(
            new Destino("Salento",
                    "Una experiencia inspirada en calles coloridas, tradición local y paisajes de montaña.",
                    CULTURA, NATURALEZA, AVENTURA),
            new Destino("Valle de Cocora",
                    "Una alternativa ideal para disfrutar senderos y paisajes inspirados en la naturaleza.",
                    NATURALEZA, AVENTURA),
            new Destino("Filandia",
                    "Un pueblo con historia, arquitectura tradicional y miradores para conocer la cultura local.",
                    CULTURA),
            new Destino("Circasia",
                    "Un lugar para vivir la cultura cafetera, con paisajes cercanos y ambiente tradicional.",
                    CAFE, CULTURA),
            new Destino("Buenavista",
                    "Miradores tranquilos y fincas cafeteras para descansar y disfrutar del paisaje.",
                    CAFE, DESCANSO),
            new Destino("Pijao",
                    "Un municipio pausado, perfecto para desconectarse y descansar entre montañas.",
                    DESCANSO)
    );

    private CheckBox cbNaturaleza, cbCultura, cbCafe, cbAventura, cbDescanso;
    private Button btnRecomendar, btnLimpiar, btnOtraAlternativa;
    private TextView tvResultadoTitulo, tvResultadoDescripcion, tvResultadoAlternativa;

    /** Destinos recomendados (ordenados por afinidad) y posición del que se muestra. */
    private List<Destino> ranking = new ArrayList<>();
    private int indiceActual = -1;

    public ButtonsFragment() {
        // Constructor vacío requerido
    }

    public static ButtonsFragment newInstance() {
        return new ButtonsFragment();
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_buttons, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Vincular vistas XML con variables Java
        cbNaturaleza = view.findViewById(R.id.cbNaturaleza);
        cbCultura = view.findViewById(R.id.cbCultura);
        cbCafe = view.findViewById(R.id.cbCafe);
        cbAventura = view.findViewById(R.id.cbAventura);
        cbDescanso = view.findViewById(R.id.cbDescanso);
        btnRecomendar = view.findViewById(R.id.btnRecomendar);
        btnLimpiar = view.findViewById(R.id.btnLimpiar);
        btnOtraAlternativa = view.findViewById(R.id.btnOtraAlternativa);
        tvResultadoTitulo = view.findViewById(R.id.tvResultadoTitulo);
        tvResultadoDescripcion = view.findViewById(R.id.tvResultadoDescripcion);
        tvResultadoAlternativa = view.findViewById(R.id.tvResultadoAlternativa);

        configurarEventos();
    }

    @Override
    public void onViewStateRestored(@Nullable Bundle savedInstanceState) {
        super.onViewStateRestored(savedInstanceState);
        // Los CheckBox ya recuperaron su estado; se reconstruye el resultado mostrado
        if (savedInstanceState != null) {
            int indice = savedInstanceState.getInt(KEY_INDICE, -1);
            if (indice >= 0) {
                ranking = calcularRanking();
                if (indice < ranking.size()) {
                    indiceActual = indice;
                    mostrarDestinoActual();
                }
            }
        }
    }

    private void configurarEventos() {
        btnRecomendar.setOnClickListener(v -> recomendarDestino());
        btnOtraAlternativa.setOnClickListener(v -> otraAlternativa());
        btnLimpiar.setOnClickListener(v -> limpiarSeleccion());
    }

    /** RF-08: genera una recomendación según las preferencias marcadas. */
    private void recomendarDestino() {
        List<Destino> resultado = calcularRanking();

        if (resultado.isEmpty()) {
            ranking = new ArrayList<>();
            indiceActual = -1;
            tvResultadoTitulo.setText("Sin preferencias");
            tvResultadoDescripcion.setText("Marca al menos una preferencia para recibir una recomendación.");
            tvResultadoAlternativa.setVisibility(View.GONE);
            Toast.makeText(requireContext(), "Selecciona al menos una preferencia", Toast.LENGTH_SHORT).show();
            return;
        }

        ranking = resultado;
        indiceActual = 0;
        mostrarDestinoActual();
    }

    /** RF-09: muestra el siguiente destino más afín a las preferencias. */
    private void otraAlternativa() {
        if (indiceActual < 0 || ranking.isEmpty()) {
            Toast.makeText(requireContext(),
                    "Primero presiona «Recomendar destino»", Toast.LENGTH_SHORT).show();
            return;
        }
        if (ranking.size() == 1) {
            Toast.makeText(requireContext(),
                    "No hay otra alternativa para estas preferencias", Toast.LENGTH_SHORT).show();
            return;
        }
        indiceActual = (indiceActual + 1) % ranking.size();
        mostrarDestinoActual();
    }

    /** RF-09: limpia las preferencias y el resultado. */
    private void limpiarSeleccion() {
        cbNaturaleza.setChecked(false);
        cbCultura.setChecked(false);
        cbCafe.setChecked(false);
        cbAventura.setChecked(false);
        cbDescanso.setChecked(false);

        ranking = new ArrayList<>();
        indiceActual = -1;
        tvResultadoTitulo.setText("Tu recomendación");
        tvResultadoDescripcion.setText("Selecciona tus preferencias y presiona «Recomendar destino».");
        tvResultadoAlternativa.setVisibility(View.GONE);
    }

    private void mostrarDestinoActual() {
        Destino d = ranking.get(indiceActual);
        tvResultadoTitulo.setText(d.nombre);
        tvResultadoDescripcion.setText(d.descripcion);
        tvResultadoAlternativa.setText("Recomendación " + (indiceActual + 1) + " de " + ranking.size());
        tvResultadoAlternativa.setVisibility(View.VISIBLE);
    }

    private List<String> obtenerPreferencias() {
        List<String> seleccionadas = new ArrayList<>();
        if (cbNaturaleza.isChecked()) seleccionadas.add(NATURALEZA);
        if (cbCultura.isChecked()) seleccionadas.add(CULTURA);
        if (cbCafe.isChecked()) seleccionadas.add(CAFE);
        if (cbAventura.isChecked()) seleccionadas.add(AVENTURA);
        if (cbDescanso.isChecked()) seleccionadas.add(DESCANSO);
        return seleccionadas;
    }

    /** Destinos con al menos una coincidencia, ordenados de mayor a menor afinidad. */
    private List<Destino> calcularRanking() {
        final List<String> seleccionadas = obtenerPreferencias();
        List<Destino> lista = new ArrayList<>();
        for (Destino d : DESTINOS) {
            if (d.puntaje(seleccionadas) > 0) lista.add(d);
        }
        // sort es estable: a igual puntaje se conserva el orden de DESTINOS
        Collections.sort(lista, (a, b) -> Integer.compare(b.puntaje(seleccionadas), a.puntaje(seleccionadas)));
        return lista;
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putInt(KEY_INDICE, indiceActual);
    }
}
