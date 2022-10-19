package de.tum.in.www1.artemis.service.scheduled;

import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledFuture;
import java.util.stream.Stream;

import javax.annotation.PreDestroy;
import javax.validation.constraints.NotNull;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Profile;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.stereotype.Service;

import de.tum.in.www1.artemis.domain.*;
import de.tum.in.www1.artemis.domain.lecture.ExerciseUnit;
import de.tum.in.www1.artemis.domain.lecture.LectureUnit;
import de.tum.in.www1.artemis.domain.lecture.LectureUnitCompletion;
import de.tum.in.www1.artemis.domain.scores.ParticipantScore;
import de.tum.in.www1.artemis.repository.*;
import de.tum.in.www1.artemis.security.SecurityUtils;
import de.tum.in.www1.artemis.service.util.RoundingUtil;

@Service
@Profile("scheduling")
public class LearningGoalProgressScheduleService {

    private final Logger logger = LoggerFactory.getLogger(LearningGoalProgressScheduleService.class);

    private final TaskScheduler scheduler;

    private final Map<Integer, ScheduledFuture<?>> scheduledTasks = new ConcurrentHashMap<>();

    private final LearningGoalRepository learningGoalRepository;

    private final LearningGoalProgressRepository learningGoalProgressRepository;

    private final StudentScoreRepository studentScoreRepository;

    private final TeamScoreRepository teamScoreRepository;

    private final ExerciseRepository exerciseRepository;

    private final LectureUnitRepository lectureUnitRepository;

    private final UserRepository userRepository;

    public LearningGoalProgressScheduleService(@Qualifier("taskScheduler") TaskScheduler scheduler, LearningGoalRepository learningGoalRepository,
            LearningGoalProgressRepository learningGoalProgressRepository, StudentScoreRepository studentScoreRepository, TeamScoreRepository teamScoreRepository,
            ExerciseRepository exerciseRepository, LectureUnitRepository lectureUnitRepository, UserRepository userRepository) {
        this.scheduler = scheduler;
        this.learningGoalRepository = learningGoalRepository;
        this.learningGoalProgressRepository = learningGoalProgressRepository;
        this.studentScoreRepository = studentScoreRepository;
        this.teamScoreRepository = teamScoreRepository;
        this.exerciseRepository = exerciseRepository;
        this.lectureUnitRepository = lectureUnitRepository;
        this.userRepository = userRepository;
    }

    /**
     * Check if the scheduler has tasks to be executed or is idle.
     * @return true if the scheduler is idle, false otherwise
     */
    public boolean isIdle() {
        return scheduledTasks.isEmpty();
    }

    /**
     * Before shutdown, cancel all running or scheduled tasks.
     */
    @PreDestroy
    public void shutdown() {
        // Stop all running tasks, we will reschedule them on startup again
        scheduledTasks.values().forEach(future -> {
            future.cancel(true);
        });
        scheduledTasks.clear();
    }

    public void updateLearningGoalProgressForExercise(@NotNull Long exerciseId, @NotNull Long userId) {
        this.scheduleTask(LearningObjectType.EXERCISE, exerciseId, userId);
    }

    public void updateLearningGoalProgressForLectureUnit(@NotNull Long lectureUnitId, @NotNull Long userId) {
        this.scheduleTask(LearningObjectType.LECTURE_UNIT, lectureUnitId, userId);
    }

    protected void scheduleTask(LearningObjectType learningObject, Long learningObjectId, Long userId) {
        final int progressHash = new LearningGoalProgressId(learningObject, learningObjectId, userId).hashCode();
        var scheduledFuture = scheduledTasks.get(progressHash);
        if (scheduledFuture != null) {
            // If a task is already scheduled, cancel it and reschedule it
            scheduledFuture.cancel(true);
            scheduledTasks.remove(progressHash);
        }

        var schedulingTime = ZonedDateTime.now().plus(3, ChronoUnit.SECONDS);
        var future = scheduler.schedule(() -> this.executeTask(learningObject, learningObjectId, userId), schedulingTime.toInstant());
        scheduledTasks.put(progressHash, future);
        logger.info("Schedule task to update progress for learning object {} and user {} at {}.", learningObjectId, userId, schedulingTime);
    }

    private void executeTask(LearningObjectType learningObject, Long learningObjectId, Long userId) {
        long start = System.currentTimeMillis();
        logger.info("Learning goal progress task for learning goal {} and user {} started", learningObjectId, userId);
        try {
            SecurityUtils.setAuthorizationObject();

            Set<LearningGoal> learningGoals;
            if (learningObject == LearningObjectType.EXERCISE) {
                learningGoals = exerciseRepository.findByIdWithLearningGoals(learningObjectId).map(Exercise::getLearningGoals).orElse(null);
            }
            else if (learningObject == LearningObjectType.LECTURE_UNIT) {
                learningGoals = lectureUnitRepository.findByIdWithLearningGoals(learningObjectId).map(LectureUnit::getLearningGoals).orElse(null);
            }
            else {
                return;
            }

            if (learningGoals == null) {
                // Learning goals couldn't be loaded, the exercise/lecture unit might have already been deleted
                return;
            }

            learningGoals.forEach(learningGoal -> {
                updateLearningGoalProgress(learningGoal.getId(), userId);
            });
        }
        catch (Exception e) {
            logger.error("Exception while processing progress task for learning goal {} and user {}:", learningObjectId, userId, e);
        }
        finally {
            scheduledTasks.remove(new LearningGoalProgressId(learningObject, learningObjectId, userId).hashCode());
        }
        long end = System.currentTimeMillis();
        logger.info("Updating the learning goal progress for learning goal {} and user {} took {} ms.", learningObjectId, userId, end - start);
    }

    private void updateLearningGoalProgress(Long learningGoalId, Long userId) {
        var user = userRepository.findById(userId).orElse(null);
        var learningGoal = learningGoalRepository.findByIdWithExercisesAndLectureUnitsAndCompletions(learningGoalId).orElse(null);

        if (user == null || learningGoal == null) {
            // If the user or learning goal no longer exist, there is nothing to do
            return;
        }

        var progress = learningGoalProgressRepository.findEagerByLearningGoalIdAndUserId(learningGoalId, userId).orElse(new LearningGoalProgress());

        List<ILearningObject> learningObjects = new ArrayList<>();

        List<LectureUnit> allLectureUnits = learningGoal.getLectureUnits().stream().filter(LectureUnit::isVisibleToStudents).toList();

        List<LectureUnit> lectureUnits = allLectureUnits.stream().filter(lectureUnit -> !(lectureUnit instanceof ExerciseUnit)).toList();
        List<Exercise> exercises = learningGoal.getExercises().stream().filter(Exercise::isVisibleToStudents).toList();

        learningObjects.addAll(lectureUnits);
        learningObjects.addAll(exercises);

        progress.setLearningGoal(learningGoal);
        progress.setUser(user);
        progress.setProgress(RoundingUtil.roundScoreSpecifiedByCourseSettings(calculateProgress(learningObjects, user), learningGoal.getCourse()));
        progress.setConfidence(RoundingUtil.roundScoreSpecifiedByCourseSettings(calculateConfidence(exercises, user), learningGoal.getCourse()));
        learningGoalProgressRepository.save(progress);
    }

    private double calculateProgress(List<ILearningObject> learningObjects, User user) {
        var completions = learningObjects.stream().map(ILearningObject -> hasUserCompleted(user, ILearningObject)).toList();
        completions.forEach(completed -> logger.debug("{} completed {}", user.getLogin(), completed));
        return completions.stream().mapToInt(completed -> completed ? 100 : 0).summaryStatistics().getAverage();
    }

    private double calculateConfidence(List<Exercise> exercises, User user) {
        var studentScores = studentScoreRepository.findAllByExercisesAndUser(exercises, user);
        var teamScores = teamScoreRepository.findAllByExercisesAndUser(exercises, user);
        return Stream.concat(studentScores.stream(), teamScores.stream()).map(ParticipantScore::getLastScore).mapToDouble(score -> score).summaryStatistics().getAverage();
    }

    private boolean hasUserCompleted(User user, ILearningObject learningObject) {
        if (learningObject instanceof LectureUnit lectureUnit) {
            return lectureUnit.getCompletedUsers().stream().map(LectureUnitCompletion::getUser).anyMatch(user1 -> user1.getId().equals(user.getId()));
        }
        else if (learningObject instanceof Exercise exercise) {
            var studentScores = studentScoreRepository.findAllByExercisesAndUser(List.of(exercise), user);
            var teamScores = teamScoreRepository.findAllByExercisesAndUser(List.of(exercise), user);
            return Stream.concat(studentScores.stream(), teamScores.stream()).findAny().isPresent();
        }
        throw new IllegalArgumentException("Completable must be either LectureUnit or Exercise");
    }

    public enum LearningObjectType {
        LECTURE_UNIT, EXERCISE
    }

    public record LearningGoalProgressId(LearningObjectType learningObjectType, Long learningObjectId, Long userId) {
    }
}
