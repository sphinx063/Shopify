package com.asu.mlenka.shopify;

import org.codehaus.jettison.json.JSONException;

import com.asu.mlenka.restclient.ShopifyClient;
import com.asu.mlenka.utility.JSONResponseParser;

/**
 * @author mlenka
 *
 */
public class ShopifyTester {

	public static void main(String[] args) {
		try {
			JSONResponseParser.Result res = new ShopifyClient().retrieveAllOrders();
			System.out.println("Total Orders : " + res.getTotalOrders());
			System.out.println("Total Customers : " + res.getTotalUniqueCustomers());
			String[] s = res.getMostAndLeastOrderedItems();
			System.out.println("Least Frequently Ordered Item : " + s[0] + "\nMost Frequently Ordered Item : " + s[1]);
			System.out.println("Median Order Value (in cents) : " + res.getMedianOrderValue());
			System.out.println(
					"Shortest Order Interval (in milliseconds) : " + res.getShortestConsecutiveOrderInterval());
		} catch (JSONException e) {
			System.out.println("ShopifyTester: " + e.getMessage());
		}
	}
}
