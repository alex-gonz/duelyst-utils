package sdk.duelyst.console.message;

import sdk.duelyst.Card;

public class CardPlayedMessage extends DuelystMessage {
	public final Card card;
	
	public CardPlayedMessage(String playerId, Card card) {
		super(MessageType.CARD_PLAY, playerId);
		
		this.card = card;
	}
	
	@Override
	public String toString() {
		return super.toString() + card.name;
	}
}
