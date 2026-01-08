package com.dmh.apigateway.filter;

import com.dmh.apigateway.util.JwtUtil;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.core.io.buffer.DataBufferFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.net.URI;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class JwtAuthenticationFilterTest {

    @Mock
    private JwtUtil jwtUtil;

    @Mock
    private GatewayFilterChain chain;

    @Mock
    private ServerWebExchange exchange;

    @Mock
    private ServerHttpRequest request;

    @Mock
    private ServerHttpResponse response;

    @Mock
    private HttpHeaders headers;

    @Mock
    private DataBufferFactory bufferFactory;

    @InjectMocks
    private JwtAuthenticationFilter filter;

    @Test
    void testApply_PublicRoute() {
        when(exchange.getRequest()).thenReturn(request);
        when(request.getURI()).thenReturn(URI.create("/api/users/register"));
        when(chain.filter(exchange)).thenReturn(Mono.empty());

        filter.filter(exchange, chain);

        verify(chain, times(1)).filter(exchange);
    }

    @Test
    void testApply_MissingToken() {
        when(exchange.getRequest()).thenReturn(request);
        when(request.getHeaders()).thenReturn(headers);
        when(headers.getFirst(HttpHeaders.AUTHORIZATION)).thenReturn(null);
        when(request.getURI()).thenReturn(URI.create("/api/accounts"));
        when(exchange.getResponse()).thenReturn(response);
        when(response.getHeaders()).thenReturn(headers);
        when(response.bufferFactory()).thenReturn(bufferFactory);
        when(bufferFactory.wrap(any(byte[].class))).thenReturn(org.mockito.Mockito.mock(org.springframework.core.io.buffer.DataBuffer.class));

        filter.filter(exchange, chain);

        verify(response, times(1)).setStatusCode(org.springframework.http.HttpStatus.UNAUTHORIZED);
    }

    @Test
    void testApply_InvalidTokenFormat() {
        when(exchange.getRequest()).thenReturn(request);
        when(request.getHeaders()).thenReturn(headers);
        when(headers.getFirst(HttpHeaders.AUTHORIZATION)).thenReturn("InvalidFormat");
        when(request.getURI()).thenReturn(URI.create("/api/accounts"));
        when(exchange.getResponse()).thenReturn(response);
        when(response.getHeaders()).thenReturn(headers);
        when(response.bufferFactory()).thenReturn(bufferFactory);
        when(bufferFactory.wrap(any(byte[].class))).thenReturn(org.mockito.Mockito.mock(org.springframework.core.io.buffer.DataBuffer.class));

        filter.filter(exchange, chain);

        verify(response, times(1)).setStatusCode(org.springframework.http.HttpStatus.UNAUTHORIZED);
    }

    @Test
    void testApply_BlacklistedToken() {
        String token = "blacklisted.token";
        when(exchange.getRequest()).thenReturn(request);
        when(request.getHeaders()).thenReturn(headers);
        when(headers.getFirst(HttpHeaders.AUTHORIZATION)).thenReturn("Bearer " + token);
        when(request.getURI()).thenReturn(URI.create("/api/accounts"));
        when(jwtUtil.validateToken(token)).thenReturn(false);
        when(exchange.getResponse()).thenReturn(response);
        when(response.getHeaders()).thenReturn(headers);
        when(response.bufferFactory()).thenReturn(bufferFactory);
        when(bufferFactory.wrap(any(byte[].class))).thenReturn(org.mockito.Mockito.mock(org.springframework.core.io.buffer.DataBuffer.class));

        filter.filter(exchange, chain);

        verify(response, times(1)).setStatusCode(org.springframework.http.HttpStatus.UNAUTHORIZED);
    }
}
