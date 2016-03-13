package sdk.duelyst.console.message;

import sdk.duelyst.Faction;

public class JoinedGameMessage extends DuelystMessage {
	public final Faction faction;
	public final int generalId;
	
	public JoinedGameMessage(String playerId, Faction faction, int generalId) {
		super(MessageType.JOINED_GAME, playerId);
		
		this.faction = faction;
		this.generalId = generalId;
	}
	
	@Override
	public String toString() {
		return super.toString() + faction;
	}
}
