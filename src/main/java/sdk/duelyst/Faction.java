package sdk.duelyst;

public enum Faction {
	LYONAR(1), SONGHAI(2), VETRUVIAN(3), ABYSSIAN(4), MAGMAR(5), VANAR(6), NEUTRAL(100), TUTORIAL(200);
	
	public final int value;
	
	Faction(int value) {
		this.value = value;
	}
	
	public static Faction fromValue(int value) {
		for (Faction faction : Faction.values())
			if (faction.value == value)
				return faction;
		
		throw new IllegalArgumentException("No faction found matching given value:" + value);
	}
}
