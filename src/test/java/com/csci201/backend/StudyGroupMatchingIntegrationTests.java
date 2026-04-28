package com.csci201.backend;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Objects;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("h2")
class StudyGroupMatchingIntegrationTests {

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void suggestionsEndpointReturnsRankedMatches() throws Exception {
        mockMvc.perform(get("/matches/suggestions")
                        .param("userId", "1")
                        .param("limit", "2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$[0].userId").exists())
                .andExpect(jsonPath("$[0].compatibilityScore").exists());
    }

    @Test
    void createJoinInviteAndAcceptGroupFlowWorks() throws Exception {
        String createPayload = """
                {
                  "ownerUserId": 1,
                  "groupName": "CSCI 201 Night Study",
                  "description": "Evening prep sessions",
                  "visibility": "PUBLIC",
                  "primaryCourse": "CSCI 201"
                }
                """;

        String createResponse = mockMvc.perform(post("/study-groups")
                        .contentType("application/json")
                        .content(createPayload))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.groupId").exists())
                .andExpect(jsonPath("$.members[0].userId").value(1))
                .andReturn()
                .getResponse()
                .getContentAsString();

        JsonNode createJson = objectMapper.readTree(createResponse);
        long groupId = createJson.get("groupId").asLong();

        mockMvc.perform(post("/study-groups/{groupId}/join", groupId)
                        .contentType("application/json")
                        .content("{\"userId\":2}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.members[?(@.userId==2)]").exists());

        String inviteResponse = mockMvc.perform(post("/study-groups/{groupId}/invites", groupId)
                        .contentType("application/json")
                        .content("{\"invitedByUserId\":1,\"invitedUserId\":4}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.invitationId").exists())
                .andExpect(jsonPath("$.status").value("PENDING"))
                .andReturn()
                .getResponse()
                .getContentAsString();

        JsonNode inviteJson = objectMapper.readTree(inviteResponse);
        long inviteId = inviteJson.get("invitationId").asLong();

        mockMvc.perform(post("/study-groups/invites/{inviteId}/accept", inviteId)
                        .contentType("application/json")
                        .content("{\"userId\":4}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ACCEPTED"));
    }

    @Test
    void groupReservationRequiresMembership() throws Exception {
        String createPayload = """
                {
                  "ownerUserId": 1,
                  "groupName": "Reservation Guardrail Group",
                  "description": "Test group",
                  "visibility": "PUBLIC",
                  "primaryCourse": "CSCI 270"
                }
                """;
        String createResponse = mockMvc.perform(post("/study-groups")
                        .contentType("application/json")
                        .content(createPayload))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();
        JsonNode createJson = objectMapper.readTree(createResponse);
        long groupId = createJson.get("groupId").asLong();

        String bookingPayload = """
                {
                  "groupId": %d,
                  "bookedByUserId": 2,
                  "roomId": 1,
                  "startTime": "2030-01-01T08:00:00Z",
                  "endTime": "2030-01-01T09:00:00Z"
                }
                """.formatted(groupId);

        mockMvc.perform(post("/group-reservations")
                        .contentType("application/json")
                        .content(Objects.requireNonNull(bookingPayload)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Only group members can book rooms for this group"));
    }
}
