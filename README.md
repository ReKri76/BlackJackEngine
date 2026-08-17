## Interaction Contract

All game interactions are performed through the `API` class. Each player action updates the internal game engine and returns a `Response` record.

### Response Object
```java
public record Response(State state, boolean insuranceIsOffered, Double win, Integer deckSize) {}
```
- `state`: Current state of the game (contains player and dealer hands, and game status).
- `insuranceIsOffered`: `true` if the dealer's visible io.rekri.blackjackengine.card is an Ace.
- `win`: The profit or loss amount if the game is over. Returns `null` if the round is still ongoing.
- `deckSize`: The number of cards remaining in the io.rekri.blackjackengine.deck. 

### Core Actions

- `newGame(double bet)`: Starts a new round. Automatically shuffles the io.rekri.blackjackengine.deck if cards are running low. Returns the initial state or instantly resolves if the player hits Blackjack.
- `hit()`: Draws a io.rekri.blackjackengine.card for the player. Can result in a bust.
- `stand()`: Ends the player's turn, executes the dealer's turn, and calculates final winnings (including insurance).
- `doubleBet()`: Doubles the initial bet, draws exactly one io.rekri.blackjackengine.card, and automatically stands (unless busted).
- `split()`: Splits the current hand. Returns a **new instance** of the `API` class specifically for the split hand.
- `surrender()`: Gives up the current hand in exchange for half of the bet. Only available on the initial hand.
- `makeInsurance()`: Places an insurance bet (half of the original bet). Can only be called immediately after `newGame` if `insuranceIsOffered` is true.

### Rules & Exceptions
- The io.rekri.blackjackengine.API enforces game rules: calling actions out of turn or when the game is over throws an `IllegalStateException`.
- Negative bets throw an `IllegalArgumentException`.
