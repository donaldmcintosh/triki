/************************************************************************************
*
*   This file is part of triki
*
*   Written by Donald McIntosh (dbm@opentechnology.net) 
*
*   triki is free software: you can redistribute it and/or modify
*   it under the terms of the GNU General Public License as published by
*   the Free Software Foundation, either version 3 of the License, or
*   (at your option) any later version.
*
*   triki is distributed in the hope that it will be useful,
*   but WITHOUT ANY WARRANTY; without even the implied warranty of
*   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
*   GNU General Public License for more details.
*
*   You should have received a copy of the GNU General Public License
*   along with triki.  If not, see <http://www.gnu.org/licenses/>.
*
************************************************************************************/

package net.opentechnology.triki.auth.resources

import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import com.auth0.jwt.interfaces.DecodedJWT
import net.opentechnology.triki.auth.AuthorisationManager
import net.opentechnology.triki.auth.module.AuthModule
import net.opentechnology.triki.core.dto.SettingDto
import groovy.util.logging.Log4j
import net.opentechnology.triki.schema.Dcterms
import net.opentechnology.triki.schema.Foaf
import org.apache.http.client.utils.URIBuilder
import org.apache.wicket.Session
import org.springframework.beans.factory.annotation.InjectionMetadata

import javax.inject.Inject
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse
import javax.servlet.http.HttpSession;
import javax.ws.rs.Produces
import javax.ws.rs.Consumes;
import javax.ws.rs.FormParam;
import javax.ws.rs.QueryParam
import javax.ws.rs.GET
import javax.ws.rs.POST;
import javax.ws.rs.Path
import javax.ws.rs.core.Context
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response

import org.apache.http.NameValuePair
import org.apache.http.client.entity.UrlEncodedFormEntity
import org.apache.http.client.methods.CloseableHttpResponse
import org.apache.http.client.methods.HttpPost
import org.apache.http.impl.client.CloseableHttpClient
import org.apache.http.impl.client.HttpClients
import org.apache.http.message.BasicNameValuePair
import org.apache.http.Consts
import org.apache.jena.rdf.model.Resource
import org.apache.log4j.Logger
import net.opentechnology.triki.auth.AuthenticationException
import net.opentechnology.triki.auth.AuthenticationManager
import net.opentechnology.triki.core.boot.CachedPropertyStore;
import net.opentechnology.triki.core.boot.Utilities
import net.opentechnology.triki.core.resources.RenderResource

@Log4j
@Path("/auth")
public class AuthenticateResource extends RenderResource {

	public static final String NOREFERRER = "NOREFERRER"
	private final CloseableHttpClient httpclient = HttpClients.createDefault();
	public static final String SESSION_PERSON = "person";
	public static final String SESSION_PROFILE = "profile";
	
	private final Logger logger = Logger.getLogger(this.getClass());
	
	@Inject
	private CachedPropertyStore propStore;
	
	@Inject
	private final AuthenticationManager authMgr;

	@Inject
	private final AuthorisationManager authorisationManager;
	
	@Inject
	private final Utilities utils;

	@Inject
	private final SettingDto settingDto;

	@Inject
	private final IdentityProviders identityProviders

	@Inject
	private final SessionUtils sessionUtils;

	public AuthenticateResource(){

	}

	public AuthenticateResource(CachedPropertyStore propStore, AuthenticationManager authMgr, Utilities utils, SettingDto settingDto, IdentityProviders identityProviders) {
		this.propStore = propStore
		this.authMgr = authMgr
		this.utils = utils
		this.settingDto = settingDto
		this.identityProviders = identityProviders
	}

	@POST
	@Consumes(MediaType.APPLICATION_FORM_URLENCODED)
	public void login(@Context HttpServletResponse resp, @Context HttpServletRequest req,
					  MultivaluedMap<String, String> formParams,
					  @FormParam("action") String action,
					  @FormParam("triki:login") String login,
					  @FormParam("triki:password") String password) throws ServletException, IOException {
		HttpSession session = req.getSession();
		try {
			// Check if known to me
			Optional<Resource> signedInPerson = authMgr.authenticate(login, password)
			sessionUtils.ifKnownSave(signedInPerson)
			if(signedInPerson.isPresent()){
				Profile profile = sessionUtils.getProfile()
				profile.setName(signedInPerson.get().getProperty(Dcterms.title)?.getString())
				profile.setEmail(signedInPerson.get().getProperty(Foaf.mbox)?.getString())
				sessionUtils.setIfAdmin(signedInPerson, profile)
				sessionUtils.setProfile(profile);
				sessionUtils.forwardCorrectly(resp, null)
			}
			else {
				resp.sendRedirect("/login");
			}
		} catch (Exception e) {
			logger.warn("Problems authenticating user ${login}");
			sessionUtils.forwardCorrectly(resp,  null)
		}
	}
	
	@Path("indie")
	@GET
	@Produces(MediaType.TEXT_HTML)
	public Response content(@Context HttpServletResponse resp,@Context HttpServletRequest req,
			@QueryParam("code") String code,
			@QueryParam("me") String site
	)
		throws ServletException, IOException, URISyntaxException
	{	
		HttpSession session = req.getSession();
		Calendar timestampCal = Calendar.getInstance();
		timestampCal.setTime(new Date());
		
		logger.info("${site} has tried to login with code ${code}...");
		
		HttpPost poster = new HttpPost(settingDto.getSetting(AuthModule.Settings.INDIELOGINROOT.toString()));
		List<NameValuePair> form = new ArrayList<NameValuePair>();
		form.add(new BasicNameValuePair("code", code));
		form.add(new BasicNameValuePair("client_id", settingDto.getSetting(AuthModule.Settings.INDIELOGINCLIENTID.toString())));
		form.add(new BasicNameValuePair("redirect_uri", settingDto.getSetting(AuthModule.Settings.INDIELOGINREDIRECTURI.toString())));
		UrlEncodedFormEntity entity = new UrlEncodedFormEntity(form, Consts.UTF_8);
		poster.setEntity(entity);

		CloseableHttpResponse response = httpclient.execute(poster);
		
		if(response.getStatusLine().getStatusCode() == Response.Status.OK.code)
		{
			Profile profile = sessionUtils.getProfile()
			logger.info("${site} successfully authenticated by indieauth");
			profile.setWebsite(site)
			sessionUtils.setProfile(profile);

			// Check if known to site
			Optional<Resource> signedInPerson = authMgr.authenticateByWebsite(site)
			sessionUtils.ifKnownSave(signedInPerson)
			sessionUtils.setIfAdmin(signedInPerson, profile)

			// Forward correctly
			sessionUtils.forwardCorrectly(resp, null)
		}
		else
		{
			logger.warn("${me} not authenticated.");
			sessionUtils.forwardCorrectly(resp, null)
		}
	}

	@Path("openidlogin")
	@GET
	public Response getStateLogin(@Context HttpServletResponse resp,@Context HttpServletRequest req, @QueryParam("idp") String idp){
		HttpSession session = req.getSession();
		String randomSecret = UUID.randomUUID().toString()
		Algorithm algorithm = Algorithm.HMAC256(randomSecret);
		URIBuilder authUrl
		authUrl = identityProviders.getAuthUri(idp)

		logger.info("Signing state token with secret ${randomSecret}")
		String secretState = JWT.create()
				.withClaim('referer', req.getHeader("Referer") ?: NOREFERRER)
				.withClaim('idp', idp)
				.sign(algorithm)
		logger.info("Saving referer ${req.getHeader("Referer")} in JWT")
		session.setAttribute(AuthModule.SessionVars.OPENID_STATE.toString(), secretState)
		authUrl.addParameter("state", secretState)
		
		logger.info("Requesting ${idp} login with ${authUrl.build().toString()}")
		resp.sendRedirect(authUrl.build().toString());
	}

	@Path("openidconnect")
	@GET
	@Produces(MediaType.TEXT_HTML)
	public void openidConnect(@Context HttpServletResponse resp, @Context HttpServletRequest req,
								  @QueryParam("state") String state,
								  @QueryParam("code") String code,
								  @QueryParam("scope") String scope)
			throws ServletException, IOException, URISyntaxException
	{
		HttpSession session = req.getSession();
		Calendar timestampCal = Calendar.getInstance();
		timestampCal.setTime(new Date());
		String referer = NOREFERRER
		String idp = "unknown"

		if(code && state) {
			String openIdState = session.getAttribute(AuthModule.SessionVars.OPENID_STATE.toString()) as String
			if (openIdState != state) {
				logger.error("${openIdState} is not equal to anti-forge code ${state}, rejecting.")
				resp.sendRedirect("/");
				return;
			} else {
				DecodedJWT jwt = JWT.decode(openIdState);
				referer = jwt.getClaim('referer').asString()
				idp = jwt.getClaim('idp').asString()
			}
		}
		else {
			logger.error("No anti-forgery state provided or code")
			sessionUtils.forwardCorrectly(resp, referer)
		}
		
		List<NameValuePair> form = new ArrayList<NameValuePair>();
		form.add(new BasicNameValuePair("redirect_uri", settingDto.getSetting(AuthModule.Settings.OPENIDCONNECTREDIRECTURI.toString())));
		form.add(new BasicNameValuePair("grant_type", "authorization_code"));
		form.add(new BasicNameValuePair("code", code));

		IdentityProvider identityProvider = identityProviders.getIdentityProvider(idp)
		HttpPost poster = identityProviders.getTokenPost(idp, form)

		logger.info("Calling ${idp} with params:")
		form.each { param ->
			logger.info("${param.name}: ${param.value}")
		}
		UrlEncodedFormEntity entity = new UrlEncodedFormEntity(form, Consts.UTF_8);
		poster.setEntity(entity);

		CloseableHttpResponse response = httpclient.execute(poster);
		if(response.getStatusLine().getStatusCode() == Response.Status.OK.code)
		{
			try {
				// Set profile
				Profile profile = sessionUtils.getProfile()
				identityProvider.getMinimalProfile(profile, response)
				logger.info("Successfully authenticated by OpenID Connect with email ${profile}");
				sessionUtils.setProfile(profile);

				// Check if known to me - only trust email address
				Optional<Resource> signedInPerson = authMgr.authenticateByEmail(profile.email)
				sessionUtils.ifKnownSave(signedInPerson)
				sessionUtils.setIfAdmin(signedInPerson, profile)

				// Forward correctly
				sessionUtils.forwardCorrectly(resp, referer)
			} catch (AuthenticationException e) {
				logger.info("Problems with authentication ${e.getMessage()}")
				sessionUtils.forwardCorrectly(resp, referer)
			}
		}
		else
		{
			logger.warn("Not authenticated.");
			sessionUtils.forwardCorrectly(resp, referer)
		}
	}

	@GET
	@Path("/logoff")
	public String logoff(@Context HttpServletResponse resp, @Context HttpServletRequest req, @QueryParam("action") String action)
	{
		HttpSession session = req.getSession();
		session.removeAttribute(SESSION_PERSON);
		session.removeAttribute(SESSION_PROFILE);

		resp.sendRedirect("/")
	}

}
