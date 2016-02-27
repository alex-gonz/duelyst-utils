package sdk.duelyst.console;

import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;

import javax.json.Json;
import javax.json.JsonArray;
import javax.json.JsonObject;
import javax.json.JsonValue;
import javax.json.JsonValue.ValueType;

import sdk.duelyst.DuelystLibrary;
import sdk.utility.ChromeUtil;

import com.neovisionaries.ws.client.WebSocket;
import com.neovisionaries.ws.client.WebSocketAdapter;
import com.neovisionaries.ws.client.WebSocketException;

public class DuelystConsole {
	private static final String URL = "beta.duelyst.com";
	
	public static Process launchDebug(String chromePath) throws IOException {
		return ChromeUtil.launchDebug(chromePath, URL);
	}
	
	private WebSocket webSocket;
	
	private List<DuelystConsoleListener> listeners = new ArrayList<DuelystConsoleListener>();

    public void addListener(DuelystConsoleListener toAdd) {
        listeners.add(toAdd);
    }

    private void sendMessage(DuelystMessage message) {
        for (DuelystConsoleListener listener : listeners)
        	listener.onMessage(message);
    }
	
	public void connect(String wsUrl) throws IOException, WebSocketException {
		webSocket = ChromeUtil.connectWebSocket(wsUrl, new WebSocketAdapter() {
		    @Override
		    public void onTextMessage(WebSocket ws, String message) throws Exception {
		    	handleWsMessage(message);
		    }
		});
		ChromeUtil.enableWsConsole(webSocket, true);
		ChromeUtil.enableWsRuntime(webSocket, true);
	}
	
	public boolean isOpen() {
		return webSocket != null && webSocket.isOpen();
	}
	
	public void disconnect() {
		if (isOpen()) {
			ChromeUtil.enableWsConsole(webSocket, false);
			ChromeUtil.enableWsRuntime(webSocket, false);
			webSocket.disconnect();
		}
	}

	private void handleWsMessage(String message) {
		try {
			JsonObject jsonObject = Json.createReader(new StringReader(message)).readObject();
			
			if (wsMessageIsResponse(jsonObject)) {
				int id = jsonObject.getInt("id");
				String callback = ChromeUtil.callbacks.get(id);
				ChromeUtil.callbacks.remove(id);
				
				System.out.println(callback + " response received (" + id + ")");
			}
			else if (wsMessageIsExit(jsonObject)) {
				sendMessage(new ExitMessage());
			}
			else if (wsMessageIsConsole(jsonObject)) {
				JsonObject msg = jsonObject.getJsonObject("params").getJsonObject("message");
				String source = msg.getString("text");
				
				if (message.contains("preview")) {
					if (source.contains("cards select")) {
						for (JsonValue parameter : msg.getJsonArray("parameters")) {
							if (parameter.getValueType() == ValueType.OBJECT) {
								if (((JsonObject)parameter).containsKey("preview")) {
									JsonArray properties = ((JsonObject)parameter).getJsonObject("preview").getJsonArray("properties");
									
									// Cards are in reverse order
									int option3Id = Integer.parseInt(properties.getJsonObject(0).getString("value"));
									int option2Id = Integer.parseInt(properties.getJsonObject(1).getString("value"));
									int option1Id = Integer.parseInt(properties.getJsonObject(2).getString("value"));
									
									sendMessage(new GauntletOptionsMessage(
											DuelystLibrary.cardsById.get(option1Id),
											DuelystLibrary.cardsById.get(option2Id),
											DuelystLibrary.cardsById.get(option3Id)));
								}
							}
						}
					}
					// TODO everything else
					/*else if (source.contains("VIEW")) {
						for (JsonValue parameter : msg.getJsonArray("parameters")) {
							if (parameter.getValueType() == ValueType.OBJECT) {
								if (((JsonObject)parameter).containsKey("preview")) {
									for (JsonValue property : ((JsonObject)parameter).getJsonObject("preview").getJsonArray("properties")) {
										if (property.getValueType() == ValueType.OBJECT) {
											String name = ((JsonObject)property).getString("name");
											
											if(name.equals("cardDataOrIndex")) {
												String objectId = ((JsonObject)parameter).getString("objectId");
												ChromeUtil.getObjectProperties(webSocket, objectId);
											}
										}
									}
								}
							}
						}
					}*/
				}
			}
		} catch (Exception ex) {
			System.out.println(ex.getMessage());
			ex.printStackTrace();
		}
	}

	private boolean wsMessageIsResponse(JsonObject jsonObject) {
		return jsonObject.containsKey("id");
	}

	private boolean wsMessageIsExit(JsonObject jsonObject) {
		return jsonObject.containsKey("method") && jsonObject.getString("method").equals("Inspector.detached");
	}

	private boolean wsMessageIsConsole(JsonObject jsonObject) {
		return jsonObject.containsKey("method") && jsonObject.getString("method").equals("Console.messageAdded");
	}
}
