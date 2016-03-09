package sdk.duelyst.console.message;

import sdk.duelyst.Card;

public class CardPlayedMessage extends DuelystMessage {
	public final int playedIndex;
	public Card card;
	
	public CardPlayedMessage(String playerId, int playedIndex) {
		super(MessageType.CARD_PLAY, playerId);
		
		this.playedIndex = playedIndex;
	}
	
	@Override
	public String toString() {
		return super.toString() + playedIndex + " " + card.name;
	}
}
