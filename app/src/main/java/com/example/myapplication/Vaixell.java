package com.example.myapplication;

public class Vaixell {

    // Constants per a l'orientació recomanades per l'enunciat
    public static final int HORITZONTAL = 0;
    public static final int VERTICAL = 1;


    // Atributs obligatoris
    private int id;
    private int mida;
    private int orientacio;
    private int color;
    private int jugador;

    // Constructor
    public Vaixell(int id, int mida, int orientacio, int color, int jugador) {
        this.id = id;
        this.mida = mida;
        this.orientacio = orientacio;
        this.color = color;
        this.jugador = jugador;
    }

    // Mètodes d'accés (Getters)
    public int getId() {
        return id;
    }

    public int getMida() {
        return mida;
    }

    public int getOrientacio() {
        return orientacio;
    }

    public int getColor() {
        return color;
    }

    public int getJugador() {
        return jugador;
    }
}