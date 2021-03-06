package com.rewedigital.composer.session;

import static java.util.stream.Collectors.toList;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import com.rewedigital.composer.composing.Composable;
import com.spotify.apollo.Response;

/**
 * Describes a fragment of a session that is constructed from http headers of a
 * (template or content) response. Session fragments can be merged to construct
 * a combined fragment.
 */
 public class SessionFragment implements Composable<SessionFragment> {

    private static final SessionFragment emptySession = new SessionFragment(new HashMap<>());

    final SessionData data;

    private SessionFragment(final Map<String, String> data) {
        this(new SessionData(data));
    }

    private SessionFragment(final SessionData data) {
        this.data = data;
    }

    public static SessionFragment empty() {
        return emptySession;
    }

    public static <T> SessionFragment of(final Response<T> response) {
        final List<Map.Entry<String, String>> data = response.headerEntries().stream()
                .filter(SessionData::isSessionEntry).collect(toList());
        return new SessionFragment(toMap(data));
    }

    public Optional<String> get(final String key) {
        return data.get(key);
    }

    public SessionFragment composedWith(final SessionFragment other) {
        return new SessionFragment(data.mergedWith(other.data));
    }

    private static Map<String, String> toMap(final List<Map.Entry<String, String>> entries) {
        final Map<String, String> result = new HashMap<String, String>();
        // not using Collectors.toMap here due to IllegalStateException if duplicate key
        for (final Map.Entry<String, String> entry : entries) {
            result.put(entry.getKey(), entry.getValue());
        }
        return result;
    }
}
