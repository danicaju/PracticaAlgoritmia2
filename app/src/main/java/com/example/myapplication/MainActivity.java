package com.example.myapplication;

import android.content.pm.ActivityInfo;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.os.Bundle;
import android.text.method.ScrollingMovementMethod;
import android.view.SurfaceView;
import android.view.View;
import android.widget.ImageButton;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import java.util.Iterator;

public class MainActivity extends AppCompatActivity {

    private static final boolean ESTAT_ATURADA = false;
    private static final boolean ESTAT_JUGANT = true;
    private boolean estatJoc = ESTAT_ATURADA;

    private ImageButton btnNouJoc, btnConnectar, btnAturar, btnPista;
    private SurfaceView surfaceJugador, surfaceRival;

    private UnsortedArraySet<View> conjuntPistes;

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

        // Fem desplaçables els textos de les pistes
        TextView textPistesJugador = findViewById(R.id.text_pistes_jugador);
        TextView textPistesRival = findViewById(R.id.text_pistes_rival);
        textPistesJugador.setMovementMethod(new ScrollingMovementMethod());
        textPistesRival.setMovementMethod(new ScrollingMovementMethod());

        // Inicialitzem el conjunt
        conjuntPistes = new UnsortedArraySet<>(8);

        // Afegim els elements de la interfície al conjunt
        conjuntPistes.add(findViewById(R.id.layout_pistes));
        conjuntPistes.add(findViewById(R.id.btn_tancar_pistes));
        conjuntPistes.add(findViewById(R.id.titol_pistes_jugador));
        conjuntPistes.add(findViewById(R.id.titol_pistes_rival));
        conjuntPistes.add(textPistesJugador);
        conjuntPistes.add(textPistesRival);
        conjuntPistes.add(findViewById(R.id.text_percentatge_jugador));
        conjuntPistes.add(findViewById(R.id.text_percentatge_rival));

        // Configurar el botó Pista per mostrar el panell
        btnPista.setOnClickListener(v -> {
            // Aquí en un futur es generarà el text de les pistes.
            // De moment només mostrem el conjunt.
            canviarVisibilitatPistes(View.VISIBLE);
        });

        // Configurar el botó "X" per amagar el panell
        ImageButton btnTancarPistes = findViewById(R.id.btn_tancar_pistes);
        btnTancarPistes.setOnClickListener(v -> canviarVisibilitatPistes(View.GONE));
    }

    //Mètode que utilitza l'ITERADOR per recórrer el conjunt i mostrar/amagar
    private void canviarVisibilitatPistes(int visibilitat) {
        Iterator<View> iterador = conjuntPistes.iterator();

        while (iterador.hasNext()) {
            View element = iterador.next();
            if (element != null) {
                element.setVisibility(visibilitat);
            }
        }
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
            pintaGraelles(c, surfaceRival);
    }

    // Métode per a repintar la cuadrícula amb casella seleccionada
    public void pintaGraelles(Casella c, SurfaceView surface) {
        if (surface.getHolder().getSurface().isValid()) {
            int amplada = surface.getWidth();
            int alt = surface.getHeight();
            Canvas canvas = surface.getHolder().lockCanvas();

            if (canvas != null) {
                canvas.drawColor(Color.parseColor("#D0E8E8"));

                Paint p = new Paint();
                p.setColor(Color.parseColor("#90C0C0"));
                p.setStrokeWidth(3);

                float casellaAmplada = (float) amplada / 10;
                float casellaAlt = (float) alt / 10;

                for (int i = 1; i < 10; i++) {
                    canvas.drawLine(casellaAmplada * i, 0, casellaAmplada * i, alt, p);
                    canvas.drawLine(0, casellaAlt * i, amplada, casellaAlt * i, p);
                }

                if (c != null) {
                    Paint pSeleccio = new Paint();
                    pSeleccio.setAntiAlias(true);
                    pSeleccio.setStyle(Paint.Style.FILL);
                    pSeleccio.setColor(Color.RED);

                    float esquerra = c.getCoordenadaX() * casellaAmplada;
                    float dalt = c.getCoordenadaY() * casellaAlt;
                    float dreta = esquerra + casellaAmplada;
                    float baix = dalt + casellaAlt;
                    float radiEsquines = 15f;

                    canvas.drawRoundRect(esquerra + 4, dalt + 4, dreta - 4, baix - 4, radiEsquines, radiEsquines, pSeleccio);
                }
                surface.getHolder().unlockCanvasAndPost(canvas);
            }
        }
    }
}