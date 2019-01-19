package external;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import entity.Item;
import entity.Item.ItemBuilder;

//import com.sun.xml.internal.bind.v2.schemagen.xmlschema.List;

public class TicketMasterAPI {
	// step 1
	private static final String URL = "https://app.ticketmaster.com/discovery/v2/events.json";
	// suppose clients do not provide any search words
	private static final String DEFAULT_KEYWORD = ""; // no restriction
	private static final String API_KEY = "kZERr47Bl8IkeBt6rrn4NilTGqoc2rER";
	
	// step a
	private static final String EMBEDDED = "_embedded";
	private static final String EVENTS = "events";
	private static final String NAME = "name";
	private static final String ID = "id";
	private static final String URL_STR = "url";
	private static final String RATING = "rating";
	private static final String DISTANCE = "distance";
	private static final String VENUES = "venues";
	private static final String ADDRESS = "address";
	private static final String LINE1 = "line1";
	private static final String LINE2 = "line2";
	private static final String LINE3 = "line3";
	private static final String CITY = "city";
	private static final String IMAGES = "images";
	private static final String CLASSIFICATIONS = "classifications";
	private static final String SEGMENT = "segment";
	
	// step 2
	// search the events 
	public List<Item> search(double lat, double lon, String keyword) {
		//return new JSONArray();
		
		// step 4: encode the keyword in URL since it may contain special characters
		// the purpose to encode: turn space into 20%, space is reserved for HTTP request line
		if (keyword == null) {
			keyword = DEFAULT_KEYWORD;
		}
		try {
			// UTF-8: 8-bit Unicode
			keyword = java.net.URLEncoder.encode(keyword, "UTF-8");
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		// step 5: convert lat/lon to geohash
		String geoHash = GeoHash.encodeGeohash(lat, lon, 8);
		
		// step 6:
		// Make URL "apikey=12345&geoPoint=abcd&keyword=music&radius=50"
		String query = String.format("apikey=%s&geoPoint=%s&keyword=%s&radius=%s",
				API_KEY, geoHash, keyword, 50);
		
		// step 7: 
		// Open a HTTP connection between your Java application and TicketMaster based URL
		// step 7.1: combine URL and query 
		// https://app.ticketmaster.com/discovery/v2/events.json?apikey=12345&geoPoint=abcd&keyword=music&radius=50
		// (HttpURLCoonection)转化是为了之后可以调用HttpURLCoonection的method
		try {
			HttpURLConnection connection = 
					(HttpURLConnection) new URL(URL+"?"+query).openConnection(); 
			
			// step 8:
			// Set request method to GET
			connection.setRequestMethod("GET");
			
			// step 9:
			// 注意：这里是同步发生的！！!
			// Send request to TicketMaster and get response, response code could be returned directly,
			// response body is saved in inputStream of connection
			// We do not need to check the response code, since later we have "catch"
			int responseCode = connection.getResponseCode();
			
			// step 10: 
			// used to debug
			// output the response 
			System.out.println("\nSending 'GET' request to URL: " + URL + "?" + query);
			System.out.println("Response code: " + responseCode);
			
			// step 12: 
			StringBuilder response = new StringBuilder();
			
			// step 11:
			// Now read response body to get events data
			// this code originates from C++ Destructor
			
			// cautions!: no catch for this try
			// this one is "try with resource"
			// it can close "in" afterwards 
			
			// InputStream stores the response message body
			// 名为“Input”Stream的原因是：对于connection来说，获得的数据是从第三方的input，
			// 我们是从connection里面读取数据
			try (BufferedReader in = new BufferedReader(
					new InputStreamReader(connection.getInputStream()))) {
				// step 13:
				// why read line by line?
				// BufferedReader has to be read line by line
				String inputLine;
				// step a: read one line from in, store it to inputLine
				// step b: check whether inputLine is null or not
				while((inputLine = in.readLine()) != null) {
					response.append(inputLine);
				}
			}
			JSONObject obj = new JSONObject(response.toString());
			// JSONObject如果没有“_embedded”这个数据, return empty json array
			if(obj.isNull("_embedded")) {
				return new ArrayList<>();
			} 
			JSONObject embedded = obj.getJSONObject("_embedded");
			// according to past experience, if embedded exits, events must exist.
			JSONArray events = embedded.getJSONArray("events");
			
			return getItemList(events);
			
		} catch (Exception e) {
			e.printStackTrace();
		}
				
		return new ArrayList<>();
	}
	
	// https://developer.ticketmaster.com/products-and-docs/apis/discovery-api/v2/#event-details-v2
	// get event details --> response structure
	private String getAddress(JSONObject event) throws JSONException {
		if (!event.isNull(EMBEDDED)) {
			JSONObject embedded = event.getJSONObject(EMBEDDED);
			
			if (!embedded.isNull(VENUES)) {
				JSONArray venues = embedded.getJSONArray(VENUES);
				
				for (int i = 0; i < venues.length(); ++i) {
					JSONObject venue = venues.getJSONObject(i);
					
					StringBuilder sb = new StringBuilder();
					
					if (!venue.isNull(ADDRESS)) {
						JSONObject address = venue.getJSONObject(ADDRESS);
						
						if (!address.isNull(LINE1)) {
							sb.append(address.getString(LINE1));
						}
						if (!address.isNull(LINE2)) {
							sb.append('\n');
							sb.append(address.getString(LINE2));
						}
						if (!address.isNull(LINE3)) {
							sb.append('\n');
							sb.append(address.getString(LINE3));
						}
					}
					
					if (!venue.isNull(CITY)) {
						JSONObject city = venue.getJSONObject(CITY);
						
						if (!city.isNull(NAME)) {
							sb.append('\n');
							sb.append(city.getString(NAME));
						}
					}
					
					String addr = sb.toString();
					if (!addr.equals("")) {
						return addr;
					}
				}
			}
		}
		return "";
	}
	
	// {"images": [{"url": "www.example.com/my_image.jpg"}, ...]}
	private String getImageUrl(JSONObject event) throws JSONException {
		if (!event.isNull(IMAGES)) {
			JSONArray array = event.getJSONArray(IMAGES);
			for (int i = 0; i < array.length(); i++) {
				JSONObject image = array.getJSONObject(i);
				if (!image.isNull(URL_STR)) {
					return image.getString(URL_STR);
				}
			}
		}
		return "";
	}

	// {"classifications" : [{"segment": {"name": "music"}}, ...]}
	private Set<String> getCategories(JSONObject event) throws JSONException {
		Set<String> categories = new HashSet<>();

		if (!event.isNull(CLASSIFICATIONS)) {
			JSONArray classifications = event.getJSONArray(CLASSIFICATIONS);
			
			for (int i = 0; i < classifications.length(); ++i) {
				JSONObject classification = classifications.getJSONObject(i);
				
				if (!classification.isNull(SEGMENT)) {
					JSONObject segment = classification.getJSONObject(SEGMENT);
					
					if (!segment.isNull(NAME)) {
						categories.add(segment.getString(NAME));
					}
				}
			}
		}
		return categories;
	}


	// Convert JSONArray to a list of item objects.
	private List<Item> getItemList(JSONArray events) throws JSONException {
		List<Item> itemList = new ArrayList<>();
		
		for (int i = 0; i < events.length(); ++i) {
			JSONObject event = events.getJSONObject(i);
			
			// builder包含event所有的信息
			ItemBuilder builder = new ItemBuilder();
			if (!event.isNull(NAME)) {
				builder.setName(event.getString(NAME));
			}	
			if (!event.isNull(ID)) {
				builder.setItemId(event.getString(ID));
			}	
			if (!event.isNull(RATING)) {
				builder.setRating(event.getDouble(RATING));
			}
			if (!event.isNull(URL_STR)) {
				builder.setUrl(event.getString(URL_STR));
			}
			if (!event.isNull(DISTANCE)) {
				builder.setDistance(event.getDouble(DISTANCE));
			}
			builder.setAddress(getAddress(event));
			builder.setCategories(getCategories(event));
			builder.setImageUrl(getImageUrl(event));
			
			itemList.add(builder.build());
		}
		return itemList;
	}

	
	// step 3
	// test above method search()
	private void queryAPI(double lat, double lon) {
//		JSONArray events = search(lat, lon, null);
//		// since the JSONArray might throw exception, so we write try-catch
//		try {
//			// to check the result, we iterate the JSONArray
//			// 这里不能用enhanced for loop，因为JSONArray没有继承iterator
//			for (int i = 0; i < events.length(); ++i) {
//				JSONObject event = events.getJSONObject(i); 
//				System.out.println("Entered");
//				System.out.println(event);
//			}
//		} catch (Exception e) {
//			e.printStackTrace();
//		}
		List<Item> itemList = search(lat, lon, "Sports");
		try {
			for (Item item : itemList) {
				JSONObject jsonObject = item.toJSONObject();
				System.out.println(jsonObject);
			}
		} catch (Exception e) {
			e.printStackTrace();
		}

	}
	
	/**
	 * Main entry for sample TicketMaster API requests.
	 */
	public static void main(String[] args) {
		TicketMasterAPI tmApi = new TicketMasterAPI();
		// Mountain View, CA
		// tmApi.queryAPI(37.38, -122.08);
		// London, UK
		// tmApi.queryAPI(51.503364, -0.12);
		// Houston, TX
		tmApi.queryAPI(29.682684, -95.295410);
	}	
}
