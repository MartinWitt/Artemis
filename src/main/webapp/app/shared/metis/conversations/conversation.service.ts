import { Injectable } from '@angular/core';
import { HttpClient, HttpParams, HttpResponse } from '@angular/common/http';
import { Observable } from 'rxjs';
import { map } from 'rxjs/operators';
import dayjs from 'dayjs/esm';
import { Conversation, ConversationDto } from 'app/entities/metis/conversation/conversation.model';
import { TranslateService } from '@ngx-translate/core';
import { AccountService } from 'app/core/auth/account.service';
import { User } from 'app/core/user/user.model';
import { isChannelDto } from 'app/entities/metis/conversation/channel.model';
import { isGroupChatDto } from 'app/entities/metis/conversation/group-chat.model';
import { ConversationUser } from 'app/entities/metis/conversation/conversation-user-dto.model';
import { isOneToOneChatDto } from 'app/entities/metis/conversation/one-to-one-chat.model';

type EntityArrayResponseType = HttpResponse<ConversationDto[]>;

export type UserSortDirection = 'asc' | 'desc';
export type UserSortProperty = keyof User;
export type UserSortingParameter = {
    sortProperty: UserSortProperty;
    sortDirection: UserSortDirection;
};

export enum ConversationMemberSearchFilter {
    ALL,
    INSTRUCTOR,
    EDITOR,
    TUTOR,
    STUDENT,
    CHANNEL_ADMIN, // this is a special role that is only used for channels
}
@Injectable({ providedIn: 'root' })
export class ConversationService {
    public resourceUrl = SERVER_API_URL + '/api/courses/';

    constructor(protected http: HttpClient, protected translationService: TranslateService, protected accountService: AccountService) {}

    getConversationName = (conversation: ConversationDto): string => {
        if (!conversation) {
            return '';
        }
        if (isChannelDto(conversation)) {
            let channelName = conversation.name ?? '';
            if (conversation.isArchived) {
                channelName += ' (' + this.translationService.instant('artemisApp.entities.channel.archived') + ')';
            }
            return channelName;
        } else if (isOneToOneChatDto(conversation)) {
            return 'ToDo';
        } else if (isGroupChatDto(conversation)) {
            const namesOfOtherMembers = conversation.namesOfOtherMembers ?? [];
            if (namesOfOtherMembers.length === 0) {
                return '';
            } else if (namesOfOtherMembers.length === 1) {
                return namesOfOtherMembers[0];
            } else if (namesOfOtherMembers.length === 2) {
                return `${namesOfOtherMembers[0]}, ${namesOfOtherMembers[1]}`;
            } else {
                return (
                    `${namesOfOtherMembers[0]}, ${namesOfOtherMembers[1]}, ` +
                    this.translationService.instant('artemisApp.messages.conversation.others', { count: namesOfOtherMembers.length - 2 })
                );
            }
        } else {
            return '';
        }
    };

    searchMembersOfConversation(
        courseId: number,
        conversationId: number,
        loginOrName: string,
        page: number,
        size: number,
        filter: ConversationMemberSearchFilter,
    ): Observable<HttpResponse<ConversationUser[]>> {
        const sortingParameters: UserSortingParameter[] = [
            { sortProperty: 'firstName', sortDirection: 'asc' },
            { sortProperty: 'lastName', sortDirection: 'asc' },
        ];
        const params = this.creatSearchPagingParams(sortingParameters, page, size, loginOrName, filter);
        return this.http.get<ConversationUser[]>(`${this.resourceUrl}${courseId}/conversations/${conversationId}/members/search`, {
            observe: 'response',
            params,
        });
    }

    getConversationsOfUser(courseId: number): Observable<EntityArrayResponseType> {
        return this.http
            .get<ConversationDto[]>(`${this.resourceUrl}${courseId}/conversations`, {
                observe: 'response',
            })
            .pipe(map(this.convertDateArrayFromServer));
    }

    public convertDateFromClient = (conversation: Conversation) => ({
        ...conversation,
        creationDate: conversation.creationDate && dayjs(conversation.creationDate).isValid() ? dayjs(conversation.creationDate).toJSON() : undefined,
        lastMessageDate: conversation.lastMessageDate && dayjs(conversation.lastMessageDate).isValid() ? dayjs(conversation.lastMessageDate).toJSON() : undefined,
    });

    public convertDateFromServer = (res: HttpResponse<ConversationDto>): HttpResponse<ConversationDto> => {
        if (res.body) {
            this.convertServerDates(res.body);
        }
        return res;
    };

    public convertServerDates = (conversation: ConversationDto) => {
        conversation.creationDate = conversation.creationDate ? dayjs(conversation.creationDate) : undefined;
        conversation.lastMessageDate = conversation.lastMessageDate ? dayjs(conversation.lastMessageDate) : undefined;
        return conversation;
    };

    public convertDateArrayFromServer = (res: HttpResponse<ConversationDto[]>): HttpResponse<ConversationDto[]> => {
        if (res.body) {
            res.body.forEach((conversation) => {
                this.convertServerDates(conversation);
            });
        }
        return res;
    };

    private creatSearchPagingParams = (sortingParameters: UserSortingParameter[], page: number, size: number, loginOrName: string, filter: ConversationMemberSearchFilter) => {
        let params = new HttpParams();
        if (`${filter}` !== `${ConversationMemberSearchFilter.ALL}`) {
            params = params.set('filter', ConversationMemberSearchFilter[filter]);
        }
        params = params.set('loginOrName', loginOrName);
        for (const sortParameter of sortingParameters) {
            params = params.append('sort', `${sortParameter.sortProperty},${sortParameter.sortDirection}`);
        }
        params = params.set('page', String(page));
        return params.set('size', String(size));
    };
}
