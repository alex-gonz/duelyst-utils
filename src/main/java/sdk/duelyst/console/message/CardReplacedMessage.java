package sdk.duelyst.console.message;

import sdk.duelyst.Card;

public class CardReplacedMessage extends DuelystMessage {
	public final int replacedIndex;
	public Card card;
	
	public CardReplacedMessage(String playerId, int replacedIndex) {
		super(MessageType.CARD_REPLACE, playerId);

		this.replacedIndex = replacedIndex;
	}
	
	@Override
	public String toString() {
		return super.toString() + replacedIndex + " " + card.name;
	}
}
