package com.rewedigital.composer.session;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import com.spotify.apollo.Request;
import com.spotify.apollo.Response;

/**
 * Describes the <em>root</em> session of a request. The root session is
 * constructed from a data map, can be written as a set of http headers to a
 * request and can be updated ({@link #mergedWith(SessionFragment)}) with data
 * from a {@link SessionFragment}. A root session is dirty if after a merge the
 * data has changed.
 *
 * A root session can be written to a response using an instance of a
 * {@link SessionRoot.Serializer}.
 *
 */
public class SessionRoot {

	public interface Serializer {
		<T> Response<T> writeTo(final Response<T> response, final Map<String, String> sessionData, boolean dirty);
	}

	private static final String sessionIdKey = "session-id";
	private static final SessionRoot emptySession = new SessionRoot(new HashMap<>(), false);

	private final SessionData data;
	private final boolean dirty;

	private SessionRoot(final Map<String, String> data, final boolean dirty) {
		this(new SessionData(data), dirty);
	}

	private SessionRoot(final SessionData data, final boolean dirty) {
		this.data = data;
		this.dirty = dirty;
	}

	public static boolean isSessionEntry(Map.Entry<String, String> entry) {
		return SessionData.isSessionEntry(entry);
	}

	public static SessionRoot empty() {
		return emptySession;
	}

	public static SessionRoot of(final Map<String, String> data) {
		return of(data, false);
	}

	public static SessionRoot of(final Map<String, String> data, final boolean dirty) {
		return new SessionRoot(data, dirty);
	}

	public Request enrich(final Request request) {
		return request.withHeaders(asHeaders());
	}

	public Optional<String> get(final String key) {
		return data.get(key);
	}

	public Optional<String> getId() {
		return get(sessionIdKey);
	}

	public <T> Response<T> writeTo(final Response<T> response, final Serializer serializer) {
		return serializer.writeTo(response, asHeaders(), dirty);
	}

	public SessionRoot mergedWith(final SessionFragment other) {
		final SessionData mergedData = data.mergedWith(other.data());
		final SessionData newData = getId().map(id -> mergedData.with(sessionIdKey, id)).orElse(mergedData);
		final boolean newDirty = !data.equals(newData);
		return new SessionRoot(newData, newDirty || dirty);
	}

	public SessionRoot mergedWith(final Map<String, String> sessionValues) {
		final SessionData mergedData = data.mergedWith(new SessionData(sessionValues));
		return of(mergedData.rawData());
	}

	public SessionRoot withId(final String sessionId) {
		return new SessionRoot(data.with(sessionIdKey, sessionId), true);
	}

	public boolean isDirty() {
		return dirty;
	}

	public Map<String, String> rawData() {
		return data.rawData();
	}

	private Map<String, String> asHeaders() {
		return data.asHeaders();
	}
}
