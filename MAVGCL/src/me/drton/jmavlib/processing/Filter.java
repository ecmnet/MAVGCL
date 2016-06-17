package me.drton.jmavlib.processing;

/**
 * Abstract filter class.
 */
abstract public class Filter {
    protected double cutoffFreqFactor;

    /**
     * Set filter cutoff frequency factor, i.e. cutoff_freq / sample_freq.
     * Value <= 0 disables filter (pass-through mode).
     */
    public void setCutoffFreqFactor(double cutoffFreqFactor) {
        this.cutoffFreqFactor = cutoffFreqFactor;
    }

    /**
     * Return the cutoff frequency factor.
     */
    public double getCutoffFreqFactor() {
        return cutoffFreqFactor;
    }

    /**
     * Add a new input value to the filter.
     *
     * @return retrieve the filtered result
     */
    public abstract double apply(double sample);

    /**
     * Reset the filter state to this value.
     */
    public abstract double reset(double sample);
}
