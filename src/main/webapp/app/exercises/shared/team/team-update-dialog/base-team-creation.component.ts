import { Component, Input, OnInit, ViewChild } from '@angular/core';
import { AbstractControl, NgForm } from '@angular/forms';
import { NgbActiveModal } from '@ng-bootstrap/ng-bootstrap';
import { HttpErrorResponse, HttpResponse } from '@angular/common/http';
import { Observable, Subject } from 'rxjs';
import { ParticipationService } from 'app/exercises/shared/participation/participation.service';
import { TeamService } from 'app/exercises/shared/team/team.service';
import { Team } from 'app/entities/team.model';
import { User } from 'app/core/user/user.model';
import { cloneDeep, isEmpty, omit } from 'lodash-es';
import { TeamAssignmentConfig } from 'app/entities/team-assignment-config.model';
import { debounceTime, switchMap } from 'rxjs/operators';
import { Exercise } from 'app/entities/exercise.model';
import { shortNamePattern } from 'app/shared/constants/input.constants';
import { faBan, faExclamationTriangle, faSave, faSpinner, faTrashAlt } from '@fortawesome/free-solid-svg-icons';

export type StudentTeamConflict = { studentLogin: string; teamId: string };

@Component({
    template: '',
    styleUrls: ['./base-team-creation.component.scss'],
})
export class BaseTeamCreationComponent implements OnInit {
    @ViewChild('editForm', { static: false }) editForm: NgForm;
    @Input() team: Team;
    @Input() exercise: Exercise;

    pendingTeam: Team;
    isSaving = false;

    searchingStudents = false;
    searchingStudentsQueryTooShort = false;
    searchingStudentsFailed = false;
    searchingStudentsNoResultsForQuery?: string;

    searchingOwner = false;
    searchingOwnerQueryTooShort = false;
    searchingOwnerFailed = false;
    searchingOwnerNoResultsForQuery?: string;

    studentTeamConflicts: StudentTeamConflict[] = [];
    ignoreTeamSizeRecommendation = false;

    private shortNameValidator = new Subject<string>();
    readonly shortNameAlreadyTakenErrorCode = 'alreadyTaken';
    readonly shortNamePattern = shortNamePattern; // must start with a letter and cannot contain special characters

    // Icons
    faSave = faSave;
    faBan = faBan;
    faSpinner = faSpinner;
    faExclamationTriangle = faExclamationTriangle;
    faTrashAlt = faTrashAlt;

    constructor(private participationService: ParticipationService, private teamService: TeamService, private activeModal: NgbActiveModal) {}

    /**
     * Life cycle hook to indicate component creation is done
     */
    ngOnInit(): void {
        this.initPendingTeam();
        this.shortNameValidation(this.shortNameValidator);
    }

    private initPendingTeam() {
        this.pendingTeam = cloneDeep(this.team);
    }

    /**
     * Hook to indicate a short team name change
     * @param {string} shortName - new short name of the team
     */
    onTeamShortNameChanged(shortName: string) {
        // automatically convert shortName to lowercase characters
        this.pendingTeam.shortName = shortName.toLowerCase();

        // check that no other team already uses this short name
        this.shortNameValidator.next(this.pendingTeam.shortName);
    }

    /**
     * Hook to indicate a team name change
     * @param {string} name - new team name
     */
    onTeamNameChanged(name: string) {
        if (!this.shortNameReadOnly) {
            // automatically set the shortName based on the name (stripping all non-alphanumeric characters)
            const shortName = name.replace(/[^0-9a-z]/gi, '');
            this.onTeamShortNameChanged(shortName);
            this.shortNameControl.markAsTouched();
        }
    }

    get shortNameReadOnly(): boolean {
        return !!this.pendingTeam.id;
    }

    get shortNameControl(): AbstractControl {
        return this.editForm.control.get('shortName')!;
    }

    get config(): TeamAssignmentConfig {
        return this.exercise.teamAssignmentConfig!;
    }

    /**
     * Check if a given user has a conflicting team
     * @param {User} student - User to search for
     */
    hasConflictingTeam(student: User): boolean {
        return this.findStudentTeamConflict(student) !== undefined;
    }

    /**
     * Get conflicting team of a given user
     * @param {User} student - User to search for
     */
    getConflictingTeam(student: User) {
        const conflict = this.findStudentTeamConflict(student);
        return conflict ? conflict['teamId'] : undefined;
    }

    private findStudentTeamConflict(student: User) {
        return this.studentTeamConflicts.find((conflict) => conflict.studentLogin === student.login);
    }

    protected resetStudentTeamConflict(student: User) {
        return (this.studentTeamConflicts = this.studentTeamConflicts.filter((conflict) => conflict.studentLogin !== student.login));
    }

    /**
     * Cancel the update-dialog
     */
    clear() {
        this.activeModal.dismiss('cancel');
    }

    /**
     * Save changes made to the team
     */
    save() {
        this.team = cloneDeep(this.pendingTeam);
        if (this.team.id !== undefined) {
            this.subscribeToSaveResponse(this.teamService.update(this.exercise, this.team));
        } else {
            this.subscribeToSaveResponse(this.teamService.create(this.exercise, this.team));
        }
    }

    private subscribeToSaveResponse(team: Observable<HttpResponse<Team>>) {
        this.isSaving = true;
        team.subscribe({
            next: (res) => this.onSaveSuccess(res),
            error: (error) => this.onSaveError(error),
        });
    }

    /**
     * Hook to indicate the saving was successful
     * @param {HttpResponse<Team>}team - The successful updated team
     */
    onSaveSuccess(team: HttpResponse<Team>) {
        this.activeModal.close(team.body);
        this.isSaving = false;
    }

    /**
     * Hook to indicate a save error occurred
     * @param {HttpErrorResponse} httpErrorResponse - The occurred error
     */
    onSaveError(httpErrorResponse: HttpErrorResponse) {
        this.isSaving = false;
        const { errorKey, params } = httpErrorResponse.error;

        switch (errorKey) {
            case 'studentsAlreadyAssignedToTeams':
                const { conflicts } = params;
                this.studentTeamConflicts = conflicts;
                break;
            default:
                break;
        }
    }

    private shortNameValidation(shortName$: Subject<string>) {
        shortName$
            .pipe(
                debounceTime(500),
                switchMap((shortName) => this.teamService.existsByShortName(this.exercise.course!, shortName)),
            )
            .subscribe((alreadyTakenResponse) => {
                const alreadyTaken = alreadyTakenResponse.body;
                const errors = alreadyTaken
                    ? { ...this.shortNameControl.errors, [this.shortNameAlreadyTakenErrorCode]: alreadyTaken }
                    : omit(this.shortNameControl.errors, this.shortNameAlreadyTakenErrorCode);
                this.shortNameControl.setErrors(isEmpty(errors) ? null : errors);
            });
    }
}
