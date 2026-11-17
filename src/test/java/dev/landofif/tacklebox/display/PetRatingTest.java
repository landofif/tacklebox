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
        return TitleOverlay.show("builtin:pet-drop", new Trigger.TitleSettings(), "+ " + pet + " Pet", "");
    }

    private static PetRating.Item pet(String id, String kind, double rating) {
        return new PetRating.Item(id, kind, rating);
    }

    private static String text(TitleOverlay.Shown shown) {
        return shown.title().getString();
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
    void percentHasOneDecimalInTheRatingsColourAndIsBold() {
        assertEquals("&#fc54fc&l 93.4%", PetRating.percent(93.4));
        assertEquals("&#54fc54&l 53.0%", PetRating.percent(53));
        assertEquals("&#a800a8&l 100.0%", PetRating.percent(100));
    }

    @Test
    void petThatArrivesAfterTheLine() {
        PetRating.scan(List.of());
        TitleOverlay.Shown shown = title("Lynx");
        PetRating.await(shown, "Lynx");
        PetRating.scan(List.of());
        assertEquals("+ Lynx Pet", text(shown));
        now[0] += 400;
        PetRating.scan(List.of(pet("a", "lynx", 71.5)));
        assertEquals("+ Lynx Pet 71.5%", text(shown));
        assertEquals("", shown.subtitle().getString(), "no subtitle");
        PetRating.scan(List.of(pet("a", "lynx", 71.5)));
        assertEquals("+ Lynx Pet 71.5%", text(shown), "added once");
    }

    @Test
    void petThatArrivedJustBeforeTheLine() {
        PetRating.scan(List.of());
        now[0] += 50;
        PetRating.scan(List.of(pet("a", "lynx", 71.5)));
        now[0] += 50;
        TitleOverlay.Shown shown = title("Lynx");
        PetRating.await(shown, "Lynx");
        PetRating.scan(List.of(pet("a", "lynx", 71.5)));
        assertEquals("+ Lynx Pet 71.5%", text(shown));
    }

    @Test
    void petsThatWereThereAlreadyAndOtherKindsAreNotTheDrop() {
        PetRating.scan(List.of(pet("old", "lynx", 10))); // the first look: here since before
        now[0] += 50;
        TitleOverlay.Shown shown = title("Lynx");
        PetRating.await(shown, "Lynx");
        PetRating.scan(List.of(pet("old", "lynx", 10), pet("fox", "fox", 99)));
        assertEquals("+ Lynx Pet", text(shown));
        PetRating.scan(List.of(pet("old", "lynx", 10), pet("fox", "fox", 99), pet("new", "lynx", 45)));
        assertEquals("+ Lynx Pet 45.0%", text(shown));
    }

    @Test
    void twoWordNamesMatchTheItemsKind() {
        PetRating.scan(List.of());
        TitleOverlay.Shown shown = title("Marsh Rabbit");
        PetRating.await(shown, "Marsh Rabbit");
        PetRating.scan(List.of(pet("a", "marshrabbit", 88)));
        assertEquals("+ Marsh Rabbit Pet 88.0%", text(shown));
    }

    @Test
    void twoDropsGetTheirOwnPets() {
        PetRating.scan(List.of());
        TitleOverlay.Shown first = title("Lynx");
        PetRating.await(first, "Lynx");
        PetRating.scan(List.of(pet("a", "lynx", 25)));
        now[0] += 1000;
        TitleOverlay.Shown second = title("Lynx");
        PetRating.await(second, "Lynx");
        PetRating.scan(List.of(pet("a", "lynx", 25)));
        assertEquals("+ Lynx Pet", text(second), "the first drop's pet is not used twice");
        PetRating.scan(List.of(pet("a", "lynx", 25), pet("b", "lynx", 95)));
        assertEquals("+ Lynx Pet 25.0%", text(first));
        assertEquals("+ Lynx Pet 95.0%", text(second));
    }

    @Test
    void stopsWaitingAfterTheWindow() {
        PetRating.scan(List.of());
        TitleOverlay.Shown shown = title("Lynx");
        PetRating.await(shown, "Lynx");
        now[0] += PetRating.WINDOW_MS + 1;
        PetRating.scan(List.of(pet("a", "lynx", 71.5)));
        assertEquals("+ Lynx Pet", text(shown), "a full inventory: no pet came, a later lynx is not this drop");
    }
}
