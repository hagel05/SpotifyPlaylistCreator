package org.hagelbrand.controller;

import org.hagelbrand.data.TrackCount;
import org.hagelbrand.service.setlistfm.ArtistSetlistPredictorService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(SetlistController.class)
@AutoConfigureMockMvc
class SetlistControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ArtistSetlistPredictorService artistSetlistPredictorService;

    @Test
    @WithMockUser
    void returnsTopTracksWithDefaultLimit() throws Exception {
        when(artistSetlistPredictorService.getTopTracksForArtist("Metallica", 20))
                .thenReturn(List.of(
                        new TrackCount("One", 10),
                        new TrackCount("Enter Sandman", 8)
                ));

        mockMvc.perform(get("/api/setlist/Metallica/top-tracks"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.trackCounts").isArray())
                .andExpect(jsonPath("$.trackCounts[0].track").value("One"))
                .andExpect(jsonPath("$.trackCounts[0].plays").value(10));

    }

    @Test
    @WithMockUser
    void respectsCustomLimit() throws Exception {
        when(artistSetlistPredictorService.getTopTracksForArtist("Metallica", 10))
                .thenReturn(List.of(
                        new TrackCount("One", 10)
                ));

        mockMvc.perform(get("/api/setlist/Metallica/top-tracks")
                        .param("limit", "10"))
                .andExpect(status().isOk());
    }
}
