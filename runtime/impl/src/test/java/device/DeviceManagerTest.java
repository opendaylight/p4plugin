/*
 * Copyright © 2017 zte and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package device;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.*;
import org.opendaylight.p4plugin.runtime.impl.device.DeviceManager;
import org.opendaylight.p4plugin.runtime.impl.device.P4Device;

import java.io.IOException;
import java.util.concurrent.ConcurrentHashMap;

public class DeviceManagerTest {
    @InjectMocks
    DeviceManager manager = DeviceManager.getInstance();

    @Spy
    private volatile ConcurrentHashMap<String, P4Device> devices = new ConcurrentHashMap<>();

    @Before
    public void before() {
        MockitoAnnotations.initMocks(this);
    }

    @Test
    public void testSingleInstance() {
        DeviceManager manager1 = DeviceManager.getInstance();
        DeviceManager manager2 = DeviceManager.getInstance();
        Assert.assertTrue(manager1 == manager2);
    }

    @Test(expected = IOException.class)
    public void testAddDevice1() throws IOException {
        manager.addDevice("zte", (long)0, "127.0.0.1", 50051, "/home/zte.txt",
                "/home/etz.txt");
    }

    @Test
    public void testAddDevice2() throws IOException {
        manager.addDevice("zte", (long)0, "127.0.0.1", 50051, null,null);
        Assert.assertTrue(manager.findDevice("zte").isPresent());
    }

    @Test(expected = IllegalArgumentException.class)
    public void testAddDevice3() throws IOException {
        manager.addDevice("zte", (long)0, "127.0.0.1", 50051, null,null);
        manager.addDevice("zte", (long)0, "127.0.0.1", 50051, null,null);
    }

    @After
    public void after() {
        devices.clear();
    }
}
