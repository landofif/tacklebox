package dev.landofif.dofisaddons.mixin;

/** Lets the bobber render state remember whether the bobber is yours. */
public interface BobberStateAccess {
    boolean dofisaddons$isMine();

    void dofisaddons$setMine(boolean mine);
}
