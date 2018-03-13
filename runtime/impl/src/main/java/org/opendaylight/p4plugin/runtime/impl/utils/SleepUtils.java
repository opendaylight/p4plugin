package org.opendaylight.p4plugin.runtime.impl.utils;

import java.util.concurrent.TimeUnit;

public class SleepUtils {
    public static final void seconds(long seconds) {
        try {
            TimeUnit.SECONDS.sleep(seconds);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}
