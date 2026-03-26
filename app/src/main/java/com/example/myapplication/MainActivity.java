package com.example.myapplication;

import android.content.pm.ActivityInfo;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.os.Bundle;
import android.text.method.ScrollingMovementMethod;
import android.view.SurfaceView;
import android.widget.ImageButton;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;

public class MainActivity extends AppCompatActivity {

    private static final int ESTAT_ATURADA = 0;
    private static final int ESTAT_JUGANT = 1;
    private int estatJoc = ESTAT_ATURADA;

    private ImageButton btnNouJoc, btnConnectar, btnAturar, btnPista;
    private SurfaceView surfaceJugador, surfaceRival;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Apaisado
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);

        // Scroll de mensajes
        TextView tvMissatges = findViewById(R.id.textViewMissatges);
        if (tvMissatges != null) {
            tvMissatges.setMovementMethod(new ScrollingMovementMethod());
        }

        // Inicializar botones
        btnNouJoc = findViewById(R.id.btn_nou_joc);
        btnConnectar = findViewById(R.id.btn_connectar);
        btnAturar = findViewById(R.id.btn_aturar);
        btnPista = findViewById(R.id.btn_pista);

        actualitzarEstatBotons(ESTAT_ATURADA);

        // Inicializar SurfaceViews
        surfaceJugador = findViewById(R.id.surface_jugador);
        surfaceRival = findViewById(R.id.surface_rival);

        // Retrasar el dibujado hasta que las vistas estén creadas
        surfaceJugador.post(() -> pintarGraella(surfaceJugador));
        surfaceRival.post(() -> pintarGraella(surfaceRival));
    }

    private void actualitzarEstatBotons(int nouEstat) {
        estatJoc = nouEstat;
        if (estatJoc == ESTAT_ATURADA) {
            btnNouJoc.setEnabled(true);
            btnConnectar.setEnabled(true);
            btnAturar.setEnabled(false);
            btnPista.setEnabled(false);
        } else if (estatJoc == ESTAT_JUGANT) {
            btnNouJoc.setEnabled(false);
            btnConnectar.setEnabled(false);
            btnAturar.setEnabled(true);
            btnPista.setEnabled(true);
        }
    }

    // Método para dibujar la cuadrícula 10x10
    private void pintarGraella(SurfaceView surface) {
        if (surface.getHolder().getSurface().isValid()) {

            // Obtenemos dimensiones
            int amplada = surface.getWidth();
            int alt = surface.getHeight();

            // Bloqueamos para dibujar
            Canvas canvas = surface.getHolder().lockCanvas();

            if (canvas != null) {
                // Pintar fondo (Azul mar muy clarito)
                canvas.drawColor(Color.parseColor("#D0E8E8"));

                // Preparar el pincel para las líneas
                Paint p = new Paint();
                p.setColor(Color.parseColor("#90C0C0")); // Color de la línea
                p.setStrokeWidth(3);

                // Calcular la separación entre líneas (10 casillas)
                float casellaAmplada = (float) amplada / 10;
                float casellaAlt = (float) alt / 10;

                // Dibujar 9 líneas verticales y 9 horizontales
                for (int i = 1; i < 10; i++) {
                    // Línea vertical: (xInicial, yInicial, xFinal, yFinal, pincel)
                    canvas.drawLine(casellaAmplada * i, 0, casellaAmplada * i, alt, p);
                    // Línea horizontal
                    canvas.drawLine(0, casellaAlt * i, amplada, casellaAlt * i, p);
                }

                // Desbloquear y mostrar
                surface.getHolder().unlockCanvasAndPost(canvas);
            }
        }
    }
}