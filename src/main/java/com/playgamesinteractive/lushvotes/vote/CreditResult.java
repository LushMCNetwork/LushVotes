package com.playgamesinteractive.lushvotes.vote;

import java.util.UUID;

/**
 * @param reason null when {@code credited} is true; one of "duplicate" or
 *               "uuid-resolution-failed" otherwise.
 */
public record CreditResult(boolean credited, UUID uuid, String reason) {

    public static CreditResult credited(UUID uuid) {
        return new CreditResult(true, uuid, null);
    }

    public static CreditResult rejected(UUID uuid, String reason) {
        return new CreditResult(false, uuid, reason);
    }
}
