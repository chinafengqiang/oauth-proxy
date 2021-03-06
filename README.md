# OAuth2 Proxy for Single Page Applications (SPAs)

## Introduction

OAuth2 Proxy for SPAs focuses on giving secure access to the SPAs 
while mitigating the following security threats,
1. The client cannot be authenticated in a completely legitimate manner.
2. The access token cannot be made invisible to the end user.

The necessity of an OAuth2 Proxy for SPAs is being explained in 
[this blog post](https://medium.com/@madumalt/oauth2-proxy-for-single-page-applications-8f01fd5fdd52).
The security concerns when integrating Identity and Access Management (IAM) to SPAs and how to address those concerns 
is being discussed in 
[this webinar](https://www.slideshare.net/prabathsiriwardena/securing-singlepage-applications-with-oauth-20).
This is where the software security expert [Prabath Siriwardhane](https://twitter.com/prabath) has introduced the 
concept of OAuth2 Proxy for SPAs.

This implementation of OAuth2 Proxy for SPAs is bound to 
[WSO2 Identity Server](https://wso2.com/identity-and-access-management).
However, once the concepts and implementation detail are understood, making it compatible with your choice of OAuth2 
IAM provider should be pretty straight forward. Highly encourage pull requests to enhance and generalize this 
implementation such that this can be adopted with any OAuth2 IAM provider.

## Architecture

OAuth2 Proxy for SPAs is comprised of two proxies. A Login Proxy and An API Proxy.
Login Proxy is responsible for the authentication and authorization part where the API Proxy is responsible for 
invoking backend APIs on behalf of the SPA. The following diagram describes the overall architecture of the proxy 
implementation.

![OAuth2 Proxy for SPAs Architecture Diagram](https://user-images.githubusercontent.com/4003149/34104700-90969f86-e417-11e7-8ef3-69d7fbefe3cc.png)

## Implementation

Every endpoint of the OAuth2 Proxy for SPAs expects a session-id. This session-id is a random and unique character 
string for a particular browser session. Since the OAuth Proxy is a stateless this should be generated at the SPA, 
should be stored in the browser sessionStore, and should be sent along with every request to the OAuth2 Proxy for SPAs.
When implementing SPAs should not use a statistically configured unique string as the session-id. The randomness of 
the session-id should be ensured to make it non-guessable.

### Login Proxy

Login Proxy has the following endpoints to cater the authentication and authorization purposes of SPAs.

1. /login   {expected "session-id" and "spa-name" query parameters}

SPA need to invoke /login endpoint to allow the user to login via Identity Server. Oauth2 Proxy will issue a browser 
redirection request to the Identity Server's /oauth2/authorize endpoint. Here as the relay state, the "session-id" 
query param value is set. Also will set a cookie (where the cookie name is <session-id>.spa_name) to retrieve the SPA
 name in other endpoints where necessary.

After successful login IS will redirect to the Oauth2 Proxy /callback endpoint with authorization_code.

2. /callback {expected "code" and "state" query parameters}

Only the Identity Server should invoke this endpoint. For that, a Service Provider should to be created at the 
Identity Server with inbound OAuth2/OpenID authentication configured. Here "code" is OAuth2 authorization_code, 
"state" is the relay state (that means the SPA's session-id that we set as the "state" in the redirection request to 
Identity Server's /oauth2/authorize endpoint).

Within this endpoint, an access_token will be obtained by providing the received authorization_code to the Identity 
Server.

Then the access_token, the refresh_token, and the payload part of the id_token (say jwt) is encrypted followed by a 
base64 (say jwe) encoding and a cookie is created (where the cookie name is <session-id>) by putting the jwe to the 
cookie as the value. Finally the cookie is included in the response to SPA from the OAuth2 Proxy.

3. /logout { expect "session-id" query parameter}

Clears all the cookies corresponding to the app session-id.

4. /users { expect "session-id" query parameter}

Retrieves the cookie with the jwe (i.e. encrypted details) set at the /callback endpoint. Decrypt the jwe and send 
id_token info as the response.

5. /userinfo {expect "session-id" query parameter, can have "scope" query parameter which is optional}

This endpoint proxies the Identity Server's /userinfo endpoint. Retrieves the cookie with the jwe, decrypt the jwe, 
get the access_token, compose a http GET request to the Identity Server's /userinfo endpoint with the scope provided 
(here openid scope is included by default). Furthermore at the authorization header access_token is included. After 
invoking the Identity Server's /userinfo endpoint the response is sent back to the SPA.

### API Proxy

In brief APIProxy passes the requests coming from the SPA client to the backend APIs after including the access token
. The APIProxy has only one endpoint. 

1. /api/* {expects "Spa-Session-Id" as a request header }

The endpoint is a wildcard endpoint. That means anything starts with "<oauth2 proxy host>/api/" will come to this 
endpoint.

Hostname mapping should be given as a proxy configuration and thereafter APIProxy will be able to pass the requests 
to the given host. Here the backend endpoint is whatever comes after "/api/" including the query parameters. As for 
the current implementation the proxy can be configured to pass the requests coming to /api to only one host. 
Therefore should use an API aggregator to expose all the business endpoint under one host name.

#### Example:

Imagine hostname mapping is `hostA.com to hostB.com`. Also, imagine the request coming from the SPA client to the 
proxy is`https://hostA.com/oauth2_proxy/api/bar?name=lionels` `headers: \['Spa-Sessio-Id':<session-id>]`. Then the 
request to the backend API from Proxy will be  `https://hostB.com/bar?name=lionels`. Also 
`Authorization: Bearer <access_token>` will be added in the header of this request.

`Note: As for the time of this writing, only GET requests are supported by the proxy.`

## Configurations

```bash
 # WSO2 Idenity Server.
 is_server_ep=https://localhost:9443

 # Client id and secret of the service provider created for the proxy.
 client_id=IAZg7ANvWJSBdsFCvTPOt_9nkrga 
 client_secret=M6FKmzbHafL4ihPDS3o1LbrmD9sa 
 # Client id and secret of the SPA specific service providers at IS.
 # After "." it should be the name of the client application.
 client_id.mobileshop=IAZg7ANvWJSBdsFCvTPOt_9nkrga 
 client_secret.mobileshop=M6FKmzbHafL4ihPDS3o1LbrmD9sa
 
 # Callback Url of the proxy. After authentication IS should issue a callback to this Url with auth_code. 
 proxy_callback_url=https://localhost:8443/oauth2-proxy/callback 

 # Callback Url mapping for each client application. After "." it should be the name of the client application.
 # Proxy issues the redirect after authentication and logout corresponding Url.
 sp_callback_url_mapping.mobileshop=https://localhost:8443/sample-spa/home.html
 sp_logout_url_mapping.mobileshop=https://localhost:8443/sample-spa/index.html
 
 # OAuth scopes mapping for each SPA client application. Multiple scopes should be separated by commas.
 # Opneid is a mandatory scope for all the applications. Without it no id_token will be available.
 # If no scope mapping is defined for the client application then will default to openid.
 # After "." it should be the name of the client application.
 scope.mobileshop=openid

 # SPA specific Hostname Mapping.
 # After the first "." it should be the name of the client application.
 # After the second "." it should be the hostname of the OAuth2 Proxy.
 # After the "=" it should be the mapped host name.
 host_name_mapping.mobileshop.localhost\:8443=localhost:8443

 # Init vector for symmetric encryption and decryption.
 iv=RandomInitVector
 # Secret key for symmetric encryption and decryption.
 secret_key=Bar12345Bar12345`
```

## Deployment and Testing

Open a terminal clone the repo, configure the proxy.properties accordingly  and move into the cloned oauth_proxy 
directory and run `mvn clean install` in the terminal. 
</br>
Deploy both the oath2-proxy.war and sample-spa.war in tomcat servlet container. Thereafter if configurations are 
right you'll be able to login to the sample-spa and will be able to invoke the dummy-API and userinfo endpoint.

Another sample application to demonstrate the usage of OAuth2 Proxy has been implemented and can be found in 
[this repo](https://github.com/madumalt/react-spa). [ReactJS](https://reactjs.org/) and the 
[React-Boilerplate](https://www.reactboilerplate.com/) are the technologies that has been used to implement this 
sample SPA.