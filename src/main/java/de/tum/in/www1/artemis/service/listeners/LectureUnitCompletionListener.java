package de.tum.in.www1.artemis.service.listeners;

import javax.persistence.PostPersist;
import javax.persistence.PostRemove;
import javax.persistence.PostUpdate;

import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

import de.tum.in.www1.artemis.domain.lecture.LectureUnitCompletion;
import de.tum.in.www1.artemis.service.messaging.InstanceMessageSendService;

/**
 * Listener for updates on {@link LectureUnitCompletion} entities to update the {@link de.tum.in.www1.artemis.domain.LearningGoalProgress}.
 * @see de.tum.in.www1.artemis.service.scheduled.LearningGoalProgressScheduleService
 */
@Component
public class LectureUnitCompletionListener {

    private InstanceMessageSendService instanceMessageSendService;

    /**
     * Empty constructor for Spring.
     */
    public LectureUnitCompletionListener() {

    }

    public LectureUnitCompletionListener(@Lazy InstanceMessageSendService instanceMessageSendService) {
        this.instanceMessageSendService = instanceMessageSendService;
    }

    /**
     * This callback method is called after a lecture unit was completed or uncompleted.
     * @param completion The completion model containing the lecture unit and user
     */
    @PostUpdate
    @PostPersist
    @PostRemove
    public void removeOrUpdateAssociatedParticipantScore(LectureUnitCompletion completion) {
        instanceMessageSendService.sendProgressUpdateForLectureUnit(completion.getLectureUnit().getId(), completion.getUser().getId());
    }

}
