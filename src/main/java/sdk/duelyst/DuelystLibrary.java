package sdk.duelyst;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.Map;

import javax.json.Json;
import javax.json.JsonArray;
import javax.json.JsonObject;
import javax.json.JsonValue;
import javax.json.JsonValue.ValueType;

import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;

public class DuelystLibrary {
	public static final String DEFAULT_URL = "http://duelystdb.com/card/all.json";

	public static Map<Integer, Card> cardsById = new HashMap<Integer, Card>();
	public static Map<String, Card> cardsByName = new HashMap<String, Card>();
	
	public static boolean loaded = false;
	
	public static void load() throws ClientProtocolException, IOException {
		load(DEFAULT_URL);
	}
	
	public static void load(String url) throws ClientProtocolException, IOException {
		if (loaded)
			throw new IllegalStateException("Library has already been loaded.");
		
		CloseableHttpClient httpClient = HttpClients.createDefault();
		HttpGet httpGet = new HttpGet(url);
		CloseableHttpResponse httpResponse = httpClient.execute(httpGet);
		
		try {
		    BufferedReader reader = new BufferedReader(new InputStreamReader(httpResponse.getEntity().getContent()));
		    JsonArray array = Json.createReader(reader).readArray();
		    
		    for (JsonValue value : array) {
		        if (value.getValueType() == ValueType.OBJECT) {
		        	JsonObject object = (JsonObject)value;
		        	if(object.getString("enabled").equals("1")) {
		        		int id = Integer.parseInt(object.getString("duelyst_id"));
		        		
		        		String name = object.getString("label");
		        		CardType type = CardType.valueOf(object.getString("type").toUpperCase());
		        		Faction faction = Faction.fromValue(Integer.parseInt(object.getString("faction_id")));
		        		Rarity rarity = Rarity.fromValue(Integer.parseInt(object.getString("rarity")));
		        		
		        		int manaCost = Integer.parseInt(object.getString("mana_cost"));
		        		int attack = Integer.parseInt(object.getString("attack"));
		        		int health = Integer.parseInt(object.getString("health"));
		        		
		        		String description = object.getString("description");
		        		
		        		Card card = new Card(id, name, type, faction, rarity, manaCost, attack, health, description);
		        		
		        		// Duplicates in there for some reason
		        		if (!cardsByName.containsKey(name.toUpperCase())) {
			        		cardsById.put(id, card);
			        		cardsByName.put(name.toUpperCase(), card);
		        		}
		        	}
		        }
		    }
		    
		    loaded = true;
		} finally {
		    httpResponse.close();
		    httpClient.close();
		    
			if (!loaded) {
				cardsById = new HashMap<Integer, Card>();
				cardsByName = new HashMap<String, Card>();
			}
		}
	}
}
