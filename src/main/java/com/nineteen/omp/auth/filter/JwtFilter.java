package com.nineteen.omp.auth.filter;

import com.nineteen.omp.auth.dto.UserDetailsImpl;
import com.nineteen.omp.auth.jwt.JwtHeaderHandler;
import com.nineteen.omp.auth.jwt.JwtProvider;
import com.nineteen.omp.user.domain.User;
import com.nineteen.omp.user.repository.UserRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Objects;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.filter.OncePerRequestFilter;

@RequiredArgsConstructor
@Slf4j
public class JwtFilter extends OncePerRequestFilter {

  private final JwtProvider jwtProvider;
  private final JwtHeaderHandler jwtHeaderHandler;
  private final UserRepository userRepository;

  @Override
  protected void doFilterInternal(
      HttpServletRequest request,
      HttpServletResponse response,
      FilterChain filterChain
  ) throws ServletException, IOException {

    String requestUri = request.getRequestURI();

    log.info("JwtFilter requestUri: {}", requestUri);

    String refreshToken = jwtHeaderHandler.getRefreshToken(request);
    String accessToken = jwtHeaderHandler.getAccessToken(request);

    if (refreshToken == null || accessToken == null) {
      log.info("토큰 없음");
      setResponseStatusUnauthorized(response);
      filterChain.doFilter(request, response);
      return;
    }
    if (jwtProvider.isExpired(accessToken)) {
      log.info("access 토큰 만료");
      setResponseStatusUnauthorized(response);
      filterChain.doFilter(request, response);
      return;
    }

    Long userId;
    try {
      userId = jwtProvider.extractUserId(accessToken);
      Long refreshTokenUserId = jwtProvider.extractUserId(refreshToken);
      if (!Objects.equals(userId, refreshTokenUserId)) {
        log.info("토큰 간의 사용자 불일치");
        setResponseStatusUnauthorized(response);
        filterChain.doFilter(request, response);
        return;
      }
    } catch (Exception e) {
      log.error("JWT 토큰 추출 실패", e);
      response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
      filterChain.doFilter(request, response);
      return;
    }

    Optional<User> optionalUser = userRepository.findById(userId);
    if (optionalUser.isEmpty()) {
      log.error("사용자 정보 없음");
      setResponseStatusUnauthorized(response);
      filterChain.doFilter(request, response);
      return;
    }
    User user = optionalUser.get();

    Authentication authentication = getAuthentication(user);
    SecurityContextHolder.getContext().setAuthentication(authentication);

    log.info("JWT 인증 성공");
    filterChain.doFilter(request, response);
  }

  private static void setResponseStatusUnauthorized(HttpServletResponse response) {
    response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
  }

  private Authentication getAuthentication(User user) {
    UserDetails userDetails = new UserDetailsImpl(user);
    return new UsernamePasswordAuthenticationToken(
        userDetails,
        user.getPassword(),
        userDetails.getAuthorities()
    );
  }

  @Override
  protected boolean shouldNotFilter(HttpServletRequest request) throws ServletException {
    String path = request.getRequestURI();
    return path.startsWith("/favicon.ico") || path.startsWith("/api/v1/users/login") ||
        path.startsWith("/api/v1/users/signup") || path.startsWith("/api/v1/profile") ||
        path.startsWith("/api/v1/users/refresh") || path.startsWith("/api/v1/profile/sleep");
  }

}
