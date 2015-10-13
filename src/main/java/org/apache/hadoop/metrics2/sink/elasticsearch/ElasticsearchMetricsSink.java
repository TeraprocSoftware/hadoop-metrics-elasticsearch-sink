package org.apache.hadoop.metrics2.sink.elasticsearch;

import org.apache.commons.configuration.SubsetConfiguration;;
import org.apache.hadoop.metrics2.MetricsRecord;
import org.apache.hadoop.metrics2.MetricsException;
import org.apache.hadoop.metrics2.AbstractMetric;

import java.io.IOException;
import java.util.*;

public class ElasticsearchMetricsSink extends AbstractElasticsearchMetricsSink {
    private String serviceName = "";
    private String appName = "";
    private String cmptName = "";
    private String containerId = "";
    private static final String SERVICE_NAME_PREFIX = "serviceName-prefix";
    private static final String APPLICATION_NAME_PREFIX = "appName-prefix";
    private static final String COMPONENT_NAME_PREFIX = "componentName-prefix";
    private static final String CONTAINER_ID_PREFIX = "containerId-prefix";

    @Override
    public void init(SubsetConfiguration conf) {
        super.init(conf);
        serviceName = getServiceName(conf);
        appName = getAppName(conf);
        cmptName = getComponentName(conf);
        containerId = getContainerId(conf);
        LOG.info("Identified serviceName = " + serviceName + " appName=" + appName + " cmptName=" + cmptName + " containerId=" + containerId);
    }

    /**
     * Return configured serviceName with or without prefix.
     * Default without serviceName or configured prefix : first config prefix
     * With prefix : configured prefix + first config prefix
     * Configured serviceName : Return serviceName as is.
     */
    private String getServiceName(SubsetConfiguration conf) {
        String serviceNamePrefix = conf.getString(SERVICE_NAME_PREFIX, "");
        return serviceNamePrefix;
    }


    private String getAppName(SubsetConfiguration conf) {
        String appNamePrefix = conf.getString(APPLICATION_NAME_PREFIX, "");
        return appNamePrefix;
    }

    private String getComponentName(SubsetConfiguration conf) {
        String cmptNamePrefix = conf.getString(COMPONENT_NAME_PREFIX, "");
        return cmptNamePrefix;
    }

    private String getContainerId(SubsetConfiguration conf) {
        String containerIdPrefix = conf.getString(CONTAINER_ID_PREFIX, "");
        return containerIdPrefix;
    }

    @Override
    public void putMetrics(MetricsRecord record) {
        try {
            String context = record.context() + "." + record.name();
            Collection<AbstractMetric> metrics = (Collection<AbstractMetric>) record.metrics();

            ElasticsearchMetric esMetric = new ElasticsearchMetric();
            esMetric.setUpdateTime(record.timestamp());
            esMetric.setAppName(appName);
            esMetric.setCmptName(cmptName);
            esMetric.setHostName(hostName);
            esMetric.setContainerId(containerId);
            esMetric.setContext(context);

            Map<String, String> metricMap = new HashMap<String, String>();
            for (AbstractMetric metric : metrics) {
                metricMap.put(metric.name(), metric.value().toString());
            }
            esMetric.setMetrics(metricMap);

            emitMetrics(appName, cmptName, esMetric);
        } catch (UnableToConnectException uce) {
            LOG.warn("Unable to send metrics to collector by address:" + uce.getConnectUrl());
        } catch (IOException io) {
            throw new MetricsException("Failed to putMetrics", io);
        }
    }
}
