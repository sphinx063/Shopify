package com.asu.mlenka.restclient;

import javax.ws.rs.core.MediaType;

import org.codehaus.jettison.json.JSONException;

import com.asu.mlenka.utility.JSONResponseParser;
import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.api.client.WebResource;

/**
 * @author mlenka
 * 
 *         Shopify REST client
 */
public class ShopifyClient {
	private Client client = null;
	private WebResource webResource = null;

	public ShopifyClient() {
		initializeClient();
	}

	private void initializeClient() {
		client = Client.create();
		webResource = client.resource(IConfig.SHOPIFY_BASE_URI);
	}

	public JSONResponseParser.Result retrieveAllOrders() throws JSONException {
		int page = 0;
		JSONResponseParser jsonResponseParser = new JSONResponseParser();
		while (true) {
			page++;
			WebResource.Builder builder = webResource.path(IConfig.SHOPIFY_ALL_ORDER_API)
					.queryParam("page", String.valueOf(page)).accept(MediaType.APPLICATION_JSON);
			builder.header(IConfig.SHOPIFY_TOKEN_KEY, IConfig.SHOPIFY_TOKEN_SECRET);
			ClientResponse response = builder.get(ClientResponse.class);

			if (response.getStatus() != 200) {
				throw new RuntimeException("Failed : HTTP error code : " + response.getStatus());
			}
			String jsonResponse = response.getEntity(String.class);
			if (!jsonResponseParser.parseJsonResponse(jsonResponse))
				break;
		}

		return jsonResponseParser.getResult();
	}

}
