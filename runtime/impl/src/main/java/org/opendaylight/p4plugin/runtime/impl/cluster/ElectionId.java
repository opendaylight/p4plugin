/*
 * Copyright © 2017 zte and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.p4plugin.runtime.impl.cluster;

public class ElectionId {
    private Long high;
    private Long low;

    public ElectionId(Long high, Long low) {
        this.high = high;
        this.low = low;
    }

    public void setHigh(Long high) {
        this.high = high;
    }

    public Long getHigh() {
        return high;
    }

    public void setLow(Long low) {
        this.low = low;
    }

    public Long getLow() {
        return low;
    }
}
