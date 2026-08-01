package pl.caltonek.simpleMarry.model;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;
import java.util.UUID;

public record Marriage(@NotNull UUID player1, @NotNull UUID player2) {

    public Marriage {
        Objects.requireNonNull(player1, "player1 cannot be null");
        Objects.requireNonNull(player2, "player2 cannot be null");
    }

    public boolean contains(final @NotNull UUID uuid) {
        return player1.equals(uuid) || player2.equals(uuid);
    }

    public @Nullable UUID getPartner(final @NotNull UUID uuid) {
        return player1.equals(uuid) ? player2 : (player2.equals(uuid) ? player1 : null);
    }
}