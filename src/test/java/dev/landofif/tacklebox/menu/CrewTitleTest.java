package dev.landofif.tacklebox.menu;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

/** The collections overview, location leveling and the crew menu share the title 핒; their items tell them apart. */
class CrewTitleTest {
    @Test
    void collectionItemsMakeItTheCollectionsOverview() {
        assertEquals("collections", FishMenu.crewTitleMenu(List.of("basses collection", "billfishes collection", "return", "close")));
        assertEquals("collections", FishMenu.crewTitleMenu(List.of("bread", "whitefishes collection")));
    }

    @Test
    void locationLevelingData() {
        assertEquals("location leveling", FishMenu.crewTitleMenu(List.of("location leveling data", "level 1", "level 2", "leveling bonuses")));
    }

    @Test
    void anythingElseIsTheCrew() {
        assertEquals("crew", FishMenu.crewTitleMenu(List.of("empty crew display", "crew info", "crew leveling", "meowfih (online)")));
        assertEquals("crew", FishMenu.crewTitleMenu(List.of("collections"))); // the button on other pages, not a collection
        assertEquals("crew", FishMenu.crewTitleMenu(List.of()));
    }
}
