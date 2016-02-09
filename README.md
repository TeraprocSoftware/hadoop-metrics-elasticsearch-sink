# About the Hadoop Metrics Elasticsearch Sink Plugin #

**`hadoop-metrics-elasticsearch-sink`** is an implementation of Hadoop Metrics2 plugin to push metrics to Elasticsearch (a distributed RESTful search engine).
The sink is capable of collecting metrics of hadoop applications that support Hadoop Metrics2 (e.g. hbase, kafka, etc.).
The plugin jar must be deployed on all Hadoop YARN Nodemanagers with the **SAME** path (e.g., `/opt/hadoop/share/hadoop/yarn/lib/hadoop-metrics-elasticsearch-sink-1.0.jar`).

# Build hadoop-metrics-elasticsearch-sink.jar #
```
git clone https://<username>@bitbucket.org/teraproc/hadoop-metrics-elasticsearch-sink.git
cd hadoop-metrics-elasticsearch-sink
./gradle clean build
```

# Prepare Apache Slider application package for HBase #
To prepare the Slider application package for HBase that uses this plugin, follow these steps:
* Download HBase binary package, and rename the file to **remove** the -bin suffix, as required by Slider. For example:
```
wget https://archive.apache.org/dist/hbase/0.98.17/hbase-0.98.17-hadoop2-bin.tar.gz
mv hbase-0.98.17-hadoop2-bin.tar.gz hbase-0.98.17-hadoop2.tar.gz
```
* Build slider app packge for HBase. Assume the hbase package downloaded in the previous step is located under `/vagrant` directory. The files will be built under `target` directory.
```
git clone -b releases/slider-0.91.0-incubating https://github.com/apache/incubator-slider.git
cd incubator-slider/app-packages/hbase
mvn clean package -Phbase-app-package -Dhbase.version=0.98.17-hadoop2 -Dpkg.name=hbase-0.98.17-hadoop2.tar.gz -Dpkg.src=/vagrant -Dpkg.version=0.98.17-hadoop2
``` 
* Change the following configuration files: 
  * **`target/slider-hbase-app-package-0.98.17-hadoop2/package/templates/hadoop-metrics2-hbase.properties-GANGLIA-MASTER.j2`**
   ```
{% if has_metric_collector %}

*.elasticsearch.plugin.urls={{metric_collector_lib}}
hbase.class=org.apache.hadoop.metrics2.sink.elasticsearch.ElasticsearchMetricsSink
hbase.period=10
hbase.collector={{metric_collector_host}}:{{metric_collector_port}}

jvm.class=org.apache.hadoop.metrics2.sink.elasticsearch.ElasticsearchMetricsSink
jvm.period=10
jvm.collector={{metric_collector_host}}:{{metric_collector_port}}

rpc.class=org.apache.hadoop.metrics2.sink.elasticsearch.ElasticsearchMetricsSink
rpc.period=10
rpc.collector={{metric_collector_host}}:{{metric_collector_port}}

hbase.sink.elasticsearch.class=org.apache.hadoop.metrics2.sink.elasticsearch.ElasticsearchMetricsSink
hbase.sink.elasticsearch.period=10
hbase.sink.elasticsearch.collector={{metric_collector_host}}:{{metric_collector_port}}
hbase.sink.elasticsearch.serviceName-prefix={{app_name}}-master
hbase.sink.elasticsearch.appName-prefix={{app_name}}
hbase.sink.elasticsearch.componentName-prefix={{component_name}}
hbase.sink.elasticsearch.containerId-prefix={{container_id}}

{% else %}
```

  * **`target/slider-hbase-app-package-0.98.17-hadoop2/package/templates/hadoop-metrics2-hbase.properties-GANGLIA-RS.j2`**
   ```
{% if has_metric_collector %}

*.elasticsearch.plugin.urls={{metric_collector_lib}}
hbase.class=org.apache.hadoop.metrics2.sink.elasticsearch.ElasticsearchMetricsSink
hbase.period=10
hbase.collector={{metric_collector_host}}:{{metric_collector_port}}

jvm.class=org.apache.hadoop.metrics2.sink.elasticsearch.ElasticsearchMetricsSink
jvm.period=10
jvm.collector={{metric_collector_host}}:{{metric_collector_port}}

rpc.class=org.apache.hadoop.metrics2.sink.elasticsearch.ElasticsearchMetricsSink
rpc.period=10
rpc.collector={{metric_collector_host}}:{{metric_collector_port}}

hbase.sink.elasticsearch.class=org.apache.hadoop.metrics2.sink.elasticsearch.ElasticsearchMetricsSink
hbase.sink.elasticsearch.period=10
hbase.sink.elasticsearch.collector={{metric_collector_host}}:{{metric_collector_port}}
hbase.sink.elasticsearch.serviceName-prefix={{app_name}}-rs
hbase.sink.elasticsearch.appName-prefix={{app_name}}
hbase.sink.elasticsearch.componentName-prefix={{component_name}}
hbase.sink.elasticsearch.containerId-prefix={{container_id}}

{% else %}
```
  * **`target/slider-hbase-app-package-0.98.17-hadoop2/package/scripts/params.py`**
   ```
#configuration for HBASE_OPTS
container_id = config['hostLevelParams']['container_id']
component_name = config['componentName']
  ```
* Copy out the application specification and resource specification files. These files have the required variable names replaced.
```
cp target/slider-hbase-app-package-0.98.17-hadoop2/appConfig-default.json .
cp target/slider-hbase-app-package-0.98.17-hadoop2/resources-default.json .
```
* Back up the original zip file, and zip the new application package
```
mv target/slider-hbase-app-package-0.98.17-hadoop2.zip target/slider-hbase-app-package-0.98.17-hadoop2.zip.orig
cd target/slider-hbase-app-package-0.98.17-hadoop2/
zip -r slider-hbase-app-package-0.98.17-hadoop2.zip ./*
mv slider-hbase-app-package-0.98.17-hadoop2.zip ../
cd ../../
```
* Deliver the following files:
```
target/slider-hbase-app-package-0.98.17-hadoop2.zip
appConfig-default.json
resources-default.json
```
# Install and start Elasticsearch
You must start Elasticsearch server before deploying and start Slider HBase package. Do the following on the Elasticsearch server host:
```
curl -o elasticsearch-1.7.1.tar.gz -O -L https://download.elastic.co/elasticsearch/elasticsearch/elasticsearch-1.7.1.tar.gz
tar -xzf elasticsearch-1.7.1.tar.gz -C /usr/local
ln -s /usr/local/elasticsearch-1.7.1  /usr/local/elasticsearch
/usr/local/elasticsearch/bin/elasticsearch
```
# Deploy and start Slider application package for HBase #
* Install the package
```
slider package --install --name HBASE --package target/slider-hbase-app-package-0.98.17-hadoop2.zip
```
* Change following configuration:
  * **`appConfig-default.json`**
   ```
"java_home": "<full_path_to_your_java_home_or_delete_this_line_if_java_home_is_set_in_the_system_path>",
"site.global.metric_collector_host": "<your_elasticsearch_server_host>",
"site.global.metric_collector_port": "9200",
"site.global.metric_collector_lib": "file://<full_path_to_hadoop-metrics-elasticsearch-sink-1.0.jar>",
```
  For example:
 ```
"site.global.metric_collector_host": "mdinglin02",
"site.global.metric_collector_port": "9200",
"site.global.metric_collector_lib": "file:///opt/hadoop/share/hadoop/yarn/lib/hadoop-metrics-elasticsearch-sink-1.0.jar",
 ```
* Create the application
```
slider create hbase --template appConfig-default.json --resources resources-default.json
```
# Elastcisearch datastore schema #
* Elasticsearch supports to store documents in three level: index, type, and document.
* hadoop-metrics-elasticsearch-sink uses app name as index, component name as type. Each document includes a snapshot of all metrics.
* Consider a slider application named "hbase38", following are the metrics collect for HBASE_REGIONSERVER.
```
{
   "hbase38": {
      "mappings": {
         "HBASE_REGIONSERVER": {
            "properties": {
               "appName": {
                  "type": "string"
               },
               "cmptName": {
                  "type": "string"
               },
               "context": {
                  "type": "string"
               },
               "hostName": {
                  "type": "string"
               },
               "metrics": {
                  "properties": {
                     "AppendSize_75th_percentile": {
                        "type": "string"
                     },
                     "AppendSize_95th_percentile": {
                        "type": "string"
                     },
                     "AppendSize_99th_percentile": {
                        "type": "string"
                     },
                     "AppendSize_max": {
                        "type": "string"
                     },
                     "AppendSize_mean": {
                        "type": "string"
                     },
                     "AppendSize_median": {
                        "type": "string"
                     },
                     "AppendSize_min": {
                        "type": "string"
                     },
                     "AppendSize_num_ops": {
                        "type": "string"
                     },
                     "AppendTime_75th_percentile": {
                        "type": "string"
                     },
                     "AppendTime_95th_percentile": {
                        "type": "string"
                     },
                     "AppendTime_99th_percentile": {
                        "type": "string"
                     },
                     "AppendTime_max": {
                        "type": "string"
                     },
                     "AppendTime_mean": {
                        "type": "string"
                     },
                     "AppendTime_median": {
                        "type": "string"
                     },
                     "AppendTime_min": {
                        "type": "string"
                     },
                     "AppendTime_num_ops": {
                        "type": "string"
                     },
                     "Append_75th_percentile": {
                        "type": "string"
                     },
                     "Append_95th_percentile": {
                        "type": "string"
                     },
                     "Append_99th_percentile": {
                        "type": "string"
                     },
                     "Append_max": {
                        "type": "string"
                     },
                     "Append_mean": {
                        "type": "string"
                     },
                     "Append_median": {
                        "type": "string"
                     },
                     "Append_min": {
                        "type": "string"
                     },
                     "Append_num_ops": {
                        "type": "string"
                     },
                     "Delete_75th_percentile": {
                        "type": "string"
                     },
                     "Delete_95th_percentile": {
                        "type": "string"
                     },
                     "Delete_99th_percentile": {
                        "type": "string"
                     },
                     "Delete_max": {
                        "type": "string"
                     },
                     "Delete_mean": {
                        "type": "string"
                     },
                     "Delete_median": {
                        "type": "string"
                     },
                     "Delete_min": {
                        "type": "string"
                     },
                     "Delete_num_ops": {
                        "type": "string"
                     },
                     "DroppedPubAll": {
                        "type": "string"
                     },
                     "FlushTime_75th_percentile": {
                        "type": "string"
                     },
                     "FlushTime_95th_percentile": {
                        "type": "string"
                     },
                     "FlushTime_99th_percentile": {
                        "type": "string"
                     },
                     "FlushTime_max": {
                        "type": "string"
                     },
                     "FlushTime_mean": {
                        "type": "string"
                     },
                     "FlushTime_median": {
                        "type": "string"
                     },
                     "FlushTime_min": {
                        "type": "string"
                     },
                     "FlushTime_num_ops": {
                        "type": "string"
                     },
                     "GcCount": {
                        "type": "string"
                     },
                     "GcCountConcurrentMarkSweep": {
                        "type": "string"
                     },
                     "GcCountCopy": {
                        "type": "string"
                     },
                     "GcTimeMillis": {
                        "type": "string"
                     },
                     "GcTimeMillisConcurrentMarkSweep": {
                        "type": "string"
                     },
                     "GcTimeMillisCopy": {
                        "type": "string"
                     },
                     "GetGroupsAvgTime": {
                        "type": "string"
                     },
                     "GetGroupsNumOps": {
                        "type": "string"
                     },
                     "Get_75th_percentile": {
                        "type": "string"
                     },
                     "Get_95th_percentile": {
                        "type": "string"
                     },
                     "Get_99th_percentile": {
                        "type": "string"
                     },
                     "Get_max": {
                        "type": "string"
                     },
                     "Get_mean": {
                        "type": "string"
                     },
                     "Get_median": {
                        "type": "string"
                     },
                     "Get_min": {
                        "type": "string"
                     },
                     "Get_num_ops": {
                        "type": "string"
                     },
                     "Increment_75th_percentile": {
                        "type": "string"
                     },
                     "Increment_95th_percentile": {
                        "type": "string"
                     },
                     "Increment_99th_percentile": {
                        "type": "string"
                     },
                     "Increment_max": {
                        "type": "string"
                     },
                     "Increment_mean": {
                        "type": "string"
                     },
                     "Increment_median": {
                        "type": "string"
                     },
                     "Increment_min": {
                        "type": "string"
                     },
                     "Increment_num_ops": {
                        "type": "string"
                     },
                     "LogError": {
                        "type": "string"
                     },
                     "LogFatal": {
                        "type": "string"
                     },
                     "LogInfo": {
                        "type": "string"
                     },
                     "LogWarn": {
                        "type": "string"
                     },
                     "LoginFailureAvgTime": {
                        "type": "string"
                     },
                     "LoginFailureNumOps": {
                        "type": "string"
                     },
                     "LoginSuccessAvgTime": {
                        "type": "string"
                     },
                     "LoginSuccessNumOps": {
                        "type": "string"
                     },
                     "MemHeapCommittedM": {
                        "type": "string"
                     },
                     "MemHeapMaxM": {
                        "type": "string"
                     },
                     "MemHeapUsedM": {
                        "type": "string"
                     },
                     "MemMaxM": {
                        "type": "string"
                     },
                     "MemNonHeapCommittedM": {
                        "type": "string"
                     },
                     "MemNonHeapMaxM": {
                        "type": "string"
                     },
                     "MemNonHeapUsedM": {
                        "type": "string"
                     },
                     "Mutate_75th_percentile": {
                        "type": "string"
                     },
                     "Mutate_95th_percentile": {
                        "type": "string"
                     },
                     "Mutate_99th_percentile": {
                        "type": "string"
                     },
                     "Mutate_max": {
                        "type": "string"
                     },
                     "Mutate_mean": {
                        "type": "string"
                     },
                     "Mutate_median": {
                        "type": "string"
                     },
                     "Mutate_min": {
                        "type": "string"
                     },
                     "Mutate_num_ops": {
                        "type": "string"
                     },
                     "Namespace_hbase_table_meta_region_1588230740_metric_appendCount": {
                        "type": "string"
                     },
                     "Namespace_hbase_table_meta_region_1588230740_metric_compactionsCompletedCount": {
                        "type": "string"
                     },
                     "Namespace_hbase_table_meta_region_1588230740_metric_deleteCount": {
                        "type": "string"
                     },
                     "Namespace_hbase_table_meta_region_1588230740_metric_get_75th_percentile": {
                        "type": "string"
                     },
                     "Namespace_hbase_table_meta_region_1588230740_metric_get_95th_percentile": {
                        "type": "string"
                     },
                     "Namespace_hbase_table_meta_region_1588230740_metric_get_99th_percentile": {
                        "type": "string"
                     },
                     "Namespace_hbase_table_meta_region_1588230740_metric_get_max": {
                        "type": "string"
                     },
                     "Namespace_hbase_table_meta_region_1588230740_metric_get_mean": {
                        "type": "string"
                     },
                     "Namespace_hbase_table_meta_region_1588230740_metric_get_median": {
                        "type": "string"
                     },
                     "Namespace_hbase_table_meta_region_1588230740_metric_get_min": {
                        "type": "string"
                     },
                     "Namespace_hbase_table_meta_region_1588230740_metric_get_num_ops": {
                        "type": "string"
                     },
                     "Namespace_hbase_table_meta_region_1588230740_metric_incrementCount": {
                        "type": "string"
                     },
                     "Namespace_hbase_table_meta_region_1588230740_metric_memStoreSize": {
                        "type": "string"
                     },
                     "Namespace_hbase_table_meta_region_1588230740_metric_mutateCount": {
                        "type": "string"
                     },
                     "Namespace_hbase_table_meta_region_1588230740_metric_numBytesCompactedCount": {
                        "type": "string"
                     },
                     "Namespace_hbase_table_meta_region_1588230740_metric_numFilesCompactedCount": {
                        "type": "string"
                     },
                     "Namespace_hbase_table_meta_region_1588230740_metric_scanNext_75th_percentile": {
                        "type": "string"
                     },
                     "Namespace_hbase_table_meta_region_1588230740_metric_scanNext_95th_percentile": {
                        "type": "string"
                     },
                     "Namespace_hbase_table_meta_region_1588230740_metric_scanNext_99th_percentile": {
                        "type": "string"
                     },
                     "Namespace_hbase_table_meta_region_1588230740_metric_scanNext_max": {
                        "type": "string"
                     },
                     "Namespace_hbase_table_meta_region_1588230740_metric_scanNext_mean": {
                        "type": "string"
                     },
                     "Namespace_hbase_table_meta_region_1588230740_metric_scanNext_median": {
                        "type": "string"
                     },
                     "Namespace_hbase_table_meta_region_1588230740_metric_scanNext_min": {
                        "type": "string"
                     },
                     "Namespace_hbase_table_meta_region_1588230740_metric_scanNext_num_ops": {
                        "type": "string"
                     },
                     "Namespace_hbase_table_meta_region_1588230740_metric_storeCount": {
                        "type": "string"
                     },
                     "Namespace_hbase_table_meta_region_1588230740_metric_storeFileCount": {
                        "type": "string"
                     },
                     "Namespace_hbase_table_meta_region_1588230740_metric_storeFileSize": {
                        "type": "string"
                     },
                     "Namespace_hbase_table_namespace_region_231a05d5245abec41522acf06d4b6652_metric_appendCount": {
                        "type": "string"
                     },
                     "Namespace_hbase_table_namespace_region_231a05d5245abec41522acf06d4b6652_metric_compactionsCompletedCount": {
                        "type": "string"
                     },
                     "Namespace_hbase_table_namespace_region_231a05d5245abec41522acf06d4b6652_metric_deleteCount": {
                        "type": "string"
                     },
                     "Namespace_hbase_table_namespace_region_231a05d5245abec41522acf06d4b6652_metric_get_75th_percentile": {
                        "type": "string"
                     },
                     "Namespace_hbase_table_namespace_region_231a05d5245abec41522acf06d4b6652_metric_get_95th_percentile": {
                        "type": "string"
                     },
                     "Namespace_hbase_table_namespace_region_231a05d5245abec41522acf06d4b6652_metric_get_99th_percentile": {
                        "type": "string"
                     },
                     "Namespace_hbase_table_namespace_region_231a05d5245abec41522acf06d4b6652_metric_get_max": {
                        "type": "string"
                     },
                     "Namespace_hbase_table_namespace_region_231a05d5245abec41522acf06d4b6652_metric_get_mean": {
                        "type": "string"
                     },
                     "Namespace_hbase_table_namespace_region_231a05d5245abec41522acf06d4b6652_metric_get_median": {
                        "type": "string"
                     },
                     "Namespace_hbase_table_namespace_region_231a05d5245abec41522acf06d4b6652_metric_get_min": {
                        "type": "string"
                     },
                     "Namespace_hbase_table_namespace_region_231a05d5245abec41522acf06d4b6652_metric_get_num_ops": {
                        "type": "string"
                     },
                     "Namespace_hbase_table_namespace_region_231a05d5245abec41522acf06d4b6652_metric_incrementCount": {
                        "type": "string"
                     },
                     "Namespace_hbase_table_namespace_region_231a05d5245abec41522acf06d4b6652_metric_memStoreSize": {
                        "type": "string"
                     },
                     "Namespace_hbase_table_namespace_region_231a05d5245abec41522acf06d4b6652_metric_mutateCount": {
                        "type": "string"
                     },
                     "Namespace_hbase_table_namespace_region_231a05d5245abec41522acf06d4b6652_metric_numBytesCompactedCount": {
                        "type": "string"
                     },
                     "Namespace_hbase_table_namespace_region_231a05d5245abec41522acf06d4b6652_metric_numFilesCompactedCount": {
                        "type": "string"
                     },
                     "Namespace_hbase_table_namespace_region_231a05d5245abec41522acf06d4b6652_metric_scanNext_75th_percentile": {
                        "type": "string"
                     },
                     "Namespace_hbase_table_namespace_region_231a05d5245abec41522acf06d4b6652_metric_scanNext_95th_percentile": {
                        "type": "string"
                     },
                     "Namespace_hbase_table_namespace_region_231a05d5245abec41522acf06d4b6652_metric_scanNext_99th_percentile": {
                        "type": "string"
                     },
                     "Namespace_hbase_table_namespace_region_231a05d5245abec41522acf06d4b6652_metric_scanNext_max": {
                        "type": "string"
                     },
                     "Namespace_hbase_table_namespace_region_231a05d5245abec41522acf06d4b6652_metric_scanNext_mean": {
                        "type": "string"
                     },
                     "Namespace_hbase_table_namespace_region_231a05d5245abec41522acf06d4b6652_metric_scanNext_median": {
                        "type": "string"
                     },
                     "Namespace_hbase_table_namespace_region_231a05d5245abec41522acf06d4b6652_metric_scanNext_min": {
                        "type": "string"
                     },
                     "Namespace_hbase_table_namespace_region_231a05d5245abec41522acf06d4b6652_metric_scanNext_num_ops": {
                        "type": "string"
                     },
                     "Namespace_hbase_table_namespace_region_231a05d5245abec41522acf06d4b6652_metric_storeCount": {
                        "type": "string"
                     },
                     "Namespace_hbase_table_namespace_region_231a05d5245abec41522acf06d4b6652_metric_storeFileCount": {
                        "type": "string"
                     },
                     "Namespace_hbase_table_namespace_region_231a05d5245abec41522acf06d4b6652_metric_storeFileSize": {
                        "type": "string"
                     },
                     "NumActiveSinks": {
                        "type": "string"
                     },
                     "NumActiveSources": {
                        "type": "string"
                     },
                     "NumAllSinks": {
                        "type": "string"
                     },
                     "NumAllSources": {
                        "type": "string"
                     },
                     "ProcessCallTime_75th_percentile": {
                        "type": "string"
                     },
                     "ProcessCallTime_95th_percentile": {
                        "type": "string"
                     },
                     "ProcessCallTime_99th_percentile": {
                        "type": "string"
                     },
                     "ProcessCallTime_max": {
                        "type": "string"
                     },
                     "ProcessCallTime_mean": {
                        "type": "string"
                     },
                     "ProcessCallTime_median": {
                        "type": "string"
                     },
                     "ProcessCallTime_min": {
                        "type": "string"
                     },
                     "ProcessCallTime_num_ops": {
                        "type": "string"
                     },
                     "PublishAvgTime": {
                        "type": "string"
                     },
                     "PublishNumOps": {
                        "type": "string"
                     },
                     "QueueCallTime_75th_percentile": {
                        "type": "string"
                     },
                     "QueueCallTime_95th_percentile": {
                        "type": "string"
                     },
                     "QueueCallTime_99th_percentile": {
                        "type": "string"
                     },
                     "QueueCallTime_max": {
                        "type": "string"
                     },
                     "QueueCallTime_mean": {
                        "type": "string"
                     },
                     "QueueCallTime_median": {
                        "type": "string"
                     },
                     "QueueCallTime_min": {
                        "type": "string"
                     },
                     "QueueCallTime_num_ops": {
                        "type": "string"
                     },
                     "Replay_75th_percentile": {
                        "type": "string"
                     },
                     "Replay_95th_percentile": {
                        "type": "string"
                     },
                     "Replay_99th_percentile": {
                        "type": "string"
                     },
                     "Replay_max": {
                        "type": "string"
                     },
                     "Replay_mean": {
                        "type": "string"
                     },
                     "Replay_median": {
                        "type": "string"
                     },
                     "Replay_min": {
                        "type": "string"
                     },
                     "Replay_num_ops": {
                        "type": "string"
                     },
                     "Sink_elasticsearchAvgTime": {
                        "type": "string"
                     },
                     "Sink_elasticsearchDropped": {
                        "type": "string"
                     },
                     "Sink_elasticsearchNumOps": {
                        "type": "string"
                     },
                     "Sink_elasticsearchQsize": {
                        "type": "string"
                     },
                     "SnapshotAvgTime": {
                        "type": "string"
                     },
                     "SnapshotNumOps": {
                        "type": "string"
                     },
                     "SplitTime_75th_percentile": {
                        "type": "string"
                     },
                     "SplitTime_95th_percentile": {
                        "type": "string"
                     },
                     "SplitTime_99th_percentile": {
                        "type": "string"
                     },
                     "SplitTime_max": {
                        "type": "string"
                     },
                     "SplitTime_mean": {
                        "type": "string"
                     },
                     "SplitTime_median": {
                        "type": "string"
                     },
                     "SplitTime_min": {
                        "type": "string"
                     },
                     "SplitTime_num_ops": {
                        "type": "string"
                     },
                     "SyncTime_75th_percentile": {
                        "type": "string"
                     },
                     "SyncTime_95th_percentile": {
                        "type": "string"
                     },
                     "SyncTime_99th_percentile": {
                        "type": "string"
                     },
                     "SyncTime_max": {
                        "type": "string"
                     },
                     "SyncTime_mean": {
                        "type": "string"
                     },
                     "SyncTime_median": {
                        "type": "string"
                     },
                     "SyncTime_min": {
                        "type": "string"
                     },
                     "SyncTime_num_ops": {
                        "type": "string"
                     },
                     "ThreadsBlocked": {
                        "type": "string"
                     },
                     "ThreadsNew": {
                        "type": "string"
                     },
                     "ThreadsRunnable": {
                        "type": "string"
                     },
                     "ThreadsTerminated": {
                        "type": "string"
                     },
                     "ThreadsTimedWaiting": {
                        "type": "string"
                     },
                     "ThreadsWaiting": {
                        "type": "string"
                     },
                     "appendCount": {
                        "type": "string"
                     },
                     "authenticationFailures": {
                        "type": "string"
                     },
                     "authenticationSuccesses": {
                        "type": "string"
                     },
                     "authorizationFailures": {
                        "type": "string"
                     },
                     "authorizationSuccesses": {
                        "type": "string"
                     },
                     "blockCacheCount": {
                        "type": "string"
                     },
                     "blockCacheCountHitPercent": {
                        "type": "string"
                     },
                     "blockCacheEvictionCount": {
                        "type": "string"
                     },
                     "blockCacheExpressHitPercent": {
                        "type": "string"
                     },
                     "blockCacheFreeSize": {
                        "type": "string"
                     },
                     "blockCacheHitCount": {
                        "type": "string"
                     },
                     "blockCacheMissCount": {
                        "type": "string"
                     },
                     "blockCacheSize": {
                        "type": "string"
                     },
                     "blockedRequestCount": {
                        "type": "string"
                     },
                     "checkMutateFailedCount": {
                        "type": "string"
                     },
                     "checkMutatePassedCount": {
                        "type": "string"
                     },
                     "compactedCellsCount": {
                        "type": "string"
                     },
                     "compactedCellsSize": {
                        "type": "string"
                     },
                     "compactionQueueLength": {
                        "type": "string"
                     },
                     "flushQueueLength": {
                        "type": "string"
                     },
                     "flushedCellsCount": {
                        "type": "string"
                     },
                     "flushedCellsSize": {
                        "type": "string"
                     },
                     "hlogFileCount": {
                        "type": "string"
                     },
                     "hlogFileSize": {
                        "type": "string"
                     },
                     "lowReplicaRollRequest": {
                        "type": "string"
                     },
                     "majorCompactedCellsCount": {
                        "type": "string"
                     },
                     "majorCompactedCellsSize": {
                        "type": "string"
                     },
                     "memStoreSize": {
                        "type": "string"
                     },
                     "mutationsWithoutWALCount": {
                        "type": "string"
                     },
                     "mutationsWithoutWALSize": {
                        "type": "string"
                     },
                     "numActiveHandler": {
                        "type": "string"
                     },
                     "numCallsInGeneralQueue": {
                        "type": "string"
                     },
                     "numCallsInPriorityQueue": {
                        "type": "string"
                     },
                     "numCallsInReplicationQueue": {
                        "type": "string"
                     },
                     "numOpenConnections": {
                        "type": "string"
                     },
                     "percentFilesLocal": {
                        "type": "string"
                     },
                     "queueSize": {
                        "type": "string"
                     },
                     "readRequestCount": {
                        "type": "string"
                     },
                     "receivedBytes": {
                        "type": "string"
                     },
                     "regionCount": {
                        "type": "string"
                     },
                     "regionServerStartTime": {
                        "type": "string"
                     },
                     "rollRequest": {
                        "type": "string"
                     },
                     "sentBytes": {
                        "type": "string"
                     },
                     "sink.ageOfLastAppliedOp": {
                        "type": "string"
                     },
                     "sink.appliedBatches": {
                        "type": "string"
                     },
                     "sink.appliedOps": {
                        "type": "string"
                     },
                     "slowAppendCount": {
                        "type": "string"
                     },
                     "slowDeleteCount": {
                        "type": "string"
                     },
                     "slowGetCount": {
                        "type": "string"
                     },
                     "slowIncrementCount": {
                        "type": "string"
                     },
                     "slowPutCount": {
                        "type": "string"
                     },
                     "splitQueueLength": {
                        "type": "string"
                     },
                     "splitRequestCount": {
                        "type": "string"
                     },
                     "splitSuccessCounnt": {
                        "type": "string"
                     },
                     "staticBloomSize": {
                        "type": "string"
                     },
                     "staticIndexSize": {
                        "type": "string"
                     },
                     "storeCount": {
                        "type": "string"
                     },
                     "storeFileCount": {
                        "type": "string"
                     },
                     "storeFileIndexSize": {
                        "type": "string"
                     },
                     "storeFileSize": {
                        "type": "string"
                     },
                     "totalRequestCount": {
                        "type": "string"
                     },
                     "updatesBlockedTime": {
                        "type": "string"
                     },
                     "writeRequestCount": {
                        "type": "string"
                     }
                  }
               },
               "timestamp": {
                  "type": "long"
               }
            }
         }
      }
   }
}
```

# Elasticsearch search example #
* Get the latest metric values of "MemNonHeapUsedM" and "QueueCallTime_mean" of hbase regionserver
```
GET /hbase38/HBASE_REGIONSERVER/_search? -d '
{
  "filter": {
    "or": [
      {
        "exists": {
          "field" : "metrics.MemNonHeapUsedM"
        }
      },
      {
        "exists": {
          "field" : "metrics.QueueCallTime_mean"
        }
      }
    ]
  },
  "fields": ["hostName", "metrics.MemNonHeapUsedM", "metrics.QueueCallTime_mean"],
  "size": 2,
  "sort": [
    {
      "timestamp": {
        "order": "desc"
      }
    }
  ]
}'
```
* Following is search result
```
{
   "took": 3,
   "timed_out": false,
   "_shards": {
      "total": 5,
      "successful": 5,
      "failed": 0
   },
   "hits": {
      "total": 168,
      "max_score": null,
      "hits": [
         {
            "_index": "hbase38",
            "_type": "HBASE_REGIONSERVER",
            "_id": "AU7Anp4I2AYboxihPjuG",
            "_score": null,
            "fields": {
               "metrics.QueueCallTime_mean": [
                  "1.1111111111111112"
               ],
               "hostName": [
                  "u1403.ambari.apache.org"
               ]
            },
            "sort": [
               1437750697471
            ]
         },
         {
            "_index": "hbase38",
            "_type": "HBASE_REGIONSERVER",
            "_id": "AU7Anp4D2AYboxihPjuF",
            "_score": null,
            "fields": {
               "metrics.MemNonHeapUsedM": [
                  "42.05668"
               ],
               "hostName": [
                  "u1403.ambari.apache.org"
               ]
            },
            "sort": [
               1437750697469
            ]
         }
      ]
   }
}
```
