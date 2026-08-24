package edu.eci.arsw.blueprints.persistence.impl;

import edu.eci.arsw.blueprints.model.Blueprint;
import edu.eci.arsw.blueprints.model.Point;
import edu.eci.arsw.blueprints.persistence.BlueprintNotFoundException;
import edu.eci.arsw.blueprints.persistence.BlueprintPersistence;
import edu.eci.arsw.blueprints.persistence.BlueprintPersistenceException;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Repository;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Repository
@Profile("postgres")
public class PostgresBlueprintPersistence implements BlueprintPersistence {

    private final BlueprintJpaRepository repo;

    public PostgresBlueprintPersistence(BlueprintJpaRepository repo) { this.repo = repo; }

    private Blueprint toDomain(BlueprintEntity e) {
        List<Point> pts = e.getPoints().stream().map(p -> new Point(p.getX(), p.getY())).toList();
        return new Blueprint(e.getAuthor(), e.getName(), pts);
    }

    private BlueprintEntity toEntity(Blueprint bp) {
        List<PointEmbeddable> pts = bp.getPoints().stream().map(p -> new PointEmbeddable(p.x(), p.y())).toList();
        return new BlueprintEntity(bp.getAuthor(), bp.getName(), pts);
    }

    @Override
    public void saveBlueprint(Blueprint bp) throws BlueprintPersistenceException {
        if (repo.findByAuthorAndName(bp.getAuthor(), bp.getName()).isPresent())
            throw new BlueprintPersistenceException("Blueprint already exists: " + bp.getAuthor() + "/" + bp.getName());
        repo.save(toEntity(bp));
    }

    @Override
    public Blueprint getBlueprint(String author, String name) throws BlueprintNotFoundException {
        return repo.findByAuthorAndName(author, name)
                .map(this::toDomain)
                .orElseThrow(() -> new BlueprintNotFoundException("Blueprint not found: " + author + "/" + name));
    }

    @Override
    public Set<Blueprint> getBlueprintsByAuthor(String author) throws BlueprintNotFoundException {
        List<BlueprintEntity> list = repo.findByAuthor(author);
        if (list.isEmpty()) throw new BlueprintNotFoundException("No blueprints for author: " + author);
        Set<Blueprint> result = new HashSet<>();
        list.forEach(e -> result.add(toDomain(e)));
        return result;
    }

    @Override
    public Set<Blueprint> getAllBlueprints() {
        Set<Blueprint> result = new HashSet<>();
        repo.findAll().forEach(e -> result.add(toDomain(e)));
        return result;
    }

    @Override
    public void addPoint(String author, String name, int x, int y) throws BlueprintNotFoundException {
        BlueprintEntity e = repo.findByAuthorAndName(author, name)
                .orElseThrow(() -> new BlueprintNotFoundException("Blueprint not found: " + author + "/" + name));
        e.getPoints().add(new PointEmbeddable(x, y));
        repo.save(e);
    }
}
