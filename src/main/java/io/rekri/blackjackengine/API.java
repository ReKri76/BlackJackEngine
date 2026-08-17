package io.rekri.blackjackengine;

import io.rekri.blackjackengine.engine.Engine;
import io.rekri.blackjackengine.engine.Status;
import io.rekri.blackjackengine.engine.Engine.State;
import io.rekri.blackjackengine.card.Card;
import io.rekri.blackjackengine.card.Value;
import io.rekri.blackjackengine.engine.config.*;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class API {
    private final Engine engine;
    State currentState;
    private double currentBet;
    private double insuranceBet = 0.0;
    private final int minSizeOfDeck;
    private boolean insuranceIsOffered = false;
    private boolean isGameOver = false;
    private final Config config;
    private boolean isSplitWasIngThisRound = false;

    public record Response(
            @NotNull State state,
            @NotNull Boolean insuranceIsOffered,
            @Nullable Double win,
            @NotNull Integer deckSize
    ) {}

    public API(@NotNull Config config) {
        this.config=config;
        engine = new Engine(this.config);
        minSizeOfDeck = 52 * this.config.countOfDecks()/3;
    }

    public API() {
        this.config= new Config(8,
                DealerStand.SOFT_17,
                Surrender.EARLY_SURRENDER,
                true,
                HideCard.EUROPEAN,
                SplitRules.ANY,
                BlackJackRules.THREE_TO_TWO
        );
        engine = new Engine(this.config);
        minSizeOfDeck = 52 * this.config.countOfDecks()/3;
    }

    API(Engine engine, Config config) {
        this.engine = engine;
        this.config=config;
        minSizeOfDeck = 52 * this.config.countOfDecks()/3;
    }

    public API(@NotNull API api){
        this.currentBet=api.currentBet;
        this.currentState=api.currentState;
        this.engine=api.engine;
        this.isGameOver=api.isGameOver;
        this.insuranceBet= api.insuranceBet;
        this.insuranceIsOffered= api.insuranceIsOffered;
        this.config=api.config;
        this.minSizeOfDeck=api.minSizeOfDeck;
        this.isSplitWasIngThisRound=api.isSplitWasIngThisRound;
    }

    @NotNull
    public Response newGame(double bet) {
        if (bet <= 0)
            throw new IllegalArgumentException("Bet must be positive");

        this.currentBet = bet;
        this.insuranceBet = 0.0;
        this.isGameOver = false;
        this.insuranceIsOffered = false;
        this.isSplitWasIngThisRound = false;

        currentState = engine.getSizeOfDeck() < minSizeOfDeck ? engine.shuffle() : engine.turn();

        if (currentState.status().equals(Status.PLAYER_BLACKJACK)) {
            isGameOver = true;
            return new Response(currentState, false,
                    currentBet * (config.blackJackRules() == BlackJackRules.THREE_TO_TWO ? 1.5 : 1.2),
                    engine.getSizeOfDeck());
        }

        if (currentState.dealer().get(0).value().equals(Value.ACE)) {
            insuranceIsOffered = true;
            return new Response(currentState, true, null, engine.getSizeOfDeck());
        }

        return new Response(currentState, false, null, engine.getSizeOfDeck());
    }

    @NotNull
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

    @NotNull
    public Response stand() {
        checkNotGameOver();
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

    @NotNull
    public Response doubleBet() {
        checkNotGameOver();
        if (isSplitWasIngThisRound && !config.isDaS())
            throw new IllegalStateException("By current rules double after split is not available.");

        currentBet *= 2;
        currentState = engine.draw();

        if (currentState.status().equals(Status.PLAYER_IS_TOO_MUCH)) {
            isGameOver = true;
            return new Response(currentState, false, -currentBet - insuranceBet, engine.getSizeOfDeck());
        }

        return this.stand();
    }

    @NotNull
    public Response surrender() {
        checkNotGameOver();

        if (!engine.isSurrenderAvailable())
            throw new IllegalStateException("Surrender is only available on the initial hand.");

        isGameOver = true;
        var resState = new State(currentState.dealer(), currentState.player(), Status.LOSE);
        return new Response(resState, false, -currentBet / 2.0, engine.getSizeOfDeck());
    }

    @NotNull
    public API split(){
        checkNotGameOver();

        if (!engine.isSplitAvailable())
            throw new IllegalStateException("Split is only available on the initial hand.");

        isSplitWasIngThisRound = true;

        var newEngine = engine.split();
        var newAPI = new API(newEngine, this.config);
        newAPI.insuranceBet = this.insuranceBet;
        newAPI.currentBet = this.currentBet;
        newAPI.currentState=this.currentState;
        newAPI.isSplitWasIngThisRound=this.isSplitWasIngThisRound;

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

    public Response getCurrentResponse(){
        State state = new State(currentState.dealer(), engine.getCurrentHand(), currentState.status());
        return new Response(state, insuranceIsOffered, null, engine.getSizeOfDeck());
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