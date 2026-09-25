package com.angelrincon.exploraquindio.fragments;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;
import androidx.fragment.app.Fragment;

import com.angelrincon.exploraquindio.R;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Fragmento del recomendador turístico de Explora Quindío.
 *
 * Permite al usuario seleccionar una o varias preferencias turísticas
 * (Naturaleza, Cultura, Café, Aventura, Descanso) y obtener un destino
 * recomendado del Quindío ordenado por coincidencia con dichas preferencias.
 *
 * Funcionalidades principales:
 *
 * Cálculo de un ranking de destinos por puntaje de coincidencia.
 * Navegación cíclica por el ranking mediante "Otra alternativa".
 * Invalidación automática de la recomendación al cambiar preferencias.
 * Limpieza completa de selecciones y resultado.
 * Persistencia del índice de recomendación ante rotaciones de pantalla.
 *
 *
 * El layout asociado es {@code R.layout.fragment_buttons}.
 */
public class ButtonsFragment extends Fragment {

    /** Clave para guardar y restaurar el índice del ranking en el Bundle. */
    private static final String KEY_INDICE = "indice_recomendacion";

    // ── Modelo de datos ──────────────────────────────────────────────────────

    /**
     * Categorías turísticas disponibles como preferencias del usuario.
     * Cada destino puede pertenecer a una o varias de estas categorías.
     */
    private enum Categoria {
        NATURALEZA,
        CULTURA,
        CAFE,
        AVENTURA,
        DESCANSO
    }

    /**
     * Representa un destino turístico del Quindío con nombre, descripción
     * y el conjunto de categorías a las que pertenece.
     */
    private static class Destino {

        /** Recurso de string con el nombre del destino. */
        @StringRes
        final int nombreRes;

        /** Recurso de string con la descripción del destino. */
        @StringRes
        final int descripcionRes;

        /** Conjunto de categorías turísticas que caracterizan al destino. */
        final Set<Categoria> categorias;

        /**
         * Crea un nuevo destino turístico.
         *
         * @param nombreRes      Recurso de string con el nombre.
         * @param descripcionRes Recurso de string con la descripción.
         * @param categorias     Categorías a las que pertenece el destino.
         */
        Destino(
                @StringRes int nombreRes,
                @StringRes int descripcionRes,
                Categoria... categorias
        ) {
            this.nombreRes      = nombreRes;
            this.descripcionRes = descripcionRes;
            this.categorias     = new HashSet<>(Arrays.asList(categorias));
        }

        /**
         * Calcula cuántas de las categorías seleccionadas por el usuario
         * coinciden con las categorías de este destino.
         *
         * @param seleccionadas Conjunto de categorías elegidas por el usuario.
         * @return Número de coincidencias (entre 0 y el total de categorías
         *         del destino).
         */
        int puntaje(Set<Categoria> seleccionadas) {
            int coincidencias = 0;
            for (Categoria categoria : seleccionadas) {
                if (categorias.contains(categoria)) {
                    coincidencias++;
                }
            }
            return coincidencias;
        }
    }

    /**
     * Catálogo estático de destinos turísticos del Quindío.
     * El orden aquí definido se preserva entre destinos con igual puntaje,
     * ya que {@link Collections#sort} es un algoritmo de ordenamiento estable.
     */
    private static final List<Destino> DESTINOS = Arrays.asList(

            new Destino(
                    R.string.buttons_destino_salento_nombre,
                    R.string.buttons_destino_salento_descripcion,
                    Categoria.CULTURA, Categoria.NATURALEZA, Categoria.AVENTURA
            ),
            new Destino(
                    R.string.buttons_destino_cocora_nombre,
                    R.string.buttons_destino_cocora_descripcion,
                    Categoria.NATURALEZA, Categoria.AVENTURA
            ),
            new Destino(
                    R.string.buttons_destino_filandia_nombre,
                    R.string.buttons_destino_filandia_descripcion,
                    Categoria.CULTURA
            ),
            new Destino(
                    R.string.buttons_destino_circasia_nombre,
                    R.string.buttons_destino_circasia_descripcion,
                    Categoria.CAFE, Categoria.CULTURA
            ),
            new Destino(
                    R.string.buttons_destino_buenavista_nombre,
                    R.string.buttons_destino_buenavista_descripcion,
                    Categoria.CAFE, Categoria.DESCANSO
            ),
            new Destino(
                    R.string.buttons_destino_pijao_nombre,
                    R.string.buttons_destino_pijao_descripcion,
                    Categoria.DESCANSO
            )
    );

    // ── Vistas ──────────────────────────────────────────────────────────────

    /** CheckBox para la preferencia de Naturaleza. */
    private CheckBox cbNaturaleza;

    /** CheckBox para la preferencia de Cultura. */
    private CheckBox cbCultura;

    /** CheckBox para la preferencia de Café. */
    private CheckBox cbCafe;

    /** CheckBox para la preferencia de Aventura. */
    private CheckBox cbAventura;

    /** CheckBox para la preferencia de Descanso. */
    private CheckBox cbDescanso;

    /** Botón que calcula y muestra la primera recomendación del ranking. */
    private Button btnRecomendar;

    /** Botón que limpia todas las selecciones y reinicia el resultado. */
    private Button btnLimpiar;

    /**
     * Botón que avanza al siguiente destino del ranking.
     * Se habilita solo cuando el ranking tiene más de un resultado.
     */
    private Button btnOtraAlternativa;

    /** Muestra el nombre del destino recomendado. */
    private TextView tvResultadoTitulo;

    /** Muestra la descripción del destino recomendado. */
    private TextView tvResultadoDescripcion;

    /** Muestra el contador "Recomendación X de Y" del ranking. */
    private TextView tvResultadoAlternativa;

    // ── Estado interno ───────────────────────────────────────────────────────

    /**
     * Lista de destinos filtrados y ordenados por puntaje
     * según las preferencias actuales del usuario.
     */
    private List<Destino> ranking = new ArrayList<>();

    /**
     * Índice del destino actualmente mostrado dentro de {@link #ranking}.
     * Vale {@code -1} cuando no hay recomendación activa.
     */
    private int indiceActual = -1;

    /**
     * Bandera que indica que {@link #limpiarSeleccion()} está modificando
     * los CheckBoxes programáticamente, evitando que el listener dispare
     * {@link #invalidarRecomendacionAnterior()} innecesariamente.
     */
    private boolean actualizandoSeleccion = false;

    // ── Constructor ──────────────────────────────────────────────────────────

    /**
     * Constructor sin argumentos requerido por el sistema de fragmentos.
     * Asocia el layout {@code R.layout.fragment_buttons} al fragmento.
     */
    public ButtonsFragment() {
        super(R.layout.fragment_buttons);
    }

    // ── Ciclo de vida ────────────────────────────────────────────────────────

    /**
     * Se ejecuta justo después de que la vista del fragmento es creada.
     * Enlaza las vistas, configura los eventos y deshabilita el botón
     * "Otra alternativa" hasta que exista un ranking calculado.
     *
     * @param view               Vista raíz inflada desde {@code R.layout.fragment_buttons}.
     * @param savedInstanceState Estado previo del fragmento, o {@code null}.
     */
    @Override
    public void onViewCreated(
            @NonNull View view,
            @Nullable Bundle savedInstanceState
    ) {
        super.onViewCreated(view, savedInstanceState);

        vincularVistas(view);
        configurarEventos();

        // El botón de alternativa solo se activa tras calcular un ranking
        btnOtraAlternativa.setEnabled(false);
    }

    // ── Inicialización ───────────────────────────────────────────────────────

    /**
     * Enlaza cada campo de la clase con su vista correspondiente en el layout.
     *
     * @param view Vista raíz del fragmento.
     */
    private void vincularVistas(@NonNull View view) {
        cbNaturaleza         = view.findViewById(R.id.cbNaturaleza);
        cbCultura            = view.findViewById(R.id.cbCultura);
        cbCafe               = view.findViewById(R.id.cbCafe);
        cbAventura           = view.findViewById(R.id.cbAventura);
        cbDescanso           = view.findViewById(R.id.cbDescanso);

        btnRecomendar        = view.findViewById(R.id.btnRecomendar);
        btnLimpiar           = view.findViewById(R.id.btnLimpiar);
        btnOtraAlternativa   = view.findViewById(R.id.btnOtraAlternativa);

        tvResultadoTitulo       = view.findViewById(R.id.tvResultadoTitulo);
        tvResultadoDescripcion  = view.findViewById(R.id.tvResultadoDescripcion);
        tvResultadoAlternativa  = view.findViewById(R.id.tvResultadoAlternativa);
    }

    /**
     * Registra los listeners de los tres botones y de los cinco CheckBoxes.
     *
     * El listener compartido de los CheckBoxes llama a
     * {@link #invalidarRecomendacionAnterior()} cuando el usuario cambia
     * manualmente una preferencia, indicando que la recomendación anterior
     * ya no es válida.
     */
    private void configurarEventos() {
        btnRecomendar.setOnClickListener(v -> recomendarDestino());
        btnOtraAlternativa.setOnClickListener(v -> otraAlternativa());
        btnLimpiar.setOnClickListener(v -> limpiarSeleccion());

        // Listener compartido para los cinco CheckBoxes
        CompoundButton.OnCheckedChangeListener listenerPreferencias =
                (buttonView, isChecked) -> {
                    // Solo invalida si el cambio lo hizo el usuario,
                    // no una actualización programática desde limpiarSeleccion()
                    if (!actualizandoSeleccion) {
                        invalidarRecomendacionAnterior();
                    }
                };

        cbNaturaleza.setOnCheckedChangeListener(listenerPreferencias);
        cbCultura.setOnCheckedChangeListener(listenerPreferencias);
        cbCafe.setOnCheckedChangeListener(listenerPreferencias);
        cbAventura.setOnCheckedChangeListener(listenerPreferencias);
        cbDescanso.setOnCheckedChangeListener(listenerPreferencias);
    }

    // ── Lógica de recomendación ──────────────────────────────────────────────

    /**
     * Calcula el ranking de destinos según las preferencias marcadas y
     * muestra el primero de la lista. Si no hay preferencias seleccionadas
     * o ningún destino coincide, muestra el mensaje correspondiente.
     */
    private void recomendarDestino() {
        Set<Categoria> preferencias = obtenerPreferencias();

        if (preferencias.isEmpty()) {
            ranking       = new ArrayList<>();
            indiceActual  = -1;

            tvResultadoTitulo.setText(R.string.buttons_sin_preferencias_titulo);
            tvResultadoDescripcion.setText(R.string.buttons_sin_preferencias_mensaje);
            tvResultadoAlternativa.setVisibility(View.GONE);
            btnOtraAlternativa.setEnabled(false);

            Toast.makeText(
                    requireContext(),
                    R.string.buttons_toast_selecciona_preferencia,
                    Toast.LENGTH_SHORT
            ).show();

            return;
        }

        ranking = calcularRanking(preferencias);

        if (ranking.isEmpty()) {
            indiceActual = -1;

            tvResultadoTitulo.setText(R.string.buttons_sin_resultados_titulo);
            tvResultadoDescripcion.setText(R.string.buttons_sin_resultados_mensaje);
            tvResultadoAlternativa.setVisibility(View.GONE);
            btnOtraAlternativa.setEnabled(false);

            return;
        }

        indiceActual = 0;
        mostrarDestinoActual();
    }

    /**
     * Avanza al siguiente destino en el ranking de forma cíclica.
     * Si el ranking tiene un solo resultado, informa al usuario y
     * deshabilita el botón.
     */
    private void otraAlternativa() {
        if (indiceActual < 0 || ranking.isEmpty()) {
            Toast.makeText(
                    requireContext(),
                    R.string.buttons_toast_recomendar_primero,
                    Toast.LENGTH_SHORT
            ).show();
            return;
        }

        if (ranking.size() <= 1) {
            Toast.makeText(
                    requireContext(),