package io.github.alexxfromgit.taf.contract.examples.petstore.model;

public record User(Long id, String username, String firstName, String lastName, String email, String phone,
                   Integer userStatus) {
}
