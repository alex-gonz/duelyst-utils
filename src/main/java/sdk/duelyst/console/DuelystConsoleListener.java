package sdk.duelyst.console;

import sdk.duelyst.console.message.DuelystMessage;

public interface DuelystConsoleListener {
	public void onMessage(DuelystMessage message);
}
