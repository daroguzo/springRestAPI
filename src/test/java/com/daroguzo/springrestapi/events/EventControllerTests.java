package com.daroguzo.springrestapi.events;

import com.daroguzo.springrestapi.accounts.Account;
import com.daroguzo.springrestapi.accounts.AccountRepository;
import com.daroguzo.springrestapi.accounts.AccountService;
import com.daroguzo.springrestapi.accounts.Accountable;
import com.daroguzo.springrestapi.common.AppProperties;
import com.daroguzo.springrestapi.common.BaseControllerTest;
import com.daroguzo.springrestapi.common.TestDescription;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.hateoas.MediaTypes;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.oauth2.common.util.Jackson2JsonParser;
import org.springframework.test.web.servlet.ResultActions;

import java.time.LocalDateTime;
import java.util.Set;
import java.util.stream.IntStream;

import static org.springframework.restdocs.headers.HeaderDocumentation.*;
import static org.springframework.restdocs.hypermedia.HypermediaDocumentation.linkWithRel;
import static org.springframework.restdocs.hypermedia.HypermediaDocumentation.links;
import static org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.document;
import static org.springframework.restdocs.payload.PayloadDocumentation.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

public class EventControllerTests extends BaseControllerTest {

    @Autowired
    EventRepository eventRepository;

    @Autowired
    AccountService accountService;

    @Autowired
    AccountRepository accountRepository;

    @Autowired
    AppProperties appProperties;

    @BeforeEach
    public void setUp() {
        this.eventRepository.deleteAll();
        this.accountRepository.deleteAll();
    }

    @Test
    @DisplayName("??????????????? ????????? ???????????? ?????????")
    public void createEvent() throws Exception {
        EventDto event = EventDto.builder()
                .name("Spring")
                .description("REST API")
                .beginEnrollmentDateTime(LocalDateTime.of(2020, 5, 11, 20, 11))
                .closeEnrollmentDateTime(LocalDateTime.of(2020, 5, 12, 18, 20))
                .beginEventDateTime(LocalDateTime.of(2020, 5, 15, 18, 20))
                .endEventDateTime(LocalDateTime.of(2020, 5, 16, 18, 20))
                .basePrice(100)
                .maxPrice(200)
                .limitOfEnrollment(100)
                .location("????????? 5?????????")
                .build();

        mockMvc.perform(post("/api/events/")
                    .header(HttpHeaders.AUTHORIZATION, getBearerToken())
                    .contentType(MediaType.APPLICATION_JSON_VALUE)
                    .accept(MediaTypes.HAL_JSON)
                    .content(objectMapper.writeValueAsString(event)))
                .andDo(print())
                .andExpect(status().isCreated())
                .andExpect(jsonPath("id").exists())
                .andExpect(header().exists(HttpHeaders.LOCATION))
                .andExpect(header().string(HttpHeaders.CONTENT_TYPE, MediaTypes.HAL_JSON_VALUE))
                .andExpect(jsonPath("free").value(false))
                .andExpect(jsonPath("offline").value(true))
                .andExpect(jsonPath("eventStatus").value(EventStatus.DRAFT.name()))
                .andDo(document("create-event",
                        links(
                                linkWithRel("self").description("link to self"),
                                linkWithRel("query-events").description("link to query events"),
                                linkWithRel("update-event").description("link to update an existing event"),
                                linkWithRel("profile").description("link to profile")
                        ),
                        requestHeaders(
                                headerWithName(HttpHeaders.ACCEPT).description("accept header"),
                                headerWithName(HttpHeaders.CONTENT_TYPE).description("content type header")
                        ),
                        requestFields(
                                fieldWithPath("name").description("Name of new event"),
                                fieldWithPath("description").description("description of new event"),
                                fieldWithPath("beginEnrollmentDateTime").description("date time of begin of new event"),
                                fieldWithPath("closeEnrollmentDateTime").description("date time of close of new event"),
                                fieldWithPath("beginEventDateTime").description("date time of begin of new event"),
                                fieldWithPath("endEventDateTime").description("date time of end of new event"),
                                fieldWithPath("location").description("location of new event"),
                                fieldWithPath("basePrice").description("base price of new event"),
                                fieldWithPath("maxPrice").description("max price of new event"),
                                fieldWithPath("limitOfEnrollment").description("limit of new event")
                        ),
                        responseHeaders(
                                headerWithName(HttpHeaders.LOCATION).description("Location header"),
                                headerWithName(HttpHeaders.CONTENT_TYPE).description("Content type")
                        ),
                        requestFields(
                                fieldWithPath("id").description("identifier of new event"),
                                fieldWithPath("name").description("Name of new event"),
                                fieldWithPath("description").description("description of new event"),
                                fieldWithPath("beginEnrollmentDateTime").description("date time of begin of new event"),
                                fieldWithPath("closeEnrollmentDateTime").description("date time of close of new event"),
                                fieldWithPath("beginEventDateTime").description("date time of begin of new event"),
                                fieldWithPath("endEventDateTime").description("date time of end of new event"),
                                fieldWithPath("location").description("location of new event"),
                                fieldWithPath("basePrice").description("base price of new event"),
                                fieldWithPath("maxPrice").description("max price of new event"),
                                fieldWithPath("limitOfEnrollment").description("limit of new event"),
                                fieldWithPath("free").description("it tells if this event is free or not"),
                                fieldWithPath("offline").description("it tells if this offline event or not"),
                                fieldWithPath("eventStatus").description("eventStatus"),
                                fieldWithPath("_links.self.href").description("link to self"),
                                fieldWithPath("_links.query-events.href").description("link to query event list"),
                                fieldWithPath("_links.update-event.href").description("link to update existing event list"),
                                fieldWithPath("_links.profile.href").description("link to profile")


                        )
                ))
        ;
    }

    private String getBearerToken() throws Exception{
        return "Bearer " + getAccessToken(true);
    }

    private String getBearerToken(Boolean needToCreateAccount) throws Exception{
        return "Bearer " + getAccessToken(needToCreateAccount);
    }


    private String getAccessToken(boolean needToCreateAccount) throws Exception {
        // Given
        if (needToCreateAccount) {
            createAccount();
        }

        ResultActions perform = this.mockMvc.perform(post("/oauth/token")
                .with(httpBasic(appProperties.getClientId(), appProperties.getClientSecret()))
                .param("username", appProperties.getUserUsername())
                .param("password", appProperties.getUserPassword())
                .param("grant_type", "password"));

        var responseBody = perform.andReturn().getResponse().getContentAsString();
        Jackson2JsonParser parser = new Jackson2JsonParser();
        return parser.parseMap(responseBody).get("access_token").toString();
    }

    private Account createAccount() {
        Account daroguzo = Account.builder()
                .email(appProperties.getUserUsername())
                .password(appProperties.getUserPassword())
                .roles(Set.of(Accountable.ADMIN, Accountable.USER))
                .build();
        return this.accountService.saveAccount(daroguzo);
    }

    @Test
    @DisplayName("???????????? ??? ?????? ?????? ????????? ????????? ????????? ???????????? ?????????")
    public void createEvent_Bad_Request() throws Exception {
        Event event = Event.builder()
                .id(100)
                .name("Spring")
                .description("REST API")
                .beginEnrollmentDateTime(LocalDateTime.of(2020, 5, 11, 20, 11))
                .closeEnrollmentDateTime(LocalDateTime.of(2020, 5, 12, 18, 20))
                .beginEventDateTime(LocalDateTime.of(2020, 5, 15, 18, 20))
                .endEventDateTime(LocalDateTime.of(2020, 5, 16, 18, 20))
                .basePrice(100)
                .maxPrice(200)
                .limitOfEnrollment(100)
                .location("????????? 5?????????")
                .free(true)
                .offline(false)
                .eventStatus(EventStatus.PUBLISHED)
                .build();

        mockMvc.perform(post("/api/events/")
                .header(HttpHeaders.AUTHORIZATION, getBearerToken())
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .accept(MediaTypes.HAL_JSON)
                .content(objectMapper.writeValueAsString(event)))
                .andDo(print())
                .andExpect(status().isBadRequest())
        ;
    }

    @Test
    @TestDescription("?????? ?????? ???????????? ????????? ????????? ???????????? ?????????")
    public void createEvent_Bad_Request_Empty_Input() throws Exception {
        EventDto eventDto = EventDto.builder().build();

        this.mockMvc.perform(post("/api/events")
                .header(HttpHeaders.AUTHORIZATION, getBearerToken())
            .contentType(MediaType.APPLICATION_JSON_VALUE)
            .content(this.objectMapper.writeValueAsString(eventDto)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("?????? ?????? ????????? ????????? ????????? ???????????? ?????????")
    public void createEvent_Bad_Request_Wrong_Input() throws Exception {
        EventDto eventDto = EventDto.builder().name("Spring")
                .description("REST API")
                .beginEnrollmentDateTime(LocalDateTime.of(2020, 5, 11, 20, 11))
                .closeEnrollmentDateTime(LocalDateTime.of(2020, 5, 2, 18, 20))
                .beginEventDateTime(LocalDateTime.of(2020, 5, 15, 18, 20))
                .endEventDateTime(LocalDateTime.of(2020, 5, 3, 18, 20))
                .basePrice(10000)
                .maxPrice(200)
                .limitOfEnrollment(100)
                .location("????????? 5?????????")
                .build();

        this.mockMvc.perform(post("/api/events")
                .header(HttpHeaders.AUTHORIZATION, getBearerToken())
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .content(this.objectMapper.writeValueAsString(eventDto)))
                .andDo(print())
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("errors[0].objectName").exists())
                .andExpect(jsonPath("errors[0].defaultMessage").exists())
                .andExpect(jsonPath("errors[0].code").exists())
                .andExpect(jsonPath("_links.index").exists())
        ;

    }

    @Test
    @DisplayName("30?????? ???????????? 10?????? ????????? ????????? ????????????")
    public void queryEvents() throws Exception {
        // Given
        IntStream.range(0, 30).forEach(this::generateEvent);

        // When & Then
        this.mockMvc.perform(get("/api/events")
                    .param("page", "1")
                    .param("size", "10")
                    .param("sort", "name,DESC")
                )
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("page").exists())
                .andExpect(jsonPath("_embedded.eventList[0]._links.self").exists())
                .andExpect(jsonPath("_links.self").exists())
                .andExpect(jsonPath("_links.profile").exists())
                .andDo(document("query-events"))
        ;
    }

    @Test
    @DisplayName("????????? ????????? 30?????? ???????????? 10?????? ????????? ????????? ????????????")
    public void queryEventsWithAuthentication() throws Exception {
        // Given
        IntStream.range(0, 30).forEach(this::generateEvent);

        // When & Then
        this.mockMvc.perform(get("/api/events")
                .header(HttpHeaders.AUTHORIZATION, getBearerToken())
                .param("page", "1")
                .param("size", "10")
                .param("sort", "name,DESC"))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("page").exists())
                .andExpect(jsonPath("_embedded.eventList[0]._links.self").exists())
                .andExpect(jsonPath("_links.self").exists())
                .andExpect(jsonPath("_links.profile").exists())
                .andExpect(jsonPath("_links.create-event").exists())
                .andDo(document("query-events"))
        ;
    }

    @Test
    @DisplayName("????????? ???????????? ?????? ????????????")
    public void getEvent() throws Exception {
        // Given
        Account account = this.createAccount();
        Event event = this.generateEvent(100, account);
        // When & Then
        this.mockMvc.perform(get("/api/events/{id}", event.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("name").exists())
                .andExpect(jsonPath("id").exists())
                .andExpect(jsonPath("_links.self").exists())
                .andExpect(jsonPath("_links.profile").exists())
                .andDo(document("get-an-event"))
        ;
    }

    @Test
    @DisplayName("?????? ???????????? ??????????????? 404 ????????????")
    public void getEvent404() throws Exception {
        // When & Then
        this.mockMvc.perform(get("/api/events/1181238"))
                .andExpect(status().isNotFound())
        ;
    }

    @Test
    @DisplayName("???????????? ??????????????? ????????????")
    public void updateEvent() throws Exception {
        // Given
        Account account = this.createAccount();
        Event event = this.generateEvent(200, account);

        EventDto eventDto = this.modelMapper.map(event, EventDto.class);
        String eventName = "Updated Event";
        eventDto.setName(eventName);

        // When & Then
        this.mockMvc.perform(put("/api/events/{id}", event.getId())
                        .header(HttpHeaders.AUTHORIZATION, getBearerToken(false))
                        .contentType(MediaType.APPLICATION_JSON_VALUE)
                        .content(this.objectMapper.writeValueAsString(eventDto))
                    )
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("name").value(eventName))
                .andExpect(jsonPath("_links.self").exists())
                .andDo(document("update-event"))
        ;
    }

    @Test
    @DisplayName("???????????? ???????????? ????????? ????????? ?????? ??????")
    public void updateEvent400_Empty() throws Exception {
        // Given
        Event event = this.generateEvent(200);

        EventDto eventDto = new EventDto();

        // When & Then
        this.mockMvc.perform(put("/api/events/{id}", event.getId())
                        .header(HttpHeaders.AUTHORIZATION, getBearerToken())
                        .contentType(MediaType.APPLICATION_JSON_VALUE)
                        .content(this.objectMapper.writeValueAsString(eventDto))
                    )
                .andDo(print())
                .andExpect(status().isBadRequest())

        ;
    }

    @Test
    @DisplayName("???????????? ????????? ????????? ????????? ?????? ??????")
    public void updateEvent400_Wrong() throws Exception {
        // Given
        Event event = this.generateEvent(200);

        EventDto eventDto = this.modelMapper.map(event, EventDto.class);
        eventDto.setBasePrice(20000);
        eventDto.setMaxPrice(1000);

        // When & Then
        this.mockMvc.perform(put("/api/events/{id}", event.getId())
                .header(HttpHeaders.AUTHORIZATION, getBearerToken())
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .content(this.objectMapper.writeValueAsString(eventDto))
        )
                .andDo(print())
                .andExpect(status().isBadRequest())

        ;
    }

    @Test
    @DisplayName("???????????? ?????? ????????? ?????? ??????")
    public void updateEvent404() throws Exception {
        // Given
        Event event = this.generateEvent(200);

        EventDto eventDto = this.modelMapper.map(event, EventDto.class);

        // When & Then
        this.mockMvc.perform(put("/api/events/123123")
                .header(HttpHeaders.AUTHORIZATION, getBearerToken())
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .content(this.objectMapper.writeValueAsString(eventDto))
        )
                .andDo(print())
                .andExpect(status().isNotFound())

        ;
    }

    private Event generateEvent(int index, Account account) {
        Event event = buildEvent(index);
        event.setManager(account);
        return this.eventRepository.save(event);
    }

    private Event generateEvent(int index) {
        Event event = buildEvent(index);
        return this.eventRepository.save(event);
    }

    private Event buildEvent(int index) {
        return Event.builder()
                    .name("event " + index)
                    .description("test event")
                    .beginEnrollmentDateTime(LocalDateTime.of(2020, 5, 11, 20, 11))
                    .closeEnrollmentDateTime(LocalDateTime.of(2020, 5, 12, 18, 20))
                    .beginEventDateTime(LocalDateTime.of(2020, 5, 15, 18, 20))
                    .endEventDateTime(LocalDateTime.of(2020, 5, 16, 18, 20))
                    .basePrice(100)
                    .maxPrice(200)
                    .limitOfEnrollment(100)
                    .location("????????? 5?????????")
                    .free(false)
                    .offline(true)
                    .eventStatus(EventStatus.DRAFT)
                    .build();
    }
}
