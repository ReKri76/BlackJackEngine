package io.rekri.blackjackengine.engine;

import io.rekri.blackjackengine.card.Card;
import io.rekri.blackjackengine.card.Suit;
import io.rekri.blackjackengine.card.Value;

import java.util.ArrayList;
import java.util.Collections;
import java.util.UUID;

public class Deck {
    final private ArrayList<Card> cards;

    public Deck(int numberOfDecks){
        cards = new ArrayList<>(52* numberOfDecks);

        for (int i = 1; i<= numberOfDecks; i++)
            for (var value : Value.values())
                for (var suit : Suit.values())
                    cards.add(new Card(suit, value, UUID.randomUUID().toString()));
        Collections.shuffle(cards);
    }

    public int getSize(){
        return cards.size();
    }

    public Card draw(){
        var res = cards.get(0);
        cards.remove(0);
        return res;
    }
}
