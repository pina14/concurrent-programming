package pc.serie2.messageQueueOptimized;

import java.util.concurrent.atomic.AtomicReference;

public class LinkedQueueNB<E> {
    private static class Node<E> {
        final E item;
        final AtomicReference<Node<E>> next;

        Node(E item, Node<E> next) {
            this.item = item;
            this.next = new AtomicReference<>(next);
        }
    }

    private final AtomicReference<Node<E>> head;
    private final AtomicReference<Node<E>> tail;
    public LinkedQueueNB() {
        Node<E> dummy = new Node<>(null, null);
        head = new AtomicReference<>(dummy);
        tail = new AtomicReference<>(dummy);
    }

    public boolean put(E item) {
        Node<E> newNode = new Node<>(item, null);
        while(true) {
            Node<E> curTail = tail.get();
            Node<E> tailNext = curTail.next.get();

            if(curTail == tail.get()) {
                if(tailNext != null) {
                    tail.compareAndSet(curTail, tailNext);
                } else if(curTail.next.compareAndSet(null, newNode)) {
                    tail.compareAndSet(curTail, newNode);
                    return true;
                }
            }
        }
    }

    public E get() {
        Node<E> curHead, newHead;
        do {
            curHead = head.get().next.get();
            newHead = curHead.next.get();
        } while(!head.get().next.compareAndSet(curHead, newHead));

        return curHead.item;
    }

    public boolean isEmpty() {
        return head.get().next.get() == null;
    }

    public boolean isNotEmpty() {
        return head.get().next.get() != null;
    }
}
