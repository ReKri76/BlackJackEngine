package io.rekri.blackjackengine.engine;

import io.rekri.blackjackengine.card.Card;
import io.rekri.blackjackengine.card.Value;
import io.rekri.blackjackengine.engine.config.Config;
import io.rekri.blackjackengine.engine.config.DealerStand;
import io.rekri.blackjackengine.engine.config.DoubleRules;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

public class Engine {
    private Deck deck;

    @NotNull private List<Card> dealerHand = new ArrayList<>();
    @NotNull private final List<Card> currentHand = new ArrayList<>();
    @NotNull private final Config config;

    public record State(
            @NotNull List<Card> dealer,
            @NotNull List<Card> player,
            @NotNull Status status
    ){}

    public Engine(@NotNull Config config){
        this.config=config;
    }

    @NotNull
    public State shuffle() {
        this.deck = new Deck(config.countOfDecks());
        return turn();
    }

    @NotNull
    public List<Card> getCurrentHand(){return currentHand;}

    @NotNull
    public State turn() {
        currentHand.clear();
        dealerHand.clear();

        currentHand.add(deck.draw());
        currentHand.add(deck.draw());

        dealerHand.add(deck.draw());

        return status(false);
    }

    @NotNull
    public State end() {
        while (config.dealerStand() == DealerStand.SOFT_17 ? softCount(dealerHand) <= 16 : hardCount(dealerHand) <= 16)
            dealerHand.add(deck.draw());
        return status(true);
    }

    @NotNull
    public State draw() {
        currentHand.add(deck.draw());
        return status(false);
    }

    public boolean isSurrenderAvailable() {
        return currentHand.size() == 2;
    }

    public boolean isSplitAvailable(){
        return isSurrenderAvailable() && currentHand.get(0).value() == currentHand.get(1).value();
    }

    public boolean isDoubleAvailable(){
        if (config.doubleRules()== DoubleRules.ANY)
            return true;

        final var firstCard = currentHand.get(0);
        final var secondCard = currentHand.get(1);
        final var sum = firstCard.value().getValue() + secondCard.value().getValue();

        return config.doubleRules() == DoubleRules.TEN_ELEVEN && (sum == 10 || sum == 11) ||
                config.doubleRules() == DoubleRules.NINE_TEN_ELEVEN && (sum == 10 || sum == 11 || sum ==9);
    }

    @NotNull
    public Engine split(){
        Engine res = new Engine(this.config);
        res.deck = this.deck;
        final var currentFirst = currentHand.get(0);
        res.currentHand.add(new Card(currentFirst.suit(), currentFirst.value(), currentFirst.uuid()));
        currentHand.remove(0);
        res.dealerHand = this.dealerHand;
        return res;
    }

    public int getSizeOfDeck() {
        return deck != null ? deck.getSize() : 0;
    }

    @NotNull
    private State status(boolean isOver) {
        Status status;

        int dealerPoints = config.dealerStand() == DealerStand.SOFT_17 ? softCount(dealerHand) : hardCount(dealerHand);
        int playerPoints = softCount(currentHand);

        if (playerPoints > 21)
            status = Status.PLAYER_IS_TOO_MUCH;
        else if (playerPoints == 21 && currentHand.size() == 2)
            status = Status.PLAYER_BLACKJACK;
        else if (dealerPoints > 21)
            status = Status.DEALER_IS_TOO_MUCH;
        else if (isOver)
            if (dealerPoints > playerPoints)
                status = Status.LOSE;
            else if (dealerPoints < playerPoints)
                status = Status.WIN;
            else
                status = Status.PUSH;
        else
            status = Status.CONTINUE;

        return new State(List.copyOf(dealerHand), List.copyOf(currentHand), status);
    }

    private int softCount(List<Card> hand) {
        int count = 0;
        int aces = 0;

        for (Card card : hand) {
            count += card.value().getValue();
            if (card.value() == Value.ACE)
                aces++;
        }

        while (count > 21 && aces > 0) {
            count -= 10;
            aces--;
        }

        return count;
    }

    private int hardCount(List<Card> hand){
        int count = 0;

        for (Card card : hand)
            count += card.value().getValue();

        return count;
    }
}