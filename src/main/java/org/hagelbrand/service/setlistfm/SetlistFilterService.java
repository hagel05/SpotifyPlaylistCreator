package org.hagelbrand.service.setlistfm;

import org.hagelbrand.data.SetlistSearchResponse.Setlist;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class SetlistFilterService {

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
        return TV_KEYWORDS.stream().anyMatch(value::contains);
    }

    private String lower(String value) {
        return value == null ? "" : value.toLowerCase();
    }
}