package api;

import card.Card;
import card.Value;
import deck.Deck;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

public class Engine {
    private Deck deck;

    private List<Card> dealerHand = new ArrayList<>();
    private final List<Card> currentHand = new ArrayList<>();

    public record State(
            @NotNull List<Card> dealer,
            @NotNull List<Card> player,
            @NotNull Status status
    ){}

    public State shuffle() {
        this.deck = new Deck();
        return turn();
    }

    public State turn() {
        currentHand.clear();
        dealerHand.clear();

        currentHand.add(deck.draw());
        currentHand.add(deck.draw());

        dealerHand.add(deck.draw());

        return status(false);
    }

    public State end() {
        while (count(dealerHand) <= 16)
            dealerHand.add(deck.draw());
        return status(true);
    }

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

    public Engine split(){
        Engine res = new Engine();
        res.deck = this.deck;
        final var currentFirst = currentHand.get(0);
        res.currentHand.add(new Card(currentFirst.suit(), currentFirst.value()));
        currentHand.remove(0);
        res.dealerHand = this.dealerHand;
        return res;
    }

    public int getSizeOfDeck() {
        return deck != null ? deck.getSize() : 0;
    }

    private State status(boolean isOver) {
        Status status;

        int dealerPoints = count(dealerHand);
        int playerPoints = count(currentHand);

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

    private int count(List<Card> hand) {
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
}