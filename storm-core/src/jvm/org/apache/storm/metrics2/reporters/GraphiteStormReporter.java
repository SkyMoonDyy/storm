/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 * <p/>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p/>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.storm.metrics2.reporters;

import com.codahale.metrics.graphite.GraphiteReporter;
import com.codahale.metrics.graphite.GraphiteSender;
import com.codahale.metrics.graphite.GraphiteUDP;
import com.codahale.metrics.graphite.Graphite;
import com.codahale.metrics.MetricRegistry;
import org.apache.storm.daemon.metrics.MetricsUtils;
import org.apache.storm.metrics2.filters.StormMetricsFilter;
import org.apache.storm.utils.Utils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.concurrent.TimeUnit;

public class GraphiteStormReporter extends ScheduledStormReporter<GraphiteReporter> {
    private final static Logger LOG = LoggerFactory.getLogger(GraphiteStormReporter.class);

    public static final String GRAPHITE_PREFIXED_WITH = "graphite.prefixed.with";
    public static final String GRAPHITE_HOST = "graphite.host";
    public static final String GRAPHITE_PORT = "graphite.port";
    public static final String GRAPHITE_TRANSPORT = "graphite.transport";

    @Override
    public void prepare(MetricRegistry metricsRegistry, Map stormConf, Map reporterConf) {
        LOG.debug("Preparing...");
        GraphiteReporter.Builder builder = GraphiteReporter.forRegistry(metricsRegistry);

        TimeUnit durationUnit = MetricsUtils.getMetricsDurationUnit(reporterConf);
        if (durationUnit != null) {
            builder.convertDurationsTo(durationUnit);
        }

        TimeUnit rateUnit = MetricsUtils.getMetricsRateUnit(reporterConf);
        if (rateUnit != null) {
            builder.convertRatesTo(rateUnit);
        }

        StormMetricsFilter filter = getMetricsFilter(reporterConf);
        if(filter != null){
            builder.filter(filter);
        }
        String prefix = getMetricsPrefixedWith(reporterConf);
        if (prefix != null) {
            builder.prefixedWith(prefix);
        }

        //defaults to 10
        reportingPeriod = getReportPeriod(reporterConf);

        //defaults to seconds
        reportingPeriodUnit = getReportPeriodUnit(reporterConf);

        // Not exposed:
        // * withClock(Clock)

        String host = getMetricsTargetHost(reporterConf);
        Integer port = getMetricsTargetPort(reporterConf);
        String transport = getMetricsTargetTransport(reporterConf);
        GraphiteSender sender = null;
        //TODO: error checking
        if (transport.equalsIgnoreCase("udp")) {
            sender = new GraphiteUDP(host, port);
        } else {
            //TODO: pickled support
            sender = new Graphite(host, port);
        }
        reporter = builder.build(sender);
    }

    private static String getMetricsPrefixedWith(Map reporterConf) {
        return Utils.getString(reporterConf.get(GRAPHITE_PREFIXED_WITH), null);
    }

    private static String getMetricsTargetHost(Map reporterConf) {
        return Utils.getString(reporterConf.get(GRAPHITE_HOST), null);
    }

    private static Integer getMetricsTargetPort(Map reporterConf) {
        return Utils.getInt(reporterConf.get(GRAPHITE_PORT), null);
    }

    private static String getMetricsTargetTransport(Map reporterConf) {
        return Utils.getString(reporterConf.get(GRAPHITE_TRANSPORT), "tcp");
    }
}