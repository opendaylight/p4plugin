/*
 * Copyright © 2017 zte and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.p4plugin.appcommon.topo;

import com.google.gson.Gson;
import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;

public class TopoParser {
    private String path;
    private Topo topo;
    public TopoParser(String path) {
        this.path = path;
    }

    public void parse() {
        try {
            String input = FileUtils.readFileToString(new File(path), Charset.forName("UTF-8"));
            topo = new Gson().fromJson(input, Topo.class);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public Topo getTopo() {
        return topo;
    }
}
