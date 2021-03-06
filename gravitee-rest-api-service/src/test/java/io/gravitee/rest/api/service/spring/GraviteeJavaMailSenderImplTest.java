/**
 * Copyright (C) 2015 The Gravitee team (http://gravitee.io)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.gravitee.rest.api.service.spring;

import io.gravitee.common.event.EventManager;
import io.gravitee.common.event.impl.SimpleEvent;
import io.gravitee.repository.management.model.Parameter;
import io.gravitee.rest.api.model.parameters.Key;
import io.gravitee.rest.api.model.parameters.ParameterReferenceType;
import io.gravitee.rest.api.service.ParameterService;
import io.gravitee.rest.api.service.common.GraviteeContext;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.mail.javamail.JavaMailSenderImpl;

import java.util.List;

import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class GraviteeJavaMailSenderImplTest {

    @Mock
    private ParameterService parameterService;
    @Mock
    private EventManager eventManager;

    private GraviteeJavaMailSenderImpl graviteeJavaMailSender;

    @Before
    public void setUp() {
        reset(parameterService, eventManager);
        GraviteeContext.setCurrentOrganization("DEFAULT");
        GraviteeContext.setCurrentEnvironment("DEFAULT");

        graviteeJavaMailSender = new GraviteeJavaMailSenderImpl(parameterService, eventManager);
    }

    @Test
    public void shouldConstruct() {
        verify(eventManager, times(1)).subscribeForEvents(graviteeJavaMailSender, Key.class);
    }

    @Test
    public void shouldInializeOnlyOnceWhenGettingSession() {
        JavaMailSenderImpl mailSender = graviteeJavaMailSender.getMailSenderByReference("DEFAULT", ParameterReferenceType.ENVIRONMENT);
        assertNull(mailSender);

        // initialize the field only when we get the session first time
        graviteeJavaMailSender.getSession();
        mailSender = graviteeJavaMailSender.getMailSenderByReference("DEFAULT", ParameterReferenceType.ENVIRONMENT);
        assertNotNull(mailSender);

        // If we call this getter a second time, then we do not initialize anymore
        graviteeJavaMailSender.getSession();

        verify(parameterService, times(1)).find(Key.EMAIL_HOST, "DEFAULT", ParameterReferenceType.ENVIRONMENT);
        verify(parameterService, times(1)).find(Key.EMAIL_PORT, "DEFAULT", ParameterReferenceType.ENVIRONMENT);
        verify(parameterService, times(1)).find(Key.EMAIL_USERNAME, "DEFAULT", ParameterReferenceType.ENVIRONMENT);
        verify(parameterService, times(1)).find(Key.EMAIL_PASSWORD, "DEFAULT", ParameterReferenceType.ENVIRONMENT);
        verify(parameterService, times(1)).find(Key.EMAIL_PROTOCOL, "DEFAULT", ParameterReferenceType.ENVIRONMENT);
        verify(parameterService, times(1))
                .findAll(argThat((List<Key> o) ->
                        o.contains(Key.EMAIL_PROPERTIES_AUTH_ENABLED)
                        && o.contains(Key.EMAIL_PROPERTIES_STARTTLS_ENABLE)
                        && o.contains(Key.EMAIL_PROPERTIES_SSL_TRUST)),
                        eq("DEFAULT"),
                        eq(ParameterReferenceType.ENVIRONMENT));
    }

    @Test
    public void shouldSetFieldsOnEvent() {
        graviteeJavaMailSender.getSession();

        graviteeJavaMailSender.onEvent(new SimpleEvent<>(Key.EMAIL_HOST, buildParameter("host")));
        graviteeJavaMailSender.onEvent(new SimpleEvent<>(Key.EMAIL_PORT, buildParameter("125")));
        graviteeJavaMailSender.onEvent(new SimpleEvent<>(Key.EMAIL_USERNAME, buildParameter("username")));
        graviteeJavaMailSender.onEvent(new SimpleEvent<>(Key.EMAIL_PASSWORD, buildParameter("password")));
        graviteeJavaMailSender.onEvent(new SimpleEvent<>(Key.EMAIL_PROTOCOL, buildParameter("protocol")));
        graviteeJavaMailSender.onEvent(new SimpleEvent<>(Key.EMAIL_PROPERTIES_AUTH_ENABLED, buildParameter("true")));
        graviteeJavaMailSender.onEvent(new SimpleEvent<>(Key.EMAIL_PROPERTIES_STARTTLS_ENABLE, buildParameter("false")));
        graviteeJavaMailSender.onEvent(new SimpleEvent<>(Key.EMAIL_PROPERTIES_SSL_TRUST, buildParameter("ssl_trust")));

        JavaMailSenderImpl mailSender = graviteeJavaMailSender.getMailSenderByReference("DEFAULT", ParameterReferenceType.ENVIRONMENT);
        assertEquals("host", mailSender.getHost());
        assertEquals(125, mailSender.getPort());
        assertEquals("username", mailSender.getUsername());
        assertEquals("password", mailSender.getPassword());
        assertEquals("protocol", mailSender.getProtocol());
        assertEquals("true", mailSender.getJavaMailProperties().get("mail.smtp.auth"));
        assertEquals("false", mailSender.getJavaMailProperties().get("mail.smtp.starttls.enable"));
        assertEquals("ssl_trust", mailSender.getJavaMailProperties().get("mail.smtp.ssl.trust"));
    }

    @Test
    public void shouldNotSetFieldsOnEventWithAnotherRef() {
        graviteeJavaMailSender.getSession();

        graviteeJavaMailSender.onEvent(new SimpleEvent<>(Key.EMAIL_HOST, buildParameter("host", "ANOTHER_ENVIRONMENT", ParameterReferenceType.ENVIRONMENT)));
        graviteeJavaMailSender.onEvent(new SimpleEvent<>(Key.EMAIL_PORT, buildParameter("125", "DEFAULT", ParameterReferenceType.ORGANIZATION)));
        graviteeJavaMailSender.onEvent(new SimpleEvent<>(Key.EMAIL_USERNAME, buildParameter("username")));
        graviteeJavaMailSender.onEvent(new SimpleEvent<>(Key.EMAIL_PASSWORD, buildParameter("password")));
        graviteeJavaMailSender.onEvent(new SimpleEvent<>(Key.EMAIL_PROTOCOL, buildParameter("protocol")));
        graviteeJavaMailSender.onEvent(new SimpleEvent<>(Key.EMAIL_PROPERTIES_AUTH_ENABLED, buildParameter("true")));
        graviteeJavaMailSender.onEvent(new SimpleEvent<>(Key.EMAIL_PROPERTIES_STARTTLS_ENABLE, buildParameter("false")));
        graviteeJavaMailSender.onEvent(new SimpleEvent<>(Key.EMAIL_PROPERTIES_SSL_TRUST, buildParameter("ssl_trust")));

        JavaMailSenderImpl mailSender = graviteeJavaMailSender.getMailSenderByReference("DEFAULT", ParameterReferenceType.ENVIRONMENT);
        assertNull(mailSender.getHost());
        assertEquals(-1, mailSender.getPort());
        assertEquals("username", mailSender.getUsername());
        assertEquals("password", mailSender.getPassword());
        assertEquals("protocol", mailSender.getProtocol());
        assertEquals("true", mailSender.getJavaMailProperties().get("mail.smtp.auth"));
        assertEquals("false", mailSender.getJavaMailProperties().get("mail.smtp.starttls.enable"));
        assertEquals("ssl_trust", mailSender.getJavaMailProperties().get("mail.smtp.ssl.trust"));
    }

    private Parameter buildParameter(String value, String referenceId, ParameterReferenceType referenceType) {
        Parameter parameter = new Parameter();
        parameter.setValue(value);
        parameter.setReferenceId(referenceId);
        parameter.setReferenceType(io.gravitee.repository.management.model.ParameterReferenceType.valueOf(referenceType.name()));
        return parameter;
    }

    private Parameter buildParameter(String value) {
        return this.buildParameter(value, "DEFAULT", ParameterReferenceType.ENVIRONMENT);
    }
}