package com.rewedigital.composer.session;

import static com.rewedigital.composer.helper.Sessions.cleanSessionRoot;
import static com.rewedigital.composer.helper.Sessions.dirtySessionRoot;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Collections;
import java.util.Optional;

import org.junit.Test;

import com.rewedigital.composer.application.DefaultConfiguration;
import com.rewedigital.composer.session.CookieBasedSessionHandler;
import com.rewedigital.composer.session.SessionConfiguration;
import com.rewedigital.composer.session.SessionHandler;
import com.rewedigital.composer.session.SessionRoot;
import com.spotify.apollo.Request;
import com.spotify.apollo.RequestContext;
import com.spotify.apollo.Response;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigValueFactory;

import io.jsonwebtoken.SignatureAlgorithm;

public class CookieBasedSessionHandlerTest {

    @Test
    public void shouldWriteAndReadCookie() throws Exception {
        final String cookieHeader = dirtySessionRoot("x-rd-key", "value", sessionHandler()).writtenTo(Response.ok())
                .header("Set-Cookie").get();
        final SessionRoot session = sessionHandler()
                .initialize(contextFor(Request.forUri("/").withHeader("Cookie", cookieHeader))).toCompletableFuture()
                .get();
        assertThat(session.get("key")).contains("value");
    }

    @Test
    public void shouldCreateNewSessionIfNonePresent() throws Exception {
        final SessionRoot session = sessionHandler().initialize(contextFor(Request.forUri("/"))).toCompletableFuture()
                .get();
        assertThat(session).isNotNull();
    }

    @Test
    public void shouldNotWriteSessionCookieIfSessionIsNotDirty() {
        final Optional<String> setCookieHeader = cleanSessionRoot("x-rd-key", "value", sessionHandler())
                .writtenTo(Response.ok()).header("Set-Cookie");
        assertThat(setCookieHeader).isEmpty();
    }

    @Test
    public void shouldBeConstructedWithFactory() {
        final SessionHandler result = CookieBasedSessionHandler.create(enabledConfig());
        assertThat(result).isInstanceOf(CookieBasedSessionHandler.class);
    }

    @Test
    public void shouldConstructNoopInstanceIfSessionHandlingIsDisabled() {
        final SessionHandler result = CookieBasedSessionHandler.create(disabledConfig());
        assertThat(result).isEqualTo(SessionHandler.noSession());
    }

    private CookieBasedSessionHandler sessionHandler() {
        return new CookieBasedSessionHandler(configuration());
    }

    private RequestContext contextFor(final Request request) {
        final RequestContext context = mock(RequestContext.class);
        when(context.request()).thenReturn(request);
        return context;
    }

    private SessionConfiguration configuration() {
        return new SessionConfiguration(true, "sessioncookie", SignatureAlgorithm.HS512.getValue(), "123",
                Collections.emptyList());
    }

    private Config enabledConfig() {
        return DefaultConfiguration.defaultConfiguration().getConfig("composer.session");
    }

    private Config disabledConfig() {
        return enabledConfig().withValue("enabled", ConfigValueFactory.fromAnyRef(Boolean.FALSE));
    }
}
