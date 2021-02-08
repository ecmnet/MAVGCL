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

import com.emxsys.chart.extension.TickUnit;
import com.emxsys.chart.extension.NumberTickUnit;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * A JFreeChart-based class used by the DateAxis and NumericAxis classes to
 * obtain a suitable {@link TickUnit}.
 *
 * @author Bruce Schubert
 */
public class TickUnitSource {

    List<TickUnit> tickUnits = new ArrayList<>();


    public void add(TickUnit tickUnit) {
        tickUnits.add(tickUnit);
        Collections.sort(tickUnits);
    }

    /**
     * Returns a tick unit that is larger than the supplied unit.
     *
     * @param unit the unit.
     *
     * @return A tick unit that is larger than the supplied unit.
     */
    public TickUnit getLargerTickUnit(TickUnit unit) {
        int index = Collections.binarySearch(this.tickUnits, unit, null);
        if (index >= 0) {
            index = index + 1;
        } else {
            index = -index;
        }
        return this.tickUnits.get(Math.min(index, this.tickUnits.size() - 1));
    }

    /**
     * Returns the tick unit in the collection that is greater than or equal to
     * (in size) the specified unit.
     *
     * @param unit the unit.
     *
     * @return A unit from the collection.
     */
    public TickUnit getCeilingTickUnit(TickUnit unit) {
        int index = Collections.binarySearch(this.tickUnits, unit, null);
        if (index >= 0) {
            return this.tickUnits.get(index);
        } else {
            index = -(index + 1);
            return this.tickUnits.get(Math.min(index, this.tickUnits.size() - 1));
        }

    }

    /**
     * Returns the tick unit in the collection that is greater than or equal to
     * the specified size.
     *
     * @param size the size.
     *
     * @return A unit from the collection.
     */
    public TickUnit getCeilingTickUnit(double size) {
        return getCeilingTickUnit(new NumberTickUnit(size));
    }
}
