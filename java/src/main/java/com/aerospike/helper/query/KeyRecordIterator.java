package com.aerospike.helper.query;

import java.io.Closeable;
import java.io.IOException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.apache.log4j.Logger;

import com.aerospike.client.Key;
import com.aerospike.client.Record;
import com.aerospike.client.query.KeyRecord;
import com.aerospike.client.query.RecordSet;
import com.aerospike.client.query.ResultSet;

public class KeyRecordIterator implements Iterator<KeyRecord>, Closeable {
	private static final String META_DATA = "meta_data";
	private static final String SET_NAME = "set_name";
	private static final String DIGEST = "digest";
	private static final String EXPIRY = "expiry";
	private static final String GENERATION = "generation";
	private static Logger log = Logger.getLogger(KeyRecordIterator.class);
	private RecordSet recordSet;
	private ResultSet resultSet;
	private Iterator<KeyRecord> recordSetIterator;
	private Iterator<Object> resultSetIterator;
	private String namespace;
	private KeyRecord singleRecord;
	
	public KeyRecordIterator(String namespace) {
		super();
		this.namespace = namespace;
	}

	public KeyRecordIterator(String namespace, KeyRecord singleRecord) {
		this(namespace);
		this.singleRecord = singleRecord;
	}

	public KeyRecordIterator(String namespace, RecordSet recordSet) {
		this(namespace);
		this.recordSet = recordSet;
		this.recordSetIterator = recordSet.iterator();
	}


	public KeyRecordIterator(String namespace, ResultSet resultSet) {
		this(namespace);
		this.resultSet = resultSet;
		this.resultSetIterator = resultSet.iterator();
		
	}

	@Override
	public void close() throws IOException {
		if (recordSet != null)
			recordSet.close();
		if (resultSet != null)
			resultSet.close();
		if (singleRecord != null)
			singleRecord = null;
	}

	@Override
	public boolean hasNext() {
		if (this.recordSetIterator != null)
			return this.recordSetIterator.hasNext();
		else if (this.resultSetIterator != null)
			return this.resultSetIterator.hasNext();
		else if (this.singleRecord != null)
			return true;
		else 
			return false;
	}

	@Override
	public KeyRecord next() {
		KeyRecord keyRecord = null;

		if (this.recordSetIterator != null) {
			keyRecord = this.recordSetIterator.next();
		} else if (this.resultSetIterator != null) {
			Map<String, Object> map = (Map) this.resultSetIterator.next();
			Map<String,Object> meta = (Map<String, Object>) map.get(META_DATA);
			map.remove(META_DATA);
			Map<String,Object> binMap = new HashMap<String, Object>(map);
			if (log.isDebugEnabled()){
				for (Map.Entry<String, Object> entry : map.entrySet())
				{
					log.debug(entry.getKey() + " = " + entry.getValue());
				}
			}
			Long generation =  (Long) meta.get(GENERATION);
			Long ttl =  (Long) meta.get(EXPIRY);
			Record record = new Record(binMap, generation.intValue(), ttl.intValue());
			Key key = new Key(namespace, (byte[]) meta.get(DIGEST), (String) meta.get(SET_NAME), null);
			keyRecord = new KeyRecord(key , record);
		} else if (singleRecord != null){
			keyRecord = singleRecord;
			singleRecord = null;
		}
		return keyRecord;
	}

	@Override
	public void remove() {
		
	}

}
