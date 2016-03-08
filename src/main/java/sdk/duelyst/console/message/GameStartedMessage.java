package sdk.duelyst.console.message;

import java.util.ArrayList;
import java.util.List;

import sdk.duelyst.Card;

public class GameStartedMessage extends DuelystMessage {
	public final String playerName;
	public final List<Card> deck = new ArrayList<Card>();
	
	public GameStartedMessage(String playerId, String playerName) {
		super(MessageType.GAME_START, playerId);
		
		this.playerName = playerName;
	}
	
	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder(super.toString());
		for (Card card : deck) {
			builder.append(card.name);
			builder.append(", ");
		}
		
		return builder.toString();
	}
}
