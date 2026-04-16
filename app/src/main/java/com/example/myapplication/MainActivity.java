package com.example.myapplication;

import android.content.pm.ActivityInfo;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.os.Bundle;
import android.text.method.ScrollingMovementMethod;
import android.view.MotionEvent;
import android.view.SurfaceView;
import android.widget.ImageButton;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;

public class MainActivity extends AppCompatActivity {

    private static final boolean ESTAT_ATURADA = false;
    private static final boolean ESTAT_JUGANT = true;
    private boolean estatJoc = ESTAT_ATURADA;

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

        // Afegir listeners per a cada botó
        btnNouJoc.setOnClickListener(v -> actualitzarEstatBotons(ESTAT_JUGANT));
        btnConnectar.setOnClickListener(v -> actualitzarEstatBotons(ESTAT_JUGANT));
        btnAturar.setOnClickListener(v -> actualitzarEstatBotons(ESTAT_ATURADA));

        // Inicializar SurfaceViews
        surfaceJugador = findViewById(R.id.surface_jugador);
        surfaceRival = findViewById(R.id.surface_rival);

        // Retrasar el dibujado hasta que las vistas estén creadas
        surfaceJugador.post(() -> pintarGraella(surfaceJugador));
        surfaceRival.post(() -> pintarGraella(surfaceRival));
    }

    private void actualitzarEstatBotons(boolean nouEstat) {
        estatJoc = nouEstat;
        if (estatJoc == ESTAT_ATURADA) {
            btnNouJoc.setEnabled(true);
            btnNouJoc.setAlpha(1.0f);
            btnConnectar.setEnabled(true);
            btnConnectar.setAlpha(1.0f);

            btnAturar.setEnabled(false);
            btnAturar.setAlpha(0.5f);
            btnPista.setEnabled(false);
            btnPista.setAlpha(0.5f);

        } else if (estatJoc == ESTAT_JUGANT) {
            btnNouJoc.setEnabled(false);
            btnNouJoc.setAlpha(0.5f);
            btnConnectar.setEnabled(false);
            btnConnectar.setAlpha(0.5f);

            btnAturar.setEnabled(true);
            btnAturar.setAlpha(1.0f);
            btnPista.setEnabled(true);
            btnPista.setAlpha(1.0f);
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

    @Override
    public boolean onTouchEvent(android.view.MotionEvent event) {
        // Només actualitzem quan s'està jugant
        if (estatJoc == ESTAT_JUGANT) {
            // Només ens interessa quan l'usuari aixeca el dit
            if (event.getAction() == android.view.MotionEvent.ACTION_UP) {
                float x = event.getX();
                float y = event.getY();

                // Comprovem si hem tocat dins els límits del surfaceRival
                if (x >= surfaceRival.getX() && x < surfaceRival.getX() + surfaceRival.getWidth() &&
                        y >= surfaceRival.getY() && y < surfaceRival.getY() + surfaceRival.getHeight()) {

                    // Obtenim la coordenada "local" respecte del surfaceview
                    float localX = x - surfaceRival.getX();
                    float localY = y - surfaceRival.getY();

                    // Calculem la casella (i, j)
                    Casella c = getCasella(localX, localY, surfaceRival);
                    //
                    processarJugada(c);
                }
            }
        }
            return super.onTouchEvent(event);
        }

        // Métode per convertir coordenades de píxels a índexos de la quadrícula
        private Casella getCasella(float x, float y, SurfaceView s) {
            float casellaAmplada = (float) s.getWidth() / 10;
            float casellaAlt = (float) s.getHeight() / 10;

            int i = (int) (x / casellaAmplada);
            int j = (int) (y / casellaAlt);

            return new Casella(i, j);
        }

        private void processarJugada(Casella c) {
            // Actualitzar el TextView de la darrera jugada
            TextView tvDarreraJugadaTeva = findViewById(R.id.text_darrera_jugada_teva);
            tvDarreraJugadaTeva.setText("Darrera jugada teva: Casella " + c.getCoordenadaX() + ", " + c.getCoordenadaY());

            // Afegir el text l'historial de missatges
            TextView tvMissatges = findViewById(R.id.textViewMissatges);
            String textActual = tvMissatges.getText().toString();
            tvMissatges.setText(textActual + "\nHas seleccionat la casella (" + c.getCoordenadaX() + "," + c.getCoordenadaY() + ")");

            // Fer scroll automàtic
            tvMissatges.post(() -> {
                android.text.Layout layout = tvMissatges.getLayout();
                if (layout != null) {
                    int scrollAmount = layout.getLineTop(tvMissatges.getLineCount()) - tvMissatges.getHeight();
                    if (scrollAmount > 0) {
                        tvMissatges.scrollTo(0, scrollAmount);
                    } else {
                        tvMissatges.scrollTo(0, 0);
                    }
                }
            });

            // Repintar el taulell de joc amb la nova casella seleccionada
            pintaGraelles(c);
    }

    // Métode per a repintar la cuadrícula amb casella seleccionada
    public void pintaGraelles(Casella c) {
        if (surfaceRival.getHolder().getSurface().isValid()) {
            int amplada = surfaceRival.getWidth();
            int alt = surfaceRival.getHeight();
            Canvas canvas = surfaceRival.getHolder().lockCanvas();

            if (canvas != null) {
                // Netejem el fons
                canvas.drawColor(Color.parseColor("#D0E8E8"));

                // Preparem pincell
                Paint p = new Paint();
                p.setColor(Color.parseColor("#90C0C0"));
                p.setStrokeWidth(3);

                float casellaAmplada = (float) amplada / 10;
                float casellaAlt = (float) alt / 10;

                // Dibuixar la quadrícula
                for (int i = 1; i < 10; i++) {
                    canvas.drawLine(casellaAmplada * i, 0, casellaAmplada * i, alt, p);
                    canvas.drawLine(0, casellaAlt * i, amplada, casellaAlt * i, p);
                }

                // Si tenim una casella pintem de vermell
                if (c != null) {
                    Paint pSeleccio = new Paint();
                    pSeleccio.setAntiAlias(true); // Perquè les boreres es vegin suaus
                    pSeleccio.setStyle(Paint.Style.FILL);
                    pSeleccio.setColor(Color.RED);
                    pSeleccio.setStrokeWidth(6); // Grosor de la línia del contorn

                    // Calculem les coordenades del quadrat a pintar
                    float esquerra = c.getCoordenadaX() * casellaAmplada;
                    float dalt = c.getCoordenadaY() * casellaAlt;
                    float dreta = esquerra + casellaAmplada;
                    float baix = dalt + casellaAlt;

                    // Definim el radi de redondeig de les esquines
                    float radiEsquines = 15f;

                    canvas.drawRoundRect(esquerra, dalt, dreta, baix, radiEsquines, radiEsquines, pSeleccio);
                }
            }
            surfaceRival.getHolder().unlockCanvasAndPost(canvas);
        }
    }
}