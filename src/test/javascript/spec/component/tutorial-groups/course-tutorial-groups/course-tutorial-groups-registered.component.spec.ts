import { ComponentFixture, TestBed } from '@angular/core/testing';

import { CourseTutorialGroupsRegisteredComponent } from 'app/overview/course-tutorial-groups/course-tutorial-groups-registered/course-tutorial-groups-registered.component';
import { Component, Input } from '@angular/core';
import { TutorialGroup } from 'app/entities/tutorial-group/tutorial-group.model';
import { MockPipe } from 'ng-mocks';
import { ArtemisTranslatePipe } from 'app/shared/pipes/artemis-translate.pipe';
import { generateExampleTutorialGroup } from '../helpers/tutorialGroupExampleModels';

@Component({ selector: 'jhi-course-tutorial-group-card', template: '' })
class MockCourseTutorialGroupCard {
    @Input()
    courseId: number;
    @Input()
    tutorialGroup: TutorialGroup;
}

describe('CourseTutorialGroupsRegisteredComponent', () => {
    let component: CourseTutorialGroupsRegisteredComponent;
    let fixture: ComponentFixture<CourseTutorialGroupsRegisteredComponent>;

    beforeEach(async () => {
        await TestBed.configureTestingModule({
            declarations: [CourseTutorialGroupsRegisteredComponent, MockCourseTutorialGroupCard, MockPipe(ArtemisTranslatePipe)],
        }).compileComponents();

        fixture = TestBed.createComponent(CourseTutorialGroupsRegisteredComponent);
        component = fixture.componentInstance;
        component.courseId = 1;
        component.registeredTutorialGroups = [generateExampleTutorialGroup({})];
        fixture.detectChanges();
    });

    it('should create', () => {
        expect(component).toBeTruthy();
    });
});
