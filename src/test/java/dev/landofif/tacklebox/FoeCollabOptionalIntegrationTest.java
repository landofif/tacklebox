package dev.landofif.tacklebox;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Tacklebox starts without foecollab: no required mixin targets it, and every mixin that does sits behind the plugin. */
class FoeCollabOptionalIntegrationTest {
    static final Path SRC = Path.of("src/main");

    static JsonObject json(String name) throws IOException {
        return JsonParser.parseString(Files.readString(SRC.resolve("resources").resolve(name))).getAsJsonObject();
    }

    static List<String> sources(JsonObject config) throws IOException {
        List<String> out = new ArrayList<>();
        String pkg = config.get("package").getAsString();
        for (var m : config.getAsJsonArray("client")) {
            out.add(Files.readString(SRC.resolve("java").resolve(pkg.replace('.', '/')).resolve(m.getAsString() + ".java")));
        }
        return out;
    }

    @Test
    void foecollabIsOnlySuggested() throws IOException {
        JsonObject mod = json("fabric.mod.json");
        assertFalse(mod.getAsJsonObject("depends").has("foecollab"));
        assertTrue(mod.getAsJsonObject("suggests").has("foecollab"));
        assertTrue(mod.getAsJsonArray("mixins").toString().contains("tacklebox.foecollab.mixins.json"));
    }

    @Test
    void everyFoecollabMixinIsInTheOptionalConfig() throws IOException {
        for (String src : sources(json("tacklebox.mixins.json"))) {
            assertFalse(src.contains("io.github.foecollab"), src.lines().filter(l -> l.contains("@Mixin")).findFirst().orElse(""));
        }
        JsonObject foe = json("tacklebox.foecollab.mixins.json");
        assertEquals("dev.landofif.tacklebox.compat.FoeMixinPlugin", foe.get("plugin").getAsString());
        assertFalse(foe.get("required").getAsBoolean());
        for (String src : sources(foe)) {
            assertTrue(src.contains("@Mixin(targets = \"io.github.foecollab."));
        }
    }

    @Test
    void hudsMoveInTackleboxsOwnMover() throws IOException {
        assertFalse(json("tacklebox.foecollab.mixins.json").getAsJsonArray("client").toString().contains("MoveHudScreen"));
        try (var files = Files.walk(SRC.resolve("java"))) {
            for (Path f : files.filter(f -> f.toString().endsWith(".java")).toList()) {
                assertFalse(Files.readString(f).contains("screens.movehud.MoveHudScreen"), f.toString());
            }
        }
        String main = Files.readString(SRC.resolve("java/dev/landofif/tacklebox/Tacklebox.java"));
        assertTrue(main.contains("new dev.landofif.tacklebox.hud.HudMover(null)"));
    }
}
