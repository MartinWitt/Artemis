import { Component, EventEmitter, Input, OnInit, Output } from '@angular/core';
import { ConversationService } from 'app/shared/metis/conversations/conversation.service';
import { Conversation } from 'app/entities/metis/conversation/conversation.model';
import { Course } from 'app/entities/course.model';
import { HttpErrorResponse, HttpHeaders, HttpResponse } from '@angular/common/http';
import { debounceTime, distinctUntilChanged, switchMap, tap } from 'rxjs/operators';
import { User } from 'app/core/user/user.model';
import { AlertService } from 'app/core/util/alert.service';
import { onError } from 'app/shared/util/global.utils';
import { from, map, Subject } from 'rxjs';
import { faMagnifyingGlass, faUser, faUserPlus } from '@fortawesome/free-solid-svg-icons';
import { NgbModal, NgbModalRef } from '@ng-bootstrap/ng-bootstrap';
import { ConversationAddUsersDialogComponent } from 'app/overview/course-messages/conversation-add-users-dialog/conversation-add-users-dialog.component';

interface SearchQuery {
    searchTerm: string;
    force: boolean;
}
@Component({
    selector: 'jhi-conversation-members',
    templateUrl: './conversation-members.component.html',
    styleUrls: ['./conversation-members.component.scss'],
})
export class ConversationMembers implements OnInit {
    private readonly search$ = new Subject<SearchQuery>();

    @Output()
    changesPerformed = new EventEmitter<void>();

    @Input()
    conversation: Conversation;
    @Input()
    course: Course;

    members: User[] = [];
    // page information
    page = 1;
    itemsPerPage = 10;
    totalItems = 0;
    isSearching = true;
    searchTerm = '';

    // icons
    faUser = faUser;
    faMagnifyingGlass = faMagnifyingGlass;
    faUserPlus = faUserPlus;
    constructor(public conversationService: ConversationService, private alertService: AlertService, private modalService: NgbModal) {}

    private onSuccess(members: User[] | null, headers: HttpHeaders): void {
        this.totalItems = Number(headers.get('X-Total-Count'));
        this.members = members || [];
    }

    openAddUsersDialog(event: MouseEvent) {
        event.stopPropagation();
        const modalRef: NgbModalRef = this.modalService.open(ConversationAddUsersDialogComponent, { size: 'lg', scrollable: false, backdrop: 'static' });
        modalRef.componentInstance.course = this.course;
        modalRef.componentInstance.conversation = this.conversation;
        from(modalRef.result).subscribe(() => {
            this.search$.next({
                searchTerm: this.searchTerm,
                force: true,
            });
            this.changesPerformed.emit();
        });
    }

    trackIdentity(index: number, item: User) {
        return item.id;
    }

    ngOnInit(): void {
        this.search$
            .pipe(
                debounceTime(300),
                distinctUntilChanged((prev, curr) => {
                    if (curr.force === true) {
                        return false;
                    } else {
                        return prev === curr;
                    }
                }),
                tap(() => (this.members = [])),
                map((query) => {
                    const searchTerm = query.searchTerm !== null && query.searchTerm !== undefined ? query.searchTerm : '';
                    return searchTerm.trim().toLowerCase();
                }),
                tap((searchTerm) => {
                    this.isSearching = true;
                    this.searchTerm = searchTerm;
                }),
                switchMap(() => this.conversationService.searchMembersOfConversation(this.course.id!, this.conversation.id!, this.searchTerm, this.page - 1, this.itemsPerPage)),
            )
            .subscribe({
                next: (res: HttpResponse<User[]>) => {
                    this.isSearching = false;
                    this.onSuccess(res.body, res.headers);
                },
                error: (errorResponse: HttpErrorResponse) => {
                    this.isSearching = false;
                    onError(this.alertService, errorResponse);
                },
            });
        this.search$.next({
            searchTerm: '',
            force: true,
        });
    }

    transition() {
        this.search$.next({
            searchTerm: this.searchTerm,
            force: true,
        });
    }

    getUserLabel(user: User) {
        let label = '';
        if (user.firstName) {
            label += user.firstName + ' ';
        }
        if (user.lastName) {
            label += user.lastName + ' ';
        }
        if (user.login) {
            label += '(' + user.login + ')';
        }
        return label.trim();
    }

    onSearchQueryInput($event: Event) {
        const searchTerm = ($event.target as HTMLInputElement).value?.trim().toLowerCase() ?? '';
        this.search$.next({
            searchTerm,
            force: false,
        });
    }
}
