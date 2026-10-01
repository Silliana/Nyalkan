package net.nyalkan.tick;

import java.util.ArrayList;

final class TwoTierQueue<T> {
    private final ArrayList<T> soon = new ArrayList<>();
    private final ArrayList<T> later = new ArrayList<>();
    private int nextSoon;
    private int nextLater;

    void add(T item, boolean soon) {
        (soon ? this.soon : this.later).add(item);
    }

    int remaining() {
        return this.soon.size() - this.nextSoon + this.later.size() - this.nextLater;
    }

    T poll() {
        return this.nextSoon < this.soon.size() ? this.soon.get(this.nextSoon++) : this.later.get(this.nextLater++);
    }

    void clear() {
        this.soon.clear();
        this.later.clear();
        this.nextSoon = 0;
        this.nextLater = 0;
    }
}
