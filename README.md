InfluxDB sink
================

This influxdb sink is compatible with newer versions of InfluxDB ( > 0.9 ).
It can read selected data from flume events, or it can be used to relay compilant data from flume event body to InfluxDB.
The current version was tested with InfluxDB 1.3.4

Build
=======

The code could be built with maven.
```
mvn package
```

Installation
==========

To deploy it, copy flume-influxdb-sink-0.0.2.jar and its dependencies in the flume classpath. A fat jar including all the dependencies in build by maven as well so it can be copied as well.


Configuration
=========

Here is an example sink configuration:

```
a1.sinks.k1.type = com.flumetest.influxdb.InfluxSink
a1.sinks.k1.host = 127.0.0.1
a1.sinks.k1.port = 8086
a1.sinks.k1.batchSize = 0
a1.sinks.k1.username = root
a1.sinks.k1.password = 123456
a1.sinks.k1.database = test
a1.sinks.k1.fieldList = r,b,swpd,free,buff,cache,si,so,bi,bo,in,cs,us,sy,id,wa
a1.sinks.k1.tagList = vmid=ip:port
```
The sink supports the following configuration parameters:
```
 host = The DNS or IP of the InfluxDB host, default localhost 
 port = The port on which influxdb listens, default 8086 
 database = The database where to write data, default "flumetest"    
 batchSize = The number of metrics sent simultaneously to InfluxDB, default 100 
 username = InfluxDB username, default root 
 password = InfluxDB password, default root 
 tagList = InfluxDB taglist
 filedList = InfluxDB value field name
 the flume source https://flume.apache.org/FlumeUserGuide.html#timestamp-interceptor 
```
