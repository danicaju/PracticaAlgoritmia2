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

    @Override
    public boolean equals(Object obj) {
        // Si és exactament el mateix objecte en memòria, són iguals
        if (this == obj) return true;

        // Si l'altre objecte és nul o no és una Casella, no són iguals
        if (obj == null || getClass() != obj.getClass()) return false;

        // Convertim l'objecte genèric a Casella
        Casella altraCasella = (Casella) obj;

        // Dues caselles són iguals si tenen la mateixa columna (i) i fila (j)
        return this.coordenadaX == altraCasella.coordenadaX && this.coordenadaY == altraCasella.coordenadaY;
    }
}
