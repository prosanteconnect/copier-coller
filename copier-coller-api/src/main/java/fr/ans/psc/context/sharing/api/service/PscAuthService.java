package fr.ans.psc.context.sharing.api.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.gson.Gson;
import fr.ans.psc.context.sharing.api.exception.PscAuthException;
import fr.ans.psc.context.sharing.api.exception.PscContextSharingException;
import fr.ans.psc.context.sharing.api.model.prosanteconnect.UserInfos;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;
import java.util.*;

@Service
@Slf4j
public class PscAuthService {

    @Value("${psc.url.introspection}")
    private String pscUrlIntrospection;

    @Value("${psc.url.userinfo}")
    private String pscUrlUserInfo;


    @Value("${psc.clientID}")
    private String pscClientID;

    @Value("${psc.clientSecret}")
    private String pscSecret;

    private ObjectMapper mapper = new ObjectMapper();

    public String introspectPscAccesToken(String accessToken) throws PscAuthException {
        introspect(accessToken);
        UserInfos userInfos = getUserInfos(accessToken);

        return userInfos.getSubjectNameID();
    }

    private void introspect(String accessToken) throws PscAuthException {
        RestTemplate restTemplate = new RestTemplate();
        log.debug("uri connection à ProSanteConnect: {}", pscUrlIntrospection);
        log.debug("ClientID pour proSanteConnect: {}", pscClientID);

        // payload
        MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
        params.add("token", accessToken);
        HttpEntity<MultiValueMap<String, String>> requestEntity = new HttpEntity<>(params, getHeadersIntrospection());
        ResponseEntity<String> result = restTemplate.postForEntity(pscUrlIntrospection, requestEntity, String.class);
        log.debug("Appel ProSanteConnect Reponse: StatusCode {} \n Introspection: {}", result.getStatusCode(),
                result.getBody());

        parsePscResponse(result.getBody());

    }

    private UserInfos getUserInfos(String accessToken) throws PscAuthException {
        RestTemplate restTemplate = new RestTemplate();
        log.debug("uri connection à ProSanteConnect: {}", pscUrlUserInfo);
        log.debug("ClientID pour proSanteConnect: {}", pscClientID);

        HttpEntity<MultiValueMap<String, String>> requestEntity = new HttpEntity<>(getHeadersUserInfo(accessToken));
        ResponseEntity<String> result = restTemplate.exchange(pscUrlUserInfo, HttpMethod.GET, requestEntity, String.class);

        log.debug("Appel ProSanteConnect pour getUserInfo. Reponse: StatusCode {} \n UserInfos: {}", result.getStatusCode(),
                result.getBody());

        UserInfos ui;
        try {
            ui = mapper.readValue(result.getBody(), UserInfos.class);
        } catch (JsonProcessingException e) {
            log.error("Error occurred while parsing UserInfos");
            throw new PscAuthException();
        }
        return ui;
    }

    /*
     * Set headers for PSC calls
     */
    private HttpHeaders getHeadersIntrospection() throws PscAuthException {
        String adminuserCredentials = pscClientID + ":" + pscSecret;
        String encodedCredentials;

        try {
            encodedCredentials = encodeBase64(adminuserCredentials);
        } catch (UnsupportedEncodingException e) {
            log.error("Credentials encoding error");
            throw new PscAuthException();
        }
        HttpHeaders httpHeaders = new HttpHeaders();
        httpHeaders.add("Authorization", "Basic " + encodedCredentials);
        httpHeaders.setAccept(Collections.singletonList(MediaType.APPLICATION_JSON));
        httpHeaders.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        return httpHeaders;
    }

    private HttpHeaders getHeadersUserInfo(String accessToken) {
        HttpHeaders httpHeaders = new HttpHeaders();
        httpHeaders.add("Authorization", "Bearer " + accessToken);
        httpHeaders.setAccept(Collections.singletonList(MediaType.APPLICATION_JSON));
        return httpHeaders;
    }

    public void parsePscResponse(String responsePSC) throws PscAuthException {
        log.debug("parsePscResponse IN with {}", responsePSC);
        ObjectNode node;
        try {
            node = new ObjectMapper().readValue(responsePSC, ObjectNode.class);

            String tokenActiveField = "active";
            if (node.has(tokenActiveField)) {
                String token_active_field = node.get(tokenActiveField).asText();
                String tokenActiveTrue = "true";
                if (!token_active_field.equalsIgnoreCase(tokenActiveTrue)) {
                    log.error("The provided token is not valid");
                    throw new PscAuthException(HttpStatus.UNAUTHORIZED);
                }
            } else {
                log.error("Invalid PSC introspection response : {} field not found", tokenActiveField);
                throw new PscAuthException();
            }

        } catch (JsonProcessingException e) {
            log.error("Technical error during PSC introspect parsing : {} field", e.getMessage());
            log.debug(e.toString());
            throw new PscAuthException();
        }
    }

    public String encodeBase64(String stringToEncode) throws UnsupportedEncodingException {
        return Base64.getEncoder().encodeToString(stringToEncode.getBytes(StandardCharsets.UTF_8));
    }
}
