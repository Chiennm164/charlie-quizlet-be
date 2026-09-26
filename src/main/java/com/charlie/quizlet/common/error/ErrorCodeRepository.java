package com.charlie.quizlet.common.error;

import org.springframework.data.jpa.repository.JpaRepository;

public interface ErrorCodeRepository extends JpaRepository<ErrorCodeEntry, String> {
}
