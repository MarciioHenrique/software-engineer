package com.mirna.hospitalmanagementapi.infra.security.filters;

import java.io.IOException;
import java.util.stream.Stream;

import javax.security.sasl.AuthenticationException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import com.mirna.hospitalmanagementapi.domain.entities.auth.User;
import com.mirna.hospitalmanagementapi.domain.services.UserService;
import com.mirna.hospitalmanagementapi.domain.services.auth.jwt.TokenService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@Component
public class AuthTokenFilter extends OncePerRequestFilter {

	@Autowired
	private TokenService tokenService;
	
	@Autowired
	private UserService userService;

	private static final String[] EXCLUDED_URLS = {
			"/api/auth",
			"/v3/api-docs",
			"/swagger-ui"
	};
	
	@Override
	protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
			throws ServletException, IOException {
		
		String authHeader = request.getHeader("Authorization");
		
		this.verifyAuthorizationToken(authHeader);
	    
		String token = authHeader.replace("Bearer ", "").trim();
		
		String tokenSubject = tokenService.getTokenSubject(token);
		
		User authenticatedUser = (User) userService.findUserByLogin(tokenSubject);
		
		Authentication auth = new UsernamePasswordAuthenticationToken(authenticatedUser, null, authenticatedUser.getAuthorities());
		
		SecurityContextHolder.getContext().setAuthentication(auth);
		
		filterChain.doFilter(request, response);
	}
	
	@Override
	protected boolean shouldNotFilter(HttpServletRequest request) throws ServletException {
		String url = request.getRequestURI();
		return Stream.of(EXCLUDED_URLS).anyMatch(url::startsWith);
	 }

	private boolean isAuthorizationTokenValid(String authorizationHeader){
		return authorizationHeader.isBlank() || !authorizationHeader.startsWith("Bearer ");
	}

	private void verifyAuthorizationToken(String authorizationHeader) throws AuthenticationException{
		if (this.isAuthorizationTokenValid(authorizationHeader)) {
			throw new AuthenticationException("Authorization token is null or invalid");
		}	
	}
}
