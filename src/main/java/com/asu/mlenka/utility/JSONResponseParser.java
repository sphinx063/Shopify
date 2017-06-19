package com.asu.mlenka.utility;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.TreeSet;

import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;

/**
 * @author mlenka
 *
 */
public class JSONResponseParser {
	private Map<String, Long> itemCountMap = null;
	private PriorityQueue<Long> smallerOrderValues = null;
	private PriorityQueue<Long> largerOrderValues = null;
	private Map<String, TreeSet<Long>> customerOrderTimesMap = null;
	private long minOrderInterval = Long.MAX_VALUE;
	private long totalOrders = 0;

	public JSONResponseParser() {
		itemCountMap = new HashMap<String, Long>();
		smallerOrderValues = new PriorityQueue<Long>();
		largerOrderValues = new PriorityQueue<Long>();
		customerOrderTimesMap = new HashMap<String, TreeSet<Long>>();
	}

	public boolean parseJsonResponse(String jsonResponse) throws JSONException {
		String customerId = null;
		DateTimeFormatter dtf = null;
		DateTime createdAt = null;
		long totalPriceInCents = 0l;
		JSONObject masterObject = new JSONObject(jsonResponse);
		JSONArray jsonOrders = masterObject.getJSONArray("orders");
		if (jsonOrders.length() == 0)
			return false;
		totalOrders += jsonOrders.length();
		for (int i = 0; i < jsonOrders.length(); i++) {
			JSONObject jsonOrder = jsonOrders.getJSONObject(i);
			customerId = jsonOrder.getJSONObject("customer").getString("id");
			dtf = DateTimeFormat.forPattern("yyyy-MM-dd'T'HH:mm:ssZ");
			createdAt = dtf.parseDateTime(jsonOrder.getString("created_at"));
			BigDecimal dollars = new BigDecimal(jsonOrder.getString("total_price"));
			if (dollars.scale() > 2) {
				throw new IllegalArgumentException();
			}
			totalPriceInCents = dollars.multiply(new BigDecimal(100)).longValue();
			
			addToOrderValueQ(totalPriceInCents);
			putInCustomerOrderTimeMap(customerId, createdAt.getMillis());
			
			JSONArray items = jsonOrder.getJSONArray("line_items");
			for (int j = 0; j < items.length(); j++) {
				JSONObject jsonItem = items.getJSONObject(j);
				String itemId = jsonItem.getString("product_id");
				Long quantity = jsonItem.getLong("quantity");
				itemCountMap.put(itemId, itemCountMap.getOrDefault(itemId, 0l) + quantity);
			}

		}
		return true;
	}

	private void putInCustomerOrderTimeMap(String customerId, long createdAt) {
		if (customerOrderTimesMap.containsKey(customerId)) {
			Long lValue = customerOrderTimesMap.get(customerId).floor(createdAt);
			Long rValue = customerOrderTimesMap.get(customerId).ceiling(createdAt);
			if (lValue != null && ((createdAt - lValue) < minOrderInterval)) {
				minOrderInterval = createdAt - lValue;
			}
			if (rValue != null && ((rValue - createdAt) < minOrderInterval)) {
				minOrderInterval = rValue - createdAt;
			}
			customerOrderTimesMap.get(customerId).add(createdAt);
		} else {
			TreeSet<Long> orderTimes = new TreeSet<Long>();
			orderTimes.add(createdAt);
			customerOrderTimesMap.put(customerId, orderTimes);
		}
	}

	private void addToOrderValueQ(long price) {
		largerOrderValues.add(price);
		smallerOrderValues.add(-largerOrderValues.poll());
		if (largerOrderValues.size() < smallerOrderValues.size())
			largerOrderValues.add(-smallerOrderValues.poll());
	}

	public JSONResponseParser.Result getResult() {
		JSONResponseParser.Result result = new Result();
		return result;
	}

	public class Result {
		private Result() {

		}

		public long getMedianOrderValue() {
			return largerOrderValues.size() > smallerOrderValues.size() ? largerOrderValues.peek()
					: new BigDecimal((largerOrderValues.peek() - smallerOrderValues.peek()) / 2.0)
							.setScale(0, BigDecimal.ROUND_HALF_UP).longValue();
		}

		public int getTotalUniqueCustomers() {
			return customerOrderTimesMap.size();
		}

		public long getTotalOrders() {
			return totalOrders;
		}

		public long getShortestConsecutiveOrderInterval() {
			return minOrderInterval;
		}

		/*
		 * Returns the least ordered item id in itemIds[0] and the most ordered
		 * item id in itemIds[1]
		 * 
		 */
		public String[] getMostAndLeastOrderedItems() {
			Map.Entry<String, Long> minItem = null;
			Map.Entry<String, Long> maxItem = null;
			for (Map.Entry<String, Long> item : itemCountMap.entrySet()) {
				if (minItem == null || minItem.getValue() > item.getValue()) {
					minItem = item;
				}
				if (maxItem == null || maxItem.getValue() < item.getValue()) {
					maxItem = item;
				}
			}
			String[] itemIds = new String[2];
			itemIds[0] = minItem.getKey();
			itemIds[1] = maxItem.getKey();
			return itemIds;
		}
	}
}
