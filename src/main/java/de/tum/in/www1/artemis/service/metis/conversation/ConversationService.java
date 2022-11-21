package de.tum.in.www1.artemis.service.metis.conversation;

import java.time.ZonedDateTime;
import java.util.*;
import java.util.stream.Collectors;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.messaging.simp.SimpMessageSendingOperations;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.RequestBody;

import de.tum.in.www1.artemis.domain.Course;
import de.tum.in.www1.artemis.domain.User;
import de.tum.in.www1.artemis.domain.metis.ConversationParticipant;
import de.tum.in.www1.artemis.domain.metis.conversation.Channel;
import de.tum.in.www1.artemis.domain.metis.conversation.Conversation;
import de.tum.in.www1.artemis.repository.UserRepository;
import de.tum.in.www1.artemis.repository.metis.ConversationParticipantRepository;
import de.tum.in.www1.artemis.repository.metis.PostRepository;
import de.tum.in.www1.artemis.repository.metis.conversation.ChannelRepository;
import de.tum.in.www1.artemis.repository.metis.conversation.ConversationRepository;
import de.tum.in.www1.artemis.repository.metis.conversation.GroupChatRepository;
import de.tum.in.www1.artemis.repository.metis.conversation.OneToOneChatRepository;
import de.tum.in.www1.artemis.web.rest.errors.AccessForbiddenException;
import de.tum.in.www1.artemis.web.rest.errors.EntityNotFoundException;
import de.tum.in.www1.artemis.web.rest.metis.conversation.dtos.ConversationDTO;
import de.tum.in.www1.artemis.web.websocket.dto.metis.ConversationWebsocketDTO;
import de.tum.in.www1.artemis.web.websocket.dto.metis.MetisCrudAction;

@Service
public class ConversationService {

    private static final String METIS_WEBSOCKET_CHANNEL_PREFIX = "/topic/metis/";

    private final ConversationDTOService conversationDTOService;

    private final UserRepository userRepository;

    private final ConversationRepository conversationRepository;

    private final ChannelRepository channelRepository;

    private final ConversationParticipantRepository conversationParticipantRepository;

    private final SimpMessageSendingOperations messagingTemplate;

    private final OneToOneChatRepository oneToOneChatRepository;

    private final PostRepository postRepository;

    private final GroupChatRepository groupChatRepository;

    public ConversationService(ConversationDTOService conversationDTOService, UserRepository userRepository, ChannelRepository channelRepository,
            ConversationParticipantRepository conversationParticipantRepository, ConversationRepository conversationRepository, SimpMessageSendingOperations messagingTemplate,
            OneToOneChatRepository oneToOneChatRepository, PostRepository postRepository, GroupChatRepository groupChatRepository) {
        this.conversationDTOService = conversationDTOService;
        this.userRepository = userRepository;
        this.channelRepository = channelRepository;
        this.conversationParticipantRepository = conversationParticipantRepository;
        this.conversationRepository = conversationRepository;
        this.messagingTemplate = messagingTemplate;
        this.oneToOneChatRepository = oneToOneChatRepository;
        this.postRepository = postRepository;
        this.groupChatRepository = groupChatRepository;
    }

    public Conversation getConversationById(Long conversationId) {
        return conversationRepository.findByIdElseThrow(conversationId);
    }

    public boolean isMember(Long conversationId, Long userId) {
        return conversationParticipantRepository.findConversationParticipantByConversationIdAndUserId(conversationId, userId).isPresent();
    }

    public List<ConversationDTO> getConversationsOfUser(Long courseId, User requestingUser) {
        var oneToOneChatsOfUser = oneToOneChatRepository.findActiveOneToOneChatsOfUserWithParticipantsAndUserGroups(courseId, requestingUser.getId());
        var channelsOfUser = channelRepository.findChannelsOfUser(courseId, requestingUser.getId());
        var groupChatsOfUser = groupChatRepository.findActiveGroupChatsOfUserWithParticipantsAndUserGroups(courseId, requestingUser.getId());

        var conversations = new ArrayList<Conversation>();
        conversations.addAll(oneToOneChatsOfUser);
        conversations.addAll(channelsOfUser);
        conversations.addAll(groupChatsOfUser);
        return conversations.stream().map(conversation -> conversationDTOService.convertToDTO(conversation, requestingUser)).collect(Collectors.toList());
    }

    public Conversation updateConversation(Conversation conversation) {
        return conversationRepository.save(conversation);
    }

    public void registerUsersToConversation(Course course, Set<User> usersToRegister, Conversation conversation, Optional<Integer> memberLimit) {
        var existingParticipants = conversationParticipantRepository.findConversationParticipantsByConversationIdAndUserIds(conversation.getId(),
                usersToRegister.stream().map(User::getId).collect(Collectors.toSet()));
        var usersToRegisterWithoutExistingParticipants = usersToRegister.stream()
                .filter(user -> existingParticipants.stream().noneMatch(participant -> participant.getUser().getId().equals(user.getId()))).collect(Collectors.toSet());

        if (memberLimit.isPresent()) {
            var currentMemberCount = conversationParticipantRepository.countByConversationId(conversation.getId());
            if (currentMemberCount + usersToRegisterWithoutExistingParticipants.size() > memberLimit.get()) {
                throw new IllegalArgumentException("The member limit of the conversation would be exceeded");
            }
        }
        Set<ConversationParticipant> newConversationParticipants = new HashSet<>();
        for (User user : usersToRegisterWithoutExistingParticipants) {
            ConversationParticipant conversationParticipant = new ConversationParticipant();
            conversationParticipant.setUser(user);
            conversationParticipant.setConversation(conversation);
            if (conversation instanceof Channel) {
                conversationParticipant.setIsAdmin(false); // special case just for channels
            }
            newConversationParticipants.add(conversationParticipant);
        }
        if (newConversationParticipants.size() > 0) {
            conversationParticipantRepository.saveAll(newConversationParticipants);
            broadcastOnConversationMembershipChannel(course, MetisCrudAction.CREATE, conversation, usersToRegisterWithoutExistingParticipants);
            notifyConversationMembersAboutUpdate(conversation);
        }
    }

    public void notifyConversationMembersAboutUpdate(Conversation conversation) {
        var usersToContact = conversationParticipantRepository.findConversationParticipantByConversationId(conversation.getId()).stream().map(ConversationParticipant::getUser)
                .collect(Collectors.toSet());
        broadcastOnConversationMembershipChannel(conversation.getCourse(), MetisCrudAction.UPDATE, conversation, usersToContact);
    }

    public void deregisterUsersFromAConversation(Course course, Set<User> usersToDeregister, Conversation conversation) {
        var participantsToRemove = conversationParticipantRepository.findConversationParticipantsByConversationIdAndUserIds(conversation.getId(),
                usersToDeregister.stream().map(User::getId).collect(Collectors.toSet()));
        var usersWithExistingParticipants = usersToDeregister.stream()
                .filter(user -> participantsToRemove.stream().anyMatch(participant -> participant.getUser().getId().equals(user.getId()))).collect(Collectors.toSet());

        // you are not allowed to deregister the creator OF A channel
        if (conversation instanceof Channel) {
            var creator = conversation.getCreator();
            if (usersWithExistingParticipants.contains(creator)) {
                throw new IllegalArgumentException("The creator of a channel cannot be deregistered");
            }
        }
        if (participantsToRemove.size() > 0) {
            conversationParticipantRepository.deleteAll(participantsToRemove);
            broadcastOnConversationMembershipChannel(course, MetisCrudAction.DELETE, conversation, usersWithExistingParticipants);
            notifyConversationMembersAboutUpdate(conversation);
        }
    }

    @Transactional // ok because of delete
    public void deleteConversation(Conversation conversation) {
        var usersToMessage = conversationParticipantRepository.findConversationParticipantByConversationId(conversation.getId()).stream().map(ConversationParticipant::getUser)
                .collect(Collectors.toSet());
        broadcastOnConversationMembershipChannel(conversation.getCourse(), MetisCrudAction.DELETE, conversation, usersToMessage);
        this.postRepository.deleteAllByConversationId(conversation.getId());
        this.conversationParticipantRepository.deleteAllByConversationId(conversation.getId());
        this.conversationRepository.deleteById(conversation.getId());
    }

    public void broadcastOnConversationMembershipChannel(Course course, MetisCrudAction metisCrudAction, Conversation conversation, Set<User> usersToMessage) {
        String courseTopicName = METIS_WEBSOCKET_CHANNEL_PREFIX + "courses/" + course.getId();
        String conversationParticipantTopicName = courseTopicName + "/conversations/user/";
        usersToMessage.forEach(user -> sendToConversationMembershipChannel(metisCrudAction, conversation, user, conversationParticipantTopicName));
    }

    private void sendToConversationMembershipChannel(MetisCrudAction metisCrudAction, Conversation conversation, User user, String conversationParticipantTopicName) {
        var dto = conversationDTOService.convertToDTO(conversation, user);
        var websocketDTO = new ConversationWebsocketDTO(dto, metisCrudAction);
        messagingTemplate.convertAndSendToUser(user.getLogin(), conversationParticipantTopicName + user.getId(), websocketDTO);
    }

    public Conversation mayInteractWithConversationElseThrow(Long conversationId, User user) {
        Optional<Conversation> conversation = conversationRepository.findById(conversationId);
        if (conversation.isEmpty() || !isMember(conversationId, user.getId())) {
            throw new AccessForbiddenException("User not allowed to access this conversation!");
        }
        return conversation.get();
    }

    public ZonedDateTime auditConversationReadTimeOfUser(Conversation conversation, User user) {
        // update the last time user has read the conversation
        ConversationParticipant readingParticipant = conversationParticipantRepository.findConversationParticipantByConversationIdAndUserId(conversation.getId(), user.getId())
                .orElseThrow(() -> new EntityNotFoundException("Conversation participant not found!"));
        readingParticipant.setLastRead(ZonedDateTime.now());
        conversationParticipantRepository.save(readingParticipant);
        return readingParticipant.getLastRead();
    }

    public Page<User> searchMembersOfConversation(Course course, Conversation conversation, Pageable pageable, String searchTerm,
            Optional<ConversationMemberSearchFilters> filter) {
        if (filter.isEmpty()) {
            return userRepository.searchAllByLoginOrNameInConversation(pageable, searchTerm, conversation.getId());
        }
        else {
            switch (filter.get()) {
                case INSTRUCTOR -> {
                    return userRepository.searchAllByLoginOrNameInConversationWithCourseGroup(pageable, searchTerm, conversation.getId(), course.getInstructorGroupName());
                }
                case EDITOR -> {
                    return userRepository.searchAllByLoginOrNameInConversationWithCourseGroup(pageable, searchTerm, conversation.getId(), course.getEditorGroupName());
                }
                case TUTOR -> {
                    return userRepository.searchAllByLoginOrNameInConversationWithCourseGroup(pageable, searchTerm, conversation.getId(), course.getTeachingAssistantGroupName());
                }
                case STUDENT -> {
                    return userRepository.searchAllByLoginOrNameInConversationWithCourseGroup(pageable, searchTerm, conversation.getId(), course.getStudentGroupName());
                }
                case CHANNEL_ADMIN -> {
                    assert conversation instanceof Channel : "The filter CHANNEL_ADMIN is only allowed for channels!";
                    return userRepository.searchChannelAdminsByLoginOrNameInConversation(pageable, searchTerm, conversation.getId());
                }
                default -> throw new IllegalArgumentException("The filter is not supported.");
            }
        }

    }

    public void switchFavoriteStatus(Long conversationId, User requestingUser, Boolean isFavorite) {
        var participation = conversationParticipantRepository.findConversationParticipantByConversationIdAndUserId(conversationId, requestingUser.getId())
                .orElseThrow(() -> new EntityNotFoundException("Conversation participant not found!"));
        participation.setIsFavorite(isFavorite);
        conversationParticipantRepository.save(participation);
    }

    public void switchHiddenStatus(Long conversationId, User requestingUser, Boolean hiddenStatus) {
        var participation = conversationParticipantRepository.findConversationParticipantByConversationIdAndUserId(conversationId, requestingUser.getId())
                .orElseThrow(() -> new EntityNotFoundException("Conversation participant not found!"));
        participation.setIsHidden(hiddenStatus);
        conversationParticipantRepository.save(participation);
    }

    /**
     * The user can select one of these roles to filter the conversation members by role
     */
    public enum ConversationMemberSearchFilters {
        INSTRUCTOR, EDITOR, TUTOR, STUDENT, CHANNEL_ADMIN // this is a special role that is only used for channels
    }

    public Set<User> findUsersInDatabase(Course course, boolean findAllStudents, boolean findAllTutors, boolean findAllEditors, boolean findAllInstructors) {
        Set<User> users = new HashSet<>();
        if (findAllStudents) {
            users.addAll(userRepository.findAllInGroupWithAuthorities(course.getStudentGroupName()));
        }
        if (findAllTutors) {
            users.addAll(userRepository.findAllInGroupWithAuthorities(course.getTeachingAssistantGroupName()));
        }
        if (findAllEditors) {
            users.addAll(userRepository.findAllInGroupWithAuthorities(course.getEditorGroupName()));
        }
        if (findAllInstructors) {
            users.addAll(userRepository.findAllInGroupWithAuthorities(course.getInstructorGroupName()));
        }
        return users;
    }

    public Set<User> findUsersInDatabase(@RequestBody List<String> userLogins) {
        Set<User> users = new HashSet<>();
        for (String userLogin : userLogins) {
            if (userLogin == null || userLogin.isEmpty()) {
                continue;
            }
            var userToRegister = userRepository.findOneWithGroupsAndAuthoritiesByLogin(userLogin);
            userToRegister.ifPresent(users::add);
        }
        return users;
    }

}
