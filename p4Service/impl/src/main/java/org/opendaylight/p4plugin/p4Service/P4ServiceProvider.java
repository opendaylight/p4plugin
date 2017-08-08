/*
 * Copyright © 2017 zte and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.p4plugin.p4Service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class P4ServiceProvider {
    private static final Logger LOG = LoggerFactory.getLogger(P4ServiceProvider.class);

    public P4ServiceProvider() {
    }

    /**
     * Method called when the blueprint container is created.
     */

    public void init() {
        LOG.info("P4ServiceProvider Session Initiated");
    }


    /**
     * Method called when the blueprint container is destroyed.
     */

    public void close() {
        LOG.info("P4ServiceProvider Closed");
    }
}
