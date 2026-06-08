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
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import java.util.Iterator;
import android.app.Dialog;
import android.os.Handler;
import android.os.Looper;

public class MainActivity extends AppCompatActivity {

    public enum EstatJoc {ATURAT, JUGANT, EN_ESPERA, ACABAT}
    private EstatJoc estatJoc = EstatJoc.ATURAT; // Iniciem aturats
    private int tornActual; // 0 = JUGADOR_PROPI, 1 = JUGADOR_RIVAL
    public static final int JUGADOR_PROPI = 0;
    public static final int JUGADOR_RIVAL = 1;
    private ImageButton btnNouJoc, btnConnectar, btnAturar, btnPista;
    private SurfaceView surfaceJugador, surfaceRival;
    private boolean connectat = false;

    private UnsortedArraySet<View> conjuntPistes;

    // UnsortedArrayMapping para vaixells y casellesEnfonsades, ja que necessitem associar una clau (Casella) a un valor (Vaixell).
   // Al ser Unsorted, l'inserció és ràpida O(1), ja que no necessitem recuperar el taulell ordenat
    private UnsortedArrayMapping<Integer, UnsortedArrayMapping<Casella, Vaixell>> vaixells;
    private UnsortedArrayMapping<Integer, UnsortedArrayMapping<Casella, Vaixell>> casellesEnfonsades;

    // A casselles destapades hem elegit dins el mapping un UnsortedArraySet ya que el tamany de caselles és petit (màxim 100) i l'ordre no importa.
    // A més evitem caselles duplicades
    private UnsortedArrayMapping<Integer, UnsortedArraySet<Casella>> casellesDestapades;
    private UnsortedArraySet<Casella> objectiusRobot;
    private GestorWebSocket gestorWebSocket;

    // Les jugades requereixen mostrar-se en l'ordre que es van anar ficant, per tant la cua és ideal
    // Com la quantitat de jugades poden arribar a ser molt variables, hem decidit emprar una llista enllaçada, que va perfecte per aquesta tasca
    private LinkedListQueue<Jugada> historialJugades;

    /* Utilitzem un Mapping on la clau és l'ID del vaixell i el valor és un Set de les seves caselles.
    Això permet saber quines caselles pertanyen a cada vaixell de forma eficient per generar les pistes. */
    private UnsortedArrayMapping<Integer, UnsortedArrayMapping<Integer, UnsortedArraySet<Casella>>> inventariVaixells;

    /*
    Efectivament, els arrays sofreixen problemes de rendiment si han de redimensionar-se sovint.
    No obstant això, en el tauler d'Enfonsar la Flota la grandària màxima és delimitat i predictible
    (100 caselles màxim, 20 parts de vaixell màxim). Per tant, l'ús de mappings basats en arrays és eficient ja que no superarem mai la capacitat inicial.
     Per contra, per a l'historial de jugades, com serà molt més variable, hem optat per una estructura enllaçada (LinkedListQueue) per a garantir operacions O(1) d'inserció sense desplaçaments de memòria.
      */

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Apaisat
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

        // Inicialitzar el gestor de WebSocket
        gestorWebSocket = new GestorWebSocket(
                new GestorWebSocket.EscoltadorWebSocket() {
                    @Override
                    public void enConnectar() {
                        runOnUiThread(() -> mostrarMissatge("WS: Connectat"));
                        connectat = true;
                    }
                    @Override
                    public void enRebreMissatge(JSONObject json) {
                        runOnUiThread(() -> gestionarMissatge(json));
                    }
                    @Override
                    public void enDesconnectar() {
                        runOnUiThread(() ->
                                mostrarMissatge("WS: Desconnectat")
                        );
                        connectat = false;
                    }
                    @Override
                    public void enError(String error) {
                        runOnUiThread(() ->
                                mostrarMissatge("WS: Error: " + error)
                        );
                    }
                }
        );

        // Inicializar SurfaceViews i dibuixar la graella buida
        surfaceJugador = findViewById(R.id.surface_jugador);
        surfaceRival = findViewById(R.id.surface_rival);
        // Afegim un Callback perquè es repinti automàticament al tornar a l'app
        surfaceJugador.getHolder().addCallback(new android.view.SurfaceHolder.Callback() {
            @Override public void surfaceCreated(android.view.SurfaceHolder holder) { pintaGraelles(null, surfaceJugador); }
            @Override public void surfaceChanged(android.view.SurfaceHolder holder, int format, int width, int height) {}
            @Override public void surfaceDestroyed(android.view.SurfaceHolder holder) {}
        });

        surfaceRival.getHolder().addCallback(new android.view.SurfaceHolder.Callback() {
            @Override public void surfaceCreated(android.view.SurfaceHolder holder) { pintaGraelles(null, surfaceRival); }
            @Override public void surfaceChanged(android.view.SurfaceHolder holder, int format, int width, int height) {}
            @Override public void surfaceDestroyed(android.view.SurfaceHolder holder) {}
        });

        // Fem desplaçables els textos de les pistes
        TextView textPistesJugador = findViewById(R.id.text_pistes_jugador);
        TextView textPistesRival = findViewById(R.id.text_pistes_rival);
        textPistesJugador.setMovementMethod(new ScrollingMovementMethod());
        textPistesRival.setMovementMethod(new ScrollingMovementMethod());

        // Inicialitzem el conjunt de pistes i els seus botons
        conjuntPistes = new UnsortedArraySet<>(8);
        conjuntPistes.add(findViewById(R.id.layout_pistes));
        conjuntPistes.add(findViewById(R.id.btn_tancar_pistes));
        conjuntPistes.add(findViewById(R.id.titol_pistes_jugador));
        conjuntPistes.add(findViewById(R.id.titol_pistes_rival));
        conjuntPistes.add(textPistesJugador);
        conjuntPistes.add(textPistesRival);
        conjuntPistes.add(findViewById(R.id.text_percentatge_jugador));
        conjuntPistes.add(findViewById(R.id.text_percentatge_rival));

        btnPista.setOnClickListener(v -> {
            generarTextPistes();
            canviarVisibilitatPistes(View.VISIBLE);
        });

        ImageButton btnTancarPistes = findViewById(R.id.btn_tancar_pistes);
        btnTancarPistes.setOnClickListener(v -> canviarVisibilitatPistes(View.GONE));

        actualitzarEstatBotons(EstatJoc.ATURAT); // Estat per defecte en obrir l'app

        btnNouJoc.setOnClickListener(v -> iniciarNouJoc());

        btnConnectar.setOnClickListener(v -> {
            gestorWebSocket.connectar("wss://hci.uib.es/ws");
            actualitzarEstatBotons(EstatJoc.EN_ESPERA);
            mostrarMissatge("Connectant al servidor remot de la UIB...");
        });

        btnAturar.setOnClickListener(v -> {
            if (connectat) {
                enviarSortirPartida(); // Avisem al rival si fugim a mitges
                gestorWebSocket.tancar(); // Tanquem el socket
                connectat = false;
            }
            aturarJoc(); // Netejem els taulells i memòria
        });
    }
    // Mètode per quan se surt en mig d'una partida online
    private void enviarSortirPartida() {
        try {
            JSONObject json = new JSONObject();
            json.put("tipus", "sortir_partida");
            gestorWebSocket.enviar(json);
        } catch (JSONException e) {
            mostrarMissatge("Error enviant sortir_partida: " + e.getMessage());
        }
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

    // GESTIONAR MISSATGES DEL SERVIDOR
    private void gestionarMissatge(JSONObject json) {
        String tipus = json.optString("tipus",""); // Llegim el tipus de missatge

        switch (tipus) {
            case "connectat":
                mostrarMissatge("El servidor ens ha acceptat la connexió.");
                // Quan ens connectem, ens hem de registrar amb la nostra flota
                enviarRegistrar("BuCaSa");
                break;
            case "registre_acceptat":
                mostrarMissatge("Registre correcte. Cercant partida...");
                enviarCercarPartida();
                break;
            case "esperant_rival":
                mostrarMissatge("Esperant rival...");
                break;
            case "partida_trobada":
                gestionarPartidaTrobada(json);
                break;
            case "tir_rebut":
                gestionarTirRebut(json);
                break;
            case "resultat_tir":
                gestionarResultatTir(json);
                break;
            case "rival_ha_sortit":
            case "rival_desconnectat":
                mostrarMissatge("El rival ha fugit de la partida!");
                gestionarAturaPartida();
                break;
            case "error":
                mostrarMissatge("ERROR: " + json.optString("missatge"));
                break;
            default:
                break;
        }
    }

    // ENVIAMENT DE MISSATGES I JSON
    private void enviarRegistrar(String nomUsuari) {
        try {
            JSONObject json = new JSONObject();
            json.put("tipus", "registrar");
            json.put("nomUsuari", nomUsuari);
            // Inicialització de jugades
            historialJugades = new LinkedListQueue<>();
            // Si la flota no està creada, la creem abans d'enviar
            if (vaixells == null) {
                vaixells = new UnsortedArrayMapping<>(2);
                vaixells.put(JUGADOR_PROPI, new UnsortedArrayMapping<>(20));
                vaixells.put(JUGADOR_RIVAL, new UnsortedArrayMapping<>(20));
                casellesDestapades = new UnsortedArrayMapping<>(2);
                casellesDestapades.put(JUGADOR_PROPI, new UnsortedArraySet<>(100));
                casellesDestapades.put(JUGADOR_RIVAL, new UnsortedArraySet<>(100));
                casellesEnfonsades = new UnsortedArrayMapping<>(2);
                casellesEnfonsades.put(JUGADOR_PROPI, new UnsortedArrayMapping<>(20));
                casellesEnfonsades.put(JUGADOR_RIVAL, new UnsortedArrayMapping<>(20));

                inventariVaixells = new UnsortedArrayMapping<>(2);
                inventariVaixells.put(JUGADOR_PROPI, new UnsortedArrayMapping<>(10));
                inventariVaixells.put(JUGADOR_RIVAL, new UnsortedArrayMapping<>(10));

                crearVaixells();
                surfaceJugador.post(() -> pintaGraelles(null, surfaceJugador));
            }

            json.put("vaixells", construirJsonVaixells(JUGADOR_PROPI));
            gestorWebSocket.enviar(json);

        } catch (JSONException e) {
            mostrarMissatge("Error enviant registre: " + e.getMessage());
        }
    }

    private JSONObject construirJsonVaixells(int jugador) throws JSONException {
        JSONObject jsonVaixells = new JSONObject();
        JSONArray jsonCasellesVaixellsVius = new JSONArray();

        UnsortedArrayMapping<Casella, Vaixell> meusVaixells = vaixells.get(jugador);

        if (meusVaixells != null) {
            Iterator<UnsortedArrayMapping<Casella, Vaixell>.Pair> iterador = meusVaixells.iterator();

            while (iterador.hasNext()) {
                UnsortedArrayMapping<Casella, Vaixell>.Pair parella = iterador.next();
                Casella c = parella.getKey();
                Vaixell v = parella.getValue();

                JSONObject jsonCasella = new JSONObject();
                jsonCasella.put("i", c.getCoordenadaX());
                jsonCasella.put("j", c.getCoordenadaY());

                JSONObject jsonVaixell = new JSONObject();
                jsonVaixell.put("mida", v.getMida());
                jsonVaixell.put("orientacio", v.getOrientacio());
                jsonVaixell.put("color", v.getColor());
                jsonVaixell.put("id", String.valueOf(v.getId()));

                JSONObject jsonEntrada = new JSONObject();
                jsonEntrada.put("casella", jsonCasella);
                jsonEntrada.put("vaixell", jsonVaixell);

                jsonCasellesVaixellsVius.put(jsonEntrada);
            }
        }
        jsonVaixells.put("casellesVaixellsVius", jsonCasellesVaixellsVius);
        return jsonVaixells;
    }

    private void enviarCercarPartida() {
        try {
            JSONObject json = new JSONObject();
            json.put("tipus", "cercar_partida");
            gestorWebSocket.enviar(json);
        } catch (JSONException e) {
            mostrarMissatge("Error enviant cercar partida: " + e.getMessage());
        }
    }
    // Mètode per gestionar l'inici d'una partida quan el servidor troba un rival
    private void gestionarPartidaTrobada(JSONObject json) {
        boolean etToca = json.optBoolean("etToca", false);
        JSONObject rival = json.optJSONObject("rival");
        String nomRival = "Desconegut";

        if (rival != null) {
            nomRival = rival.optString("nomUsuari");
            JSONObject vaixellsRival = rival.optJSONObject("vaixells");
            if (vaixellsRival != null) {
                carregarVaixellsRival(vaixellsRival);
            }
        }

        mostrarMissatge("¡Partida trobada contra " + nomRival + "!");

        if (etToca) {
            tornActual = JUGADOR_PROPI;
            actualitzarEstatBotons(EstatJoc.JUGANT);
            mostrarMissatge("Comences tu! Selecciona una casella.");
        } else {
            tornActual = JUGADOR_RIVAL;
            actualitzarEstatBotons(EstatJoc.EN_ESPERA);
            mostrarMissatge("Comença el rival. Esperant el seu atac...");
        }
    }
    // Mètode per carregar la flota del rival a partir del JSON rebut del servidor
    private void carregarVaixellsRival(JSONObject jsonVaixells) {
        try {
            JSONArray arr = jsonVaixells.getJSONArray("casellesVaixellsVius");

            // Destruïm el mapa ple de vaixells fantasma i en creem un de net
            vaixells.put(JUGADOR_RIVAL, new UnsortedArrayMapping<>(20));
            UnsortedArrayMapping<Casella, Vaixell> mappingRival = vaixells.get(JUGADOR_RIVAL);

            UnsortedArrayMapping<String, Vaixell> vaixellsUnics = new UnsortedArrayMapping<>(10);
            int nextIdLocal = 0;

            for (int k = 0; k < arr.length(); k++) {
                JSONObject element = arr.getJSONObject(k);
                JSONObject jsonCasella = element.getJSONObject("casella");
                JSONObject jsonVaixell = element.getJSONObject("vaixell");

                Casella c = new Casella(jsonCasella.getInt("i"), jsonCasella.getInt("j"));
                String idOriginal = jsonVaixell.getString("id");

                Vaixell v = vaixellsUnics.get(idOriginal);
                if (v == null) {
                    v = new Vaixell(
                            nextIdLocal++,
                            jsonVaixell.getInt("mida"),
                            jsonVaixell.getInt("orientacio"),
                            jsonVaixell.getInt("color"),
                            JUGADOR_RIVAL
                    );
                    vaixellsUnics.put(idOriginal, v);
                }

                mappingRival.put(c, v);
            }
        } catch (org.json.JSONException e) {
            mostrarMissatge("Error carregant flota rival: " + e.getMessage());
        }
    }
    // ENVIAR UN ATAC
    private void enviarTirar(Casella c) {
        try {
            JSONObject json = new JSONObject();
            json.put("tipus", "tirar");
            json.put("fila", c.getCoordenadaY());
            json.put("columna", c.getCoordenadaX());
            gestorWebSocket.enviar(json);
        } catch (org.json.JSONException e) {
            mostrarMissatge("Error enviant tir: " + e.getMessage());
        }
    }

    // REBRE EL RESULTAT DEL NOSTRE ATAC
    private void gestionarResultatTir(JSONObject json) {
        try {
            int fila = json.getInt("fila");
            int columna = json.getInt("columna");
            String resultat = json.getString("resultat");
            boolean acabat = json.optBoolean("acabat", false);

            Casella c = new Casella(columna, fila);

            UnsortedArrayMapping<Casella, Vaixell> vaixellsRival = vaixells.get(JUGADOR_RIVAL);
            UnsortedArrayMapping<Casella, Vaixell> enfonsadesRival = casellesEnfonsades.get(JUGADOR_RIVAL);
            UnsortedArraySet<Casella> destapadesRival = casellesDestapades.get(JUGADOR_RIVAL);

            destapadesRival.add(c);
            Vaixell vaixellAtacat = vaixellsRival.get(c);

            if (resultat.equals("aigua")) {
                mostrarMissatge("El teu atac a " + c.toString() + " -> AIGUA!");
                mostrarMissatge("Li toca al rival!");
                tornActual = JUGADOR_RIVAL;
                actualitzarEstatBotons(EstatJoc.EN_ESPERA);
            } else {
                if (vaixellAtacat != null) {
                    vaixellAtacat.rebreTret();
                    vaixellsRival.remove(c);
                    enfonsadesRival.put(c, vaixellAtacat);
                }
                mostrarMissatge("El teu atac a " + c.toString() + " -> " + resultat.toUpperCase() + "!");

                if (acabat) {
                    mostrarMissatge("¡HAS GUANYAT LA PARTIDA ONLINE!");
                    actualitzarEstatBotons(EstatJoc.ACABAT);
                    mostrarResum();
                } else {
                    mostrarMissatge("Continues tirant tu!");
                }
            }
            registrarJugada(JUGADOR_PROPI, c, vaixellAtacat);

            TextView tvDarreraTeva = findViewById(R.id.text_darrera_jugada_teva);
            if (tvDarreraTeva != null) {
                tvDarreraTeva.setText("Darrera jugada teva: " + c.toString() + " -> " + resultat.toUpperCase());
            }
            surfaceRival.post(() -> pintaGraelles(null, surfaceRival));

        } catch (org.json.JSONException e) {
            mostrarMissatge("Error processant resultat: " + e.getMessage());
        }
    }

    // GESTIONAR UN TIR REBUT PEL RIVAL
    private void gestionarTirRebut(JSONObject json) {
        try {
            int fila = json.getInt("fila");
            int columna = json.getInt("columna");
            Casella c = new Casella(columna, fila);

            UnsortedArrayMapping<Casella, Vaixell> meusVaixells = vaixells.get(JUGADOR_PROPI);
            UnsortedArrayMapping<Casella, Vaixell> mevesEnfonsades = casellesEnfonsades.get(JUGADOR_PROPI);
            UnsortedArraySet<Casella> mevesDestapades = casellesDestapades.get(JUGADOR_PROPI);

            mevesDestapades.add(c);
            Vaixell vaixellAtacat = meusVaixells.get(c);

            String resultatStr = "aigua";
            boolean acabat = false;

            if (vaixellAtacat == null) {
                mostrarMissatge("El rival ataca " + c.toString() + " -> AIGUA!");
                mostrarMissatge("Te toca!");
                tornActual = JUGADOR_PROPI;
                actualitzarEstatBotons(EstatJoc.JUGANT);
            } else {
                vaixellAtacat.rebreTret();
                meusVaixells.remove(c);
                mevesEnfonsades.put(c, vaixellAtacat);

                if (vaixellAtacat.esEnfonsat()) resultatStr = "enfonsat";
                else resultatStr = "tocat";

                mostrarMissatge("El rival ataca " + c.toString() + " -> " + resultatStr.toUpperCase() + "!");

                if (meusVaixells.isEmpty()) {
                    acabat = true;
                    mostrarMissatge("¡EL RIVAL HA GUANYAT LA PARTIDA ONLINE!");
                    actualitzarEstatBotons(EstatJoc.ACABAT);
                    mostrarResum();
                }
            }
            registrarJugada(JUGADOR_RIVAL, c, vaixellAtacat);
            TextView tvDarreraRival = findViewById(R.id.text_darrera_jugada_seva);
            if (tvDarreraRival != null) {
                tvDarreraRival.setText("Darrera jugada rival: " + c.toString() + " -> " + resultatStr.toUpperCase());
            }
            // Avisem al servidor del resultat
            enviarResultatTir(fila, columna, resultatStr, acabat);
            surfaceJugador.post(() -> pintaGraelles(null, surfaceJugador));

        } catch (org.json.JSONException e) {
            mostrarMissatge("Error rebent tir: " + e.getMessage());
        }
    }

    // Mètode per enviar el resultat d'un tir realitzat
    private void enviarResultatTir(int fila, int columna, String resultat, boolean acabat) {
        try {
            JSONObject json = new JSONObject();
            json.put("tipus", "resultat_tir");
            json.put("fila", fila);
            json.put("columna", columna);
            json.put("resultat", resultat);
            json.put("acabat", acabat);
            gestorWebSocket.enviar(json);
        } catch (org.json.JSONException e) {
            mostrarMissatge("Error enviant resultat_tir: " + e.getMessage());
        }
    }

    private void gestionarAturaPartida() {
        // Si la partida ja ha acabat, ignorem si el rival fuig, estem veient el resum
        if (estatJoc != EstatJoc.ACABAT) {
            aturarJoc();
        }
    }

    // Mètode per mostrar un missatge al tvMissatges
    public void mostrarMissatge(String missatge) {
        TextView tvMissatges = findViewById(R.id.textViewMissatges);
        if (tvMissatges != null) {
            tvMissatges.append("\n" + missatge + "\n");
            ferScrollMissatges(tvMissatges);
        }
    }

    // Mètode per actualitzar els estats dels botons, habilitant o deshabilitant els corresponents
    private void actualitzarEstatBotons(EstatJoc nouEstat) {
        estatJoc = nouEstat;
        if (estatJoc == EstatJoc.ATURAT) {
            btnNouJoc.setEnabled(true);
            btnNouJoc.setAlpha(1.0f);
            btnConnectar.setEnabled(true);
            btnConnectar.setAlpha(1.0f);

            btnAturar.setEnabled(false);
            btnAturar.setAlpha(0.5f);
            btnPista.setEnabled(false);
            btnPista.setAlpha(0.5f);

        } else if (estatJoc == EstatJoc.ACABAT) {
            // Permetem que quan hagi ACABAT, el botó d'Aturar quedi actiu (true) i visible, per reiniciar la graella
            btnNouJoc.setEnabled(true);
            btnNouJoc.setAlpha(1.0f);
            btnConnectar.setEnabled(true);
            btnConnectar.setAlpha(1.0f);

            btnAturar.setEnabled(true);
            btnAturar.setAlpha(1.0f);
            btnPista.setEnabled(false);
            btnPista.setAlpha(0.5f);

        } else { // JUGANT o EN_ESPERA
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
            if (connectat) {
                UnsortedArraySet<Casella> destapadesRival = casellesDestapades.get(JUGADOR_RIVAL);
                if (destapadesRival.contains(c)) {
                    mostrarMissatge("Ja havies atacat la casella " + c.toString() + "!");
                    mostrarMissatge("Torna a tirar!");
                    return;
                }
                enviarTirar(c);
                return; // Aturem aquí. L'actualització es farà quan rebem 'resultat_tir'
            }
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
                //  AIGUA
                tvMissatges.append("\nEl teu atac " + c.toString() + " -> AIGUA!\n");

                // Canvi de torn
                tornActual = JUGADOR_RIVAL;
                actualitzarEstatBotons(EstatJoc.EN_ESPERA);
                tvMissatges.append("\nTorn del rival. El robot està pensant...\n");

                // El robot actua perquè has fallat!
                ferJugadaRobot();
            } else {
                // TOCAT O ENFONSAT
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
                    mostrarResum();
                } else {
                    tvMissatges.append("\nContinues tirant tu!\n");
                }
            }
            registrarJugada(JUGADOR_PROPI, c, vaixellAtacat);
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

            colocarVaixellAleatori(mida, idVaixell, JUGADOR_PROPI, mappingPropi);
            colocarVaixellAleatori(mida, idVaixell, JUGADOR_RIVAL, mappingRival);
            idVaixell++;
        }
    }

    private void colocarVaixellAleatori(int mida, int id, int jugador, UnsortedArrayMapping<Casella, Vaixell> mapping) {
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
        // Si veníem del mode online, ens desconnectem abans de jugar contra el Robot
        if (connectat) {
            gestorWebSocket.tancar();
            connectat = false;
        }

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

        historialJugades = new LinkedListQueue<>();

        inventariVaixells = new UnsortedArrayMapping<>(2);
        inventariVaixells.put(JUGADOR_PROPI, new UnsortedArrayMapping<>(10));
        inventariVaixells.put(JUGADOR_RIVAL, new UnsortedArrayMapping<>(10));

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
                    mostrarResum();
                } else {
                    tvMissatges.append("\nEl robot torna a tirar...\n");
                    ferJugadaRobot(); // Recursivitat: el robot torna a jugar perquè ha encertat
                }
            }

            registrarJugada(JUGADOR_RIVAL, casellaObjectiu, vaixellAtacat);
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
        historialJugades = null;
        inventariVaixells = null;

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

    private void mostrarResum() {
        Dialog dialog = new Dialog(this);
        dialog.setContentView(R.layout.final_joc);

        // Fem que el diàleg ocupi el 90% de la pantalla
        dialog.getWindow().setLayout(
                (int) (getResources().getDisplayMetrics().widthPixels * 0.9),
                (int) (getResources().getDisplayMetrics().heightPixels * 0.9)
        );
        dialog.setCancelable(false); // No es pot tancar tocant a fora

        // Determinar guanyador
        TextView tvGuanyador = dialog.findViewById(R.id.textViewGuanyador);
        if (vaixells.get(JUGADOR_PROPI).isEmpty()) {
            tvGuanyador.setText("Ha guanyat el RIVAL!");
        } else {
            tvGuanyador.setText("Has guanyat TU!");
        }

        // Botó Tancar
        ImageButton btnTancar = dialog.findViewById(R.id.btnTancarResum);
        btnTancar.setOnClickListener(v -> {
            dialog.dismiss();
            // Si estàvem online, ens desconnectem per tancar-ho bé
            if (connectat) {
                enviarSortirPartida();
                gestorWebSocket.tancar();
                connectat = false;
            }
            aturarJoc(); // Reiniciem el joc en tancar el resum
        });

        // Botó Reproduir
        android.widget.Button btnVeureResum = dialog.findViewById(R.id.btnVeureResum);
        btnVeureResum.setOnClickListener(v -> {
            btnVeureResum.setVisibility(View.GONE);
            reproduirHistorial(dialog);
        });

        dialog.show();
    }

    private void registrarJugada(int jugador, Casella c, Vaixell v) {
        if (historialJugades != null) {
            boolean tocat = (v != null);
            int color = tocat ? v.getColor() : Color.WHITE;

            // Afegim la jugada al final de la cua
            historialJugades.put(new Jugada(jugador, c, tocat, color));
        }
    }

    private void reproduirHistorial(Dialog dialog) {
        SurfaceView svMeu = dialog.findViewById(R.id.surfaceViewResumMeu);
        SurfaceView svSeu = dialog.findViewById(R.id.surfaceViewResumSeu);

        // Aquest conjunt anirà acumulant les jugades per repintar-les a cada "fotograma"
        UnsortedArraySet<Jugada> jugadesMostrades = new UnsortedArraySet<>(200);

        Handler handler = new Handler(Looper.getMainLooper());
        Runnable[] task = new Runnable[1];

        task[0] = new Runnable() {
            @Override
            public void run() {
                // Comprovem si l'historial existeix i si queden jugades a la nostra cua
                if (historialJugades != null && !historialJugades.isEmpty()) {

                    // Agafem la primera jugada de la cua (la més antiga)
                    Jugada j = historialJugades.getFirst();

                    // La traiem de la cua perquè no es torni a repetir
                    historialJugades.removeFirst();

                    // L'afegim al conjunt de jugades que s'han de pintar
                    jugadesMostrades.add(j);

                    // Repintem els taulells
                    pintarResum(svMeu, jugadesMostrades, JUGADOR_RIVAL); // Atacs que m'han fet a mi
                    pintarResum(svSeu, jugadesMostrades, JUGADOR_PROPI); // Atacs que he fet jo

                    // Programem el següent "fotograma"
                    handler.postDelayed(this, 200); // 200 mil·lisegons de retard
                }
            }
        };
        handler.post(task[0]);
    }

    private void pintarResum(SurfaceView surface, UnsortedArraySet<Jugada> jugades, int jugadorQueAtaca) {
        if (surface.getHolder().getSurface().isValid()) {
            Canvas canvas = surface.getHolder().lockCanvas();

            if (canvas != null) {
                canvas.drawColor(Color.parseColor("#D0E8E8"));

                Paint p = new Paint();
                p.setColor(Color.parseColor("#90C0C0"));
                p.setStrokeWidth(3);

                float casellaAmplada = (float) surface.getWidth() / 10;
                float casellaAlt = (float) surface.getHeight() / 10;

                for (int i = 1; i < 10; i++) {
                    canvas.drawLine(casellaAmplada * i, 0, casellaAmplada * i, surface.getHeight(), p);
                    canvas.drawLine(0, casellaAlt * i, surface.getWidth(), casellaAlt * i, p);
                }

                // Dibuixem només les jugades
                Iterator<Jugada> it = jugades.iterator();
                while (it.hasNext()) {
                    Jugada j = it.next();
                    if (j.getJugadorQueDispara() == jugadorQueAtaca) {
                        float esq = j.getCasellaAtacada().getCoordenadaX() * casellaAmplada;
                        float dlt = j.getCasellaAtacada().getCoordenadaY() * casellaAlt;

                        Paint pJugada = new Paint();
                        pJugada.setAntiAlias(true);
                        pJugada.setStyle(Paint.Style.FILL);

                        if (!j.isHiHaVaixell()) {
                            // Aigua
                            pJugada.setColor(Color.WHITE);
                            canvas.drawRoundRect(esq + 4, dlt + 4, esq + casellaAmplada - 4, dlt + casellaAlt - 4, 15f, 15f, pJugada);
                        } else {
                            // Tocat/Enfonsat
                            pJugada.setColor(j.getColorVaixell());
                            canvas.drawRoundRect(esq + 4, dlt + 4, esq + casellaAmplada - 4, dlt + casellaAlt - 4, 15f, 15f, pJugada);

                            Paint pPunt = new Paint();
                            pPunt.setColor(Color.BLACK);
                            canvas.drawCircle(esq + casellaAmplada / 2, dlt + casellaAlt / 2, 8f, pPunt);
                        }
                    }
                }
                surface.getHolder().unlockCanvasAndPost(canvas);
            }
        }
    }

    private void generarTextPistes() {
        if (vaixells == null) return; // Si no hi ha partida en curs, no fem res

        // Assegurem que l'inventari té la informació més recent
        construirInventari(JUGADOR_PROPI);
        construirInventari(JUGADOR_RIVAL);

        // Generem els textos HTML
        // Si estem connectats online, amaguem les caselles vives del rival
        String pistesPropi = generarStringPistes(JUGADOR_PROPI, false);
        String pistesRival = generarStringPistes(JUGADOR_RIVAL, connectat);

        // Posem els textos als TextViews amb suport per HTML
        TextView tvPistesPropi = findViewById(R.id.text_pistes_jugador);
        tvPistesPropi.setText(android.text.Html.fromHtml(pistesPropi, android.text.Html.FROM_HTML_MODE_LEGACY));

        TextView tvPistesRival = findViewById(R.id.text_pistes_rival);
        tvPistesRival.setText(android.text.Html.fromHtml(pistesRival, android.text.Html.FROM_HTML_MODE_LEGACY));

        // Calculem i mostrem els percentatges (Tocat / 20 caselles totals * 100)
        int percentatgePropi = comptarCasellesMortes(JUGADOR_PROPI) * 100 / 20;
        int percentatgeRival = comptarCasellesMortes(JUGADOR_RIVAL) * 100 / 20;

        TextView tvPercentatgePropi = findViewById(R.id.text_percentatge_jugador);
        tvPercentatgePropi.setText("Enfonsat: " + percentatgePropi + "%");

        TextView tvPercentatgeRival = findViewById(R.id.text_percentatge_rival);
        tvPercentatgeRival.setText("Enfonsat: " + percentatgeRival + "%");
    }

    private void construirInventari(int jugador) {
        UnsortedArrayMapping<Integer, UnsortedArraySet<Casella>> inventariJugador = inventariVaixells.get(jugador);
        UnsortedArrayMapping<Casella, Vaixell> vius = vaixells.get(jugador);
        UnsortedArrayMapping<Casella, Vaixell> morts = casellesEnfonsades.get(jugador);

        // Buidem l'inventari per refer-lo actualitzat
        for (int i = 0; i < 10; i++) inventariJugador.put(i, new UnsortedArraySet<>(4));

        // Afegim caselles vives
        if (vius != null) {
            Iterator<UnsortedArrayMapping<Casella, Vaixell>.Pair> itVius = vius.iterator();
            while (itVius.hasNext()) {
                UnsortedArrayMapping<Casella, Vaixell>.Pair p = itVius.next();
                inventariJugador.get(p.getValue().getId()).add(p.getKey());
            }
        }
        // Afegim caselles tocades/enfonsades
        if (morts != null) {
            Iterator<UnsortedArrayMapping<Casella, Vaixell>.Pair> itMorts = morts.iterator();
            while (itMorts.hasNext()) {
                UnsortedArrayMapping<Casella, Vaixell>.Pair p = itMorts.next();
                inventariJugador.get(p.getValue().getId()).add(p.getKey());
            }
        }
    }

    private String generarStringPistes(int jugador, boolean amagarVives) {
        StringBuilder sb = new StringBuilder();
        UnsortedArrayMapping<Integer, UnsortedArraySet<Casella>> inventariJugador = inventariVaixells.get(jugador);
        UnsortedArrayMapping<Casella, Vaixell> enfonsadesJugador = casellesEnfonsades.get(jugador);

        for (int idVaixell = 0; idVaixell < 10; idVaixell++) {
            UnsortedArraySet<Casella> casellesVaixell = inventariJugador.get(idVaixell);
            if (casellesVaixell == null || casellesVaixell.isEmpty()) continue;

            sb.append("<b>Vaixell ").append(idVaixell).append(":</b> ");

            Iterator<Casella> it = casellesVaixell.iterator();
            boolean first = true;
            while (it.hasNext()) {
                Casella c = it.next();
                if (!first) sb.append(", ");
                first = false;

                boolean estaTocada = (enfonsadesJugador.get(c) != null);

                if (estaTocada) {
                    sb.append("<strong><font color='red'>").append(c.toString()).append("</font></strong>");
                } else {
                    if (amagarVives) sb.append("<i>[Ocult]</i>");
                    else sb.append(c.toString());
                }
            }
            sb.append("<br>");
        }
        return sb.toString();
    }

    private int comptarCasellesMortes(int jugador) {
        int count = 0;
        UnsortedArrayMapping<Casella, Vaixell> morts = casellesEnfonsades.get(jugador);
        if (morts != null) {
            Iterator<UnsortedArrayMapping<Casella, Vaixell>.Pair> it = morts.iterator();
            while (it.hasNext()) { it.next(); count++; }
        }
        return count;
    }

}