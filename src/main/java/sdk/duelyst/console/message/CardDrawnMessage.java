package sdk.duelyst.console.message;

import sdk.duelyst.Card;

public class CardDrawnMessage extends DuelystMessage {
	public final Card card;
	
	public CardDrawnMessage(String playerId, Card card) {
		super(MessageType.CARD_DRAW, playerId);
		
		this.card = card;
	}
	
	@Override
	public String toString() {
		return super.toString() + card.name;
	}
}
