package io.rekri.blackjackengine.engine;

import io.rekri.blackjackengine.card.Card;
import io.rekri.blackjackengine.card.Value;
import io.rekri.blackjackengine.engine.config.*;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class Engine {
    private Deck deck;

    @NotNull private List<@NotNull Card> dealerHand = new ArrayList<>();
    @Nullable private Card hideCard;
    @NotNull private final List<@NotNull Card> currentHand = new ArrayList<>();
    @NotNull private final Config config;

    public record State(
            @NotNull List<Card> dealer,
            @NotNull List<Card> player,
            @NotNull Status status
    ){}

    public Engine(@NotNull Config config){
        this.config = config;
    }

    @NotNull
    public State shuffle() {
        this.deck = new Deck(config.countOfDecks());
        return turn();
    }

    @NotNull
    public List<Card> getCurrentHand(){ return currentHand; }

    @NotNull
    public State turn() {
        currentHand.clear();
        dealerHand.clear();

        currentHand.add(deck.draw());
        currentHand.add(deck.draw());

        dealerHand.add(deck.draw());
        hideCard = config.hideCardRules() == HideCard.AMERICAN ? deck.draw() : null;

        return status(false);
    }

    @NotNull
    public State end() {
        revealHideCard();

        while (config.dealerStand() == DealerStand.SOFT_17 ? softCount(dealerHand) <= 16 : hardCount(dealerHand) <= 16)
            dealerHand.add(deck.draw());
        return status(true);
    }

    @NotNull
    public State draw() {
        currentHand.add(deck.draw());
        return status(false);
    }

    @NotNull
    public State dealerDraw() {
        if (dealerHand.get(0).value().getValue() != 11 && dealerHand.get(0).value().getValue() != 10)
            return status(false);

        else if (config.hideCardRules()==HideCard.EUROPEAN)
            dealerHand.add(deck.draw());
        else if (hideCard != null)
            revealHideCard();

        return status(false);
    }

    public boolean isSurrenderAvailable() {
        if (config.surrender() == Surrender.NO_SURRENDER)
            return false;

        return currentHand.size() == 2;
    }

    public boolean isSplitAvailable(){
        return currentHand.size() == 2 && currentHand.get(0).value() == currentHand.get(1).value() &&
                !isDealerBlackJack();
    }

    public boolean isDealerBlackJack(){
        if (config.hideCardRules()==HideCard.AMERICAN)
            dealerHand.add(hideCard);
        var res = status(false);
        dealerHand.remove(hideCard);
        return res.status==Status.DEALER_BLACKJACK;
    }

    public boolean isDoubleAvailable(){
        if (config.doubleRules() == DoubleRules.ANY)
            return true;

        if (currentHand.size() < 2)
            return false;

        final var firstCard = currentHand.get(0);
        final var secondCard = currentHand.get(1);
        final var sum = firstCard.value().getValue() + secondCard.value().getValue();

        return config.doubleRules() == DoubleRules.TEN_ELEVEN && (sum == 10 || sum == 11) ||
                config.doubleRules() == DoubleRules.NINE_TEN_ELEVEN && (sum == 10 || sum == 11 || sum == 9);
    }

    @NotNull
    public Engine split(){
        Engine res = new Engine(this.config);
        res.deck = this.deck;
        final var currentFirst = currentHand.get(0);
        res.currentHand.add(new Card(currentFirst.suit(), currentFirst.value(), UUID.randomUUID().toString()));
        currentHand.remove(0);
        res.dealerHand = this.dealerHand;
        res.hideCard = this.hideCard;
        return res;
    }

    public int getSizeOfDeck() {
        return deck != null ? deck.getSize() : 0;
    }

    private void revealHideCard() {
        if (hideCard != null) {
            dealerHand.add(new Card(hideCard.suit(), hideCard.value(), UUID.randomUUID().toString()));
            hideCard = null;
        }
    }

    @NotNull
    private State status(boolean isOver) {
        Status status;

        int playerPoints = softCount(currentHand);

        if (playerPoints==21 && dealerHand.size()==1)
            dealerDraw();

        int dealerPoints = config.dealerStand() == DealerStand.SOFT_17 ? softCount(dealerHand) : hardCount(dealerHand);

        if (playerPoints > 21)
            status = Status.PLAYER_IS_TOO_MUCH;
        else if (playerPoints == 21 && currentHand.size() == 2 && dealerPoints == 21 && dealerHand.size() == 2)
            status = Status.PUSH;
        else if (playerPoints == 21 && currentHand.size() == 2)
            status = Status.PLAYER_BLACKJACK;
        else if (dealerPoints == 21 && currentHand.size() == 2)
            status = Status.DEALER_BLACKJACK;
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