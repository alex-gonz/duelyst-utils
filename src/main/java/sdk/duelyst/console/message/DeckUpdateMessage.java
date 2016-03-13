package sdk.duelyst.console.message;

import java.util.ArrayList;
import java.util.List;

import sdk.duelyst.Card;

public class DeckUpdateMessage extends DuelystMessage {
	public final String playerName;
	public final List<Card> deck = new ArrayList<Card>();
	public int count;
	
	public DeckUpdateMessage(String playerId, String playerName) {
		super(MessageType.DECK_UPDATE, playerId);
		
		this.playerName = playerName;
	}
	
	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder(super.toString() + playerName + ": ");
		for (Card card : deck) {
			builder.append(card.name);
			builder.append(", ");
		}
		
		return builder.toString();
	}
}
