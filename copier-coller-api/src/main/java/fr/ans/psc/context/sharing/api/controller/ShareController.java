package fr.ans.psc.context.sharing.api.controller;

import fr.ans.psc.context.sharing.api.exception.*;
import fr.ans.psc.context.sharing.api.model.PscContext;
import fr.ans.psc.context.sharing.api.service.PscAuthService;
import fr.ans.psc.context.sharing.api.service.ShareService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import javax.servlet.http.HttpServletRequest;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;

@Slf4j
@RestController
@CrossOrigin
@RequestMapping("/share")
public class ShareController {

    @Autowired
    private PscAuthService authService;

    @Autowired
    private ShareService shareService;

    @GetMapping()
    public ResponseEntity<PscContext> getPsContextFromCache() {
        try {
            log.debug("Get context requested");
            String accessToken = getAccessToken();
            String nationalId = authService.introspectPscAccesToken(accessToken);
            log.debug("PS key is {}", nationalId);
            PscContext pscContext = shareService.getPscContext(nationalId);
            return new ResponseEntity<>(pscContext, HttpStatus.OK);
        } catch (PscContextSharingException e) {
            return new ResponseEntity<>(e.getStatus());
        }
    }

    @PutMapping(consumes = {"application/json"}, produces = {"application/json"})
    public ResponseEntity<PscContext> putPscContextInCache(@RequestBody PscContext pscContext) {
        try {
            log.debug("Put context requested");
            String accessToken = getAccessToken();
            String nationalId = authService.introspectPscAccesToken(accessToken);
            log.debug("PS key is {}", nationalId);
            PscContext toStore = new PscContext(null, pscContext.getSchemaId(), pscContext.getBag());
            toStore.setPsId(nationalId);
            PscContext savedContext = shareService.putPsContext(toStore);
            return new ResponseEntity<>(savedContext, HttpStatus.OK);
        } catch (PscContextSharingException e) {
            return new ResponseEntity<>(e.getStatus());
        }
    }

    /**
     * Gets the accessToken
     *
     * @return the PSC accessToken
     */
    public String getAccessToken() throws PscMissingAccessTokenException {
        List<String> tmp = new ArrayList<String>();
        String headerNameAuthorization = "Authorization";
        Enumeration<String> tokens = getHttpRequest().getHeaders(headerNameAuthorization);
        while (tokens.hasMoreElements()) {
            log.debug("At least one 'Authorization' header has been found");
            String token = tokens.nextElement();
            String tokenHeaderPrefixBearer = "Bearer";
            if (token.startsWith(tokenHeaderPrefixBearer)) {
                tmp.add(StringUtils.deleteWhitespace(token).substring(tokenHeaderPrefixBearer.length()));
                log.debug("'Bearer' token found in an 'Authorization' header: {} ", token);
            }
        }
        if (tmp.size() != 1) {
            log.error("No access token provided");
            throw new PscMissingAccessTokenException(HttpStatus.FORBIDDEN);
        }
        log.debug("accessToken received (without prefix): {}", tmp.get(0));

        return tmp.get(0);
    }

    /**
     * Gets the HttpServletRequest
     *
     * @return the HttpServletRequest
     */
    public HttpServletRequest getHttpRequest() {
        ServletRequestAttributes attrs = (ServletRequestAttributes) RequestContextHolder.currentRequestAttributes();
        return attrs.getRequest();
    }
}
