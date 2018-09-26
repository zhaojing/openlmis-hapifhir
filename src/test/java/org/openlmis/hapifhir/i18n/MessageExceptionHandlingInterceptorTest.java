/*
 * This program is part of the OpenLMIS logistics management information system platform software.
 * Copyright © 2017 VillageReach
 *
 * This program is free software: you can redistribute it and/or modify it under the terms
 * of the GNU Affero General Public License as published by the Free Software Foundation, either
 * version 3 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU Affero General Public License for more details. You should have received a copy of
 * the GNU Affero General Public License along with this program. If not, see
 * http://www.gnu.org/licenses.  For additional information contact info@OpenLMIS.org.
 */

package org.openlmis.hapifhir.i18n;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;

import ca.uhn.fhir.rest.api.server.RequestDetails;
import ca.uhn.fhir.rest.server.exceptions.BaseServerResponseException;
import ca.uhn.fhir.rest.server.interceptor.ExceptionHandlingInterceptor;
import java.io.IOException;
import java.util.Locale;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.openlmis.hapifhir.i18n.MessageExceptionHandlingInterceptor.LocalizedMessageException;
import org.springframework.context.MessageSource;

@RunWith(MockitoJUnitRunner.class)
public class MessageExceptionHandlingInterceptorTest {

  private static final Locale ENGLISH_LOCALE = Locale.ENGLISH;
  private static final int STATUS_CODE = 404;
  private static final String ERROR_MESSAGE = "error-message";

  private static final Message MESSAGE = new Message(ERROR_MESSAGE);

  @Mock
  private MessageService messageService;

  @Mock
  private ExceptionHandlingInterceptor delegate;

  @Mock
  private MessageSource messageSource;

  @Mock
  private RequestDetails details;

  @Mock
  private HttpServletRequest request;

  @Mock
  private HttpServletResponse response;

  @Mock
  private BaseServerResponseException fhirException;

  @Mock
  private BaseMessageException messageException;

  @Captor
  private ArgumentCaptor<BaseServerResponseException> exceptionCaptor;

  private MessageExceptionHandlingInterceptor interceptor;

  @Before
  public void setUp() {
    interceptor = new MessageExceptionHandlingInterceptor(messageService, delegate);
  }

  @Test
  public void shouldPassExceptionWithoutChangesToSuperMethodIfExceptionIsNotMessageBased()
      throws ServletException, IOException {
    // when
    when(fhirException.getStatusCode()).thenReturn(STATUS_CODE);
    when(fhirException.getMessage()).thenReturn(ERROR_MESSAGE);

    interceptor.handleException(details, fhirException, request, response);

    // then
    verify(delegate)
        .handleException(eq(details), exceptionCaptor.capture(), eq(request), eq(response));
    verifyZeroInteractions(messageService);

    BaseServerResponseException capturedException = exceptionCaptor.getValue();
    assertThat(capturedException)
        .isInstanceOf(BaseServerResponseException.class)
        .hasFieldOrPropertyWithValue("statusCode", STATUS_CODE)
        .hasFieldOrPropertyWithValue("message", ERROR_MESSAGE);
  }

  @Test
  public void shouldModifyExceptionBeforeItWillBeMovedToSuperMethodIfExceptionIsMessageBased()
      throws ServletException, IOException {
    // when
    when(messageException.getStatusCode()).thenReturn(STATUS_CODE);
    when(messageException.asMessage()).thenReturn(MESSAGE);

    when(messageService.localize(MESSAGE))
        .thenAnswer(invocation -> {
          Message message = invocation.getArgumentAt(0, Message.class);
          return message.localMessage(messageSource, ENGLISH_LOCALE);
        });

    when(messageSource.getMessage(ERROR_MESSAGE, null, ENGLISH_LOCALE))
        .thenReturn(ERROR_MESSAGE);

    interceptor.handleException(details, messageException, request, response);

    // then
    verify(delegate)
        .handleException(eq(details), exceptionCaptor.capture(), eq(request), eq(response));

    BaseServerResponseException capturedException = exceptionCaptor.getValue();
    assertThat(capturedException)
        .isInstanceOf(LocalizedMessageException.class)
        .hasFieldOrPropertyWithValue("statusCode", STATUS_CODE)
        .hasFieldOrPropertyWithValue("message", ERROR_MESSAGE);
  }
}