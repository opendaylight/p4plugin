/*
 * Copyright © 2017 zte and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.p4plugin.core.impl.table;

import org.opendaylight.p4plugin.core.impl.device.DeviceManager;
import org.opendaylight.p4plugin.core.impl.device.P4Device;
import org.opendaylight.p4plugin.p4runtime.proto.*;
import org.opendaylight.yang.gen.v1.urn.opendaylight.p4plugin.core.table.rev170808.EntryKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.p4plugin.core.table.rev170808.TableEntry;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public abstract class TableEntryOperator {
    protected P4Device device;
    protected Update.Type type;
    protected org.opendaylight.p4plugin.p4runtime.proto.TableEntry entry;

    private TableEntryOperator(String nodeId) {
        this.device = DeviceManager.getInstance().findConfiguredDevice(nodeId);
    }

    private TableEntryOperator(String nodeId, Update.Type type) {
        this(nodeId);
        this.type = type;
    }

    /**
     * This constructor is used to add and modify an entry.
     */
    private TableEntryOperator(String nodeId, Update.Type type, TableEntry entry) {
        this(nodeId, type);
        this.entry = device.toMessage(entry);
    }

    /**
     * This constructor is used to delete an entry.
     */
    private TableEntryOperator(String nodeId, Update.Type type, EntryKey key) {
        this(nodeId, type);
        this.entry = device.toMessage(key);
    }

    /**
     * Write table entry actually.
     */
    public boolean operate() {
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

    /**
     * Add an entry, update type is INSERT.
     */
    public static class AddTableEntryOperator extends TableEntryOperator {
        public AddTableEntryOperator(String nodeId, TableEntry entry) {
            super(nodeId, Update.Type.INSERT, entry);
        }
    }

    /**
     * Delete an entry, update type is DELETE. Only table name and match fields
     * are required to delete an entry.
     */
    public static class DeleteTableEntryOperator extends TableEntryOperator {
        public DeleteTableEntryOperator(String nodeId, EntryKey key ) {
            super(nodeId, Update.Type.DELETE, key);
        }
    }

    /**
     * Modify an entry, update type is MODIFY, similar with add.
     */
    public static class ModifyTableEntryOperator extends TableEntryOperator {
        public ModifyTableEntryOperator(String nodeId, TableEntry entry) {
            super(nodeId, Update.Type.MODIFY, entry);
        }
    }

    /**
     * Read all entries of a table.
     */
    public static class ReadTableEntryOperator extends TableEntryOperator {
        private String tableName;
        public ReadTableEntryOperator(String nodeId, String tableName) {
            super(nodeId);
            this.tableName = tableName;
        }

        public List<String> read() {
            ReadRequest.Builder request = ReadRequest.newBuilder();
            Entity.Builder entityBuilder = Entity.newBuilder();
            org.opendaylight.p4plugin.p4runtime.proto.TableEntry.Builder entryBuilder =
                    org.opendaylight.p4plugin.p4runtime.proto.TableEntry.newBuilder();
            entryBuilder.setTableId(tableName == null ? 0 : device.getTableId(tableName));
            entityBuilder.setTableEntry(entryBuilder);
            request.addEntities(entityBuilder);
            request.setDeviceId(device.getDeviceId());

            Iterator<ReadResponse> responses = device.read(request.build());
            List<String> result = new ArrayList<>();

            while (responses.hasNext()) {
                ReadResponse response = responses.next();
                List<Entity> entityList = response.getEntitiesList();
                boolean isCompleted = response.getComplete();
                entityList.forEach(entity-> {
                    String str = device.toString(entity.getTableEntry());
                    result.add(str);
                });
                if (isCompleted) break;
            }
            return result;
        }
    }
}