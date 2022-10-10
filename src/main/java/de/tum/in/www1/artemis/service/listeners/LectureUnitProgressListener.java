package de.tum.in.www1.artemis.service.listeners;

import javax.persistence.PostPersist;
import javax.persistence.PostRemove;
import javax.persistence.PostUpdate;

import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

import de.tum.in.www1.artemis.domain.lecture.LectureUnitCompletion;
import de.tum.in.www1.artemis.service.messaging.InstanceMessageSendService;

@Component
public class LectureUnitProgressListener {

    private InstanceMessageSendService instanceMessageSendService;

    /**
     * Empty constructor for Spring.
     */
    public LectureUnitProgressListener() {

    }

    public LectureUnitProgressListener(@Lazy InstanceMessageSendService instanceMessageSendService) {
        this.instanceMessageSendService = instanceMessageSendService;
    }

    @PostUpdate
    @PostPersist
    @PostRemove
    public void removeOrUpdateAssociatedParticipantScore(LectureUnitCompletion completion) {
        System.out.println("LectureUnitProgressListener");
        instanceMessageSendService.sendProgressInvalidForLectureUnit(completion.getLectureUnit().getId(), completion.getUser().getId());
    }

}
