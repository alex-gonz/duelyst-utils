package sdk.duelyst.console;

import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;

import javax.json.Json;
import javax.json.JsonArray;
import javax.json.JsonObject;
import javax.json.JsonValue;
import javax.json.JsonValue.ValueType;

import sdk.duelyst.Card;
import sdk.duelyst.DuelystLibrary;
import sdk.duelyst.console.message.CancelMessage;
import sdk.duelyst.console.message.CardDrawnMessage;
import sdk.duelyst.console.message.CardPlayedMessage;
import sdk.duelyst.console.message.CardReplacedMessage;
import sdk.duelyst.console.message.DuelystMessage;
import sdk.duelyst.console.message.ExitMessage;
import sdk.duelyst.console.message.GameStartedMessage;
import sdk.duelyst.console.message.GauntletOptionsMessage;
import sdk.duelyst.console.message.MessageType;
import sdk.duelyst.console.message.StartingHandMessage;
import sdk.utility.ChromeUtil;

import com.neovisionaries.ws.client.WebSocket;
import com.neovisionaries.ws.client.WebSocketAdapter;
import com.neovisionaries.ws.client.WebSocketException;

public class DuelystConsole {
	private static final String URL = "beta.duelyst.com";
	private static final int DECK_SIZE = 40;
	private static final int HAND_SIZE = 3;
	
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
				Object callbackTag = ChromeUtil.callbacks.get(id);
				DuelystMessageState state = callbackTag instanceof DuelystMessageState ? (DuelystMessageState)callbackTag : null;
				ChromeUtil.callbacks.remove(id);
				
				// Handle empty responses, seems to happen on startup when querying old messages
				if (message.contains("\"result\":{}") || state == null) {
					return;
				}
				
				switch (state.type)
				{
				case CANCEL:
				case EXIT:
				case GAUNTLET_OPTIONS:
					throw new IllegalStateException("MessageType." + state.type + " encountered in message stage switching, but shouldn't be.");
				case CARD_PLAY:
				{
					switch (state.stage)
					{
					case 0:
						getResultObjectWs(jsonObject, "cardDataOrIndex", state);
						getResultObjectWs(jsonObject, "_subActions", new DuelystMessageState(state, MessageType.CARD_DRAW));
						break;
					case 1:
						int cardId = getResultInt(jsonObject, "id");
						sendMessage(new CardPlayedMessage(state.playerId, DuelystLibrary.cardsById.get(cardId)));
						break;
					}
					break;
				}
				case TURN_END:
				{
					getResultObjectWs(jsonObject, "_subActions", new DuelystMessageState(state, MessageType.CARD_DRAW));
					break;
				}
				case CARD_DRAW:
				{
					switch (state.stage)
					{
					// Starts at 1 because it's sent from other results
					case 1:
						// More than just card draw
						int i = 0;
						for (JsonValue result : jsonObject.getJsonObject("result").getJsonArray("result")) {
							if (!result.toString().contains("length")) {
								getResultObjectWs(jsonObject, i, new DuelystMessageState(state));
								i++;
							} else {
								break;
							}
						}
						
						break;
					case 2:
						getResultObjectWs(jsonObject, "cardDataOrIndex", state, true);
						break;
					case 3:
						int cardId = getResultInt(jsonObject, "id");
						sendMessage(new CardDrawnMessage(state.playerId, DuelystLibrary.cardsById.get(cardId)));
						break;
					}
					break;
				}
				case GAME_START:
				{
					switch (state.stage)
					{
					case 0:
						getResultObjectWs(jsonObject, "deck", state);
						break;
					case 1:
						for (int i = 0; i < DECK_SIZE; i++) {
							// Need separate messages so stages don't get messed up
							getResultObjectWs(jsonObject, i, new DuelystMessageState(state));
						}
						
						break;
					case 2:
						int cardId = getResultInt(jsonObject, "id");
						List<Card> deck = ((GameStartedMessage)state.message).deck;
						deck.add(DuelystLibrary.cardsById.get(cardId));
						
						if (deck.size() == DECK_SIZE) {
							sendMessage(state.message);
						}
						
						break;
					}
					break;
				}
				case CARD_REPLACE:
				{
					switch (state.stage)
					{
					case 0:
						int replacedIndex = getResultInt(jsonObject, "indexOfCardInHand");
						state.message = new CardReplacedMessage(state.playerId, replacedIndex);
						getResultObjectWs(jsonObject, "cardDataOrIndex", state);
						break;
					case 1:
						int cardId = getResultInt(jsonObject, "id");
						((CardReplacedMessage)state.message).card = DuelystLibrary.cardsById.get(cardId);
						sendMessage(state.message);
						break;
					}
					break;
				}
				case STARTING_HAND:
				{
					switch (state.stage)
					{
					case 0:
						getResultObjectWs(jsonObject, "_gameSession", state);
						break;
					case 1:
						getResultObjectWs(jsonObject, "players", state);
						break;
					case 2:
						for (int i = 0; i < 2; i++) {
							// Need separate messages so stages don't get messed up
							getResultObjectWs(jsonObject, i, new DuelystMessageState(state));
						}
						
						break;
					case 3:
						if (getResultString(jsonObject, "playerId").equals(state.playerId)) {
							getResultObjectWs(jsonObject, "deck", state);
						}
						
						break;
					case 4:
						getResultObjectWs(jsonObject, "_cachedCardsInHandExcludingMissing", state);
						break;
					case 5:
						state.message = new StartingHandMessage(state.playerId);
						for (int i = 0; i < 3; i++) {
							// Need separate messages so stages don't get messed up
							getResultObjectWs(jsonObject, i, new DuelystMessageState(state));
						}
						
						break;
					case 6:
						List<Card> hand = ((StartingHandMessage)state.message).hand;
						hand.add(DuelystLibrary.cardsById.get(getResultInt(jsonObject, "id")));
						
						if (hand.size() == HAND_SIZE) {
							sendMessage(state.message);
						}
						
						break;
					}
					break;
				}
				}
			}
			else if (wsMessageIsExit(jsonObject)) {
				sendMessage(new ExitMessage());
			}
			else if (wsMessageIsConsole(jsonObject)) {
				// TODO: gazer won't go back into deck if all spaces occupied
				// TODO: make method for all the repeated code here
				// TODO: handle card draw from everything other than cards played
				// TODO: get end game event
				// TODO: could be weird interaction with cancel on gambit that draws? should I just pass new hand on card draw???
				
				// Opening gambit cancelled
				if (message.contains("App:onUserTriggeredCancel")) {
					sendMessage(new CancelMessage());
				} else {
					JsonObject msg = jsonObject.getJsonObject("params").getJsonObject("message");
					String source = msg.getString("text");
					
					// Game start
					if (message.contains("GameSetup.setupNewSession") && message.contains("userId") && message.contains("SDK")) {
						for (int i = 0; i < 2; i++) {
							String objectId = msg.getJsonArray("parameters").getJsonObject(4 + i).getString("objectId");
							String playerId = msg.getJsonArray("parameters").getJsonObject(4 + i).getJsonObject("preview").getJsonArray("properties").getJsonObject(0).getString("value");
							String playerName = msg.getJsonArray("parameters").getJsonObject(4 + i).getJsonObject("preview").getJsonArray("properties").getJsonObject(1).getString("value");
							
							DuelystMessageState state = new DuelystMessageState(playerName, MessageType.GAME_START);
							state.message = new GameStartedMessage(playerId, playerName);
							ChromeUtil.getObjectProperties(webSocket, objectId, state);
						}
					}
					// Starting hand
					else if (message.contains("DrawStartingHandAction") && message.contains("VIEW")) {
						String objectId = msg.getJsonArray("parameters").getJsonObject(5).getString("objectId");
						String playerId = msg.getJsonArray("parameters").getJsonObject(5).getJsonObject("preview").getJsonArray("properties").getJsonObject(1).getString("value");
						ChromeUtil.getObjectProperties(webSocket, objectId, new DuelystMessageState(playerId, MessageType.STARTING_HAND));
					}
					// Replace card
					else if (message.contains("ReplaceCardFromHandAction") && message.contains("VIEW")) {
						String objectId = msg.getJsonArray("parameters").getJsonObject(5).getString("objectId");
						String playerId = msg.getJsonArray("parameters").getJsonObject(5).getJsonObject("preview").getJsonArray("properties").getJsonObject(2).getString("value");
						ChromeUtil.getObjectProperties(webSocket, objectId, new DuelystMessageState(playerId, MessageType.CARD_REPLACE));
					}
					// End turn
					else if (message.contains("EndTurnAction") && message.contains("VIEW")) {
						String objectId = msg.getJsonArray("parameters").getJsonObject(5).getString("objectId");
						String playerId = msg.getJsonArray("parameters").getJsonObject(5).getJsonObject("preview").getJsonArray("properties").getJsonObject(1).getString("value");
						ChromeUtil.getObjectProperties(webSocket, objectId, new DuelystMessageState(playerId, MessageType.TURN_END));
					}
					// Play card
					else if (message.contains("PlayCardFromHandAction") && message.contains("VIEW")) {
						String objectId = msg.getJsonArray("parameters").getJsonObject(5).getString("objectId");
						String playerId = msg.getJsonArray("parameters").getJsonObject(5).getJsonObject("preview").getJsonArray("properties").getJsonObject(2).getString("value");
						ChromeUtil.getObjectProperties(webSocket, objectId, new DuelystMessageState(playerId, MessageType.CARD_PLAY));
					}
					// Gauntlet picks
					else if (message.contains("preview")) {
						if (source.contains("cards select")) {
							for (JsonValue parameter : msg.getJsonArray("parameters")) {
								if (parameter.getValueType() == ValueType.OBJECT) {
									if (((JsonObject)parameter).containsKey("preview")) {
										JsonArray properties = ((JsonObject)parameter).getJsonObject("preview").getJsonArray("properties");
										
										// Cards are in reverse order
										int option3Id = Integer.parseInt(properties.getJsonObject(0).getString("value"));
										int option2Id = Integer.parseInt(properties.getJsonObject(1).getString("value"));
										int option1Id = Integer.parseInt(properties.getJsonObject(2).getString("value"));
										
										sendMessage(new GauntletOptionsMessage("",
												DuelystLibrary.cardsById.get(option1Id),
												DuelystLibrary.cardsById.get(option2Id),
												DuelystLibrary.cardsById.get(option3Id)));
									}
								}
							}
						}
					}
				}
			}
		} catch (Exception ex) {
			System.out.println(ex.getMessage());
			ex.printStackTrace();
		}
	}
	
	private JsonObject getResultObject(JsonObject jsonObject, String name) {
		for (JsonValue result : jsonObject.getJsonObject("result").getJsonArray("result")) {
			if (result.getValueType() == ValueType.OBJECT) {
				JsonObject resultObject = (JsonObject)result;
				if (resultObject.containsKey("name") && resultObject.getString("name").equals(name)) {
					return resultObject;
				}
			}
		}
		
		return null;
	}
	
	private void getResultObjectWs(JsonObject jsonObject, String name, DuelystMessageState state) {
		getResultObjectWs(jsonObject, name, state, false);
	}
	
	private void getResultObjectWs(JsonObject jsonObject, String name, DuelystMessageState state, boolean ignoreMissing) {
		JsonObject resultObject = getResultObject(jsonObject, name);
		if (resultObject == null) {
			if (ignoreMissing) {
				return;
			} else {
				throw new NoSuchElementException("Name '" + name + "' not found in results.");
			}
		}
		
		getResultObjectWs(resultObject, state);
	}

	private void getResultObjectWs(JsonObject jsonObject, int index, DuelystMessageState state) {
		JsonObject resultObject = jsonObject.getJsonObject("result").getJsonArray("result").getJsonObject(index);
		getResultObjectWs(resultObject, state);
	}
	
	private void getResultObjectWs(JsonObject jsonObject, DuelystMessageState state) {
		state.stage++;
		String objectId = jsonObject.getJsonObject("value").getString("objectId");
		ChromeUtil.getObjectProperties(webSocket, objectId, state);
	}

	private int getResultInt(JsonObject jsonObject, String name) {
		return getResultObject(jsonObject, name).getJsonObject("value").getInt("value");
	}

	private String getResultString(JsonObject jsonObject, String name) {
		return getResultObject(jsonObject, name).getJsonObject("value").getString("value");
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
