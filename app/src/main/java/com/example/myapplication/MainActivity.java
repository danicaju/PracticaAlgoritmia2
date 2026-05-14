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

    public enum EstatJoc {ATURAT, JUGANT, EN_ESPERA, ACABAT}
    private EstatJoc estatJoc = EstatJoc.ATURAT; // Iniciem aturats
    private int tornActual; // 0 = JUGADOR_PROPI, 1 = JUGADOR_RIVAL
    public static final int JUGADOR_PROPI = 0;
    public static final int JUGADOR_RIVAL = 1;
    private ImageButton btnNouJoc, btnConnectar, btnAturar, btnPista;
    private SurfaceView surfaceJugador, surfaceRival;

    private UnsortedArraySet<View> conjuntPistes;
    private UnsortedArrayMapping<Integer, UnsortedArrayMapping<Casella, Vaixell>> vaixells;
    private UnsortedArrayMapping<Integer, UnsortedArraySet<Casella>> casellesDestapades;
    private UnsortedArrayMapping<Integer, UnsortedArrayMapping<Casella, Vaixell>> casellesEnfonsades;
    private UnsortedArraySet<Casella> objectiusRobot;

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

        // Assegurem que l'estat inicial és ATURAT i corregim els listeners
        actualitzarEstatBotons(EstatJoc.ATURAT);
        btnNouJoc.setOnClickListener(v -> iniciarNouJoc());
        btnConnectar.setOnClickListener(v -> actualitzarEstatBotons(EstatJoc.JUGANT));
        btnAturar.setOnClickListener(v -> actualitzarEstatBotons(EstatJoc.ATURAT));

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

        // Inicialitzem el conjunt de pistes
        conjuntPistes = new UnsortedArraySet<>(8);

        // Afegim els elements de pistes al conjunt
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

        // Configurar el botó per amagar el panell
        ImageButton btnTancarPistes = findViewById(R.id.btn_tancar_pistes);
        btnTancarPistes.setOnClickListener(v -> canviarVisibilitatPistes(View.GONE));

        // Assegurem que l'estat inicial és ATURAT i corregim els listeners
        actualitzarEstatBotons(EstatJoc.ATURAT);
        btnNouJoc.setOnClickListener(v -> iniciarNouJoc());
        btnConnectar.setOnClickListener(v -> actualitzarEstatBotons(EstatJoc.JUGANT));
        btnAturar.setOnClickListener(v -> aturarJoc());
    }

    //Mètode que utilitza l'iterador per recórrer el conjunt i mostrar/amagar
    private void canviarVisibilitatPistes(int visibilitat) {
        Iterator<View> iterador = conjuntPistes.iterator();

        while (iterador.hasNext()) {
            View element = iterador.next();
            if (element != null) {
                element.setVisibility(visibilitat);
            }
        }
    }

// Mètode per actualitzar els estats dels botons, habilitant o deshabilitant els corresponents
    private void actualitzarEstatBotons(EstatJoc nouEstat) {
        estatJoc = nouEstat;
        // Si el joc està aturat o ha acabat, activem els botons de començar
        if (estatJoc == EstatJoc.ATURAT || estatJoc == EstatJoc.ACABAT) {
            btnNouJoc.setEnabled(true);
            btnNouJoc.setAlpha(1.0f);
            btnConnectar.setEnabled(true);
            btnConnectar.setAlpha(1.0f);

            btnAturar.setEnabled(false);
            btnAturar.setAlpha(0.5f);
            btnPista.setEnabled(false);
            btnPista.setAlpha(0.5f);

        } else {
            // Si estem JUGANT o EN_ESPERA, activem els botons d'aturar/pista
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
// Mètode per processar el joc a partir d'un toc per part de l'usuari
    @Override
    public boolean onTouchEvent(android.view.MotionEvent event) {
        // Només processem el toc si estem JUGANT i és el NOSTRE TORN
        if (estatJoc == EstatJoc.JUGANT && tornActual == JUGADOR_PROPI) {

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

                    // Calculem la casella (x, y)
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
            // Obtenim les estructures del rival (qui rep l'atac)
            UnsortedArrayMapping<Casella, Vaixell> vaixellsRival = vaixells.get(JUGADOR_RIVAL);
            UnsortedArrayMapping<Casella, Vaixell> enfonsadesRival = casellesEnfonsades.get(JUGADOR_RIVAL);
            UnsortedArraySet<Casella> destapadesRival = casellesDestapades.get(JUGADOR_RIVAL);

            TextView tvMissatges = findViewById(R.id.textViewMissatges);
            // Si el jugador ja havia disparat a aquella casella
            if (destapadesRival.contains(c)) {
                tvMissatges.append("\nJa havies atacat la casella " + c.toString() + "!\n");
                ferScrollMissatges(tvMissatges);
                return; // No fem res més
            }
            // Sinó, l'afegim a les destapades i provam a atacar-la
            destapadesRival.add(c);
            Vaixell vaixellAtacat = vaixellsRival.get(c);
            // Si no s'ha atacat el vaixell és aigua i canviam de torn, i sinó, és tocat o enfonsat i continua jugant
            if (vaixellAtacat == null) {
                // ---------- AIGUA ----------
                tvMissatges.append("\nEl teu atac " + c.toString() + " -> AIGUA!\n");

                // Canvi de torn
                tornActual = JUGADOR_RIVAL;
                actualitzarEstatBotons(EstatJoc.EN_ESPERA);
                tvMissatges.append("\nTorn del rival. El robot està pensant...\n");

                // El robot actua perquè has fallat!
                ferJugadaRobot();
            } else {
                // ---------- TOCAT O ENFONSAT ----------
                vaixellAtacat.rebreTret(); // Sumem 1 al dany del vaixell

                vaixellsRival.remove(c); // Lllevem aquesta coordenada dels vius
                enfonsadesRival.put(c, vaixellAtacat); // L'afegim als morts

                if (vaixellAtacat.esEnfonsat()) {
                    tvMissatges.append("\nEl teu atac " + c.toString() + " -> ENFONSAT!\n");
                } else {
                    tvMissatges.append("\nEl teu atac " + c.toString() + " -> TOCAT!\n");
                }

                // Comprovem condició de victòria (si no queden vaixells vius al rival)
                if (vaixellsRival.isEmpty()) {
                    tvMissatges.append("\n¡HAS GUANYAT LA PARTIDA!\n");
                    actualitzarEstatBotons(EstatJoc.ACABAT);
                } else {
                    tvMissatges.append("\nContinues tirant tu!\n");
                }
            }

            ferScrollMissatges(tvMissatges);

            // Repintem la graella del rival perquè es vegi el resultat de la jugada
            surfaceRival.post(() -> pintaGraelles(null, surfaceRival));

            TextView tvDarreraTeva = findViewById(R.id.text_darrera_jugada_teva);
            if (tvDarreraTeva != null) {
                String resumTret = (vaixellAtacat == null) ? "AIGUA" : (vaixellAtacat.esEnfonsat() ? "ENFONSAT" : "TOCAT");
                tvDarreraTeva.setText("Darrera jugada teva: " + c.toString() + " -> " + resumTret);
            }
        }

    // Mètode d'ajuda per fer l'scroll net
    private void ferScrollMissatges(TextView tvMissatges) {
        tvMissatges.post(() -> {
            android.text.Layout layout = tvMissatges.getLayout();
            if (layout != null) {
                int scrollAmount = layout.getLineTop(tvMissatges.getLineCount()) - tvMissatges.getHeight();
                if (scrollAmount > 0) tvMissatges.scrollTo(0, scrollAmount);
                else tvMissatges.scrollTo(0, 0);
            }
        });
    }

    // Mètode per pintar i repintar la graella i mostrar els vaixells
    public void pintaGraelles(Casella c, SurfaceView surface) {
        if (surface.getHolder().getSurface().isValid()) {
            int amplada = surface.getWidth();
            int alt = surface.getHeight();
            Canvas canvas = surface.getHolder().lockCanvas();

            if (canvas != null) {
                // 1. Netejem el fons
                canvas.drawColor(Color.parseColor("#D0E8E8"));

                // 2. Preparem pincell per la graella
                Paint p = new Paint();
                p.setColor(Color.parseColor("#90C0C0"));
                p.setStrokeWidth(3);

                float casellaAmplada = (float) amplada / 10;
                float casellaAlt = (float) alt / 10;

                // 3. Dibuixar la quadrícula (Línies)
                for (int i = 1; i < 10; i++) {
                    canvas.drawLine(casellaAmplada * i, 0, casellaAmplada * i, alt, p);
                    canvas.drawLine(0, casellaAlt * i, amplada, casellaAlt * i, p);
                }

                // DIBUIXAR SURFACE JUGADOR
                if (surface == surfaceJugador && vaixells != null) {

                    // Pintar els  vaixells vius del jugador propi
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

                            float esq = casellaVaixell.getCoordenadaX() * casellaAmplada;
                            float dlt = casellaVaixell.getCoordenadaY() * casellaAlt;
                            canvas.drawRoundRect(esq + 4, dlt + 4, esq + casellaAmplada - 4, dlt + casellaAlt - 4, 15f, 15f, pVaixell);
                        }
                    }

                    // Pintar els atacs rebuts del robot
                    UnsortedArraySet<Casella> destapadesPropies = casellesDestapades.get(JUGADOR_PROPI);
                    UnsortedArrayMapping<Casella, Vaixell> enfonsadesPropies = casellesEnfonsades.get(JUGADOR_PROPI);

                    if (destapadesPropies != null) {
                        Iterator<Casella> itDestapades = destapadesPropies.iterator();
                        while (itDestapades.hasNext()) {
                            Casella cDestapada = itDestapades.next();
                            Vaixell vTocat = enfonsadesPropies.get(cDestapada);

                            float esq = cDestapada.getCoordenadaX() * casellaAmplada;
                            float dlt = cDestapada.getCoordenadaY() * casellaAlt;

                            Paint pDestapada = new Paint();
                            pDestapada.setAntiAlias(true);
                            pDestapada.setStyle(Paint.Style.FILL);

                            if (vTocat == null) {
                                // Aigua: Pintem quadrat blanc
                                pDestapada.setColor(Color.WHITE);
                                canvas.drawRoundRect(esq + 4, dlt + 4, esq + casellaAmplada - 4, dlt + casellaAlt - 4, 15f, 15f, pDestapada);
                            } else {
                                // Tocat/Enfonsat: Pintem del color del vaixell i hi posem la bola
                                pDestapada.setColor(vTocat.getColor());
                                canvas.drawRoundRect(esq + 4, dlt + 4, esq + casellaAmplada - 4, dlt + casellaAlt - 4, 15f, 15f, pDestapada);

                                Paint pPunt = new Paint();
                                pPunt.setAntiAlias(true);
                                pPunt.setStyle(Paint.Style.FILL);
                                // Si està enfonsat, bola VERMELLA. Si no, NEGRA.
                                pPunt.setColor(vTocat.esEnfonsat() ? Color.RED : Color.BLACK);
                                canvas.drawCircle(esq + casellaAmplada / 2, dlt + casellaAlt / 2, 8f, pPunt);
                            }
                        }
                    }
                }

                // DIBUIXAR EL SURFACE RIVAL
                if (surface == surfaceRival && casellesDestapades != null) {

                    // Pintar els teus atacs sobre el rival
                    UnsortedArraySet<Casella> destapadesRival = casellesDestapades.get(JUGADOR_RIVAL);
                    UnsortedArrayMapping<Casella, Vaixell> enfonsadesRival = casellesEnfonsades.get(JUGADOR_RIVAL);

                    if (destapadesRival != null) {
                        Iterator<Casella> itDestapades = destapadesRival.iterator();
                        while (itDestapades.hasNext()) {
                            Casella cDestapada = itDestapades.next();
                            Vaixell vTocat = enfonsadesRival.get(cDestapada);

                            float esq = cDestapada.getCoordenadaX() * casellaAmplada;
                            float dlt = cDestapada.getCoordenadaY() * casellaAlt;

                            Paint pDestapada = new Paint();
                            pDestapada.setAntiAlias(true);
                            pDestapada.setStyle(Paint.Style.FILL);

                            if (vTocat == null) {
                                // Aigua: Pintem quadrat blanc
                                pDestapada.setColor(Color.WHITE);
                                canvas.drawRoundRect(esq + 4, dlt + 4, esq + casellaAmplada - 4, dlt + casellaAlt - 4, 15f, 15f, pDestapada);
                            } else {
                                // Tocat/Enfonsat: Pintem del color del vaixell i hi posem la bola
                                pDestapada.setColor(vTocat.getColor());
                                canvas.drawRoundRect(esq + 4, dlt + 4, esq + casellaAmplada - 4, dlt + casellaAlt - 4, 15f, 15f, pDestapada);

                                Paint pPunt = new Paint();
                                pPunt.setAntiAlias(true);
                                pPunt.setStyle(Paint.Style.FILL);
                                // Si està enfonsat, bola VERMELLA. Si no, NEGRA.
                                pPunt.setColor(vTocat.esEnfonsat() ? Color.RED : Color.BLACK);
                                canvas.drawCircle(esq + casellaAmplada / 2, dlt + casellaAlt / 2, 8f, pPunt);
                            }
                        }
                    }

                    // Pintar selecció activa (Casella vermella transitòria)
                    if (c != null) {
                        Paint pSeleccio = new Paint();
                        pSeleccio.setAntiAlias(true);
                        pSeleccio.setStyle(Paint.Style.FILL);
                        pSeleccio.setColor(Color.RED);

                        float esquerra = c.getCoordenadaX() * casellaAmplada;
                        float dalt = c.getCoordenadaY() * casellaAlt;
                        canvas.drawRoundRect(esquerra + 4, dalt + 4, esquerra + casellaAmplada - 4, dalt + casellaAlt - 4, 15f, 15f, pSeleccio);
                    }
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

    private void iniciarNouJoc() {
        TextView tvMissatges = findViewById(R.id.textViewMissatges);
        tvMissatges.setText("--- NOVA PARTIDA ---");

        // INICIALITZEM TOTES LES ESTRUCTURES AQUÍ (Al donar-li al Play)
        vaixells = new UnsortedArrayMapping<>(2);
        vaixells.put(JUGADOR_PROPI, new UnsortedArrayMapping<>(20));
        vaixells.put(JUGADOR_RIVAL, new UnsortedArrayMapping<>(20));

        casellesDestapades = new UnsortedArrayMapping<>(2);
        casellesDestapades.put(JUGADOR_PROPI, new UnsortedArraySet<>(100));
        casellesDestapades.put(JUGADOR_RIVAL, new UnsortedArraySet<>(100));

        casellesEnfonsades = new UnsortedArrayMapping<>(2);
        casellesEnfonsades.put(JUGADOR_PROPI, new UnsortedArrayMapping<>(20));
        casellesEnfonsades.put(JUGADOR_RIVAL, new UnsortedArrayMapping<>(20));

        objectiusRobot = new UnsortedArraySet<>(4);

        // Generem els vaixells ara
        crearVaixells();

        // Repintem els taulells per mostrar els vaixells generats
        surfaceJugador.post(() -> pintaGraelles(null, surfaceJugador));
        surfaceRival.post(() -> pintaGraelles(null, surfaceRival));

        // Netejem els textos de darrera jugada per la nova partida
        TextView tvDarreraTeva = findViewById(R.id.text_darrera_jugada_teva);
        if (tvDarreraTeva != null) tvDarreraTeva.setText("Darrera jugada teva: ");
        TextView tvDarreraRival = findViewById(R.id.text_darrera_jugada_seva);
        if (tvDarreraRival != null) tvDarreraRival.setText("Darrera jugada rival: ");

        // Decidim aleatòriament qui comença
        tornActual = (int) (Math.random() * 2);

        if (tornActual == JUGADOR_PROPI) {
            actualitzarEstatBotons(EstatJoc.JUGANT);
            tvMissatges.append("\nComences tu! Selecciona una casella per atacar.");
        } else {
            actualitzarEstatBotons(EstatJoc.EN_ESPERA);
            tvMissatges.append("\nComença el rival. El robot està pensant...");
            ferJugadaRobot();
        }
    }

    private void ferJugadaRobot() {
        // Posem un retard de 400ms perquè sembli que el robot "pensa"
        surfaceJugador.postDelayed(() -> {
            if (estatJoc == EstatJoc.ACABAT) return; // Si ja s'ha acabat la partida, no fem res

            UnsortedArrayMapping<Casella, Vaixell> meusVaixells = vaixells.get(JUGADOR_PROPI);
            UnsortedArrayMapping<Casella, Vaixell> mevesEnfonsades = casellesEnfonsades.get(JUGADOR_PROPI);
            UnsortedArraySet<Casella> mevesDestapades = casellesDestapades.get(JUGADOR_PROPI);

            Casella casellaObjectiu = null;

            // MODE REMAT: Busquem si tenim algun objectiu pendent de trets anteriors
            while (!objectiusRobot.isEmpty() && casellaObjectiu == null) {
                Iterator<Casella> it = objectiusRobot.iterator();
                Casella candidat = it.next();
                objectiusRobot.remove(candidat); // El traiem de la llista d'objectius

                // Només és un objectiu vàlid si no l'hem atacat ja
                if (!mevesDestapades.contains(candidat)) {
                    casellaObjectiu = candidat;
                }
            }

            // MODE CERCA: Si no hi havia objectiu vàlid, tirem aleatòriament
            if (casellaObjectiu == null) {
                boolean trobada = false;
                while (!trobada) {
                    int rX = (int) (Math.random() * 10);
                    int rY = (int) (Math.random() * 10);
                    Casella rC = new Casella(rX, rY);
                    if (!mevesDestapades.contains(rC)) {
                        casellaObjectiu = rC;
                        trobada = true;
                    }
                }
            }

            // PROCESSEM L'ATAC DEL ROBOT (a la nostra graella)
            mevesDestapades.add(casellaObjectiu);
            Vaixell vaixellAtacat = meusVaixells.get(casellaObjectiu);

            TextView tvMissatges = findViewById(R.id.textViewMissatges);

            if (vaixellAtacat == null) {
                // ---------- AIGUA ----------
                tvMissatges.append("\nEl robot ataca " + casellaObjectiu.toString() + " -> AIGUA!");
                tornActual = JUGADOR_PROPI;
                actualitzarEstatBotons(EstatJoc.JUGANT);
                tvMissatges.append("\nTorn teu. Tira!");
            } else {
                // ---------- TOCAT O ENFONSAT ----------
                vaixellAtacat.rebreTret();
                meusVaixells.remove(casellaObjectiu);
                mevesEnfonsades.put(casellaObjectiu, vaixellAtacat);

                if (vaixellAtacat.esEnfonsat()) {
                    tvMissatges.append("\nEl robot ataca " + casellaObjectiu.toString() + " -> ENFONSAT!");
                } else {
                    tvMissatges.append("\nEl robot ataca " + casellaObjectiu.toString() + " -> TOCAT!");

                    // INTEL·LIGÈNCIA: Afegim les caselles veïnes als objectius (Dalt, Baix, Esq, Dreta)
                    int x = casellaObjectiu.getCoordenadaX();
                    int y = casellaObjectiu.getCoordenadaY();
                    if (x > 0) objectiusRobot.add(new Casella(x - 1, y));
                    if (x < 9) objectiusRobot.add(new Casella(x + 1, y));
                    if (y > 0) objectiusRobot.add(new Casella(x, y - 1));
                    if (y < 9) objectiusRobot.add(new Casella(x, y + 1));
                }

                if (meusVaixells.isEmpty()) {
                    tvMissatges.append("\n¡EL ROBOT ET GUANYA LA PARTIDA!\n");
                    actualitzarEstatBotons(EstatJoc.ACABAT);
                } else {
                    tvMissatges.append("\nEl robot torna a tirar...\n");
                    ferJugadaRobot(); // Recursivitat: el robot torna a jugar perquè ha encertat
                }
            }

            ferScrollMissatges(tvMissatges);

            // Repintem LA TEVA graella perquè es vegi l'atac del robot
            surfaceJugador.post(() -> pintaGraelles(null, surfaceJugador));
            TextView tvDarreraRival = findViewById(R.id.text_darrera_jugada_seva);
            if (tvDarreraRival != null) {
                String resumTretRival = (vaixellAtacat == null) ? "AIGUA" : (vaixellAtacat.esEnfonsat() ? "ENFONSAT" : "TOCAT");
                tvDarreraRival.setText("Darrera jugada rival: " + casellaObjectiu.toString() + " -> " + resumTretRival);
            }

        }, 400); // 400 mil·lisegons de retard
    }

    // MÈTODE PER ATURAR I NETEJAR EL JOC
    private void aturarJoc() {
        actualitzarEstatBotons(EstatJoc.ATURAT);

        // Buidem la memòria de totes les estructures
        vaixells = null;
        casellesDestapades = null;
        casellesEnfonsades = null;
        objectiusRobot = null;

        // Restablim els textos de la interfície
        TextView tvMissatges = findViewById(R.id.textViewMissatges);
        tvMissatges.setText("--- JOC ATURAT ---");

        TextView tvDarreraTeva = findViewById(R.id.text_darrera_jugada_teva);
        if (tvDarreraTeva != null) tvDarreraTeva.setText("Darrera jugada teva: ");

        TextView tvDarreraRival = findViewById(R.id.text_darrera_jugada_seva);
        if (tvDarreraRival != null) tvDarreraRival.setText("Darrera jugada rival: ");

        // Repintem els taulells en buit
        surfaceJugador.post(() -> pintaGraelles(null, surfaceJugador));
        surfaceRival.post(() -> pintaGraelles(null, surfaceRival));
    }

}