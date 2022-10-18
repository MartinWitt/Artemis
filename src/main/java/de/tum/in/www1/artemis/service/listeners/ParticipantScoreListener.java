package de.tum.in.www1.artemis.service.listeners;

import java.util.HashSet;
import java.util.Set;

import javax.persistence.PostPersist;
import javax.persistence.PostUpdate;

import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

import de.tum.in.www1.artemis.domain.User;
import de.tum.in.www1.artemis.domain.scores.ParticipantScore;
import de.tum.in.www1.artemis.domain.scores.StudentScore;
import de.tum.in.www1.artemis.domain.scores.TeamScore;
import de.tum.in.www1.artemis.service.messaging.InstanceMessageSendService;

/**
 * Listener for updates on {@link ParticipantScore} entities to update the {@link de.tum.in.www1.artemis.domain.LearningGoalProgress}.
 * @see de.tum.in.www1.artemis.service.scheduled.LearningGoalProgressScheduleService
 */
@Component
public class ParticipantScoreListener {

    private InstanceMessageSendService instanceMessageSendService;

    /**
     * Empty constructor for Spring.
     */
    public ParticipantScoreListener() {

    }

    public ParticipantScoreListener(@Lazy InstanceMessageSendService instanceMessageSendService) {
        this.instanceMessageSendService = instanceMessageSendService;
    }

    /**
     * This callback method is called after a participant score is created or updated.
     * @param participantScore The participant score that was modified
     */
    @PostUpdate
    @PostPersist
    public void createOrUpdateAssociatedParticipantScore(ParticipantScore participantScore) {
        Set<User> users = new HashSet<>();
        if (participantScore instanceof StudentScore) {
            users.add(((StudentScore) participantScore).getUser());
        }
        else if (participantScore instanceof TeamScore) {
            users.addAll(((TeamScore) participantScore).getTeam().getStudents());
        }
        else {
            return;
        }

        users.stream().map(User::getId).forEach(userId -> instanceMessageSendService.sendProgressUpdateForExercise(participantScore.getExercise().getId(), userId));
    }

}
