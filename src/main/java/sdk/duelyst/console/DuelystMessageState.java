package sdk.duelyst.console;

import sdk.duelyst.console.message.DuelystMessage;
import sdk.duelyst.console.message.MessageType;

class DuelystMessageState {
	public String playerId;
	public MessageType type;
	public int stage;
	public DuelystMessage message;
	
	public DuelystMessageState(String playerId, MessageType type) {
		this.playerId = playerId;
		this.type = type;
		this.stage = 0;
		this.message = null;
	}
	
	public DuelystMessageState(DuelystMessageState other) {
		this.playerId = other.playerId;
		this.type = other.type;
		this.stage = other.stage;
		this.message = other.message;
	}
	
	public DuelystMessageState(DuelystMessageState other, MessageType type) {
		this(other.playerId, type);
	}
}
