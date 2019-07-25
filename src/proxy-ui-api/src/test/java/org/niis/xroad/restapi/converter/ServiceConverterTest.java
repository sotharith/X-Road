/**
 * The MIT License
 * Copyright (c) 2018 Estonian Information System Authority (RIA),
 * Nordic Institute for Interoperability Solutions (NIIS), Population Register Centre (VRK)
 * Copyright (c) 2015-2017 Estonian Information System Authority (RIA), Population Register Centre (VRK)
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package org.niis.xroad.restapi.converter;

import ee.ria.xroad.common.identifier.ClientId;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import static org.junit.Assert.assertEquals;

/**
 * test ServiceConverter
 */
@RunWith(SpringRunner.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class ServiceConverterTest {

    public static final String CLIENT_ID_PREFIX_SS1 = "XRD2:GOV:M4:SS1:";
    @Autowired
    private ServiceConverter serviceConverter;

    @Test
    public void convertStringIdWithoutVersion() throws Exception {
        String serviceCode = "awesomeService";
        String encodedServiceId = CLIENT_ID_PREFIX_SS1 + serviceCode;
        ClientId clientId = serviceConverter.parseClientId(encodedServiceId);
        assertEquals("XRD2", clientId.getXRoadInstance());
        assertEquals("GOV", clientId.getMemberClass());
        assertEquals("M4", clientId.getMemberCode());

        String fullServiceCode = serviceConverter.parseFullServiceCode(encodedServiceId);
        assertEquals(serviceCode, fullServiceCode);
    }

    @Test
    public void convertDifficultStringId() throws Exception {
        String difficultServiceCode = "FOO SS-;/?@=&-X<!-- o -->BAR";
        String serviceVersion = "v2";
        String encodedFullServiceCode = difficultServiceCode + "." + serviceVersion;
        String encodedServiceId = CLIENT_ID_PREFIX_SS1 + encodedFullServiceCode;
        ClientId clientId = serviceConverter.parseClientId(encodedServiceId);
        assertEquals("XRD2", clientId.getXRoadInstance());
        assertEquals("GOV", clientId.getMemberClass());
        assertEquals("M4", clientId.getMemberCode());

        String fullServiceCode = serviceConverter.parseFullServiceCode(encodedServiceId);
        assertEquals(encodedFullServiceCode, fullServiceCode);
    }

    @Test(expected = RuntimeException.class)
    public void convertBadStringIdClientId() throws Exception {
        serviceConverter.parseClientId("XRD2:GOV:M4:SS1:aa:asd");
    }

    @Test(expected = RuntimeException.class)
    public void convertBadStringIdServiceCode() throws Exception {
        serviceConverter.parseFullServiceCode("XRD2:GOV:M4:SS1:aa:asd");
    }

    @Test(expected = RuntimeException.class)
    public void convertBadStringId2ClientId() throws Exception {
        serviceConverter.parseClientId("XRD2:GOV:M4:SS1");
    }

    @Test(expected = RuntimeException.class)
    public void convertBadStringId2ServiceCode() throws Exception {
        serviceConverter.parseFullServiceCode("XRD2:GOV:M4:SS1");
    }

}
