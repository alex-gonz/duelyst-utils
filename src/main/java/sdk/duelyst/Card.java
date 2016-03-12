package sdk.duelyst;

public class Card {
	public final int id;
	
	public final String name;
	public final CardType type;
	public final Faction faction;
	public final Rarity rarity;
	
	public final int manaCost;
	public final int attack;
	public final int health;

	public String description;
	
	public Card(int id, String name, CardType type, Faction faction, Rarity rarity, int manaCost, int attack, int health, String description) {
		this.id = id;
		
		this.name = name;
		this.type = type;
		this.faction = faction;
		this.rarity = rarity;
		
		this.manaCost = manaCost;
		this.attack = attack;
		this.health = health;
		
		this.description = description;
	}
	
	public boolean isMinion() {
		switch (type) {
		case ARCANYST:
		case DERVISH:
		case GOLEM:
		case MECH:
		case MINION:
		case STRUCTURE:
		case VESPYR:
			return true;
		default:
			return false;
		}
	}
}
