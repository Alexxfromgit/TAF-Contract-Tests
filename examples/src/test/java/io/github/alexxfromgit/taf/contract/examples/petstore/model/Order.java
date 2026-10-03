package io.github.alexxfromgit.taf.contract.examples.petstore.model;

public record Order(Long id, Long petId, Integer quantity, String shipDate, String status, Boolean complete) {
}
