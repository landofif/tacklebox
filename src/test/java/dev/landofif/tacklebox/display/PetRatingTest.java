package dev.landofif.tacklebox.display;

import dev.landofif.tacklebox.trigger.Trigger;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

/** A pet drop title gets the rating of the pet that turns up new in the inventory. */
class PetRatingTest {
    private final long[] now = {1_000_000};

    @BeforeEach
    void setUp() {
        PetRating.clock = () -> now[0];
        TitleOverlay.clock = () -> now[0];
        PetRating.reset();
        TitleOverlay.clear();
    }

    @AfterEach
    void tearDown() {
        PetRating.clock = System::currentTimeMillis;
        TitleOverlay.clock = System::currentTimeMillis;
        PetRating.reset();
        TitleOverlay.clear();
    }

    private static TitleOverlay.Shown title(String pet) {
        return TitleOverlay.show("builtin:pet-drop", new Trigger.TitleSettings(), "+ " + pet + " pet", "");
    }

    private static PetRating.Item pet(String id, String kind, double rating) {
        return new PetRating.Item(id, kind, rating);
    }

    private static String sub(TitleOverlay.Shown shown) {
        return shown.subtitle().getString();
    }

    @Test
    void wordsFollowTheServersSteps() {
        assertEquals("sickly", PetRating.word(0));
        assertEquals("sickly", PetRating.word(20));
        assertEquals("bad", PetRating.word(20.25));
        assertEquals("below average", PetRating.word(30));
        assertEquals("average", PetRating.word(49.75));
        assertEquals("good", PetRating.word(53), "luna's duck: good [53%]");
        assertEquals("great", PetRating.word(60));
        assertEquals("great", PetRating.word(79.75));
        assertEquals("excellent", PetRating.word(80));
        assertEquals("amazing", PetRating.word(90));
        assertEquals("amazing", PetRating.word(99.75));
        assertEquals("perfect", PetRating.word(100));
    }

    @Test
    void lineIsTheWordAndOneDecimalInTheWordsColour() {
        assertEquals("&#fc54fcamazing 93.4%", PetRating.line(93.4));
        assertEquals("&#54fc54good 53.0%", PetRating.line(53));
        assertEquals("&#a800a8perfect 100.0%", PetRating.line(100));
    }

    @Test
    void petThatArrivesAfterTheLine() {
        PetRating.scan(List.of());
        TitleOverlay.Shown shown = title("lynx");
        PetRating.await(shown, "Lynx");
        PetRating.scan(List.of());
        assertEquals("", sub(shown));
        now[0] += 400;
        PetRating.scan(List.of(pet("a", "lynx", 71.5)));
        assertEquals("great 71.5%", sub(shown));
        assertEquals("+ lynx pet", shown.title().getString(), "the title stays");
    }

    @Test
    void petThatArrivedJustBeforeTheLine() {
        PetRating.scan(List.of());
        now[0] += 50;
        PetRating.scan(List.of(pet("a", "lynx", 71.5)));
        now[0] += 50;
        TitleOverlay.Shown shown = title("lynx");
        PetRating.await(shown, "Lynx");
        PetRating.scan(List.of(pet("a", "lynx", 71.5)));
        assertEquals("great 71.5%", sub(shown));
    }

    @Test
    void petsThatWereThereAlreadyAndOtherKindsAreNotTheDrop() {
        PetRating.scan(List.of(pet("old", "lynx", 10))); // the first look: here since before
        now[0] += 50;
        TitleOverlay.Shown shown = title("lynx");
        PetRating.await(shown, "Lynx");
        PetRating.scan(List.of(pet("old", "lynx", 10), pet("fox", "fox", 99)));
        assertEquals("", sub(shown));
        PetRating.scan(List.of(pet("old", "lynx", 10), pet("fox", "fox", 99), pet("new", "lynx", 45)));
        assertEquals("average 45.0%", sub(shown));
    }

    @Test
    void twoWordNamesMatchTheItemsKind() {
        PetRating.scan(List.of());
        TitleOverlay.Shown shown = title("marsh rabbit");
        PetRating.await(shown, "Marsh Rabbit");
        PetRating.scan(List.of(pet("a", "marshrabbit", 88)));
        assertEquals("excellent 88.0%", sub(shown));
    }

    @Test
    void twoDropsGetTheirOwnPets() {
        PetRating.scan(List.of());
        TitleOverlay.Shown first = title("lynx");
        PetRating.await(first, "Lynx");
        PetRating.scan(List.of(pet("a", "lynx", 25)));
        now[0] += 1000;
        TitleOverlay.Shown second = title("lynx");
        PetRating.await(second, "Lynx");
        PetRating.scan(List.of(pet("a", "lynx", 25)));
        assertEquals("", sub(second), "the first drop's pet is not used twice");
        PetRating.scan(List.of(pet("a", "lynx", 25), pet("b", "lynx", 95)));
        assertEquals("bad 25.0%", sub(first));
        assertEquals("amazing 95.0%", sub(second));
    }

    @Test
    void stopsWaitingAfterTheWindow() {
        PetRating.scan(List.of());
        TitleOverlay.Shown shown = title("lynx");
        PetRating.await(shown, "Lynx");
        now[0] += PetRating.WINDOW_MS + 1;
        PetRating.scan(List.of(pet("a", "lynx", 71.5)));
        assertEquals("", sub(shown), "a full inventory: no pet came, a later lynx is not this drop");
    }
}
