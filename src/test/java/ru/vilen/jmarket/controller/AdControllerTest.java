package ru.vilen.jmarket.controller;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import ru.vilen.jmarket.dto.*;
import ru.vilen.jmarket.entity.AdStatus;
import ru.vilen.jmarket.exception.AdAccessDeniedException;
import ru.vilen.jmarket.exception.AdNotFoundException;
import ru.vilen.jmarket.exception.UserNotFoundException;
import ru.vilen.jmarket.service.AdService;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AdController.class)
@AutoConfigureMockMvc(addFilters = false)
class AdControllerTest {

    private static final String AUTHOR_EMAIL = "ivan@example.com";
    private static final UsernamePasswordAuthenticationToken AUTHENTICATION =
            UsernamePasswordAuthenticationToken.authenticated(AUTHOR_EMAIL, null, List.of());

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AdService adService;

    @Test
    void create_shouldReturnAdResponse_whenRequestIsValid() throws Exception {
        AdResponse response = new AdResponse(
                10L,
                "Phone",
                "Good phone",
                new BigDecimal("15000.00"),
                1L,
                AdStatus.ACTIVE
        );

        when(adService.create(any(AdCreateRequest.class), eq(AUTHOR_EMAIL)))
                .thenReturn(response);

        mockMvc.perform(post("/api/ads")
                        .principal(AUTHENTICATION)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "title": "Phone",
                                  "description": "Good phone",
                                  "price": 15000.00
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(10))
                .andExpect(jsonPath("$.title").value("Phone"))
                .andExpect(jsonPath("$.description").value("Good phone"))
                .andExpect(jsonPath("$.price").value(15000.00))
                .andExpect(jsonPath("$.authorId").value(1))
                .andExpect(jsonPath("$.status").value("ACTIVE"));

        ArgumentCaptor<AdCreateRequest> requestCaptor =
                ArgumentCaptor.forClass(AdCreateRequest.class);

        verify(adService).create(requestCaptor.capture(), eq(AUTHOR_EMAIL));

        AdCreateRequest actualRequest = requestCaptor.getValue();

        assertThat(actualRequest.title()).isEqualTo("Phone");
        assertThat(actualRequest.description()).isEqualTo("Good phone");
        assertThat(actualRequest.price()).isEqualByComparingTo("15000.00");
    }

    @Test
    void create_shouldReturnBadRequest_whenTitleIsBlank() throws Exception {
        mockMvc.perform(post("/api/ads")
                        .principal(AUTHENTICATION)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "title": "",
                                  "description": "Good phone",
                                  "price": 15000.00
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.message")
                        .value("Validation failed for one or more arguments"));

        verify(adService, never())
                .create(any(AdCreateRequest.class), anyString());
    }

    @Test
    void create_shouldReturnNotFound_whenAuthorDoesNotExist() throws Exception {
        when(adService.create(any(AdCreateRequest.class), eq(AUTHOR_EMAIL)))
                .thenThrow(new UserNotFoundException(AUTHOR_EMAIL));

        mockMvc.perform(post("/api/ads")
                        .principal(AUTHENTICATION)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "title": "Phone",
                                  "description": "Good phone",
                                  "price": 15000.00
                                }
                                """))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.message")
                        .value("User with email ivan@example.com not found"));

        verify(adService).create(any(AdCreateRequest.class), eq(AUTHOR_EMAIL));
    }

    @Test
    void findById_shouldReturnAdResponse_whenAdExists() throws Exception {
        AdResponse response = new AdResponse(
                1L,
                "Phone",
                "Good phone",
                new BigDecimal("15000.00"),
                1L,
                AdStatus.ACTIVE
        );

        when(adService.findById(1L))
                .thenReturn(response);

        mockMvc.perform(get("/api/ads/1")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.title").value("Phone"))
                .andExpect(jsonPath("$.description").value("Good phone"))
                .andExpect(jsonPath("$.price").value(15000.00))
                .andExpect(jsonPath("$.authorId").value(1))
                .andExpect(jsonPath("$.status").value("ACTIVE"));

        verify(adService).findById(1L);
    }

    @Test
    void findById_shouldReturnNotFound_whenAdDoesNotExist() throws Exception {
        when(adService.findById(999L))
                .thenThrow(new AdNotFoundException(999L));

        mockMvc.perform(get("/api/ads/999")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.message")
                        .value("Ad with ID 999 not found"));

        verify(adService).findById(999L);
    }

    @Test
    void findAll_shouldReturnAdPageResponse_whenAdsExist() throws Exception {
        AdPageResponse response = new AdPageResponse(
                List.of(
                        new AdResponse(
                                1L,
                                "Phone",
                                "Good phone",
                                new BigDecimal("15000.00"),
                                1L,
                                AdStatus.ACTIVE
                        )
                ),
                0,
                20,
                1,
                1,
                true,
                true
        );

        when(adService.findAll(
                any(Pageable.class),
                any(AdFilter.class)
        )).thenReturn(response);

        mockMvc.perform(get("/api/ads")
                        .accept(MediaType.APPLICATION_JSON)
                        .param("page", "0")
                        .param("size", "20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.content[0].id").value(1))
                .andExpect(jsonPath("$.content[0].title").value("Phone"))
                .andExpect(jsonPath("$.content[0].description").value("Good phone"))
                .andExpect(jsonPath("$.content[0].price").value(15000.00))
                .andExpect(jsonPath("$.content[0].authorId").value(1))
                .andExpect(jsonPath("$.content[0].status").value("ACTIVE"))
                .andExpect(jsonPath("$.page").value(0))
                .andExpect(jsonPath("$.size").value(20))
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.totalPages").value(1))
                .andExpect(jsonPath("$.first").value(true))
                .andExpect(jsonPath("$.last").value(true));


        verify(adService).findAll(
                any(Pageable.class),
                any(AdFilter.class)
        );
    }

    @Test
    void findAll_shouldReturnEmptyPage_whenAdsDoNotExist() throws Exception {
        AdPageResponse response = new AdPageResponse(
                List.of(),
                0,
                20,
                0,
                0,
                true,
                true
        );

        when(adService.findAll(
                any(Pageable.class),
                any(AdFilter.class)
        )).thenReturn(response);

        mockMvc.perform(get("/api/ads")
                        .accept(MediaType.APPLICATION_JSON)
                        .param("page", "0")
                        .param("size", "20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(0))
                .andExpect(jsonPath("$.page").value(0))
                .andExpect(jsonPath("$.size").value(20))
                .andExpect(jsonPath("$.totalElements").value(0))
                .andExpect(jsonPath("$.totalPages").value(0))
                .andExpect(jsonPath("$.first").value(true))
                .andExpect(jsonPath("$.last").value(true));

        ArgumentCaptor<Pageable> captor =
                ArgumentCaptor.forClass(Pageable.class);
        verify(adService).findAll(
                captor.capture(),
                any(AdFilter.class)
        );

        Pageable actual = captor.getValue();
        assertThat(actual.getPageNumber()).isZero();
        assertThat(actual.getPageSize()).isEqualTo(20);
    }

    @Test
    void findAll_shouldUseDefaultPaginationAndSorting_whenParametersAreNotProvided() throws Exception {
        AdPageResponse response = new AdPageResponse(
                List.of(),
                0,
                20,
                0,
                0,
                true,
                true
        );

        when(adService.findAll(
                any(Pageable.class),
                any(AdFilter.class)
        )).thenReturn(response);

        mockMvc.perform(get("/api/ads")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());

        ArgumentCaptor<Pageable> captor =
                ArgumentCaptor.forClass(Pageable.class);
        verify(adService).findAll(
                captor.capture(),
                any(AdFilter.class)
        );

        Pageable actual = captor.getValue();
        assertThat(actual.getPageNumber()).isZero();
        assertThat(actual.getPageSize()).isEqualTo(20);

        Sort.Order idOrder = actual.getSort().getOrderFor("id");
        assertThat(idOrder).isNotNull();
        assertThat(idOrder.getDirection()).isEqualTo(Sort.Direction.DESC);
    }

    @Test
    void findAll_shouldLimitPageSize_whenRequestedSizeExceedsMaximum() throws Exception {
        AdPageResponse response = new AdPageResponse(
                List.of(),
                0,
                100,
                0,
                0,
                true,
                true
        );

        when(adService.findAll(
                any(Pageable.class),
                any(AdFilter.class)
        )).thenReturn(response);

        mockMvc.perform(get("/api/ads")
                        .param("size", "500")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());

        ArgumentCaptor<Pageable> captor =
                ArgumentCaptor.forClass(Pageable.class);
        verify(adService).findAll(
                captor.capture(),
                any(AdFilter.class)
        );

        Pageable actual = captor.getValue();
        assertThat(actual.getPageSize()).isEqualTo(100);
    }

    @Test
    void findAll_shouldBindPriceFilters_whenFilterParametersAreProvided() throws Exception {
        AdPageResponse response = new AdPageResponse(
                List.of(),
                0,
                20,
                0,
                0,
                true,
                true
        );

        when(adService.findAll(
                any(Pageable.class),
                any(AdFilter.class)
        )).thenReturn(response);

        mockMvc.perform(get("/api/ads")
                        .param("minPrice", "1000.00")
                        .param("maxPrice", "50000.00")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());

        ArgumentCaptor<AdFilter> filterCaptor =
                ArgumentCaptor.forClass(AdFilter.class);

        verify(adService).findAll(
                any(Pageable.class),
                filterCaptor.capture()
        );

        AdFilter actualFilter = filterCaptor.getValue();
        assertThat(actualFilter.minPrice())
                .isEqualByComparingTo("1000.00");
        assertThat(actualFilter.maxPrice())
                .isEqualByComparingTo("50000.00");
    }

    @Test
    void findAll_shouldReturnBadRequest_whenMinPriceIsNegative() throws Exception {
        mockMvc.perform(get("/api/ads")
                        .param("minPrice", "-1000.00")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.message")
                        .value("Validation failed for one or more arguments"));

        verify(adService, never()).findAll(
                any(Pageable.class),
                any(AdFilter.class)
        );
    }

    @Test
    void findAll_shouldBindAuthorIdFilter_whenAuthorIdParameterIsProvided() throws Exception {
        AdPageResponse response = new AdPageResponse(
                List.of(),
                0,
                20,
                0,
                0,
                true,
                true
        );

        when(adService.findAll(
                any(Pageable.class),
                any(AdFilter.class)
        )).thenReturn(response);

        mockMvc.perform(get("/api/ads")
                        .param("authorId", "1")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());

        ArgumentCaptor<AdFilter> filterCaptor =
                ArgumentCaptor.forClass(AdFilter.class);

        verify(adService).findAll(
                any(Pageable.class),
                filterCaptor.capture()
        );

        AdFilter actualFilter = filterCaptor.getValue();
        assertThat(actualFilter.authorId())
                .isEqualTo(1L);
    }

    @Test
    void findAll_shouldReturnBadRequest_whenAuthorIdIsNotPositive() throws Exception {
        mockMvc.perform(get("/api/ads")
                        .param("authorId", "-1")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.message")
                        .value("Validation failed for one or more arguments"));

        verify(adService, never()).findAll(
                any(Pageable.class),
                any(AdFilter.class)
        );
    }

    @Test
    void findAll_shouldBindQueryFilter_whenQueryParameterIsProvided() throws Exception {
        AdPageResponse response = new AdPageResponse(
                List.of(),
                0,
                20,
                0,
                0,
                true,
                true
        );

        when(adService.findAll(
                any(Pageable.class),
                any(AdFilter.class)
        )).thenReturn(response);

        mockMvc.perform(get("/api/ads")
                        .param("query", "phone")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());

        ArgumentCaptor<AdFilter> filterCaptor =
                ArgumentCaptor.forClass(AdFilter.class);

        verify(adService).findAll(
                any(Pageable.class),
                filterCaptor.capture()
        );

        AdFilter actualFilter = filterCaptor.getValue();
        assertThat(actualFilter.query())
                .isEqualTo("phone");
    }

    @Test
    void findAll_shouldReturnBadRequest_whenStatusIsUnknown() throws Exception {
        mockMvc.perform(get("/api/ads")
                        .param("status", "abc")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.message")
                        .value("Validation failed for one or more arguments"));

        verify(adService, never()).findAll(
                any(Pageable.class),
                any(AdFilter.class)
        );
    }

    @Test
    void update_shouldReturnAdResponse_whenRequestIsValid() throws Exception {
        AdResponse response = new AdResponse(
                1L,
                "Updated Phone",
                "Updated description",
                new BigDecimal("20000.00"),
                1L,
                AdStatus.ACTIVE
        );

        when(adService.update(eq(1L), any(AdUpdateRequest.class), eq(AUTHOR_EMAIL)))
                .thenReturn(response);

        mockMvc.perform(put("/api/ads/1")
                        .principal(AUTHENTICATION)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "title": "Updated Phone",
                                  "description": "Updated description",
                                  "price": 20000.00
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.title").value("Updated Phone"))
                .andExpect(jsonPath("$.description").value("Updated description"))
                .andExpect(jsonPath("$.price").value(20000.00))
                .andExpect(jsonPath("$.authorId").value(1))
                .andExpect(jsonPath("$.status").value("ACTIVE"));

        ArgumentCaptor<AdUpdateRequest> requestCaptor =
                ArgumentCaptor.forClass(AdUpdateRequest.class);

        verify(adService)
                .update(eq(1L), requestCaptor.capture(), eq(AUTHOR_EMAIL));

        AdUpdateRequest actualRequest = requestCaptor.getValue();

        assertThat(actualRequest.title()).isEqualTo("Updated Phone");
        assertThat(actualRequest.description()).isEqualTo("Updated description");
        assertThat(actualRequest.price()).isEqualByComparingTo("20000.00");
    }

    @Test
    void update_shouldReturnNotFound_whenAdDoesNotExist() throws Exception {
        when(adService.update(eq(999L), any(AdUpdateRequest.class), eq(AUTHOR_EMAIL)))
                .thenThrow(new AdNotFoundException(999L));

        mockMvc.perform(put("/api/ads/999")
                        .principal(AUTHENTICATION)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "title": "Updated Phone",
                                  "description": "Updated description",
                                  "price": 20000.00
                                }
                                """))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.message")
                        .value("Ad with ID 999 not found"));

        verify(adService)
                .update(eq(999L), any(AdUpdateRequest.class), eq(AUTHOR_EMAIL));
    }

    @Test
    void update_shouldReturnBadRequest_whenTitleIsBlank() throws Exception {
        mockMvc.perform(put("/api/ads/1")
                        .principal(AUTHENTICATION)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "title": "",
                                  "description": "Updated description",
                                  "price": 20000.00
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.message")
                        .value("Validation failed for one or more arguments"));

        verify(adService, never())
                .update(eq(1L), any(AdUpdateRequest.class), eq(AUTHOR_EMAIL));
    }

    @Test
    void update_shouldReturnForbidden_whenUserIsNotAuthor() throws Exception {
        when(adService.update(eq(1L), any(AdUpdateRequest.class), eq(AUTHOR_EMAIL)))
                .thenThrow(new AdAccessDeniedException(
                        "You do not have permission to manage ad with ID 1"
                ));

        mockMvc.perform(put("/api/ads/1")
                        .principal(AUTHENTICATION)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "title": "Updated Phone",
                                  "description": "Updated description",
                                  "price": 20000.00
                                }
                                """))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403))
                .andExpect(jsonPath("$.message")
                        .value("You do not have permission to manage ad with ID 1"));

        verify(adService)
                .update(eq(1L), any(AdUpdateRequest.class), eq(AUTHOR_EMAIL));
    }

    @Test
    void updateStatus_shouldReturnAdResponse_whenRequestIsValid() throws Exception {
        AdResponse response = new AdResponse(
                1L,
                "Phone",
                "Good phone",
                new BigDecimal("15000.00"),
                1L,
                AdStatus.SOLD
        );

        when(adService.updateStatus(
                eq(1L),
                any(AdStatusUpdateRequest.class),
                eq(AUTHOR_EMAIL)
        )).thenReturn(response);

        mockMvc.perform(patch("/api/ads/1/status")
                        .principal(AUTHENTICATION)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "status": "SOLD"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.status").value("SOLD"));

        ArgumentCaptor<AdStatusUpdateRequest> requestCaptor =
                ArgumentCaptor.forClass(AdStatusUpdateRequest.class);

        verify(adService).updateStatus(
                eq(1L),
                requestCaptor.capture(),
                eq(AUTHOR_EMAIL)
        );
        assertThat(requestCaptor.getValue().status()).isEqualTo(AdStatus.SOLD);
    }

    @Test
    void updateStatus_shouldReturnBadRequest_whenStatusIsMissing() throws Exception {
        mockMvc.perform(patch("/api/ads/1/status")
                        .principal(AUTHENTICATION)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.message")
                        .value("Validation failed for one or more arguments"));

        verifyNoInteractions(adService);
    }

    @Test
    void updateStatus_shouldReturnBadRequest_whenStatusIsUnknown() throws Exception {
        mockMvc.perform(patch("/api/ads/1/status")
                        .principal(AUTHENTICATION)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "status": "PUBLISHED"
                                }
                                """))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(adService);
    }

    @Test
    void updateStatus_shouldReturnNotFound_whenAdDoesNotExist() throws Exception {
        when(adService.updateStatus(
                eq(999L),
                any(AdStatusUpdateRequest.class),
                eq(AUTHOR_EMAIL)
        )).thenThrow(new AdNotFoundException(999L));

        mockMvc.perform(patch("/api/ads/999/status")
                        .principal(AUTHENTICATION)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "status": "SOLD"
                                }
                                """))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.message")
                        .value("Ad with ID 999 not found"));

        verify(adService).updateStatus(
                eq(999L),
                any(AdStatusUpdateRequest.class),
                eq(AUTHOR_EMAIL)
        );
    }

    @Test
    void updateStatus_shouldReturnForbidden_whenUserIsNotAuthor() throws Exception {
        when(adService.updateStatus(
                eq(1L),
                any(AdStatusUpdateRequest.class),
                eq(AUTHOR_EMAIL)
        )).thenThrow(new AdAccessDeniedException(
                "You do not have permission to manage ad with ID 1"
        ));

        mockMvc.perform(patch("/api/ads/1/status")
                        .principal(AUTHENTICATION)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "status": "SOLD"
                                }
                                """))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403))
                .andExpect(jsonPath("$.message")
                        .value("You do not have permission to manage ad with ID 1"));

        verify(adService).updateStatus(
                eq(1L),
                any(AdStatusUpdateRequest.class),
                eq(AUTHOR_EMAIL)
        );
    }

    @Test
    void delete_shouldReturnNoContent_whenAdExists() throws Exception {
        mockMvc.perform(delete("/api/ads/1")
                        .principal(AUTHENTICATION))
                .andExpect(status().isNoContent());

        verify(adService).delete(1L, AUTHOR_EMAIL);
    }

    @Test
    void delete_shouldReturnNotFound_whenAdDoesNotExist() throws Exception {
        doThrow(new AdNotFoundException(999L))
                .when(adService).delete(999L, AUTHOR_EMAIL);

        mockMvc.perform(delete("/api/ads/999")
                        .principal(AUTHENTICATION))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.message")
                        .value("Ad with ID 999 not found"));

        verify(adService).delete(999L, AUTHOR_EMAIL);
    }

    @Test
    void delete_shouldReturnForbidden_whenUserIsNotAuthor() throws Exception {
        doThrow(new AdAccessDeniedException(
                "You do not have permission to manage ad with ID 1"
        )).when(adService).delete(1L, AUTHOR_EMAIL);

        mockMvc.perform(delete("/api/ads/1")
                        .principal(AUTHENTICATION))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403))
                .andExpect(jsonPath("$.message")
                        .value("You do not have permission to manage ad with ID 1"));

        verify(adService).delete(1L, AUTHOR_EMAIL);
    }
}
