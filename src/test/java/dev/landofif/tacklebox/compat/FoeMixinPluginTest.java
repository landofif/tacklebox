package dev.landofif.tacklebox.compat;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FoeMixinPluginTest {
    @Test
    void foecollabMixinsOnlyWithFoecollab() {
        assertTrue(FoeMixinPlugin.applies(true));
        assertFalse(FoeMixinPlugin.applies(false));
    }

    @Test
    void noLoaderMeansNoFoecollab() {
        assertFalse(FoeCollab.loaded()); // unit tests run without fabric loader or foecollab
    }
}
