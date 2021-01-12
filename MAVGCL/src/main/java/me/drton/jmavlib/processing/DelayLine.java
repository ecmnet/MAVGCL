package me.drton.jmavlib.processing;

import java.util.LinkedList;
import java.util.List;

/**
 * User: ton Date: 01.07.13 Time: 11:16
 */
public class DelayLine<T> {
    private double delay = 0.0;
    private List<Tick<T>> buffer = new LinkedList<Tick<T>>();
    T value = null;

    public void reset() {
        buffer.clear();
        value = null;
    }

    public double getDelay() {
        return delay;
    }

    public void setDelay(double delay) {
        this.delay = delay;
    }

    public T getOutput(double time) {
        while (!buffer.isEmpty()) {
            Tick<T> tick = buffer.get(0);
            if (time - tick.time < delay) {
                break;
            }
            value = tick.value;
            buffer.remove(0);
        }
        return value;
    }

    public T getOutput(double time, T in) {
        buffer.add(new Tick<T>(time, in));
        return getOutput(time);
    }

    public static class Tick<V> {
        public final double time;
        public final V value;

        public Tick(double time, V value) {
            this.time = time;
            this.value = value;
        }
    }
}
