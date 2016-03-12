package sdk.duelyst.console.message;

import java.util.ArrayList;
import java.util.List;

import sdk.duelyst.Card;

public class DeckUpdateMessage extends DuelystMessage {
	public final List<Card> deck = new ArrayList<Card>();
	public final int count;
	
	public DeckUpdateMessage(String playerId, int count) {
		super(MessageType.DECK_UPDATE, playerId);
		
		this.count = count;
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
