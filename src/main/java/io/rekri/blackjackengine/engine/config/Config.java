package io.rekri.blackjackengine.engine.config;

import org.jetbrains.annotations.NotNull;

public record Config (
        @NotNull Integer countOfDecks,
        @NotNull DealerStand dealerStand,
        @NotNull Surrender surrender,
        @NotNull Boolean isDaS,
        @NotNull HideCard hideCardRules,
        @NotNull SplitRules splitRules,
        @NotNull BlackJackRules blackJackRules
        )
{}
