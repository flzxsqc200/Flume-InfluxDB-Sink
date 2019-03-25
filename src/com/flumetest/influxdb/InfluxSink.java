package com.flumetest.influxdb;




import java.util.Map;

import org.apache.flume.Channel;
import org.apache.flume.Context;
import org.apache.flume.Event;
import org.apache.flume.EventDeliveryException;
import org.apache.flume.Transaction;
import org.apache.flume.conf.Configurable;
import org.apache.flume.instrumentation.SinkCounter;
import org.apache.flume.sink.AbstractSink;
import org.apache.log4j.Logger;
import org.influxdb.InfluxDB;
import org.influxdb.InfluxDB.ConsistencyLevel;
import org.influxdb.InfluxDBFactory;

public class InfluxSink extends AbstractSink implements Configurable {
    private static final Logger LOG = Logger.getLogger(InfluxSink.class);
    private String url;
    private int batchSize;
    private String database;
    private String username;
    private String password;
    private Boolean influxsource;
    private InfluxDB influxDB;
    private String truncateTimestamp;
    private Boolean metricnamefromfield;
    private String metricnamefromconf;
    private String metricvaluefield;
    private String[] tagsfromfieldslist;
    private String timestampfromfield;
    private String metricnamefield;
    private SinkCounter sinkCounter;
    private String fieldList;
    private String tagList;
    @Override
  	public void configure(Context context) {
	    String host = context.getString("host", "localhost");
	    String port = context.getString("port", "8086");
	    String database = context.getString("database", "flumetest");   
	    int batchSize = context.getInteger("batchSize", 100);
	    String username = context.getString("username","root");
	    String password = context.getString("password","root");
	    Boolean influxsource = context.getBoolean("influxdatafrombody",false);
	    String url = "http://"+host+":"+port;
	    this.url = url;
	    this.batchSize = batchSize;
	    this.database = database;
	    this.username = username;
	    this.password = password;
	    this.influxsource = influxsource;
	    this.metricnamefromfield = metricnamefromfield;
	    this.truncateTimestamp = truncateTimestamp;
	    this.metricnamefromconf = metricnamefromconf;
	    this.metricvaluefield = metricvaluefield;
	    this.tagsfromfieldslist = tagsfromfields.split(",");
	    this.timestampfromfield = timestampfromfield;
	    this.metricnamefield = metricnamefield;
            this.fieldList=context.getString("fieldList",null);
            this.tagList=context.getString("tagList",null);

	    if (sinkCounter == null) {
			sinkCounter = new SinkCounter(getName());
		}
    }

    @Override
  	public void start() {
	  LOG.info("Starting Influx Sink {} ...Connecting to "+url);
	  try {
          InfluxDB influxDB = InfluxDBFactory.connect(url,username,password);
    	      this.influxDB = influxDB;
    	      sinkCounter.incrementConnectionCreatedCount();
          }
	  
	  catch ( Throwable e ){
    	      LOG.error(e.getStackTrace());
    	      sinkCounter.incrementConnectionFailedCount();
      }
	  sinkCounter.start();
    }

    @Override
    public void stop () {
    	  LOG.info("Stopping Influx Sink {} ...");
    	  sinkCounter.incrementConnectionClosedCount();
	  sinkCounter.stop();
    }

    @Override
    public Status process() throws EventDeliveryException {
    	Status status = null;
	    // Start transaction
	    Channel ch = getChannel();
	    Transaction txn = ch.getTransaction();
	    txn.begin();
	    try {
	    	StringBuilder batch = new StringBuilder();
	    	Event event = null;
	    	int count = 0;
	    	sinkCounter.incrementEventDrainAttemptCount();
	    	for (count = 0; count <= batchSize; ++count) {
	    		event = ch.take();
	    		if (event == null) {
	    			break;
	    		}
	    		String InfluxEvent = ExtractInfluxEvent(event, influxsource, truncateTimestamp);
	    		if ( batch.length() > 0) {
	    			batch.append("\n");
	    		}
	    		batch.append(InfluxEvent);
	    		sinkCounter.incrementConnectionCreatedCount();
          
	    	}
	    	if (count <= 0) {
	    		sinkCounter.incrementBatchEmptyCount();
	    		sinkCounter.incrementEventDrainSuccessCount();
	    		status = Status.BACKOFF;
	    		txn.commit();
	    	} 
	    	else {
	    		try {
	    	        LOG.info(batch.toString());
	    			influxDB.write(database, "", InfluxDB.ConsistencyLevel.ONE, batch.toString());
	    			txn.commit();
	     		if ( count < batchSize ) {
	    	    		sinkCounter.incrementBatchUnderflowCount();
	    	    	}
	     			sinkCounter.incrementBatchCompleteCount();
	    		}
	    		catch ( Exception e) {
	    			e.printStackTrace();
	    			LOG.info(e.getMessage());
	    			if ( e.getMessage().contains("unable to parse")) {
	    				LOG.info("This contains bogus data that InfluxDB will never accept. Silently dropping events in order not to fill up channels");
	    				txn.commit();
	    			}
	    			else {
	    			txn.rollback();
	    			}
	    			status = Status.BACKOFF;
	    			sinkCounter.incrementConnectionFailedCount();
	    		}
	    	}
	    	
	    	if(event == null) {
	    		status = Status.BACKOFF;
	    	}

	    	return status;
	    }
	    catch (Throwable t) {
	    	txn.rollback();
	    	// Log exception, handle individual exceptions as needed
	    	LOG.info(t.getMessage());
	    	status = Status.BACKOFF;

	    	// re-throw all Errors
	    	if (t instanceof Error) {
	    		throw (Error)t;
	    	    }
	    }
	    finally {
	    	txn.close();
	    }
	    return status;
  }

private String ExtractInfluxEvent(Event event, Boolean influx_source, String truncate_timestamp) {
	//get event resource data body.
	String body = new String(event.getBody());
	//split String with "\\s+" ,and get value array.
	String v[]=body.trim().split("\\s+");
	//get field name list from conf/XXX.conf file
	String n[]=fieldList.split(",");
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
	StringBuilder sb= new StringBuilder("test,");
	sb.append(tagList);
	// thw whiespace  seperate tag from value.
	sb.append(" ");
	for(int i=0;i<n.length;i++){
		sb.append(n[i]);
		sb.append("=");
		sb.append(v[i]);
		if(i<n.length-1)
		sb.append(",");
		}
	//flume log may only display 16 bytes,so you can use system print function or change flume limit.
	LOG.debug(sb.toString());
	return sb.toString();
}
}

