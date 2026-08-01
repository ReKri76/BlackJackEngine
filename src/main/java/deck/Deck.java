package deck;

import card.Card;
import card.Suit;
import card.Value;

import java.util.ArrayList;
import java.util.Collections;

public class Deck {
    final private ArrayList<Card> cards = new ArrayList<>(52*6);

    public Deck(){
        for (int i = 1; i<=6; i++)
            for (var value : Value.values())
                for (var suit : Suit.values())
                    cards.add(new Card(suit, value));
        Collections.shuffle(cards);
    }

    public int getSize(){
        return cards.size();
    }

    public Card draw(){
        var res = cards.getFirst();
        cards.removeFirst();
        return res;
    }
}
