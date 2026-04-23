package com.example.myapplication; // Asegúrate de que coincida con tu paquete

import java.util.Iterator;

public class UnsortedArraySet<E> {
    private E[] array;
    private int n;

    // Prepara l'array per emmagatzemar el conjunt
    @SuppressWarnings("unchecked")
    public UnsortedArraySet(int max) {
        // En Java no se pueden crear arrays de tipos genéricos directamente,
        // por lo que creamos un array de Object y hacemos el cast.
        array = (E[]) new Object[max];
        n = 0;
    }

    // Consultar si el conjunt conté l'element
    public boolean contains(E elem) {
        int i = 0;
        boolean trobat = false;
        while (!trobat && i < n) {
            trobat = elem.equals(array[i]);
            i++;
        }
        return trobat;
    }

    // Afegir un element al conjunt (si encara no es troba present)
    public boolean add(E elem) {
        if (n < array.length && !contains(elem)) {
            array[n] = elem;
            n++;
            return true; // S'ha afegit correctament
        } else {
            return false; // L'array està ple o ja hi és
        }
    }

    // Eliminar un element del conjunt (si es troba present)
    public boolean remove(E elem) {
        int i = 0;
        boolean trobat = false;
        while (!trobat && i < n) {
            trobat = elem.equals(array[i]);
            i++;
        }
        if (trobat) {
            // Movem el darrer element a la posició del que eliminem
            array[i - 1] = array[n - 1];
            // Eliminem la referència del darrer (opcional però recomanable per memòria)
            array[n - 1] = null;
            n--;
            return true;
        }
        return false;
    }

    // Consultar si el conjunt es troba buit
    public boolean isEmpty() {
        return n == 0;
    }

    // Retorna l'iterador per poder recórrer el conjunt
    public Iterator<E> iterator() {
        return new IteratorUnsortedArraySet();
    }

    // Classe interna per a l'iterador
    private class IteratorUnsortedArraySet implements Iterator<E> {
        private int idxIterator;

        private IteratorUnsortedArraySet() {
            idxIterator = 0;
        }

        @Override
        public boolean hasNext() {
            return idxIterator < n;
        }

        @Override
        public E next() {
            idxIterator++;
            return array[idxIterator - 1];
        }
    }
}