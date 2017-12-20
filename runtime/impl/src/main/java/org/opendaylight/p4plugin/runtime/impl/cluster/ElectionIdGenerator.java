/*
 * Copyright © 2017 zte and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.p4plugin.runtime.impl.cluster;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.ArrayList;

public class ElectionIdGenerator {
    private static final Logger LOG = LoggerFactory.getLogger(ElectionIdGenerator.class);
    private static ElectionIdGenerator singleton = new ElectionIdGenerator();
    private ElectionId electionId;
    private ArrayList<ElectionIdObserver> observers = new ArrayList<>();

    private ElectionIdGenerator() {
        electionId = new ElectionId((long)0, (long)0);
    }

    public static ElectionIdGenerator getInstance() {
        return singleton;
    }

    public ElectionId getElectionId() {
        return electionId;
    }

    public void setElectionId(ElectionId electionId) {
        this.electionId = electionId;
        notifyObservers();
    }

    public void addObserver(ElectionIdObserver observer) {
        observers.add(observer);
    }

    public void deleteObserver(ElectionIdObserver observer) {
        observers.remove(observer);
    }

    private void notifyObservers() {
        observers.forEach(observer -> observer.update(electionId));
    }


}

