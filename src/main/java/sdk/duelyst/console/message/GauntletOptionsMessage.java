package sdk.duelyst.console.message;

import sdk.duelyst.Card;

public class GauntletOptionsMessage extends DuelystMessage {
	public final Card option1;
	public final Card option2;
	public final Card option3;
	
	public GauntletOptionsMessage(Card option1, Card option2, Card option3) {
		super(MessageType.GAUNTLET_OPTIONS, "");
		
		this.option1 = option1;
		this.option2 = option2;
		this.option3 = option3;
	}
	
	@Override
	public String toString() {
		return type + ": " + option1.name + ", " + option2.name + ", " + option3.name;
	}
}
