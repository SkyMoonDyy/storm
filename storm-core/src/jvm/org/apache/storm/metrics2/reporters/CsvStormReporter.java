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

import com.codahale.metrics.CsvReporter;
import com.codahale.metrics.MetricRegistry;
import org.apache.storm.daemon.metrics.MetricsUtils;
import org.apache.storm.metrics2.filters.StormMetricsFilter;
import org.apache.storm.utils.ConfigUtils;
import org.apache.storm.utils.Utils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.TimeUnit;

public class CsvStormReporter extends ScheduledStormReporter<CsvReporter> {
    private final static Logger LOG = LoggerFactory.getLogger(CsvStormReporter.class);

    public static final String CSV_LOG_DIR = "csv.log.dir";

    @Override
    public void prepare(MetricRegistry metricsRegistry, Map stormConf, Map reporterConf) {
        LOG.debug("Preparing...");
        CsvReporter.Builder builder = CsvReporter.forRegistry(metricsRegistry);

        Locale locale = MetricsUtils.getMetricsReporterLocale(reporterConf);
        if (locale != null) {
            builder.formatFor(locale);
        }

        TimeUnit rateUnit = MetricsUtils.getMetricsRateUnit(reporterConf);
        if (rateUnit != null) {
            builder.convertRatesTo(rateUnit);
        }

        TimeUnit durationUnit = MetricsUtils.getMetricsDurationUnit(reporterConf);
        if (durationUnit != null) {
            builder.convertDurationsTo(durationUnit);
        }

        StormMetricsFilter filter = getMetricsFilter(reporterConf);
        if(filter != null){
            builder.filter(filter);
        }


        //defaults to 10
        reportingPeriod = getReportPeriod(reporterConf);

        //defaults to seconds
        reportingPeriodUnit = getReportPeriodUnit(reporterConf);

        File csvMetricsDir = getCsvLogDir(stormConf, reporterConf);
        reporter = builder.build(csvMetricsDir);
    }


    private static File getCsvLogDir(Map stormConf, Map reporterConf) {
        String csvMetricsLogDirectory = Utils.getString(reporterConf.get(CSV_LOG_DIR), null);
        if (csvMetricsLogDirectory == null) {
            csvMetricsLogDirectory = ConfigUtils.absoluteStormLocalDir(stormConf);
            csvMetricsLogDirectory = csvMetricsLogDirectory + ConfigUtils.FILE_SEPARATOR + "csvmetrics";
        }
        File csvMetricsDir = new File(csvMetricsLogDirectory);
        validateCreateOutputDir(csvMetricsDir);
        return csvMetricsDir;
    }

    private static void validateCreateOutputDir(File dir) {
        if (!dir.exists()) {
            dir.mkdirs();
        }
        if (!dir.canWrite()) {
            throw new IllegalStateException(dir.getName() + " does not have write permissions.");
        }
        if (!dir.isDirectory()) {
            throw new IllegalStateException(dir.getName() + " is not a directory.");
        }
    }
}