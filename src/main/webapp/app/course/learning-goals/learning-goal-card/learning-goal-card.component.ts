import { Component, Input, OnDestroy, OnInit } from '@angular/core';
import { NgbModal } from '@ng-bootstrap/ng-bootstrap';
import { TranslateService } from '@ngx-translate/core';
import { LearningGoal, LearningGoalTaxonomy } from 'app/entities/learningGoal.model';
import { LectureUnitService } from 'app/lecture/lecture-unit/lecture-unit-management/lectureUnit.service';
import { LearningGoalDetailModalComponent } from 'app/course/learning-goals/learning-goal-detail-modal/learning-goal-detail-modal.component';
import { LearningGoalCourseDetailModalComponent } from 'app/course/learning-goals/learning-goal-course-detail-modal/learning-goal-course-detail-modal.component';
import { IconProp } from '@fortawesome/fontawesome-svg-core';
import { faBrain, faComments, faCubesStacked, faMagnifyingGlass, faPenFancy, faPlusMinus, faQuestion } from '@fortawesome/free-solid-svg-icons';

@Component({
    selector: 'jhi-learning-goal-card',
    templateUrl: './learning-goal-card.component.html',
    styleUrls: ['../../../overview/course-exercises/course-exercise-row.scss'],
})
export class LearningGoalCardComponent implements OnInit, OnDestroy {
    @Input()
    learningGoal: LearningGoal;
    @Input()
    progress?: number;
    @Input()
    isPrerequisite: boolean;
    @Input()
    displayOnly: boolean;

    public predicate = 'id';
    public reverse = false;
    public isProgressAvailable = false;

    public DetailModalComponent = LearningGoalDetailModalComponent;
    public CourseDetailModalComponent = LearningGoalCourseDetailModalComponent;

    constructor(private modalService: NgbModal, public lectureUnitService: LectureUnitService, public translateService: TranslateService) {}

    ngOnInit(): void {
        this.isProgressAvailable = !this.isPrerequisite;
    }

    ngOnDestroy(): void {
        if (this.modalService.hasOpenModals()) {
            this.modalService.dismissAll();
        }
    }

    getIcon(learningGoalTaxonomy?: LearningGoalTaxonomy): IconProp {
        if (!learningGoalTaxonomy) {
            return faQuestion as IconProp;
        }

        const icons = {
            [LearningGoalTaxonomy.REMEMBER]: faBrain,
            [LearningGoalTaxonomy.UNDERSTAND]: faComments,
            [LearningGoalTaxonomy.APPLY]: faPenFancy,
            [LearningGoalTaxonomy.ANALYZE]: faMagnifyingGlass,
            [LearningGoalTaxonomy.EVALUATE]: faPlusMinus,
            [LearningGoalTaxonomy.CREATE]: faCubesStacked,
        };

        return icons[learningGoalTaxonomy] as IconProp;
    }

    getIconTooltip(learningGoalTaxonomy?: LearningGoalTaxonomy): string {
        if (!learningGoalTaxonomy) {
            return '';
        }

        const tooltips = {
            [LearningGoalTaxonomy.REMEMBER]: '',
            [LearningGoalTaxonomy.UNDERSTAND]: '',
            [LearningGoalTaxonomy.APPLY]: '',
            [LearningGoalTaxonomy.ANALYZE]: '',
            [LearningGoalTaxonomy.EVALUATE]: '',
            [LearningGoalTaxonomy.CREATE]: '',
        };

        return tooltips[learningGoalTaxonomy];
    }
}
