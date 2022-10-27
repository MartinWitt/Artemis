package de.tum.in.www1.artemis.domain;

import java.io.Serial;
import java.io.Serializable;
import java.time.Instant;
import java.util.Objects;

import javax.persistence.*;

import org.springframework.data.annotation.LastModifiedDate;

import com.fasterxml.jackson.annotation.JsonIgnore;

/**
 * This class models the 'progress' association between a user and a learning goal.
 */
@Entity
@Table(name = "learning_goal_user")
@EntityListeners(AbstractAuditingEntity.class)
public class LearningGoalProgress {

    /**
     * The primary key of the association, composited through {@link LearningGoalUserId}.
     */
    @EmbeddedId
    @JsonIgnore
    private LearningGoalUserId id = new LearningGoalUserId();

    @ManyToOne
    @MapsId("userId")
    @JsonIgnore
    private User user;

    @ManyToOne
    @MapsId("learningGoalId")
    @JsonIgnore
    private LearningGoal learningGoal;

    @Column(name = "progress")
    private Double progress;

    @Column(name = "confidence")
    private Double confidence;

    @LastModifiedDate
    @Column(name = "last_modified_date")
    @JsonIgnore
    private Instant lastModifiedDate;

    public LearningGoalUserId getId() {
        return id;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public LearningGoal getLearningGoal() {
        return learningGoal;
    }

    public void setLearningGoal(LearningGoal learningGoal) {
        this.learningGoal = learningGoal;
    }

    public Double getProgress() {
        return progress;
    }

    public void setProgress(Double progress) {
        this.progress = progress;
    }

    public Double getConfidence() {
        return confidence;
    }

    public void setConfidence(Double confidence) {
        this.confidence = confidence;
    }

    public Instant getLastModifiedDate() {
        return lastModifiedDate;
    }

    @Override
    public String toString() {
        return "LearningGoalProgress{" + "id=" + id + ", user=" + user + ", learningGoal=" + learningGoal + ", progress=" + progress + ", confidence=" + confidence + '}';
    }

    /**
     * This class is used to create a composite primary key (user_id, learning_goal_id).
     * See also https://www.baeldung.com/spring-jpa-embedded-method-parameters
     */
    @Embeddable
    @SuppressWarnings("unused")
    public static class LearningGoalUserId implements Serializable {

        @Serial
        private static final long serialVersionUID = 1L;

        private Long userId;

        private Long learningGoalId;

        public LearningGoalUserId() {
            // Empty constructor for Spring
        }

        public LearningGoalUserId(Long userId, Long learningGoalId) {
            this.userId = userId;
            this.learningGoalId = learningGoalId;
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if (obj == null || getClass() != obj.getClass()) {
                return false;
            }
            LearningGoalUserId that = (LearningGoalUserId) obj;
            return userId.equals(that.userId) && learningGoalId.equals(that.learningGoalId);
        }

        @Override
        public int hashCode() {
            return Objects.hash(userId, learningGoalId);
        }
    }
}
