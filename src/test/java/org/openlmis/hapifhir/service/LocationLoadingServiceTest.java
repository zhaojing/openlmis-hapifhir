/*
 * This program is part of the OpenLMIS logistics management information system platform software.
 * Copyright © 2018 VillageReach
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

package org.openlmis.hapifhir.service;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.anyListOf;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.openlmis.hapifhir.service.LocationLoadingService.CLIENT_SOCKET_TIMEOUT;

import ca.uhn.fhir.rest.client.api.IGenericClient;
import ca.uhn.fhir.rest.client.interceptor.BearerTokenAuthInterceptor;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import org.hl7.fhir.dstu3.model.Location;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.openlmis.hapifhir.FacilityDtoDataBuilder;
import org.openlmis.hapifhir.GeographicZoneDtoDataBuilder;
import org.openlmis.hapifhir.service.referencedata.FacilityDto;
import org.openlmis.hapifhir.service.referencedata.FacilityReferenceDataService;
import org.openlmis.hapifhir.service.referencedata.GeographicZoneDto;
import org.openlmis.hapifhir.service.referencedata.GeographicZoneReferenceDataService;
import org.springframework.test.util.ReflectionTestUtils;

@RunWith(MockitoJUnitRunner.class)
public class LocationLoadingServiceTest {
  
  private static final String BASE_URL = "http://localhost";
  private static final String ACCESS_TOKEN = UUID.randomUUID().toString();

  @Mock
  private AuthService authService;
  
  @Mock
  private GeographicZoneReferenceDataService geoZoneService;
  
  @Mock
  private FacilityReferenceDataService facilityService;
  
  @InjectMocks
  private LocationLoadingService service;
  
  private IGenericClient mockClient;

  @Before
  public void setUp() {
    when(authService.obtainAccessToken()).thenReturn(ACCESS_TOKEN);
    mockClient = mock(IGenericClient.class, RETURNS_DEEP_STUBS);
    ReflectionTestUtils.setField(service, "serviceUrl", BASE_URL);
  }
  
  @Test
  public void initializeShouldInitializeFhirClient() {
    //when
    IGenericClient client = service.initialize();
    
    //then
    assertEquals(CLIENT_SOCKET_TIMEOUT,
        client.getFhirContext().getRestfulClientFactory().getSocketTimeout());
    assertEquals(BASE_URL + "/hapifhir/", client.getServerBase());
    assertEquals(1, client.getInterceptors().size());
    assertTrue(client.getInterceptors().get(0) instanceof BearerTokenAuthInterceptor);
    BearerTokenAuthInterceptor interceptor = 
        (BearerTokenAuthInterceptor) client.getInterceptors().get(0);
    assertEquals(ACCESS_TOKEN, interceptor.getToken());
  }
  
  @Test
  public void loadGeographicZonesShouldLoad() {
    //given
    GeographicZoneDto geoZone1 = new GeographicZoneDtoDataBuilder().build();
    GeographicZoneDto geoZone2 = new GeographicZoneDtoDataBuilder()
        .withLongitude(null)
        .withLatitude(null)
        .withParent(geoZone1)
        .build();
    List<GeographicZoneDto> geoZones = Arrays.asList(geoZone1, geoZone2);
    PageDto<GeographicZoneDto> geoZonePage = new PageDto<>();
    geoZonePage.setContent(geoZones);
    when(geoZoneService.getPage("", RequestParameters.init())).thenReturn(geoZonePage);
    
    //when
    service.loadGeographicZones(mockClient);

    //then
    verify(mockClient.transaction().withResources(anyListOf(Location.class))).execute();
  }

  @Test
  public void loadFacilitiesShouldLoad() {
    //given
    FacilityDto facility1 = new FacilityDtoDataBuilder().build();
    FacilityDto facility2 = new FacilityDtoDataBuilder()
        .withLocation(null)
        .withActive(false)
        .withEnabled(null)
        .build();
    List<FacilityDto> facilities = Arrays.asList(facility1, facility2);
    when(facilityService.findAll()).thenReturn(facilities);

    //when
    service.loadFacilities(mockClient);

    //then
    verify(mockClient.transaction().withResources(anyListOf(Location.class))).execute();
  }
}