package sdk.duelyst.console;

public abstract class DuelystMessage {
	public final MessageType type;
	
	public DuelystMessage(MessageType type) {
		this.type = type;
	}
}
