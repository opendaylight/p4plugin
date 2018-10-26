/*
 * Copyright © 2017 zte and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.p4plugin.gnmi;

import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import org.checkerframework.checker.nullness.compatqual.NullableDecl;
import org.opendaylight.p4plugin.gnmi.proto.Update;
import org.opendaylight.yang.gen.v1.opendaylight.tsdr.rev150219.DataCategory;
import org.opendaylight.yang.gen.v1.opendaylight.tsdr.rev150219.tsdrrecord.RecordKeys;
import org.opendaylight.yang.gen.v1.opendaylight.tsdr.rev150219.tsdrrecord.RecordKeysBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.config.tsdr.collector.spi.rev150915.InsertTSDRMetricRecordInputBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.config.tsdr.collector.spi.rev150915.InsertTSDRMetricRecordOutput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.config.tsdr.collector.spi.rev150915.TsdrCollectorSpiService;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.config.tsdr.collector.spi.rev150915.inserttsdrmetricrecord.input.TSDRMetricRecord;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.config.tsdr.collector.spi.rev150915.inserttsdrmetricrecord.input.TSDRMetricRecordBuilder;
import org.opendaylight.yangtools.yang.common.RpcError;
import org.opendaylight.yangtools.yang.common.RpcResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class DataStorage {
    private static final Logger LOG = LoggerFactory.getLogger(DataStorage.class);
    private TsdrCollectorSpiService tsdrCollectorSpiService;
    private static DataStorage instance = new DataStorage();

    private DataStorage() {}

    public void setTsdrCollectorSpiService(TsdrCollectorSpiService tsdrCollectorSpiService) {
        this.tsdrCollectorSpiService = tsdrCollectorSpiService;
    }

    public static DataStorage getInstance() {
        return instance;
    }

    private String buildPath(org.opendaylight.p4plugin.gnmi.proto.Path path) {
        StringBuilder pathBuilder = new StringBuilder();
        path.getElemList().forEach(pathElem -> {
            Map<String, String> keyMap = pathElem.getKeyMap();
            StringBuilder attributeBuilder = new StringBuilder();
            keyMap.forEach((k, v) -> {
                attributeBuilder.append("$").append(k).append("=").append(v).append("$");
            });

            String name = pathElem.getName();
            pathBuilder.append("/").append(name).append(attributeBuilder.toString());
        });
        return pathBuilder.toString();
    }

    private void recordRpcErrors(Collection<RpcError> errors) {
        StringBuilder builder = new StringBuilder();
        for(RpcError e : errors) {
            builder.append("type = ").append(e.getErrorType())
                    .append("message = ").append(e.getMessage())
                    .append(";");
        }
        LOG.error("Write data to TSDR failed, " + builder.toString());
    }

    public void writeData2Tsdr(String nodeId, org.opendaylight.p4plugin.gnmi.proto.Notification notification) {
        InsertTSDRMetricRecordInputBuilder builder = new InsertTSDRMetricRecordInputBuilder();
        builder.setCollectorCodeName("P4-gNMI");
        List<TSDRMetricRecord> tsdrMetricRecordList = new ArrayList<>();
        List<Update> updateList = notification.getUpdateList();
        long timestamp = notification.getTimestamp();

        updateList.forEach(update -> {
            org.opendaylight.p4plugin.gnmi.proto.Path path = update.getPath();
            org.opendaylight.p4plugin.gnmi.proto.TypedValue typedValue = update.getVal();
            String pathStr = buildPath(path);

            String metric_name;
            String reg = "(/.*)+/(.*)";
            Pattern pattern = Pattern.compile(reg);
            Matcher matcher = pattern.matcher(pathStr);
            if  (matcher.matches()) {
                metric_name = matcher.group(matcher.groupCount());
            } else {
                metric_name = pathStr;
            }

            TSDRMetricRecordBuilder tsdrMetricRecordBuilder = new TSDRMetricRecordBuilder();
            List<RecordKeys> recordKeysList = new ArrayList<>();
            RecordKeysBuilder recordKeysBuilder = new RecordKeysBuilder();
            recordKeysBuilder.setKeyName("OpenConfig-Path");
            recordKeysBuilder.setKeyValue(pathStr);
            recordKeysList.add(recordKeysBuilder.build());
            //tsdrMetricRecordBuilder.setMetricName(pathStr);
            tsdrMetricRecordBuilder.setMetricName(metric_name);
            tsdrMetricRecordBuilder.setMetricValue(BigDecimal.valueOf(typedValue.getUintVal()));
            tsdrMetricRecordBuilder.setNodeID(nodeId);
            tsdrMetricRecordBuilder.setTimeStamp(timestamp);
            tsdrMetricRecordBuilder.setRecordKeys(recordKeysList);
            tsdrMetricRecordBuilder.setTSDRDataCategory(DataCategory.EXTERNAL);
            tsdrMetricRecordList.add(tsdrMetricRecordBuilder.build());
        });
        builder.setTSDRMetricRecord(tsdrMetricRecordList);
        try {
            ListenableFuture<RpcResult<InsertTSDRMetricRecordOutput>> output =
                    tsdrCollectorSpiService.insertTSDRMetricRecord(builder.build());
            boolean result = output.get().isSuccessful();
            LOG.info("Write to tsdr {}.", result ? "success" : "failed");
        } catch (InterruptedException | ExecutionException e) {
            e.printStackTrace();
            LOG.error("Write to tsdr exception, message = {}.", e.getMessage());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
