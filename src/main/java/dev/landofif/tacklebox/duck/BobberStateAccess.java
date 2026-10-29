package dev.landofif.tacklebox.duck;

/** Lets the bobber render state remember whether the bobber is yours. */
public interface BobberStateAccess {
    boolean tacklebox$isMine();

    void tacklebox$setMine(boolean mine);
}
