package com.charlie.quizlet.topic;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import com.charlie.quizlet.topic.dto.TopicResponse;

public interface TopicRepository extends JpaRepository<Topic, Long> {

    boolean existsByNameIgnoreCase(String name);

    boolean existsByNameIgnoreCaseAndIdNot(String name, Long id);

    @Query("""
            select new com.charlie.quizlet.topic.dto.TopicResponse(t.id, t.name,
                (select count(q) from Quiz q where q.topic = t))
            from Topic t
            order by lower(t.name)
            """)
    List<TopicResponse> findAllWithQuizCount();
}
