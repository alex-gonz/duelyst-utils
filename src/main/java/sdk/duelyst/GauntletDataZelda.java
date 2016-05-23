package sdk.duelyst;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

// TODO handle faction-specific neutral minion ratings
public class GauntletDataZelda {
  public static String DEFAULT_URL_STRING = "https://docs.google.com/document/export?format=txt&id=1r3tX0myAjXHo-EzGmQ2v3E-P2fLCB-8lcCdKkRzeQq0";

  public static Map<Faction, Map<Integer, Collection<Rating>>> load(Collection<Card> cards) throws IOException {
    try {
      URL defaultUrl = new URL(DEFAULT_URL_STRING);
      return load(cards, defaultUrl);
    } catch (MalformedURLException e) {
      e.printStackTrace();
      throw new RuntimeException("Malformed default url: " + DEFAULT_URL_STRING);
    }
  }

  public static Map<Faction, Map<Integer, Collection<Rating>>> load(Collection<Card> cards, URL url) throws IOException {
    Map<String, Card> nameToCard = generateCardNameMap(cards);
    Collection<Card> generalCards = cards.stream()
            .filter(card -> card.type == CardType.GENERAL)
            .map(card -> card)
            .collect(Collectors.toList());
    generalCards.add(new Card(1, "Neutral Minions", CardType.GENERAL, Faction.NEUTRAL, Rarity.BASIC, 0, 0, 0, "Fake neutral general card"));

    Scanner s = new Scanner(url.openStream());
    Faction maybeCurrentFaction = null;

    Map<Faction, Map<Integer, Collection<Rating>>> factionToCardIdToRatings = new HashMap<>();
    while (s.hasNextLine()) {
      String line = s.nextLine();

      for (Card general: generalCards) {
        if (line.toLowerCase().startsWith(general.name.toLowerCase())) {
          maybeCurrentFaction = general.faction;
          factionToCardIdToRatings.put(general.faction, new HashMap<>());
          break;
        }
      }
      if (maybeCurrentFaction != null) {
        int openBracketIndex = line.indexOf('(');
        int closeBracketIndex = line.indexOf(')');

        if (openBracketIndex != -1 && closeBracketIndex != -1) {
          Optional<String> maybeGeneralPrefix = Optional.of(line.indexOf(':'))  // Other general lines separate name with a colon
                  .filter(idx -> idx != -1 && idx < openBracketIndex)
                  .map(idx -> line.substring(0, idx));
          String maybeCardName = line.substring(maybeGeneralPrefix.map(String::length).orElse(0), openBracketIndex); // Eg. "Aspect of the Drake "
          Card maybeCard = nameToCard.get(cleanCardName(maybeCardName));
          if (maybeCard != null) {
            try {
              int score = Integer.parseInt(line.substring(openBracketIndex, closeBracketIndex).replaceAll("[^0-9]", ""));
              String note = maybeGeneralPrefix.map(p -> p + ": ").orElse("") + line.substring(closeBracketIndex + 4);

              Map<Integer, Collection<Rating>> currentFactionMap = factionToCardIdToRatings.get(maybeCurrentFaction);
              Optional<Collection<Rating>> existingRatings = Optional.ofNullable(currentFactionMap.get(maybeCard.id));
              Collection<Rating> ratingsList = existingRatings.orElse(new ArrayList<>());
              ratingsList.add(new Rating(score, note));
              currentFactionMap.put(maybeCard.id, ratingsList);
            } catch (NumberFormatException e) {
              e.printStackTrace();
              throw e;
            }
          } else {
            System.out.println("Line wasn't matched but had brackets: \"" + line + "\"");
          }
        }
      }
    }

    // add neutral cards to all
    Optional.ofNullable(factionToCardIdToRatings.get(Faction.NEUTRAL)).ifPresent(neutralMap -> {
      factionToCardIdToRatings.entrySet().stream().filter(f -> f.getKey() != Faction.NEUTRAL).forEach(playerFactionToMap -> {
        playerFactionToMap.getValue().putAll(neutralMap);
      });
    });

    return factionToCardIdToRatings;
  }

  private static String cleanCardName(String cardName) {
    return cardName.trim().toLowerCase()
            .replaceAll("[^A-z0-9]", "")
            .replace("obelisk", "obelysk")
            .replace("maelstorm", "maelstrom")
            .replace("artic", "arctic")
            .replace("judgment", "judgement")
            .replace("harvestor", "harvester")
            .replace("draguar", "draugar")
            .replace("sorceror", "sorcerer");
  }

  private static Map<String, Card> generateCardNameMap(Collection<Card> cards) {
    return cards.stream().collect(Collectors.toMap(card -> cleanCardName(card.name), Function.identity()));
  }
}
