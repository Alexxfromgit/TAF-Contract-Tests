package io.github.alexxfromgit.taf.contract.examples.petstore.model;

import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

/** Request/response model. Unknown response fields are ignored here - the drift report surfaces them. */
public record Pet(Long id, String name, Category category, List<String> photoUrls, List<Tag> tags, String status) {

    /** A valid pet with a random id, so parallel runs against a shared live API do not collide. */
    public static Pet random(String name) {
        long id = ThreadLocalRandom.current().nextLong(100_000_000L, 999_999_999L);
        return new Pet(id, name, new Category(1L, "Dogs"), List.of("https://example.org/" + name + ".png"),
                List.of(new Tag(1L, "contract-taf")), "available");
    }

    public Pet withStatus(String newStatus) {
        return new Pet(id, name, category, photoUrls, tags, newStatus);
    }
}
