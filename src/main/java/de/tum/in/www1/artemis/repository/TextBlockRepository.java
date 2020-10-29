package de.tum.in.www1.artemis.repository;

import static org.springframework.data.jpa.repository.EntityGraph.EntityGraphType.LOAD;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import de.tum.in.www1.artemis.domain.text.TextBlock;
import de.tum.in.www1.artemis.domain.text.TextCluster;

/**
 * Spring Data repository for the TextBlock entity.
 */
@SuppressWarnings("unused")
@Repository
public interface TextBlockRepository extends JpaRepository<TextBlock, String> {

    Optional<Set<TextBlock>> findAllByCluster(TextCluster textCluster);

    @EntityGraph(type = LOAD, attributePaths = { "cluster" })
    List<TextBlock> findAllWithEagerClusterBySubmissionId(Long id);

    List<TextBlock> findAllBySubmission_Participation_Exercise_IdAndTreeIdNotNull(Long exerciseId);

    // Used for the migration of old text exercises
    List<TextBlock> findAllBySubmission_Participation_Exercise_Id(Long exerciseId);

    List<TextBlock> findAllBySubmissionId(Long id);

    @EntityGraph(type = LOAD, attributePaths = { "submission" })
    List<TextBlock> findAllBySubmissionIdIn(List<Long> submissionIdList);
}
