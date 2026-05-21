package com.example.myapplication;

public class Jugada {
    private int jugadorQueDispara; // JUGADOR_PROPI o JUGADOR_RIVAL
    private Casella casellaAtacada;
    private boolean hiHaVaixell;
    private int colorVaixell; // Guardem el color si l'ha tocat (o Color.WHITE si és aigua)

    public Jugada(int jugadorQueDispara, Casella casellaAtacada, boolean hiHaVaixell, int colorVaixell) {
        this.jugadorQueDispara = jugadorQueDispara;
        this.casellaAtacada = casellaAtacada;
        this.hiHaVaixell = hiHaVaixell;
        this.colorVaixell = colorVaixell;
    }

    public int getJugadorQueDispara() { return jugadorQueDispara; }
    public Casella getCasellaAtacada() { return casellaAtacada; }
    public boolean isHiHaVaixell() { return hiHaVaixell; }
    public int getColorVaixell() { return colorVaixell; }
}