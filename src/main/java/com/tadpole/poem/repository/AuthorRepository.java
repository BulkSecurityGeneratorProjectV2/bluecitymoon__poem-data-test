package com.tadpole.poem.repository;

import com.tadpole.poem.domain.Author;

import org.springframework.data.jpa.repository.*;

import java.util.List;

/**
 * Spring Data JPA repository for the Author entity.
 */
public interface AuthorRepository extends JpaRepository<Author,Long> {

    Author findByLink(String link);

    List<Author> findByDescriptionIsNull();

    List<Author> findByBirthYearIsNullAndDieYearIsNull();

    List<Author> findByBirthYearIsNotNullAndDieYearIsNotNullAndAgeIsNull();
}
