/*
 * Copyright (c) 2015, Bruce Schubert <bruce@emxsys.com>
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 *     - Redistributions of source code must retain the above copyright
 *       notice, this list of conditions and the following disclaimer.
 *
 *     - Redistributions in binary form must reproduce the above copyright
 *       notice, this list of conditions and the following disclaimer in the
 *       documentation and/or other materials provided with the distribution.
 *
 *     - Neither the name of Bruce Schubert, Emxsys nor the names of its 
 *       contributors may be used to endorse or promote products derived
 *       from this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR
 * ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package com.emxsys.chart.extension;


/**
 *
 * Immutable.
 *
 * @author Bruce Schubert
 */
public abstract class TickUnit implements Comparable {

    /**
     * The size of the tick unit.
     */
    private double size;
    /**
     * The number of minor tick units between major units. The default value established in
     * ValueAxis is 5, which is the number is minor subdivisions within the major unit.
     */
    private int minorTickCount;


    public TickUnit() {
        this(1, 9);
    }


    public TickUnit(double size) {
        this(size, 9);
    }


    // Compatible with JFree signature
    public TickUnit(double size, int minorTickCount) {
        this.size = size;
        this.minorTickCount = minorTickCount;
    }


    public double getSize() {
        return size;
    }


    public int getMinorTickCount() {
        return minorTickCount;
    }


    /**
     * Get the string label name for a tick mark with the given value
     *
     * @param value The value to format into a tick label string
     * @return A formatted string for the given value
     */
    public abstract String getTickMarkLabel(Number value);


    @Override
    public int compareTo(Object obj) {
        if (this.equals(obj)) {
            return 0;
        }
        final TickUnit other = (TickUnit) obj;
        if (this.getSize() < other.getSize()) {
            return -1;
        }
        if (this.getSize() > other.getSize()) {
            return 1;
        }
        return 0;
    }


    @Override
    public int hashCode() {
        int hash = 7;
        hash = 67 * hash + (int) (Double.doubleToLongBits(this.size) ^ (Double.doubleToLongBits(this.size) >>> 32));
        hash = 67 * hash + this.minorTickCount;
        return hash;
    }


    @Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final TickUnit other = (TickUnit) obj;
        if (this.size != other.size) {
            return false;
        }
        return this.minorTickCount == other.minorTickCount;
    }

}
