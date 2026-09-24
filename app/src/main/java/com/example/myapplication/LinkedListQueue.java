package com.example.myapplication;

/**
 * Una implementación de cola (Queue) utilizando una lista enlazada simple.
 *
 * @param <E> El tipo de elementos almacenados en esta cola.
 */
public class LinkedListQueue<E> {
    private static class Node<E> {
        private final E elem;
        private Node<E> next;

        public Node(E item, Node<E> next) {
            this.elem = item;
            this.next = next;
        }
    }

    private Node<E> head; // Nodo apuntando al frente de la cola (para eliminar)
    private Node<E> tail; // Nodo apuntando al final de la cola (para insertar)

    /**
     * Constructor por defecto para inicializar una cola vacía.
     */
    public LinkedListQueue() {
        head = null;
        tail = null;
    }

    /**
     * Inserta un elemento al final de la cola.
     *
     * @param item El elemento a insertar.
     * @return true si se añadió con éxito, false si no hay memoria disponible.
     */
    public boolean put(E item) {
        try {
            Node<E> r = new Node<>(item, null);
            if (tail == null) {
                tail = r;
                head = r;
            } else {
                tail.next = r;
                tail = r;
            }
            return true;
        } catch (OutOfMemoryError e) {
            return false;
        }
    }

    /**
     * Elimina el primer elemento de la cola.
     *
     * @return true si se eliminó con éxito, false si la cola estaba vacía.
     */
    public boolean removeFirst() {
        if (head == null) {
            return false;
        } else {
            head = head.next;
            if (head == null) {
                tail = null;
            }
            return true;
        }
    }

    /**
     * Obtiene el primer elemento de la cola sin eliminarlo.
     *
     * @return El primer elemento, o null si la cola está vacía.
     */
    public E getFirst() {
        if (head == null) {
            return null;
        } else {
            return head.elem;
        }
    }

    /**
     * Verifica si la cola está vacía.
     *
     * @return true si la cola no contiene elementos, false en caso contrario.
     */
    public boolean isEmpty() {
        return head == null;
    }

    /**
     * Vacía completamente la cola.
     */
    public void empty() {
        tail = null;
        head = null;
    }
}
