package pl.caltonek.simpleMarry.model;

import java.util.UUID;

public record Marriage(UUID player1, UUID player2) {

    public boolean contains(UUID uuid) {
        return player1.equals(uuid) || player2.equals(uuid);
    }

    public UUID getPartner(UUID uuid) {
        return player1.equals(uuid) ? player2 : (player2.equals(uuid) ? player1 : null);
    }
}