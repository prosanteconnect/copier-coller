package fr.ans.psc.context.sharing.api.prosanteconnect;


public interface ProSanteConnectClient {

    /*
     * Get the PSC introspection response
     */
    public String getIntrospectionResult(String accessToken);

    public void isPSCvalidateToken(String introspectionResult);

    /*
     * Get the userInfo
     */
    public String getUserInfo(String accessToken);
}
