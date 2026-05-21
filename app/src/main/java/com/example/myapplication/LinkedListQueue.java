package com.example.myapplication;

public class LinkedListQueue<E> {
    private class Node {
        private E elem;
        private Node next;

        public Node(E item, Node next) {
            this.elem = item;
            this.next = next;
        }
    }
    private Node p, q;
    public LinkedListQueue(){
        p = null;
        q = null;
    }
    public boolean put(E item) {
        try {
            Node r = new Node(item, null);
            if (p == null) {
                p = r;
                q = r;
            } else {
                p.next = r;
                p = r;
            }
            return true;
        } catch (OutOfMemoryError e) {
            return false;
        }
    }
    public boolean removeFirst() {
        if (q == null) {
            return false;
        } else {
            q = q.next;
            if (q == null) {
                p = null;
            }
            return true;
        }
    }

    public E getFirst() {
        if (q == null) {
            return null;
        } else {
            return q.elem;
        }
    }
    public boolean isEmpty() {
        return q == null;
    }
    public void empty() {
        p = null;
        q = null;
    }
}
