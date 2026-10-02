package dev.landofif.dofisaddons.duck;

/** Lets the bobber render state remember whether the bobber is yours. */
public interface BobberStateAccess {
    boolean dofisaddons$isMine();

    void dofisaddons$setMine(boolean mine);
}
