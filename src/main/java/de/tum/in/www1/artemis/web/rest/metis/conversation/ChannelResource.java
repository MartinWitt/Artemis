package de.tum.in.www1.artemis.web.rest.metis.conversation;

import static de.tum.in.www1.artemis.service.metis.conversation.ChannelService.CHANNEL_ENTITY_NAME;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.*;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import de.tum.in.www1.artemis.domain.User;
import de.tum.in.www1.artemis.domain.metis.conversation.Channel;
import de.tum.in.www1.artemis.repository.CourseRepository;
import de.tum.in.www1.artemis.repository.UserRepository;
import de.tum.in.www1.artemis.security.Role;
import de.tum.in.www1.artemis.service.AuthorizationCheckService;
import de.tum.in.www1.artemis.service.metis.conversation.ChannelAuthorizationService;
import de.tum.in.www1.artemis.service.metis.conversation.ChannelService;
import de.tum.in.www1.artemis.service.metis.conversation.ConversationService;
import de.tum.in.www1.artemis.web.rest.errors.BadRequestAlertException;
import de.tum.in.www1.artemis.web.rest.metis.conversation.dtos.ChannelDTO;

@RestController
@RequestMapping("/api/courses")
public class ChannelResource {

    private final Logger log = LoggerFactory.getLogger(ChannelResource.class);

    private final ChannelService channelService;

    private final ChannelAuthorizationService channelAuthorizationService;

    private final AuthorizationCheckService authorizationCheckService;

    private final CourseRepository courseRepository;

    private final UserRepository userRepository;

    private final ConversationService conversationService;

    public ChannelResource(ChannelService channelService, ChannelAuthorizationService channelAuthorizationService, AuthorizationCheckService authorizationCheckService,
            CourseRepository courseRepository, UserRepository userRepository, ConversationService conversationService) {
        this.channelService = channelService;
        this.channelAuthorizationService = channelAuthorizationService;
        this.authorizationCheckService = authorizationCheckService;
        this.courseRepository = courseRepository;
        this.userRepository = userRepository;
        this.conversationService = conversationService;
    }

    @GetMapping("/{courseId}/channels/overview")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<List<ChannelDTO>> getCourseChannelsOverview(@PathVariable Long courseId) {
        log.debug("REST request to all channels of course: {}", courseId);
        var user = userRepository.getUserWithGroupsAndAuthorities();
        authorizationCheckService.checkHasAtLeastRoleInCourseElseThrow(Role.STUDENT, courseRepository.findByIdElseThrow(courseId), user);
        var result = channelService.getChannels(courseId).stream().map(channel -> {
            var channelDTO = new ChannelDTO(channel);
            channelDTO.setIsMember(conversationService.isMember(channel.getId(), user.getId()));
            channelDTO.setNumberOfMembers(conversationService.getMemberCount(channel.getId()));
            return channelDTO;
        })
                // we only want to show public channels and in addition private channels that the user is a member of
                .filter(channelDTO -> channelDTO.getIsPublic() || channelDTO.getIsMember()).sorted(Comparator.comparing(ChannelDTO::getName)).toList();
        return ResponseEntity.ok(result);
    }

    @PostMapping("/{courseId}/channels")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<ChannelDTO> createChannel(@PathVariable Long courseId, @RequestBody ChannelDTO channelDTO) throws URISyntaxException {
        var course = courseRepository.findByIdElseThrow(courseId);
        channelAuthorizationService.isAllowedToCreateChannel(course, null);
        var channelToCreate = channelDTO.toChannel();
        var createdChannel = channelService.createChannel(course, channelToCreate);
        var dto = new ChannelDTO(createdChannel);
        dto.setIsMember(true);
        dto.setNumberOfMembers(1);
        return ResponseEntity.created(new URI("/api/channels/" + createdChannel.getId())).body(dto);
    }

    @PutMapping("/{courseId}/channels/{channelId}")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<ChannelDTO> updateChannel(@PathVariable Long courseId, @PathVariable Long channelId, @RequestBody ChannelDTO channelDTO) {
        var originalChannel = channelService.getChannelOrThrow(channelId);
        var requestingUser = userRepository.getUserWithGroupsAndAuthorities();
        if (!originalChannel.getCourse().getId().equals(courseId)) {
            throw new BadRequestAlertException("The channel does not belong to the course", CHANNEL_ENTITY_NAME, "channel.course.mismatch");
        }
        channelAuthorizationService.isAllowedToUpdateChannel(originalChannel, requestingUser);
        // now that we know the user is authorized we perform the expensive operation of loading all participants
        originalChannel = channelService.getChannelWithParticipantsOrThrow(channelId);

        var isChanged = false;
        if (channelDTO.getName() != null && !channelDTO.getName().equals(originalChannel.getName())) {
            originalChannel.setName(channelDTO.getName().trim().isBlank() ? null : channelDTO.getName().trim());
            isChanged = true;
        }
        if (channelDTO.getDescription() != null && !channelDTO.getDescription().equals(originalChannel.getDescription())) {
            originalChannel.setDescription(channelDTO.getDescription().trim().isBlank() ? null : channelDTO.getDescription().trim());
            isChanged = true;
        }
        if (channelDTO.getTopic() != null && !channelDTO.getTopic().equals(originalChannel.getTopic())) {
            originalChannel.setTopic(channelDTO.getTopic().trim().isBlank() ? null : channelDTO.getTopic().trim());
            isChanged = true;
        }
        if (!isChanged) {
            return ResponseEntity.ok(new ChannelDTO(originalChannel));
        }

        var updatedChannel = channelService.updateChannel(originalChannel);
        var dto = new ChannelDTO(updatedChannel);
        channelDTO.setIsMember(conversationService.isMember(updatedChannel.getId(), requestingUser.getId()));
        channelDTO.setNumberOfMembers(conversationService.getMemberCount(updatedChannel.getId()));
        return ResponseEntity.ok().body(dto);
    }

    @PostMapping("/{courseId}/channels/{channelId}/register")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<Void> registerUsersToChannel(@PathVariable Long courseId, @PathVariable Long channelId, @RequestBody List<String> userLogins) {
        if (userLogins == null || userLogins.isEmpty()) {
            throw new BadRequestAlertException("No user logins provided", CHANNEL_ENTITY_NAME, "userLoginsEmpty");
        }
        // ToDo: maybe limit how many users can be registered at once?

        log.debug("REST request to register {} users to channel : {}", userLogins.size(), channelId);
        var course = courseRepository.findByIdElseThrow(courseId);

        var channelFromDatabase = this.channelService.getChannelOrThrow(channelId);
        checkEntityIdMatchesPathIds(channelFromDatabase, Optional.of(courseId), Optional.of(channelId));

        var requestingUser = userRepository.getUserWithGroupsAndAuthorities();

        channelAuthorizationService.isAllowedToRegisterUsersToChannel(course, channelFromDatabase, userLogins, requestingUser);
        var usersToRegister = findUsersInDatabase(userLogins);
        conversationService.registerUsers(course, usersToRegister, channelFromDatabase);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{courseId}/channels/{channelId}/deregister")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<Void> deregisterUsers(@PathVariable Long courseId, @PathVariable Long channelId, @RequestBody List<String> userLogins) {
        if (userLogins == null || userLogins.isEmpty()) {
            throw new BadRequestAlertException("No user logins provided", CHANNEL_ENTITY_NAME, "userLoginsEmpty");
        }
        // ToDo: maybe limit how many users can be deregistered at once?
        log.debug("REST request to deregister {} users from the channel : {}", userLogins.size(), channelId);
        var course = courseRepository.findByIdElseThrow(courseId);

        var channelFromDatabase = this.channelService.getChannelOrThrow(channelId);
        checkEntityIdMatchesPathIds(channelFromDatabase, Optional.of(courseId), Optional.of(channelId));

        var requestingUser = userRepository.getUserWithGroupsAndAuthorities();

        channelAuthorizationService.isAllowedToDeregisterUsersFromChannel(course, channelFromDatabase, userLogins, requestingUser);

        var usersToDeRegister = findUsersInDatabase(userLogins);

        conversationService.deregisterUsers(course, usersToDeRegister, channelFromDatabase);
        return ResponseEntity.noContent().build();
    }

    private void checkEntityIdMatchesPathIds(Channel channel, Optional<Long> courseId, Optional<Long> conversationId) {
        courseId.ifPresent(courseIdValue -> {
            if (!channel.getCourse().getId().equals(courseIdValue)) {
                throw new BadRequestAlertException("The courseId in the path does not match the courseId in the channel", CHANNEL_ENTITY_NAME, "courseIdMismatch");
            }
        });
        conversationId.ifPresent(conversationIdValue -> {
            if (!channel.getId().equals(conversationIdValue)) {
                throw new BadRequestAlertException("The conversationId in the path does not match the channelId in the channel", CHANNEL_ENTITY_NAME, "channelIdMismatch");
            }
        });
    }

    private Set<User> findUsersInDatabase(@RequestBody List<String> userLogins) {
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
