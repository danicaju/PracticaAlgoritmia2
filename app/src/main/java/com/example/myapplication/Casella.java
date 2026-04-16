package com.example.myapplication;

public class Casella {
    private int coordenadaX;
    private int coordenadaY;

    public Casella(int coordenadaX, int coordenadaY) {
        this.coordenadaX = coordenadaX;
        this.coordenadaY = coordenadaY;
    }

    public int getCoordenadaX() {
        return coordenadaX;
    }

    public int getCoordenadaY() {
        return coordenadaY;
    }

    public void setCoordenadaX(int coordenadaX) {
        this.coordenadaX = coordenadaX;
    }

    public void setCoordenadaY(int coordenadaY) {
        this.coordenadaY = coordenadaY;
    }
    @Override
    public String toString() {
        return "(" + coordenadaX + "," + coordenadaY + ")";
    }
}
