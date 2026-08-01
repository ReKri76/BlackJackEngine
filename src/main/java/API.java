import api.Engine;
import api.Status;
import api.Engine.State;
import card.Card;
import card.Value;

import java.util.List;

public class API {
    private final Engine engine;
    State currentState;
    private double currentBet;
    private double insuranceBet = 0.0;
    private static final int MIN_SIZE_OF_DECK = 52 * 2;
    private boolean insuranceIsOffered = false;
    private boolean isGameOver = false;

    public record Response(
            State state,
            boolean insuranceIsOffered,
            Double win,
            Integer deckSize
    ) {}

    public API() {
        engine = new Engine();
    }

    API(Engine engine){
        this.engine = engine;
    }

    public Response newGame(double bet) {
        if (bet <= 0)
            throw new IllegalArgumentException("Bet must be positive");

        this.currentBet = bet;
        this.insuranceBet = 0.0;
        this.isGameOver = false;
        this.insuranceIsOffered = false;

        currentState = engine.getSizeOfDeck() < MIN_SIZE_OF_DECK ? engine.shuffle() : engine.turn();

        if (currentState.status().equals(Status.PLAYER_BLACKJACK)) {
            isGameOver = true;
            return new Response(currentState, false, currentBet * 1.5, engine.getSizeOfDeck());
        }

        if (currentState.dealer().getFirst().value().equals(Value.ACE)) {
            insuranceIsOffered = true;
            return new Response(currentState, true, null, engine.getSizeOfDeck());
        }

        return new Response(currentState, false, null, engine.getSizeOfDeck());
    }

    public Response hit() {
        checkNotGameOver();
        insuranceIsOffered = false;

        currentState = engine.draw();

        if (currentState.status().equals(Status.PLAYER_IS_TOO_MUCH)) {
            isGameOver = true;
            return new Response(currentState, false, -currentBet - insuranceBet, engine.getSizeOfDeck());
        }

        return new Response(currentState, false, null, engine.getSizeOfDeck());
    }

    public Response stand() {
        checkNotGameOver();
        insuranceIsOffered = false;
        isGameOver = true;

        currentState = engine.end();

        boolean dealerHasBlackjack = isDealerBlackjack(currentState);
        double insuranceProfit = insuranceBet > 0
                ? (dealerHasBlackjack ? insuranceBet * 2.0 : -insuranceBet)
                : 0.0;

        Status status = currentState.status();
        double mainBetProfit;

        if (status.equals(Status.LOSE) || status.equals(Status.PLAYER_IS_TOO_MUCH))
            mainBetProfit = -currentBet;
        else if (status.equals(Status.PUSH))
            mainBetProfit = 0.0;
        else
            mainBetProfit = currentBet;


        return new Response(currentState, false, mainBetProfit + insuranceProfit, engine.getSizeOfDeck());
    }

    public Response doubleBet() {
        checkNotGameOver();
        insuranceIsOffered = false;

        currentBet *= 2;
        currentState = engine.draw();

        if (currentState.status().equals(Status.PLAYER_IS_TOO_MUCH)) {
            isGameOver = true;
            return new Response(currentState, false, -currentBet - insuranceBet, engine.getSizeOfDeck());
        }

        return this.stand();
    }

    public Response surrender() {
        checkNotGameOver();

        if (!engine.isSurrenderAvailable())
            throw new IllegalStateException("Surrender is only available on the initial hand.");

        isGameOver = true;
        insuranceIsOffered = false;
        return new Response(currentState, false, -currentBet / 2.0, engine.getSizeOfDeck());
    }

    public API split(){
        checkNotGameOver();

        if (!engine.isSplitAvailable())
            throw new IllegalStateException("Split is only available on the initial hand.");

        var newEngine = engine.split();
        var newAPI = new API(newEngine);
        newAPI.insuranceBet = this.insuranceBet;
        newAPI.currentBet = this.currentBet;

        return newAPI;
    }

    public Response makeInsurance() {
        checkNotGameOver();

        if (!insuranceIsOffered)
            throw new IllegalStateException("Insurance is not offered now");

        insuranceIsOffered = false;
        insuranceBet = currentBet / 2.0;

        return new Response(currentState, false, null, engine.getSizeOfDeck());
    }

    private void checkNotGameOver() {
        if (isGameOver)
            throw new IllegalStateException("Game is already over");
    }

    private boolean isDealerBlackjack(State state) {
        List<Card> dealerHand = state.dealer();
        if (dealerHand.size() != 2) return false;

        int count = 0;
        int aces = 0;
        for (Card card : dealerHand) {
            count += card.value().getValue();
            if (card.value() == Value.ACE)
                aces++;
        }
        while (count > 21 && aces > 0) {
            count -= 10;
            aces--;
        }
        return count == 21;
    }
}