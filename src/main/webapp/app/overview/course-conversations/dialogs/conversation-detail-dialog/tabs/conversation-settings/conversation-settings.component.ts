import { Component, EventEmitter, Input, Output } from '@angular/core';
import { getAsChannelDto } from 'app/entities/metis/conversation/channel.model';
import { ConversationDto } from 'app/entities/metis/conversation/conversation.model';
import { Course } from 'app/entities/course.model';
import { NgbModal, NgbModalRef } from '@ng-bootstrap/ng-bootstrap';
import { ChannelService } from 'app/shared/metis/conversations/channel.service';
import { GenericConfirmationDialog } from 'app/overview/course-conversations/dialogs/generic-confirmation-dialog/generic-confirmation-dialog.component';
import { onError } from 'app/shared/util/global.utils';
import { from, Subject } from 'rxjs';
import { HttpErrorResponse } from '@angular/common/http';
import { AlertService } from 'app/core/util/alert.service';
import { faTimes } from '@fortawesome/free-solid-svg-icons';

@Component({
    selector: 'jhi-conversation-settings',
    templateUrl: './conversation-settings.component.html',
    styleUrls: ['./conversation-settings.component.scss'],
})
export class ConversationSettingsComponent {
    getAsChannel = getAsChannelDto;

    @Input()
    activeConversation: ConversationDto;

    @Input()
    course: Course;

    @Output()
    channelArchivalChange: EventEmitter<void> = new EventEmitter<void>();

    @Output()
    channelDeleted: EventEmitter<void> = new EventEmitter<void>();

    private dialogErrorSource = new Subject<string>();
    dialogError$ = this.dialogErrorSource.asObservable();

    faTimes = faTimes;

    constructor(private modalService: NgbModal, private channelService: ChannelService, private alertService: AlertService) {}

    openArchivalModal(event: MouseEvent) {
        const channel = getAsChannelDto(this.activeConversation);
        if (!channel) {
            return;
        }

        const keys = {
            titleKey: 'artemisApp.pages.archiveChannel.title',
            questionKey: 'artemisApp.pages.archiveChannel.question',
            descriptionKey: 'artemisApp.pages.archiveChannel.description',
            confirmButtonKey: 'artemisApp.pages.archiveChannel.confirmButton',
        };

        const translationParams = {
            channelName: channel.name,
        };

        event.stopPropagation();
        const modalRef: NgbModalRef = this.modalService.open(GenericConfirmationDialog, {
            size: 'lg',
            scrollable: false,
            backdrop: 'static',
        });
        modalRef.componentInstance.translationParameters = translationParams;
        modalRef.componentInstance.translationKeys = keys;
        modalRef.componentInstance.canBeUndone = true;
        modalRef.componentInstance.initialize();

        from(modalRef.result).subscribe(() => {
            this.channelService.archive(this.course?.id!, channel.id!).subscribe({
                next: () => {
                    this.channelArchivalChange.emit();
                },
                error: (errorResponse: HttpErrorResponse) => onError(this.alertService, errorResponse),
            });
        });
    }

    openUnArchivalModal(event: MouseEvent) {
        const channel = getAsChannelDto(this.activeConversation);
        if (!channel) {
            return;
        }

        const keys = {
            titleKey: 'artemisApp.pages.unArchiveChannel.title',
            questionKey: 'artemisApp.pages.unArchiveChannel.question',
            descriptionKey: 'artemisApp.pages.unArchiveChannel.description',
            confirmButtonKey: 'artemisApp.pages.unArchiveChannel.confirmButton',
        };

        const translationParams = {
            channelName: channel.name,
        };

        event.stopPropagation();
        const modalRef: NgbModalRef = this.modalService.open(GenericConfirmationDialog, {
            size: 'lg',
            scrollable: false,
            backdrop: 'static',
        });
        modalRef.componentInstance.translationParameters = translationParams;
        modalRef.componentInstance.translationKeys = keys;
        modalRef.componentInstance.canBeUndone = true;
        modalRef.componentInstance.initialize();

        from(modalRef.result).subscribe(() => {
            this.channelService.unarchive(this.course?.id!, channel.id!).subscribe({
                next: () => {
                    this.channelArchivalChange.emit();
                },
                error: (errorResponse: HttpErrorResponse) => onError(this.alertService, errorResponse),
            });
        });
    }

    deleteChannel() {
        const channel = getAsChannelDto(this.activeConversation);
        if (!channel) {
            return;
        }
        this.channelService.delete(this.course?.id!, channel.id!).subscribe({
            next: () => {
                this.dialogErrorSource.next('');
                this.channelDeleted.emit();
            },
            error: (errorResponse: HttpErrorResponse) => this.dialogErrorSource.next(errorResponse.message),
        });
    }
}
