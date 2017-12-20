/*
 * Copyright © 2017 zte and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.p4plugin.runtime.impl.table.entry;

import org.opendaylight.p4plugin.runtime.impl.device.DeviceManager;
import org.opendaylight.p4plugin.runtime.impl.device.P4Device;
import org.opendaylight.p4plugin.p4runtime.proto.*;
import org.opendaylight.yang.gen.v1.urn.opendaylight.p4plugin.runtime.table.rev170808.EntryKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.p4plugin.runtime.table.rev170808.TableEntry;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class TableEntryOperator {
    private String nodeId;

    public TableEntryOperator(String nodeId) {
        this.nodeId = nodeId;
    }

    private P4Device getDevice(String nodeId) {
        return DeviceManager.getInstance().findConfiguredDevice(nodeId);
    }

    public boolean add(TableEntry inputEntry) {
        org.opendaylight.p4plugin.p4runtime.proto.TableEntry tableEntry =
                getDevice(nodeId).toMessage(inputEntry);
        return operate(tableEntry, Update.Type.INSERT);
    }

    public boolean modify(TableEntry inputEntry) {
        org.opendaylight.p4plugin.p4runtime.proto.TableEntry tableEntry =
                getDevice(nodeId).toMessage(inputEntry);
        return operate(tableEntry, Update.Type.MODIFY);
    }

    public boolean delete(EntryKey key) {
        org.opendaylight.p4plugin.p4runtime.proto.TableEntry tableEntry =
                getDevice(nodeId).toMessage(key);
        return operate(tableEntry, Update.Type.DELETE);
    }

    public List<String> read(String tableName) {
        P4Device device = getDevice(nodeId);
        ReadRequest.Builder request = ReadRequest.newBuilder();
        Entity.Builder entityBuilder = Entity.newBuilder();
        org.opendaylight.p4plugin.p4runtime.proto.TableEntry.Builder entryBuilder =
                org.opendaylight.p4plugin.p4runtime.proto.TableEntry.newBuilder();
        entryBuilder.setTableId(tableName == null ? 0 : device.getTableId(tableName));
        entityBuilder.setTableEntry(entryBuilder);
        request.addEntities(entityBuilder);
        request.setDeviceId(getDevice(nodeId).getDeviceId());

        Iterator<ReadResponse> responses = device.read(request.build());
        List<java.lang.String> result = new ArrayList<>();

        while (responses.hasNext()) {
            ReadResponse response = responses.next();
            List<Entity> entityList = response.getEntitiesList();
            boolean isCompleted = response.getComplete();
            entityList.forEach(entity-> {
                java.lang.String str = device.toString(entity.getTableEntry());
                result.add(str);
            });
            if (isCompleted) break;
        }
        return result;
    }

    private boolean operate(org.opendaylight.p4plugin.p4runtime.proto.TableEntry entry, Update.Type type) {
        P4Device device = getDevice(nodeId);
        WriteRequest.Builder requestBuilder = WriteRequest.newBuilder();
        Update.Builder updateBuilder = Update.newBuilder();
        Entity.Builder entityBuilder = Entity.newBuilder();
        entityBuilder.setTableEntry(entry);
        updateBuilder.setType(type);
        updateBuilder.setEntity(entityBuilder);
        requestBuilder.setDeviceId(device.getDeviceId());
        requestBuilder.addUpdates(updateBuilder);
        return device.write(requestBuilder.build()) != null;
    }
}