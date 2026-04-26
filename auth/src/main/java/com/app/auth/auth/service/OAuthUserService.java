package com.app.auth.auth.service;

import com.app.auth.LogMessages;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;

/**
 * Custom OAuth2 user service that normalizes Google attributes and upserts the user.
 *
 * <p>Algorithm (§9.1 of design doc):
 * <ol>
 *   <li>Delegate to super.loadUser() — hits Google's userinfo endpoint</li>
 *   <li>Extract "sub" → googleId (stable, never changes)</li>
 *   <li>Extract "email" and "name"</li>
 *   <li>Delegate to UserService.upsertUser() — create or update Neo4j node</li>
 *   <li>Return original OAuth2User unchanged (Spring Security needs it downstream)</li>
 * </ol>
 *
 * <p>NOTE: Repository is never called directly here — that would violate the
 * domain layering. We always delegate through UserService.</p>
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class OAuthUserService extends DefaultOAuth2UserService {

    @Override
    public OAuth2User loadUser(OAuth2UserRequest userRequest) throws OAuth2AuthenticationException {
        // 1. Fetch user info from Google
        OAuth2User oAuth2User = super.loadUser(userRequest);

        // 2. Extract stable Google identity fields
        String email    = oAuth2User.getAttribute("email");

        log.debug(LogMessages.OAUTH2_USER_LOADED_GOOGLE, email);

        // 3. Return unchanged OAuth2User — Spring Security needs its original attributes
        return oAuth2User;
    }
}
