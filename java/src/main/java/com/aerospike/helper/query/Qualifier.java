/* Copyright 2012-2017 Aerospike, Inc.
 *
 * Portions may be licensed to Aerospike, Inc. under one or more contributor
 * license agreements.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
/* Copyright 2012-2015 Aerospike, Inc.
 *
 * Portions may be licensed to Aerospike, Inc. under one or more contributor
 * license agreements.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package com.aerospike.helper.query;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import com.aerospike.client.Value;
import com.aerospike.client.command.ParticleType;
import com.aerospike.client.query.Filter;
import com.aerospike.client.query.IndexCollectionType;
import com.aerospike.client.query.PredExp;

/**
 * Generic Bin qualifier. It acts as a filter to exclude records that do not met this criteria.
 * The operations supported are:
 * <ul>
 * <li>EQ - Equals</li>
 * <li>GT - Greater than</li>
 * <li>GTEQ - Greater than or equal to</li>
 * <li>LT - Less than</li>
 * <li>LTEQ - Less than or equal to</li>
 * <li>NOTEQ - Not equal</li>
 * <li>BETWEEN - Between two value (inclusive)</li>
 * <li>START_WITH - A string that starts with</li>
 * <li>ENDS_WITH - A string that ends with</li>
 * </ul><p>
 *
 * @author Peter Milne
 */
public class Qualifier implements Map<String, Object>, Serializable {
	private static final long serialVersionUID = -2689196529952712849L;
	private static final String FIELD = "field";
	private static final String IGNORE_CASE = "ignoreCase";
	private static final String VALUE2 = "value2";
	private static final String VALUE1 = "value1";
	private static final String QUALIFIERS = "qualifiers";
	private static final String OPERATION = "operation";
	private static final String AS_FILTER = "queryAsFilter";
	protected Map<String, Object> internalMap;

	public enum FilterOperation {
		EQ, GT, GTEQ, LT, LTEQ, NOTEQ, BETWEEN, START_WITH, ENDS_WITH, CONTAINING, IN,
		LIST_CONTAINS, MAP_KEYS_CONTAINS, MAP_VALUES_CONTAINS,
		LIST_BETWEEN, MAP_KEYS_BETWEEN, MAP_VALUES_BETWEEN, GEO_WITHIN,
		OR, AND
	}

	public Qualifier() {
		super();
		internalMap = new HashMap<String, Object>();
	}

	public Qualifier(FilterOperation operation, Qualifier... qualifiers) {
		this();
		internalMap.put(QUALIFIERS, qualifiers);
		internalMap.put(OPERATION, operation);
	}
	
	public Qualifier(String field, FilterOperation operation, Value value1) {
		this(field, operation, Boolean.FALSE, value1);
	}
	
	public Qualifier(String field, FilterOperation operation, Boolean ignoreCase, Value value1) {
		this();
		internalMap.put(FIELD, field);
		internalMap.put(OPERATION, operation);
		internalMap.put(VALUE1, value1);
		internalMap.put(IGNORE_CASE, ignoreCase);
	}

	public Qualifier(String field, FilterOperation operation, Value value1, Value value2) {
		this(field, operation, Boolean.FALSE, value1);
		internalMap.put(VALUE2, value2);
	}

	public FilterOperation getOperation() {
		return (FilterOperation) internalMap.get(OPERATION);
	}

	public String getField() {
		return (String) internalMap.get(FIELD);
	}

	public void asFilter(Boolean queryAsFilter) {
		internalMap.put(AS_FILTER, queryAsFilter);
	}

	public Boolean queryAsFilter() {
		return internalMap.containsKey(AS_FILTER) && (Boolean) internalMap.get(AS_FILTER);
	}

	public Qualifier[] getQualifiers() {
		return (Qualifier[]) internalMap.get(QUALIFIERS);
	}

	public Value getValue1() {
		return (Value) internalMap.get(VALUE1);
	}

	public Value getValue2() {
		return (Value) internalMap.get(VALUE2);
	}

	public Filter asFilter() {
		FilterOperation op = getOperation();
		switch (op) {
			case EQ:
				if (getValue1().getType() == ParticleType.INTEGER)
					return Filter.equal(getField(), getValue1().toLong());
				else
					return Filter.equal(getField(), getValue1().toString());
			case GTEQ:
			case BETWEEN:
				return Filter.range(getField(), getValue1().toLong(), getValue2()==null?Long.MAX_VALUE:getValue2().toLong());
			case GT:
				return Filter.range(getField(), getValue1().toLong()+1, getValue2()==null?Long.MAX_VALUE:getValue2().toLong());
			case LT:
				return Filter.range(getField(), Long.MIN_VALUE, getValue1().toLong()-1);
			case LTEQ:
				return Filter.range(getField(),  Long.MIN_VALUE, getValue1().toLong()+1);
			case LIST_CONTAINS:
				return collectionContains(IndexCollectionType.LIST);
			case MAP_KEYS_CONTAINS:
				return collectionContains(IndexCollectionType.MAPKEYS);
			case MAP_VALUES_CONTAINS:
				return collectionContains(IndexCollectionType.MAPVALUES);
			case LIST_BETWEEN:
				return collectionRange(IndexCollectionType.LIST);
			case MAP_KEYS_BETWEEN:
				return collectionRange(IndexCollectionType.MAPKEYS);
			case MAP_VALUES_BETWEEN:
				return collectionRange(IndexCollectionType.MAPKEYS);
			case GEO_WITHIN:
				return geoWithinRadius(IndexCollectionType.DEFAULT);
			default:
				return null;
		}
	}
	
	private Filter geoWithinRadius(IndexCollectionType collectionType) {
		return  Filter.geoContains(getField(), getValue1().toString());
	}
	
	private Filter collectionContains(IndexCollectionType collectionType) {
		Value val = getValue1();
		int valType = val.getType();
		switch (valType) {
			case ParticleType.INTEGER:
				return Filter.contains(getField(), collectionType, val.toLong());
			case ParticleType.STRING:
				return Filter.contains(getField(), collectionType, val.toString());
		}
		return null;
	}

	private Filter collectionRange(IndexCollectionType collectionType) {
		return Filter.range(getField(), collectionType, getValue1().toLong(), getValue2().toLong());
	}
	
	public List<PredExp> toPredExp() throws PredExpException{
		List<PredExp> rs = new ArrayList<PredExp>();
		switch(getOperation()){
		case AND:
			Qualifier[] qs = (Qualifier[])get(QUALIFIERS);
			for(Qualifier q : qs) rs.addAll(q.toPredExp());
			rs.add(PredExp.and(qs.length));
			break;
		case OR:
			qs = (Qualifier[])get(QUALIFIERS);
			for(Qualifier q : qs) rs.addAll(q.toPredExp());
			rs.add(PredExp.or(qs.length));
			break;
		case IN: // Conver IN to a collection of or as Aerospike has not support for IN query
			Value val = getValue1();
			int valType = val.getType();
			if(valType != ParticleType.LIST) 
				throw new IllegalArgumentException("FilterOperation.IN expects List argument with type: " + ParticleType.LIST + ", but got: " + valType);
			List<?> inList = (List<?>) val.getObject();
			for(Object value : inList) rs.addAll(new Qualifier(this.getField(), FilterOperation.EQ, Value.get(value)).toPredExp());
			rs.add(PredExp.or(inList.size()));		
			break;
		case EQ:
			val = getValue1();
			valType = val.getType();
			switch (valType) {
				case ParticleType.INTEGER: 
					rs.add(PredExp.integerBin(getField()));
					rs.add(PredExp.integerValue(val.toLong()));
					rs.add(PredExp.integerEqual());
					break;
				case ParticleType.STRING:
					rs.add(PredExp.stringBin(getField()));
					rs.add(PredExp.stringValue(val.toString()));
					rs.add(PredExp.stringEqual());
					break;
					default:
						throw new PredExpException("PredExp Unsupported Particle Type: " + valType);
			}
			break;
		case NOTEQ:
			rs.addAll(Arrays.asList(valToPredExp(getValue1())));
			rs.add(getValue1().getType()==ParticleType.INTEGER?PredExp.integerUnequal():PredExp.stringUnequal());
			break;
		case GT:
			rs.addAll(Arrays.asList(valToPredExp(getValue1())));
			rs.add(PredExp.integerGreater());
			break;
		case GTEQ:
			rs.addAll(Arrays.asList(valToPredExp(getValue1())));
			rs.add(PredExp.integerGreaterEq());
			break;
		case LT:
			rs.addAll(Arrays.asList(valToPredExp(getValue1())));
			rs.add(PredExp.integerLess());
			break;
		case LTEQ:
			rs.addAll(Arrays.asList(valToPredExp(getValue1())));
			rs.add(PredExp.integerLessEq());
			break;
		case BETWEEN:
			return new Qualifier(FilterOperation.AND, new Qualifier(getField(), FilterOperation.GTEQ, getValue1()), new Qualifier(getField(), FilterOperation.LTEQ, getValue2())).toPredExp();
		case GEO_WITHIN:
			rs.addAll(Arrays.asList(valToPredExp(getValue1())));
			rs.add(PredExp.geoJSONWithin());
			break;
		case LIST_CONTAINS:
		case MAP_KEYS_CONTAINS:
		case MAP_VALUES_CONTAINS:
		case LIST_BETWEEN:
		case MAP_KEYS_BETWEEN:
		case MAP_VALUES_BETWEEN:
		case START_WITH:			
		case ENDS_WITH:
		case CONTAINING:
		default:
			throw new PredExpException("PredExp Unsupported Operation: " + getOperation());
		}
		return rs;
	}
	
	private PredExp[] valToPredExp(Value val) throws PredExpException{
		switch (val.getType()) {
			case ParticleType.INTEGER:
				return new PredExp[]{
					PredExp.integerBin(getField()),
					PredExp.integerValue(val.toLong())};
			case ParticleType.STRING:
				return new PredExp[]{
					PredExp.stringBin(getField()),
					PredExp.stringValue(val.toString())};
			case ParticleType.GEOJSON:
				return new PredExp[]{
					PredExp.geoJSONBin(getField()),
					PredExp.geoJSONValue(val.toString())};
			default:
				throw new PredExpException("PredExp Unsupported Particle Type: " + val.getType());
		}		
	}

	public String luaFilterString(){
		String value1 = luaValueString(getValue1());
		FilterOperation op = getOperation();
		switch (op) {
			case AND:
				return new StringBuffer()
						.append("(")
						.append(Arrays.asList((Qualifier[])get(QUALIFIERS)).stream().map(Qualifier::luaFilterString).collect(Collectors.joining(" and ")))
						.append(")").toString();
			case OR:
				return new StringBuffer()
						.append("(")
						.append(Arrays.asList((Qualifier[])get(QUALIFIERS)).stream().map(Qualifier::luaFilterString).collect(Collectors.joining(" or ")))
						.append(")").toString();
			case EQ:
				return String.format("%s == %s", luaFieldString(getField()), value1);
			case LIST_CONTAINS:
				return String.format("containsValue(%s, %s)", luaFieldString(getField()), value1);
			case MAP_KEYS_CONTAINS:
				return String.format("containsKey(%s, %s)", luaFieldString(getField()), value1);
			case MAP_VALUES_CONTAINS:
				return String.format("containsValue(%s, %s)", luaFieldString(getField()), value1);
			case NOTEQ:
				return String.format("%s ~= %s", luaFieldString(getField()), value1);
			case GT:
				return String.format("%s > %s", luaFieldString(getField()), value1);
			case GTEQ:
				return String.format("%s >= %s", luaFieldString(getField()), value1);
			case LT:
				return String.format("%s < %s", luaFieldString(getField()), value1);
			case LTEQ:
				return String.format("%s <= %s", luaFieldString(getField()), value1);
			case BETWEEN:
				String value2 = luaValueString(getValue2());
				String fieldString = luaFieldString(getField());
				return String.format("%s >= %s and %s <= %s  ", fieldString, value1, luaFieldString(getField()), value2);
			case LIST_BETWEEN:
				value2 = luaValueString(getValue2());
				return String.format("rangeValue(%s, %s, %s)", luaFieldString(getField()), value1, value2);
			case MAP_KEYS_BETWEEN:
				value2 = luaValueString(getValue2());
				return String.format("rangeKey(%s, %s, %s)", luaFieldString(getField()), value1, value2);
			case MAP_VALUES_BETWEEN:
				value2 = luaValueString(getValue2());
				return String.format("rangeValue(%s, %s, %s)", luaFieldString(getField()), value1, value2);
			case START_WITH:
				if((Boolean) internalMap.get(IGNORE_CASE))
					return String.format("string.upper(string.sub(%s,1,string.len(%s)))==%s", luaFieldString(getField()), value1, value1.toUpperCase());
				else
					return String.format("string.sub(%s,1,string.len(%s))==%s", luaFieldString(getField()), value1, value1);
			case ENDS_WITH:
				return String.format("%s=='' or string.sub(%s,-string.len(%s))==%s",
						value1,
						luaFieldString(getField()),
						value1,
						value1);
			case CONTAINING:
				if((Boolean) internalMap.get(IGNORE_CASE))
					return String.format("string.find(string.upper(%s), %s)", luaFieldString(getField()), value1.toUpperCase());
				else
					return String.format("string.find(%s, %s)", luaFieldString(getField()), value1);
			case GEO_WITHIN:
				return String.format("%s %d %s %s)", getField(), ParticleType.GEOJSON, value1, value1);
			default:
				break;
		}
		return "";
	}

	protected String luaFieldString(String field) {
		return String.format("rec['%s']", field);
	}

	protected String luaValueString(Value value) {
		String res = null;
		if(null == value) return res;
		int type = value.getType();
		switch (type) {
			//		case ParticleType.LIST:
			//			res = value.toString();
			//			break;
			//		case ParticleType.MAP:
			//			res = value.toString();
			//			break;
			//		case ParticleType.DOUBLE:
			//			res = value.toString();
			//			break;
		case ParticleType.STRING:
			res = String.format("'%s'", value.toString());
			break;
		case ParticleType.GEOJSON:
			res = String.format("'%s'", value.toString());
			break;
		default:
				res = value.toString();
				break;
		}
		return res;
	}


	/*
	 * (non-Javadoc)
	 * @see java.util.Map#size()
	 */
	@Override
	public int size() {
		return internalMap.size();
	}

	/*
	 * (non-Javadoc)
	 * @see java.util.Map#isEmpty()
	 */
	@Override
	public boolean isEmpty() {
		return internalMap.isEmpty();
	}

	/*
	 * (non-Javadoc)
	 * @see java.util.Map#containsKey(java.lang.Object)
	 */
	@Override
	public boolean containsKey(java.lang.Object key) {
		return internalMap.containsKey(key);
	}

	/*
	 * (non-Javadoc)
	 * @see java.util.Map#containsValue(java.lang.Object)
	 */
	@Override
	public boolean containsValue(java.lang.Object value) {
		return internalMap.containsValue(value);
	}

	/*
	 * (non-Javadoc)
	 * @see java.util.Map#get(java.lang.Object)
	 */
	@Override
	public Object get(java.lang.Object key) {
		return internalMap.get(key);
	}

	/*
	 * (non-Javadoc)
	 * @see java.util.Map#put(java.lang.Object, java.lang.Object)
	 */
	@Override
	public Object put(String key, Object value) {
		return internalMap.put(key, value);
	}

	/*
	 * (non-Javadoc)
	 * @see java.util.Map#remove(java.lang.Object)
	 */
	@Override
	public Object remove(java.lang.Object key) {
		return internalMap.remove(key);
	}

	/*
	 * (non-Javadoc)
	 * @see java.util.Map#putAll(java.util.Map)
	 */
	@Override
	public void putAll(Map<? extends String, ? extends Object> m) {
		internalMap.putAll(m);
	}

	/*
	 * (non-Javadoc)
	 * @see java.util.Map#clear()
	 */
	@Override
	public void clear() {
		internalMap.clear();
	}

	/*
	 * (non-Javadoc)
	 * @see java.util.Map#keySet()
	 */
	@Override
	public Set<String> keySet() {
		return internalMap.keySet();
	}

	/*
	 * (non-Javadoc)
	 * @see java.util.Map#values()
	 */
	@Override
	public Collection<Object> values() {
		return internalMap.values();
	}

	/*
	 * (non-Javadoc)
	 * @see java.util.Map#entrySet()
	 */
	@Override
	public Set<java.util.Map.Entry<String, Object>> entrySet() {
		return internalMap.entrySet();
	}

	@Override
	public String toString() {
		String output = String.format("%s:%s:%s:%s", getField(), getOperation(), getValue1(), getValue2());
		return output;
	}
}
