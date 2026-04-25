package org.hagelbrand.service.setlistfm;

import org.hagelbrand.controller.PlaylistController;
import org.hagelbrand.data.SetlistSearchResponse.Setlist;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class SetlistFilterService {
    private static final Logger log = LoggerFactory.getLogger(SetlistFilterService.class);

    private static final List<String> TV_KEYWORDS = List.of(
            "good morning america",
            "tonight show",
            "jimmy kimmel",
            "american idol",
            "late show",
            "tv",
            "television"
    );

    public boolean isVisibleConcert(Setlist setlist) {
        return !isTvOrPromo(setlist);
    }

    private boolean isTvOrPromo(Setlist setlist) {
        String venueName = lower(setlist.venue().name());
        String info = lower(setlist.info());
        String venueUrl = lower(setlist.venue().url());

        return containsAny(venueName)
                || containsAny(info)
                || containsAny(venueUrl);
    }

    private boolean containsAny(String value) {
        // Short-circuit on first match instead of streaming through all keywords
        for (String keyword : TV_KEYWORDS) {
            if (value.contains(keyword)) {
                return true;
            }
        }
        return false;
    }

    private String lower(String value) {
        return value == null ? "" : value.toLowerCase();
    }
}