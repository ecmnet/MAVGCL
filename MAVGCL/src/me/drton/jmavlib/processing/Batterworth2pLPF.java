package me.drton.jmavlib.processing;

/**
 * 2-nd order Batterworth low pass filter.
 */
public class Batterworth2pLPF extends Filter {
    private double a1 = 0.0;
    private double a2 = 0.0;
    private double b0 = 0.0;
    private double b1 = 0.0;
    private double b2 = 0.0;
    private double[] elements = new double[2];    // buffered elements

    @Override
    public void setCutoffFreqFactor(double cutoffFreqFactor) {
        super.setCutoffFreqFactor(cutoffFreqFactor);
        double r = Math.tan(Math.PI * this.cutoffFreqFactor);
        double c = 1.0 + 2.0 * Math.cos(Math.PI / 4.0) * r + r * r;
        b0 = r * r / c;
        b1 = 2.0 * b0;
        b2 = b0;
        a1 = 2.0 * (r * r - 1.0) / c;
        a2 = (1.0 - 2.0 * Math.cos(Math.PI / 4.0) * r + r * r) / c;
        reset(0.0);
    }

    /**
     * Add a new raw value to the filter
     *
     * @return retrieve the filtered result
     */
    @Override
    public double apply(double sample) {
        if (cutoffFreqFactor <= 0.0) {
            return sample;
        }

        double element_0 = sample - elements[0] * a1 - elements[1] * a2;

        if (Double.isNaN(element_0)) {
            element_0 = sample;
        }

        double output = element_0 * b0 + elements[0] * b1 + elements[1] * b2;

        elements[1] = elements[0];
        elements[0] = element_0;

        return output;
    }

    /**
     * Reset the filter state to this value
     */
    @Override
    public double reset(double sample) {
        double v = sample / (b0 + b1 + b2);
        elements[0] = v;
        elements[1] = v;
        return apply(sample);
    }
}
