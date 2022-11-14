package de.tum.in.www1.artemis.web.rest.metis.conversation;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import de.tum.in.www1.artemis.repository.CourseRepository;
import de.tum.in.www1.artemis.repository.UserRepository;
import de.tum.in.www1.artemis.service.metis.conversation.ConversationService;
import de.tum.in.www1.artemis.service.metis.conversation.OneToOneChatService;
import de.tum.in.www1.artemis.service.metis.conversation.auth.OneToOneChatAuthorizationService;
import de.tum.in.www1.artemis.web.rest.errors.BadRequestAlertException;
import de.tum.in.www1.artemis.web.rest.metis.conversation.dtos.OneToOneChatDTO;

@RestController
@RequestMapping("/api/courses")
public class OneToOneChatResource {

    private final Logger log = LoggerFactory.getLogger(OneToOneChatResource.class);

    private final OneToOneChatAuthorizationService oneToOneChatAuthorizationService;

    private final UserRepository userRepository;

    private final CourseRepository courseRepository;

    private final OneToOneChatService oneToOneChatService;

    private final ConversationService conversationService;

    public OneToOneChatResource(OneToOneChatAuthorizationService oneToOneChatAuthorizationService, UserRepository userRepository, CourseRepository courseRepository,
            OneToOneChatService oneToOneChatService, ConversationService conversationService) {
        this.oneToOneChatAuthorizationService = oneToOneChatAuthorizationService;
        this.userRepository = userRepository;
        this.courseRepository = courseRepository;
        this.oneToOneChatService = oneToOneChatService;
        this.conversationService = conversationService;
    }

    @PostMapping("/{courseId}/one-to-one-chats")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<OneToOneChatDTO> startOneToOneChat(@PathVariable Long courseId, @RequestBody List<String> userLogins) throws URISyntaxException {
        log.debug("REST request to create channel in course {} between : {} and : {}", courseId, userLogins);

        var requestingUser = userRepository.getUserWithGroupsAndAuthorities();
        var course = courseRepository.findByIdElseThrow(courseId);
        oneToOneChatAuthorizationService.isAllowedToCreateOneToOneChat(course, requestingUser);

        var chatMembers = new ArrayList<>(conversationService.findUsersInDatabase(userLogins));

        if (chatMembers.size() != 2) {
            throw new BadRequestAlertException("A one-to-one chat can only be started with two users", "OneToOneChat", "invalidUserCount");
        }
        if (!chatMembers.contains(requestingUser)) {
            throw new BadRequestAlertException("The requesting user must be part of the one-to-one chat", "OneToOneChat", "invalidUser");
        }

        var userA = chatMembers.get(0);
        var userB = chatMembers.get(1);

        var oneToOneChat = oneToOneChatService.startOneToOneChat(course, userA, userB);
        return ResponseEntity.created(new URI("/api/channels/" + oneToOneChat.getId())).body(oneToOneChatService.convertToDTO(oneToOneChat, requestingUser));
    }

}
