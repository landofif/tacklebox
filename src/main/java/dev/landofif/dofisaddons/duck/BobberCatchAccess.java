package dev.landofif.dofisaddons.duck;

/**
 * Read access to a fishing bobber's synced "a fish is biting" flag ({@code caughtFish}). Reading it off
 * your own {@code player.fishHook} is an owner-correct bite signal: another player fishing next to you
 * can never set it.
 */
public interface BobberCatchAccess {
    boolean dofisaddons$caughtFish();
}
