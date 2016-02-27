package sdk.duelyst;

public enum Rarity {
	BASIC(0), COMMON(1), RARE(2), EPIC(3), LEGENDARY(4), TOKEN(5);
	
	public final int value;
	
	Rarity(int value) {
		this.value = value;
	}
	
	public static Rarity fromValue(int value) {
		for (Rarity rarity : Rarity.values())
			if (rarity.value == value)
				return rarity;
		
		throw new IllegalArgumentException("No rarity found matching given value:" + value);
	}
}
