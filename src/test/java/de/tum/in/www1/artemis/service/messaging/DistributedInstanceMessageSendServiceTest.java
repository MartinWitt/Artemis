package de.tum.in.www1.artemis.service.messaging;

import static org.mockito.Mockito.*;
import static tech.jhipster.config.JHipsterConstants.SPRING_PROFILE_TEST;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@AutoConfigureMockMvc
@AutoConfigureTestDatabase
@ActiveProfiles({ SPRING_PROFILE_TEST, "artemis", "bamboo", "bitbucket", "jira", "ldap", "athene", "apollon" }) // no scheduling
// Todo: Find a way to test this class including the "distributed" Hazelcast setup
class DistributedInstanceMessageSendServiceTest {

    @SpyBean
    DistributedInstanceMessageSendService sendService;

    @Test
    void processScheduleProgrammingExercise() {
        sendService.sendProgrammingExerciseSchedule(1L);
        verify(sendService, times(1)).sendMessageDelayed(MessageTopic.PROGRAMMING_EXERCISE_SCHEDULE, 1L);
    }

    @Test
    void sendProgrammingExerciseScheduleCancel() {
        sendService.sendProgrammingExerciseScheduleCancel(1L);
        verify(sendService, times(1)).sendMessageDelayed(MessageTopic.PROGRAMMING_EXERCISE_SCHEDULE_CANCEL, 1L);
    }

    @Test
    void sendModelingExerciseSchedule() {
        sendService.sendModelingExerciseSchedule(1L);
        verify(sendService, times(1)).sendMessageDelayed(MessageTopic.MODELING_EXERCISE_SCHEDULE, 1L);
    }

    @Test
    void sendModelingExerciseScheduleCancel() {
        sendService.sendModelingExerciseScheduleCancel(1L);
        verify(sendService, times(1)).sendMessageDelayed(MessageTopic.MODELING_EXERCISE_SCHEDULE_CANCEL, 1L);
    }

    @Test
    void sendModelingExerciseInstantClustering() {
        sendService.sendModelingExerciseInstantClustering(1L);
        verify(sendService, times(1)).sendMessageDelayed(MessageTopic.MODELING_EXERCISE_INSTANT_CLUSTERING, 1L);
    }

    @Test
    void sendTextExerciseSchedule() {
        sendService.sendTextExerciseSchedule(1L);
        verify(sendService, times(1)).sendMessageDelayed(MessageTopic.TEXT_EXERCISE_SCHEDULE, 1L);
    }

    @Test
    void sendTextExerciseScheduleCancel() {
        sendService.sendTextExerciseScheduleCancel(1L);
        verify(sendService, times(1)).sendMessageDelayed(MessageTopic.TEXT_EXERCISE_SCHEDULE_CANCEL, 1L);
    }

    @Test
    void sendTextExerciseInstantClustering() {
        sendService.sendTextExerciseInstantClustering(1L);
        verify(sendService, times(1)).sendMessageDelayed(MessageTopic.TEXT_EXERCISE_INSTANT_CLUSTERING, 1L);
    }

    @Test
    void sendUnlockAllRepositories() {
        sendService.sendUnlockAllRepositories(1L);
        verify(sendService, times(1)).sendMessageDelayed(MessageTopic.PROGRAMMING_EXERCISE_UNLOCK_REPOSITORIES, 1L);
    }

    @Test
    void sendLockAllRepositories() {
        sendService.sendLockAllRepositories(1L);
        verify(sendService, times(1)).sendMessageDelayed(MessageTopic.PROGRAMMING_EXERCISE_LOCK_REPOSITORIES, 1L);
    }

    @Test
    void sendRemoveNonActivatedUserSchedule() {
        sendService.sendRemoveNonActivatedUserSchedule(1L);
        verify(sendService, times(1)).sendMessageDelayed(MessageTopic.USER_MANAGEMENT_REMOVE_NON_ACTIVATED_USERS, 1L);
    }

    @Test
    void sendCancelRemoveNonActivatedUserSchedule() {
        sendService.sendCancelRemoveNonActivatedUserSchedule(1L);
        verify(sendService, times(1)).sendMessageDelayed(MessageTopic.USER_MANAGEMENT_CANCEL_REMOVE_NON_ACTIVATED_USERS, 1L);
    }

    @Test
    void sendExerciseReleaseNotificationSchedule() {
        sendService.sendExerciseReleaseNotificationSchedule(1L);
        verify(sendService, times(1)).sendMessageDelayed(MessageTopic.EXERCISE_RELEASED_SCHEDULE, 1L);
    }

    @Test
    void sendAssessedExerciseSubmissionNotificationSchedule() {
        sendService.sendAssessedExerciseSubmissionNotificationSchedule(1L);
        verify(sendService, times(1)).sendMessageDelayed(MessageTopic.ASSESSED_EXERCISE_SUBMISSION_SCHEDULE, 1L);
    }

    @Test
    void sendExamMonitoringSchedule() {
        sendService.sendExamMonitoringSchedule(1L);
        verify(sendService, times(1)).sendMessageDelayed(MessageTopic.EXAM_MONITORING_SCHEDULE, 1L);
    }

    @Test
    void sendExamMonitoringScheduleCancel() {
        sendService.sendExamMonitoringScheduleCancel(1L);
        verify(sendService, times(1)).sendMessageDelayed(MessageTopic.EXAM_MONITORING_SCHEDULE_CANCEL, 1L);
    }

    @Test
    void sendParticipantScoreSchedule() {
        sendService.sendParticipantScoreSchedule(1L, 5L, null);
        verify(sendService, times(1)).sendMessageDelayed(MessageTopic.PARTICIPANT_SCORE_SCHEDULE, 1L, 5L, null);
    }

    @Test
    void sendProgressUpdateForExercise() {
        sendService.sendProgressUpdateForExercise(1L, 5L);
        verify(sendService, times(1)).sendMessageDelayed(MessageTopic.SCHEDULE_PROGRESS_EXERCISE, 1L, 5L);
    }

    @Test
    void sendProgressUpdateForLectureUnit() {
        sendService.sendProgressUpdateForLectureUnit(1L, 5L);
        verify(sendService, times(1)).sendMessageDelayed(MessageTopic.SCHEDULE_PROGRESS_LECTURE_UNIT, 1L, 5L);
    }

}
