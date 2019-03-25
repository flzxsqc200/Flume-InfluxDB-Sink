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
example:
```
/***sb is data-binary body
*curl -i -XPOST 'http://localhost:8086/write?db=test&u=root&p=123456' --data-binary  \
*	'tablename,tagname=tag value1=XXX,value2=XXX,value3=XXX'
*example:
*when sb="test,vmid=10.64.4.218_5400 r=0,b=0,swpd=0,free=437976,buff=7012,cache=716852,si=0,so=0,bi=0,bo=0,in=0,cs=1359,us=34,sy=1,id=65,wa=0"
*in influxdb,you can see:
*> select * from test;
*  name: test
*  time                b bi bo buff cache  cs free   id  in r si so swpd sy us vmid             wa
*  ----                - -- -- ---- -----  -- ----   --  -- - -- -- ---- -- -- ----             --
*  1553490725748307650 0 0  0  5320 638840 30 799568 100 0  0 0  0  0    0  0  10.64.4.218_5400 0
*  1553490725885754400 0 0  0  5320 638840 30 799568 100 0  0 0  0  0    0  0  10.64.4.218_5400 0
***/
```
