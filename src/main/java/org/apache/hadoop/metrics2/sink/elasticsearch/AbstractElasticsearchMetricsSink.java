package org.apache.hadoop.metrics2.sink.elasticsearch;

import org.apache.commons.logging.Log;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.methods.PostMethod;
import org.apache.commons.httpclient.methods.StringRequestEntity;
import org.apache.commons.logging.LogFactory;
import org.apache.commons.configuration.SubsetConfiguration;

import org.apache.hadoop.metrics2.MetricsSink;
import org.apache.hadoop.metrics2.util.Servers;
import org.apache.hadoop.net.DNS;
import org.codehaus.jackson.map.ObjectMapper;

import java.io.IOException;
import java.net.ConnectException;
import java.net.SocketAddress;
import java.net.UnknownHostException;
import java.util.List;

public abstract class AbstractElasticsearchMetricsSink implements MetricsSink {
    public static final String COLLECTOR_HOST_PROPERTY = "collector";

    protected String hostName = "UNKNOWN.example.com";
    private List<? extends SocketAddress> metricsServers;
    private String collectorUri;

    public final Log LOG = LogFactory.getLog(this.getClass());
    private HttpClient httpClient = new HttpClient();

    protected static ObjectMapper mapper = new ObjectMapper();

    @Override
    public void init(SubsetConfiguration conf) {
        LOG.info("Initializing Elasticsearch metrics sink.");

        // Take the hostname from the DNS class.
        if (conf.getString("slave.host.name") != null) {
            hostName = conf.getString("slave.host.name");
        } else {
            try {
                hostName = DNS.getDefaultHost(
                        conf.getString("dfs.datanode.dns.interface", "default"),
                        conf.getString("dfs.datanode.dns.nameserver", "default"));
            } catch (UnknownHostException uhe) {
                LOG.error(uhe);
                hostName = "UNKNOWN.example.com";
            }
        }

        LOG.info("Identified hostname = " + hostName);

        // Load collector configs
        metricsServers = Servers.parse(conf.getString(COLLECTOR_HOST_PROPERTY), 9200);

        if (metricsServers == null || metricsServers.isEmpty()) {
            LOG.error("No Metric collector configured.");
        } else {
            collectorUri = "http://" + conf.getString(COLLECTOR_HOST_PROPERTY).trim();
        }

        LOG.info("Collector Uri: " + collectorUri);
    }

    protected void emitMetrics(String index, String type, ElasticsearchMetric metrics) throws IOException {
        String connectUrl = getCollectorUri() + "/" + index + "/" + type;
        LOG.debug("connectUrl: " + connectUrl);
        try {
            String jsonData = mapper.writeValueAsString(metrics);
            LOG.debug("Json elasticsearch metrics: " + jsonData);

            StringRequestEntity requestEntity = new StringRequestEntity(jsonData, "application/json", "UTF-8");
            PostMethod postMethod = new PostMethod(connectUrl);
            postMethod.setRequestEntity(requestEntity);
            int statusCode = httpClient.executeMethod(postMethod);
            if (statusCode != 201) {
                LOG.info("Unable to POST metrics to collector, " + connectUrl);
            } else {
                LOG.debug("Metrics posted to Collector " + connectUrl);
            }
        } catch (ConnectException e) {
            throw new UnableToConnectException(e).setConnectUrl(connectUrl);
        }
    }

    public void setHttpClient(HttpClient httpClient) {
        this.httpClient = httpClient;
    }

    protected String getCollectorUri() {
        return collectorUri;
    }

    @Override
    public void flush() {
        // nothing to do as we are not buffering data
    }
}
