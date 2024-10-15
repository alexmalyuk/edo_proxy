package org.nautilus;

import org.apache.http.Header;
import org.apache.http.HttpResponse;
import org.apache.http.StatusLine;
import org.apache.http.entity.BasicHttpEntity;
import org.apache.http.message.BasicHeader;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.nautilus.service.InnerRequestService;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.*;

import static org.mockito.Mockito.*;
import static org.mockito.MockitoAnnotations.openMocks;

public class EdoProxyServletTest {

    @Mock
    private InnerRequestService requestService;

    @Mock
    private HttpServletRequest req;

    @Mock
    private HttpServletResponse resp;

    @Mock
    HttpResponse innerResponse;

    @Mock
    private PrintWriter writer;

    private EdoProxyServlet servlet;

    private static final String VALID_UUID_PATH = "/task/123e4567-e89b-12d3-a456-426614174000";

    private static final String VALID_JSON = "{\n" +
            "  \"id\": \"1717d6e4-19c1-11ee-a570-00155d0a0186\",\n" +
            "  \"comment\": \"Все фігня - переробити\",\n" +
            "  \"answer\": \"decline\"\n" +
            "}\n";

    private static final String INVALID_JSON = "{\n" +
            "  \"id\": \"1717d6e4-19c1-11ee-a570-00155***d0a0186\",\n" +
            "  \"comment\": \"Все фігня - переробити\",\n" +
            "  \"answer\": \"decline\"\n" +
            "}\n";

    private void mockInnerResponse(HttpResponse innerResponse, int statusCode, String responseBody, Header[] headers) {
        when(innerResponse.getStatusLine()).thenReturn(mock(StatusLine.class));
        when(innerResponse.getStatusLine().getStatusCode()).thenReturn(statusCode);

        BasicHttpEntity entity = new BasicHttpEntity();
        entity.setContent(new ByteArrayInputStream(responseBody.getBytes()));
        when(innerResponse.getEntity()).thenReturn(entity);

        when(innerResponse.getAllHeaders()).thenReturn(headers);
    }

    @BeforeEach
    void setUp() throws IOException {
        openMocks(this);
        servlet = new EdoProxyServlet(requestService);
        when(resp.getWriter()).thenReturn(writer);
    }


    @Test
    void testDoPost_ValidJson_Redirect() throws IOException {
        BufferedReader reader = new BufferedReader(new StringReader(VALID_JSON));
        when(req.getReader()).thenReturn(reader);
        when(req.getPathInfo()).thenReturn("/task/approve");

        Header[] headers = new Header[]{
                new BasicHeader("Location", "new-location")
        };
        mockInnerResponse(innerResponse, HttpServletResponse.SC_FOUND, "", headers);
        when(requestService.forwardPost(anyString(), anyString())).thenReturn(innerResponse);

        servlet.doPost(req, resp);
        verify(resp).setStatus(HttpServletResponse.SC_FOUND);
        verify(resp).setHeader("Location", "new-location");
    }

    @Test
    void testDoPost_ValidJson_Ok() throws IOException {
        String pathInfo = "/task/approve";
        BufferedReader reader = new BufferedReader(new StringReader(VALID_JSON));
        when(req.getReader()).thenReturn(reader);
        when(req.getPathInfo()).thenReturn(pathInfo);

        mockInnerResponse(innerResponse, HttpServletResponse.SC_OK, "Ok", new Header[0]);
        when(requestService.forwardPost(anyString(), anyString())).thenReturn(innerResponse);

        servlet.doPost(req, resp);
        verify(resp).setStatus(HttpServletResponse.SC_OK);
    }

    @Test
    void testDoPost_ValidJson_InternalServerError() throws IOException {
        BufferedReader reader = new BufferedReader(new StringReader(VALID_JSON));
        when(req.getReader()).thenReturn(reader);
        when(req.getPathInfo()).thenReturn("/task/approve");

        mockInnerResponse(innerResponse, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "", new Header[0]);
        when(requestService.forwardPost(anyString(), anyString())).thenReturn(innerResponse);

        servlet.doPost(req, resp);
        verify(resp).setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
    }

    @Test
    void testDoPost_InvalidPath() {
        when(req.getPathInfo()).thenReturn("/invalid/path");

        servlet.doPost(req, resp);
        verify(resp).setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
    }

    @Test
    void testDoPost_InvalidJson() throws IOException {
        BufferedReader reader = new BufferedReader(new StringReader(INVALID_JSON));
        when(req.getReader()).thenReturn(reader);
        when(req.getPathInfo()).thenReturn("/task/approve");

        servlet.doPost(req, resp);
        verify(resp).setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
    }

    @Test
    void testDoGet_ThanksPath_Ok() throws IOException {
        when(req.getPathInfo()).thenReturn("/task/thanks");

        mockInnerResponse(innerResponse, HttpServletResponse.SC_OK, "", new Header[0]);
        when(requestService.forwardGet(anyString())).thenReturn(innerResponse);

        servlet.doGet(req, resp);
        verify(resp).setStatus(HttpServletResponse.SC_OK);
    }

    @Test
    void testDoGet_ThanksPath_RequestTimeout() throws IOException {
        when(req.getPathInfo()).thenReturn("/task/thanks");

        mockInnerResponse(innerResponse, HttpServletResponse.SC_REQUEST_TIMEOUT, "", new Header[0]);
        when(requestService.forwardGet(anyString())).thenReturn(innerResponse);

        servlet.doGet(req, resp);
        verify(resp).setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
    }

    @Test
    void testDoGet_ThanksPath_InternalServiceError() throws IOException {
        when(req.getPathInfo()).thenReturn("/task/thanks");

        mockInnerResponse(innerResponse, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "", new Header[0]);
        when(requestService.forwardGet(anyString())).thenReturn(innerResponse);

        servlet.doGet(req, resp);
        verify(resp).setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
    }

    @Test
    void testDoGet_UuidPath_Ok() throws IOException {
        when(req.getPathInfo()).thenReturn(VALID_UUID_PATH);

        mockInnerResponse(innerResponse, HttpServletResponse.SC_OK, "", new Header[0]);
        when(requestService.forwardGet(anyString())).thenReturn(innerResponse);

        servlet.doGet(req, resp);
        verify(resp).setStatus(HttpServletResponse.SC_OK);
    }

    @Test
    void testDoGet_UuidPath_InternalServiceError() throws IOException {
        when(req.getPathInfo()).thenReturn(VALID_UUID_PATH);

        mockInnerResponse(innerResponse, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "", new Header[0]);
        when(requestService.forwardGet(anyString())).thenReturn(innerResponse);

        servlet.doGet(req, resp);
        verify(resp).setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
    }

    @Test
    void testDoGet_UuidPath_BadRequest() throws IOException {
        when(req.getPathInfo()).thenReturn(VALID_UUID_PATH);

        mockInnerResponse(innerResponse, HttpServletResponse.SC_BAD_REQUEST, "", new Header[0]);
        when(requestService.forwardGet(anyString())).thenReturn(innerResponse);

        servlet.doGet(req, resp);
        verify(resp).setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
    }

    @Test
    void testDoGet_UuidPath_InvalidPath() {
        when(req.getPathInfo()).thenReturn("/invalid/path");

        servlet.doGet(req, resp);
        verify(resp).setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
    }

    @Test
    void testDoGet_UuidPath_InvalidUuid() {
        when(req.getPathInfo()).thenReturn("/task/123e4567-e89b-12d3-a456-4266141***74000");

        servlet.doGet(req, resp);
        verify(resp).setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
    }

    @Test
    void testDoGet_ShouldNotCopyAuthorizationHeaderFromInnerResponse() throws IOException {
        when(req.getPathInfo()).thenReturn(VALID_UUID_PATH);

        Header[] headers = new Header[]{
                new BasicHeader("AnyHeader", "Any value"),
                new BasicHeader("Authorization", "basic qwerty")
        };
        mockInnerResponse(innerResponse, HttpServletResponse.SC_OK, "", headers);
        when(requestService.forwardGet(anyString())).thenReturn(innerResponse);

        servlet.doGet(req, resp);
        verify(resp).setStatus(HttpServletResponse.SC_OK);
        verify(resp).setHeader("AnyHeader", "Any value");
        verify(resp, never()).setHeader(eq("Authorization"), anyString());
    }

}