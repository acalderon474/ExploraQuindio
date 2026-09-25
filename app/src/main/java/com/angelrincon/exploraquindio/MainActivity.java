package com.angelrincon.exploraquindio;

import android.os.Bundle;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

/**
 * Actividad principal y única de Explora Quindío.
 *
 * Actúa como contenedor raíz de toda la interfaz. Su responsabilidad
 * es mínima e intencional: inflar {@code activity_main.xml} y configurar
 * el modo Edge-to-Edge para que el contenido se extienda detrás de las
 * barras del sistema.
 *
 * La navegación entre secciones y la lógica de la interfaz residen
 * en {@code MenuFragment} y en los fragmentos de contenido
 * (ProfileFragment, PhotosFragment, VideoFragment, WebFragment,
 * ButtonsFragment).
 */
public class MainActivity extends AppCompatActivity {

    /**
     * Punto de entrada de la actividad.
     *
     * Habilita Edge-to-Edge, infla el layout raíz y aplica los insets
     * de las barras del sistema como padding al contenedor principal,
     * evitando que el contenido quede tapado por la barra de estado
     * o la barra de navegación del dispositivo.
     *
     * @param savedInstanceState Estado previo de la actividad, o {@code null}
     *                           si se está creando por primera vez.
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Permite que el contenido se dibuje detrás de las barras del sistema
        EdgeToEdge.enable(this);

        // Infla la interfaz definida en activity_main.xml
        setContentView(R.layout.activity_main);

        // Aplica padding dinámico según las barras del sistema (status bar,
        // navigation bar) para que ningún elemento quede oculto bajo ellas
        ViewCompat.setOnApplyWindowInsetsListener(
                findViewById(R.id.main),
                (v, insets) -> {
                    Insets systemBars =
                            insets.getInsets(WindowInsetsCompat.Type.systemBars());

                    v.setPadding(
                            systemBars.left,
                            systemBars.top,
                            systemBars.right,
                            systemBars.bottom
                    );

                    return insets;
                }
        );
    }
}