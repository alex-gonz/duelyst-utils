package sdk.duelyst;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

public class GauntletDataCyno {
	public static final String DEFAULT_URL = "https://docs.google.com/document/d/1JKJ5fjvwhchefhJcrQDOgRIwqaDKpj13PRstVyn7mG0/pub";
	private static final int TIMEOUT = 30 * 1000;
    
    private static Map<String, String> nameIssues = new HashMap<String, String>();

    public static Map<Faction, Integer> factions = new HashMap<Faction, Integer>();
    public static Map<Character, String> symbols = new HashMap<Character, String>();
    public static Map<Faction, Map<Integer, Rating>> ratings = new HashMap<Faction, Map<Integer, Rating>>();
	
	public static boolean loaded = false;
    
    public static void load() throws IOException {
    	load(DEFAULT_URL);
    }
    
    public static void load(String url) throws IOException {
		if (loaded)
			throw new IllegalStateException("Library has already been loaded.");
		
		// Issues with Cyno's names
		nameIssues.put("Aspect of the Mountain", "Aspect of the Mountains");
		nameIssues.put("Reaper of Nine Moons", "Reaper of the Nine Moons");
		nameIssues.put("Shadow Dancer", "Shadowdancer");
		nameIssues.put("Song Weaver", "Songweaver");
		nameIssues.put("Star's Fury", "Stars' Fury");
		nameIssues.put("Wings of Mechazor", "Wings of Mechaz0r");
		nameIssues.put("Wrathling Fury", "Wraithling Fury");
		nameIssues.put("Wrathling Swarm", "Wraithling Swarm");
		
    	Document doc = Jsoup.connect(url).timeout(TIMEOUT).get();
    	
    	try {
	    	int tableIndex = 0;
	    	for (Element table : doc.select("table")) {
	    		switch (tableIndex) {
	    		case 0:
	    		{
	    			Elements rows = table.select("tr");
	    			for (int i = 1; i < rows.size(); i++) { // Skip first row (column titles)
	    			    Element row = rows.get(i);
	    			    Elements columns = row.select("td");
	    			    
	    			    Faction faction = Faction.valueOf(columns.get(0).text().toUpperCase());
	    			    int rating = Integer.parseInt(columns.get(1).text());
	    			    
	    			    factions.put(faction, rating);
	    			}
	    			break;
	    		}
	    		case 1:
	    		{
	    			Elements rows = table.select("tr");
	    			for (int i = 1; i < rows.size(); i++) { // Skip first row (column titles)
	    			    Element row = rows.get(i);
	    			    Elements columns = row.select("td");
	    			    
	    			    char symbol = columns.get(0).text().toCharArray()[0];
	    			    String description = columns.get(1).text();
	
	                    // Anal
	                    if (!description.endsWith("."))
	                        description += ".";
	    			    
	    			    symbols.put(symbol, description);
	    			}
	    			break;
	    		}
	    		default:
	    		{
	    			Elements rows = table.select("tr");
	    			Element row = rows.get(0);
	    			Elements columns = row.select("td");
	    			
	    			String title = columns.get(0).text();
	    			title = title.substring(0, title.indexOf(' '));
	    			
	    			if (!title.equals("Rating")) { // Currently there's a second rating table on the page
	    				Faction faction = Faction.valueOf(title.toUpperCase());
	    				ratings.put(faction, new HashMap<Integer, Rating>());
	    				
	    				for (int i = 2; i < rows.size(); i++) { // Skip first row (column titles)
	        			    row = rows.get(i);
	        			    columns = row.select("td");
	        			    
	        			    for (int j = 0; j < 2; j++)
	                        {
	                            String name = fixName(columns.get(0 + (j * 3)).text());
	                            if (!name.isEmpty()) {
	                                Card card = DuelystLibrary.cardsByName.get(name.toUpperCase());
	                                
	                                if (card == null)
	                                	System.out.println("Card with name '" + name + "' not found.");
	                                
	                            	String valueString = columns.get(1 + (j * 3)).text();
	                            	int value = valueString.isEmpty() ? -1 : Integer.parseInt(valueString);
	                                String symbols = columns.get(2 + (j * 3)).text();
	                                
	                                String notes = "";
	                                
	                        		char[] symbolChars = symbols.toCharArray();
	                        		for (char symbol : symbolChars) {
	                        			if (!notes.isEmpty())
	                        				notes += System.lineSeparator();
	                        			
	                        			if (GauntletDataCyno.symbols.containsKey(symbol))
	                        				notes += GauntletDataCyno.symbols.get(symbol);
	                                    else
	                                    	notes += "Unknown symbol encountered.";
	                        		}
	                              if (card != null) {
																	ratings.get(faction).put(card.id, new Rating(value, notes));
																}
	                            }
	                        }
	        			}
	    			}
	    			break;
	    		}
	    		}
	    		
	    		tableIndex++;
	    	}
	    	
	    	loaded = true;
		} finally {
			if (!loaded) {
				factions = new HashMap<Faction, Integer>();
			    symbols = new HashMap<Character, String>();
			    ratings = new HashMap<Faction, Map<Integer, Rating>>();
			}
		}
    }

	private static String fixName(String name) {
				name = name.replaceAll("\\u2019", "'");

        if (nameIssues.containsKey(name))
        	name = nameIssues.get(name);
        
        return name;
	}
}
