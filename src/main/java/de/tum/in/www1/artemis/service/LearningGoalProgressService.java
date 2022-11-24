package de.tum.in.www1.artemis.service;

import java.util.*;
import java.util.stream.Stream;

import org.hibernate.Hibernate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
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
public class LearningGoalProgressService {

    private final Logger logger = LoggerFactory.getLogger(LearningGoalProgressService.class);

    private final LearningGoalRepository learningGoalRepository;

    private final LearningGoalProgressRepository learningGoalProgressRepository;

    private final StudentScoreRepository studentScoreRepository;

    private final TeamScoreRepository teamScoreRepository;

    private final ExerciseRepository exerciseRepository;

    private final LectureUnitRepository lectureUnitRepository;

    private final UserRepository userRepository;

    public LearningGoalProgressService(LearningGoalRepository learningGoalRepository, LearningGoalProgressRepository learningGoalProgressRepository,
            StudentScoreRepository studentScoreRepository, TeamScoreRepository teamScoreRepository, ExerciseRepository exerciseRepository,
            LectureUnitRepository lectureUnitRepository, UserRepository userRepository) {
        this.learningGoalRepository = learningGoalRepository;
        this.learningGoalProgressRepository = learningGoalProgressRepository;
        this.studentScoreRepository = studentScoreRepository;
        this.teamScoreRepository = teamScoreRepository;
        this.exerciseRepository = exerciseRepository;
        this.lectureUnitRepository = lectureUnitRepository;
        this.userRepository = userRepository;
    }

    @Async
    public void executeTask(ILearningObject learningObject, Long userId) {
        try {
            SecurityUtils.setAuthorizationObject();

            Set<LearningGoal> learningGoals;
            if (learningObject instanceof Exercise exercise) {
                if (Hibernate.isPropertyInitialized(exercise, "learningGoals")) {
                    learningGoals = exercise.getLearningGoals();
                }
                else {
                    learningGoals = exerciseRepository.findByIdWithLearningGoals(exercise.getId()).map(Exercise::getLearningGoals).orElse(null);
                }
            }
            else if (learningObject instanceof LectureUnit lectureUnit) {
                if (Hibernate.isPropertyInitialized(lectureUnit, "learningGoals")) {
                    learningGoals = lectureUnit.getLearningGoals();
                }
                else {
                    learningGoals = lectureUnitRepository.findByIdWithLearningGoals(lectureUnit.getId()).map(LectureUnit::getLearningGoals).orElse(null);
                }
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
            logger.error("Exception while updating progress for learning goal", e);
        }
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
        var completions = learningObjects.stream().map(learningObject -> hasUserCompleted(user, learningObject)).toList();
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

}
