import { Component, Input, OnInit } from '@angular/core';
import { debounceTime, distinctUntilChanged, finalize, map, Subject } from 'rxjs';
import { HttpErrorResponse, HttpResponse } from '@angular/common/http';
import { onError } from 'app/shared/util/global.utils';
import { AlertService } from 'app/core/util/alert.service';
import { NgbActiveModal } from '@ng-bootstrap/ng-bootstrap';
import { ChannelOverviewDTO, ChannelService } from 'app/shared/metis/conversations/channel.service';
import { ChannelAction } from 'app/overview/course-messages/channels/channels-overview-dialog/channel-item/channel-item.component';
import { ConversationService } from 'app/shared/metis/conversations/conversation.service';

@Component({
    selector: 'jhi-channels-overview-dialog',
    templateUrl: './channels-overview-dialog.component.html',
    styleUrls: ['./channels-overview-dialog.component.scss'],
})
export class ChannelsOverviewDialogComponent implements OnInit {
    channelActions$ = new Subject<ChannelAction>();

    @Input() courseId: number;
    noOfChannels = 0;

    channelActionPerformed = false;
    isLoading = false;
    channels: ChannelOverviewDTO[] = [];
    idsOfUnsubscribedChannels: number[] = [];

    constructor(
        private channelService: ChannelService,
        private conversationService: ConversationService,
        private alertService: AlertService,
        private activeModal: NgbActiveModal,
    ) {}

    ngOnInit(): void {
        if (this.courseId) {
            this.loadChannels();

            this.channelActions$.pipe(debounceTime(500), distinctUntilChanged()).subscribe((channelAction) => {
                this.performChannelAction(channelAction);
            });
        }
    }

    clear() {
        if (this.channelActionPerformed) {
            this.activeModal.close(this.idsOfUnsubscribedChannels);
        } else {
            this.activeModal.dismiss();
        }
    }

    trackIdentity(index: number, item: ChannelOverviewDTO) {
        return item.channelId;
    }

    onChannelAction(channelAction: ChannelAction) {
        this.channelActions$.next(channelAction);
    }

    performChannelAction(channelAction: ChannelAction) {
        switch (channelAction.action) {
            case 'register':
                this.channelService.registerUsersToChannel(this.courseId, channelAction.channel.channelId).subscribe(() => {
                    if (this.idsOfUnsubscribedChannels.includes(channelAction.channel.channelId)) {
                        this.idsOfUnsubscribedChannels = this.idsOfUnsubscribedChannels.filter((id) => id !== channelAction.channel.channelId);
                    }
                    this.loadChannels();
                    this.channelActionPerformed = true;
                });
                break;
            case 'deregister':
                this.channelService.deregisterUsersFromChannel(this.courseId, channelAction.channel.channelId).subscribe(() => {
                    this.idsOfUnsubscribedChannels.push(channelAction.channel.channelId);
                    this.loadChannels();
                    this.channelActionPerformed = true;
                });
                break;
            case 'view':
                this.activeModal.close(channelAction.channel.channelId);
                break;
        }
    }
    loadChannels() {
        this.isLoading = true;
        this.channelService
            .getChannelsOfCourse(this.courseId)
            .pipe(
                map((res: HttpResponse<ChannelOverviewDTO[]>) => res.body),
                finalize(() => {
                    this.isLoading = false;
                }),
            )
            .subscribe({
                next: (channels: ChannelOverviewDTO[]) => {
                    this.channels = channels ?? [];
                    this.noOfChannels = this.channels.length;
                },
                error: (errorResponse: HttpErrorResponse) => onError(this.alertService, errorResponse),
            });
    }
}
