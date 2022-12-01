package fr.ans.psc.context.sharing.api.prosanteconnect;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class PscClientImpl implements ProSanteConnectClient {

    @Value("${psc.url.introspection}")
    public String pscUrlIntrospection;

    @Value("${psc.url.userinfo}")
    public String pscUrlUserInfo;


    @Value("${psc.clientID}")
    public String pscClientID;

    @Value("${psc.clientSecret}")
    public String pscSecret;

    @Override
    public String getIntrospectionResult(String accessToken) {
        return null;
    }

    @Override
    public void isPSCvalidateToken(String introspectionResult) {
    }

    @Override
    public String getUserInfo(String accessToken) {
        return null;
    }
}
