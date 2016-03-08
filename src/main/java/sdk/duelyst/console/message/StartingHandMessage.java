package sdk.duelyst.console.message;

import java.util.ArrayList;
import java.util.List;

import sdk.duelyst.Card;

public class StartingHandMessage extends DuelystMessage {
	public final List<Card> hand = new ArrayList<Card>();
	
	public StartingHandMessage(String playerId) {
		super(MessageType.STARTING_HAND, playerId);
	}
	
	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder(super.toString());
		for (Card card : hand) {
			builder.append(card.name);
			builder.append(", ");
		}
		
		return builder.toString();
	}
}
