package de.tum.in.www1.artemis.repository;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import de.tum.in.www1.artemis.domain.LearningGoalProgress;

@Repository
public interface LearningGoalProgressRepository extends JpaRepository<LearningGoalProgress, Long> {

    @Transactional
    @Modifying
    void deleteAllByLearningGoalId(Long learningGoalId);

    @Transactional
    @Modifying
    void deleteAllByUserId(Long userId);

    @Query("""
            SELECT learningGoalProgress
            FROM LearningGoalProgress learningGoalProgress
            WHERE learningGoalProgress.learningGoal.id = :learningGoalId
            AND learningGoalProgress.user.id = :userId
            """)
    Optional<LearningGoalProgress> findByLearningGoalIdAndUserId(@Param("learningGoalId") Long learningGoalId, @Param("userId") Long userId);

    @Query("""
            SELECT learningGoalProgress
            FROM LearningGoalProgress learningGoalProgress
            LEFT JOIN FETCH learningGoalProgress.user
            LEFT JOIN FETCH learningGoalProgress.learningGoal
            WHERE learningGoalProgress.learningGoal.id = :learningGoalId
            AND learningGoalProgress.user.id = :userId
            """)
    Optional<LearningGoalProgress> findEagerByLearningGoalIdAndUserId(@Param("learningGoalId") Long learningGoalId, @Param("userId") Long userId);

    @Query("""
            SELECT learningGoalProgress
            FROM LearningGoalProgress learningGoalProgress
            WHERE learningGoalProgress.learningGoal.id IN :learningGoalIds
            AND learningGoalProgress.user.id = :userId
            """)
    Set<LearningGoalProgress> findAllByLearningGoalIdInAndUserId(@Param("learningGoalIds") List<Long> learningGoalIds, @Param("userId") Long userId);

    @Query("""
            SELECT learningGoalProgress
            FROM LearningGoalProgress learningGoalProgress
            LEFT JOIN FETCH learningGoalProgress.user
            LEFT JOIN FETCH learningGoalProgress.learningGoal
            WHERE learningGoalProgress.progress IS NULL
            OR learningGoalProgress.confidence IS NULL
            """)
    List<LearningGoalProgress> findAllInvalid();

    @Transactional
    @Modifying
    @Query("""
            UPDATE LearningGoalProgress learningGoalProgress
            SET learningGoalProgress.progress = NULL, learningGoalProgress.confidence = NULL
            WHERE learningGoalProgress.user.id = :userId
            AND learningGoalProgress.learningGoal.id IN :learningGoalIds
            """)
    void invalidateAllByUserIdAndLearningGoalIds(@Param("userId") Long userId, @Param("learningGoalIds") List<Long> learningGoalIds);

    @Query("""
            SELECT AVG(learningGoalProgress.confidence)
            FROM LearningGoalProgress learningGoalProgress
            WHERE learningGoalProgress.learningGoal.id = :learningGoalId
            """)
    Optional<Double> findAverageConfidenceByLearningGoalId(@Param("learningGoalId") Long learningGoalId);

    @Query("""
            SELECT AVG(learningGoalProgress.progress)
            FROM LearningGoalProgress learningGoalProgress
            WHERE learningGoalProgress.learningGoal.id = :learningGoalId
            """)
    Optional<Double> findAverageProgressByLearningGoalId(@Param("learningGoalId") Long learningGoalId);
}
