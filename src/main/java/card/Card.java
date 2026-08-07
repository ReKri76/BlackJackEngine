package card;

public record Card (
    Suit suit,
    Value value,
    String uuid
) {}
