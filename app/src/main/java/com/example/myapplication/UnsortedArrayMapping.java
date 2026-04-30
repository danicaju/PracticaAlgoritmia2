package com.example.myapplication;

import java.util.Iterator;

public class UnsortedArrayMapping<K, V> {
    private K[] claus;
    private V[] valors;
    private int n;

    // Prepara els arrays per emmagatzemar el mapping
    @SuppressWarnings("unchecked")
    public UnsortedArrayMapping(int max) {
        claus = (K[]) new Object[max];
        valors = (V[]) new Object[max];
        n = 0;
    }

    // Consultar el valor associat a la clau
    public V get(K key) {
        for (int i = 0; i < n; i++) {
            if (claus[i].equals(key)) {
                return valors[i];
            }
        }
        return null; // Si no el troba
    }

    // Afegir una parella clau-valor
    public V put(K key, V value) {
        // Primer comprovem si la clau ja existeix
        for (int i = 0; i < n; i++) {
            if (claus[i].equals(key)) {
                V valorAntic = valors[i];
                valors[i] = value; // Actualitzem el valor
                return valorAntic; // Retorna el valor anterior associat a la clau
            }
        }

        // Si no existeix i hi ha espai, l'afegim al final
        if (n < claus.length) {
            claus[n] = key;
            valors[n] = value;
            n++;
        }
        return null; // Retorna null si no hi havia assignació prèvia
    }

    // Eliminar l'associació d'una clau
    public V remove(K key) {
        for (int i = 0; i < n; i++) {
            if (claus[i].equals(key)) {
                V valorAntic = valors[i];

                // Movem el darrer element a la posició esborrada (com al Set)
                claus[i] = claus[n - 1];
                valors[i] = valors[n - 1];

                claus[n - 1] = null;
                valors[n - 1] = null;
                n--;

                return valorAntic; // Retorna el valor associat a la clau
            }
        }
        return null;
    }

    // Consultar si el mapping es troba buit
    public boolean isEmpty() {
        return n == 0;
    }

    // CLASSE PAIR I ITERADOR

    // Classe per encapsular la parella clau-valor i retornar-la en l'iterador
    public class Pair {
        private K key;
        private V value;

        public Pair(K key, V value) {
            this.key = key;
            this.value = value;
        }

        public K getKey() { return key; }
        public V getValue() { return value; }
    }

    public Iterator<Pair> iterator() {
        return new IteratorUnsortedArrayMapping();
    }

    private class IteratorUnsortedArrayMapping implements Iterator<Pair> {
        private int idxIterator;

        private IteratorUnsortedArrayMapping() {
            idxIterator = 0;
        }

        @Override
        public boolean hasNext() {
            return idxIterator < n;
        }

        @Override
        public Pair next() {
            // Retornem un Pair amb la clau i el valor actual
            Pair p = new Pair(claus[idxIterator], valors[idxIterator]);
            idxIterator++;
            return p;
        }
    }
}
