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

    public static final int JUGADOR_PROPI = 0;
    public static final int JUGADOR_RIVAL = 1;
    private ImageButton btnNouJoc, btnConnectar, btnAturar, btnPista;
    private SurfaceView surfaceJugador, surfaceRival;

    private UnsortedArraySet<View> conjuntPistes;

    private UnsortedArrayMapping<Integer, UnsortedArrayMapping<Casella, Vaixell>> vaixells;

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

        // Inicializar botons
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

        // Dibuixem la graella i els vaixells
        surfaceJugador.post(() -> pintaGraelles(null, surfaceJugador));
        surfaceRival.post(() -> pintaGraelles(null, surfaceRival));

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

        // PART DELS VAIXELLS

        // Creem el mapping principal (capacitat 2 jugadors)
        vaixells = new UnsortedArrayMapping<>(2);

        // Fiquem els sub-mappings de 20 caselles per a cada jugador
        vaixells.put(JUGADOR_PROPI, new UnsortedArrayMapping<>(20));
        vaixells.put(JUGADOR_RIVAL, new UnsortedArrayMapping<>(20));

        // Generem tots els vaixells aleatòriament
        crearVaixells();
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
                    processarJugada(c);
                }
            }
        }
            return super.onTouchEvent(event);
        }

        // Mètode per convertir coordenades de píxels a índexos de la quadrícula
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

    // Mètode per pintar i repintar la graella i mostrar els vaixells
    public void pintaGraelles(Casella c, SurfaceView surface) {
        if (surface.getHolder().getSurface().isValid()) {
            int amplada = surface.getWidth();
            int alt = surface.getHeight();
            Canvas canvas = surface.getHolder().lockCanvas();

            if (canvas != null) {
                // Netejem el fons
                canvas.drawColor(Color.parseColor("#D0E8E8"));

                // Preparem pincell per la graella
                Paint p = new Paint();
                p.setColor(Color.parseColor("#90C0C0"));
                p.setStrokeWidth(3);

                float casellaAmplada = (float) amplada / 10;
                float casellaAlt = (float) alt / 10;

                // Dibuixar la quadrícula (Línies)
                for (int i = 1; i < 10; i++) {
                    canvas.drawLine(casellaAmplada * i, 0, casellaAmplada * i, alt, p);
                    canvas.drawLine(0, casellaAlt * i, amplada, casellaAlt * i, p);
                }

                // PINTAR VAIXELLS DEL JUGADOR
                if (surface == surfaceJugador && vaixells != null) {
                    UnsortedArrayMapping<Casella, Vaixell> mappingPropi = vaixells.get(JUGADOR_PROPI);

                    if (mappingPropi != null) {
                        Iterator<UnsortedArrayMapping<Casella, Vaixell>.Pair> iterador = mappingPropi.iterator();

                        while (iterador.hasNext()) {
                            UnsortedArrayMapping<Casella, Vaixell>.Pair parella = iterador.next();
                            Casella casellaVaixell = parella.getKey();
                            Vaixell vaixell = parella.getValue();

                            Paint pVaixell = new Paint();
                            pVaixell.setAntiAlias(true);
                            pVaixell.setStyle(Paint.Style.FILL);
                            pVaixell.setColor(vaixell.getColor());

                            float esquerra = casellaVaixell.getCoordenadaX() * casellaAmplada;
                            float dalt = casellaVaixell.getCoordenadaY() * casellaAlt;
                            float dreta = esquerra + casellaAmplada;
                            float baix = dalt + casellaAlt;
                            float radiEsquines = 15f;

                            canvas.drawRoundRect(esquerra + 4, dalt + 4, dreta - 4, baix - 4, radiEsquines, radiEsquines, pVaixell);
                        }
                    }
                }

                // PINTAR SELECCIÓ (Casella vermella al rival)
                if (c != null && surface == surfaceRival) {
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

                // Finalment, alliberem i mostrem (Això s'ha d'executar SEMPRE)
                surface.getHolder().unlockCanvasAndPost(canvas);
            }
        }
    }
    private void crearVaixells() {
        int[] configuracioFlota = {4, 3, 3, 2, 2, 2, 1, 1, 1, 1};
        int idVaixell = 0;

        for (int mida : configuracioFlota) {
            // Obtenim els mappings específics de l'estructura principal
            UnsortedArrayMapping<Casella, Vaixell> mappingPropi = vaixells.get(JUGADOR_PROPI);
            UnsortedArrayMapping<Casella, Vaixell> mappingRival = vaixells.get(JUGADOR_RIVAL);

            collocarVaixellAleatori(mida, idVaixell, JUGADOR_PROPI, mappingPropi);
            idVaixell++;

            collocarVaixellAleatori(mida, idVaixell, JUGADOR_RIVAL, mappingRival);
            idVaixell++;
        }
    }

    private void collocarVaixellAleatori(int mida, int id, int jugador, UnsortedArrayMapping<Casella, Vaixell> mapping) {
        boolean colocat = false;
        int color = getColorPerMida(mida); // Assignem un color segons la mida

        while (!colocat) {
            // Triem orientació (0 o 1) i coordenada inicial aleatòria
            int orientacio = (int) (Math.random() * 2);
            int iInici = (int) (Math.random() * 10);
            int jInici = (int) (Math.random() * 10);

            // Comprovem si la posició és vàlida
            if (esPosicioValida(iInici, jInici, mida, orientacio, mapping)) {

                // Creem l'objecte Vaixell
                Vaixell nouVaixell = new Vaixell(id, mida, orientacio, color, jugador);

                // Guardem TOTES les caselles del vaixell al mapping
                for (int k = 0; k < mida; k++) {
                    if (orientacio == Vaixell.HORITZONTAL) {
                        mapping.put(new Casella(iInici + k, jInici), nouVaixell);
                    } else {
                        mapping.put(new Casella(iInici, jInici + k), nouVaixell);
                    }
                }
                colocat = true; // Sortim del bucle while
            }
        }
    }

    private boolean esPosicioValida(int iInici, int jInici, int mida, int orientacio, UnsortedArrayMapping<Casella, Vaixell> mapping) {
        // Comprovar que no surt de la graella (0 a 9)
        if (orientacio == Vaixell.HORITZONTAL && iInici + mida > 10) return false;
        if (orientacio == Vaixell.VERTICAL && jInici + mida > 10) return false;

        // Comprovar col·lisions i veïns (horitzontal, vertical i diagonal)
        for (int k = 0; k < mida; k++) {
            // Calculem la coordenada (i, j) exacta de la part del vaixell que estem mirant
            int currentI = (orientacio == Vaixell.HORITZONTAL) ? iInici + k : iInici;
            int currentJ = (orientacio == Vaixell.VERTICAL) ? jInici + k : jInici;

            // Mirem la casella actual i les 8 caselles del voltant (di = -1, 0, 1 i dj = -1, 0, 1)
            for (int di = -1; di <= 1; di++) {
                for (int dj = -1; dj <= 1; dj++) {
                    int checkI = currentI + di;
                    int checkJ = currentJ + dj;

                    // Si la casella a mirar està dins el tauler
                    if (checkI >= 0 && checkI < 10 && checkJ >= 0 && checkJ < 10) {
                        //.i ja hi ha un vaixell al mapping, posició invàlida
                        if (mapping.get(new Casella(checkI, checkJ)) != null) {
                            return false;
                        }
                    }
                }
            }
        }
        return true; // Si ha superat totes les proves, la posició és fantàstica
    }

    private int getColorPerMida(int mida) {
        switch (mida) {
            case 4: return Color.parseColor("#A79AFF"); // Lila
            case 3: return Color.parseColor("#84b6f4"); // Blau
            case 2: return Color.parseColor("#77DD77"); // Verd
            default: return Color.parseColor("#FF7477"); // Vermell
        }
    }
}