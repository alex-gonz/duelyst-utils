package sdk.duelyst.console.message;


public abstract class DuelystMessage {
	public final MessageType type;
	public final String playerId;
	
	public DuelystMessage(MessageType type, String playerId) {
		this.type = type;
		this.playerId = playerId;
	}
	
	@Override
	public String toString() {
		return playerId + " " + type + ": ";
	}
}
