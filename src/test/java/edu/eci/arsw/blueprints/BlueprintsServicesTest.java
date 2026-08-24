package edu.eci.arsw.blueprints;

import edu.eci.arsw.blueprints.filters.RedundancyFilter;
import edu.eci.arsw.blueprints.filters.UndersamplingFilter;
import edu.eci.arsw.blueprints.model.Blueprint;
import edu.eci.arsw.blueprints.model.Point;
import edu.eci.arsw.blueprints.persistence.BlueprintNotFoundException;
import edu.eci.arsw.blueprints.persistence.BlueprintPersistenceException;
import edu.eci.arsw.blueprints.persistence.InMemoryBlueprintPersistence;
import edu.eci.arsw.blueprints.services.BlueprintsServices;
import edu.eci.arsw.blueprints.filters.IdentityFilter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class BlueprintsServicesTest {

    private BlueprintsServices services;
    private InMemoryBlueprintPersistence persistence;

    @BeforeEach
    void setUp() {
        persistence = new InMemoryBlueprintPersistence();
        services = new BlueprintsServices(persistence, new IdentityFilter());
    }

    @Test
    void getAllBlueprintsReturnsPreloadedData() {
        assertFalse(services.getAllBlueprints().isEmpty());
    }

    @Test
    void getBlueprintsByAuthorReturnsCorrectSet() throws BlueprintNotFoundException {
        var bps = services.getBlueprintsByAuthor("john");
        assertTrue(bps.stream().allMatch(b -> b.getAuthor().equals("john")));
    }

    @Test
    void getBlueprintsByUnknownAuthorThrows() {
        assertThrows(BlueprintNotFoundException.class, () -> services.getBlueprintsByAuthor("nobody"));
    }

    @Test
    void addNewBlueprintAndRetrieveIt() throws Exception {
        Blueprint bp = new Blueprint("alice", "tower", List.of(new Point(1, 2)));
        services.addNewBlueprint(bp);
        Blueprint found = services.getBlueprint("alice", "tower");
        assertEquals("alice", found.getAuthor());
        assertEquals("tower", found.getName());
    }

    @Test
    void addDuplicateBlueprintThrows() throws Exception {
        Blueprint bp = new Blueprint("bob", "shed", List.of(new Point(0, 0)));
        services.addNewBlueprint(bp);
        assertThrows(BlueprintPersistenceException.class, () -> services.addNewBlueprint(bp));
    }

    @Test
    void addPointIncreasesPointCount() throws Exception {
        int before = services.getBlueprint("john", "house").getPoints().size();
        services.addPoint("john", "house", 99, 99);
        int after = persistence.getBlueprint("john", "house").getPoints().size();
        assertEquals(before + 1, after);
    }

    @Test
    void redundancyFilterRemovesDuplicates() {
        Blueprint bp = new Blueprint("x", "y", List.of(
                new Point(1, 1), new Point(1, 1), new Point(2, 2)));
        Blueprint filtered = new RedundancyFilter().apply(bp);
        assertEquals(2, filtered.getPoints().size());
    }

    @Test
    void undersamplingFilterKeepsEvenIndexes() {
        Blueprint bp = new Blueprint("x", "y", List.of(
                new Point(0,0), new Point(1,1), new Point(2,2), new Point(3,3)));
        Blueprint filtered = new UndersamplingFilter().apply(bp);
        assertEquals(2, filtered.getPoints().size());
        assertEquals(new Point(0,0), filtered.getPoints().get(0));
        assertEquals(new Point(2,2), filtered.getPoints().get(1));
    }
}
