/**
 * Acceso Inteligente
 *
 * Copyright (C) 2010-2012 Fundación Ciudadano Inteligente
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.accesointeligente.server.robots;

import org.accesointeligente.model.RemoteIdentifier;
import org.accesointeligente.model.Request;
import org.accesointeligente.server.ApplicationProperties;
import org.accesointeligente.shared.RequestStatus;

import org.apache.http.Header;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.params.HttpProtocolParams;
import org.apache.http.util.EntityUtils;
import org.apache.log4j.Logger;
import org.htmlcleaner.HtmlCleaner;
import org.htmlcleaner.TagNode;

import java.beans.ConstructorProperties;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class SIAC extends Robot {
	private static final Logger logger = Logger.getLogger(SIAC.class);
	private HttpClient client;
	private HtmlCleaner cleaner;
	private Boolean loggedIn = false;
	private String characterEncoding = null;
	private String baseUrl;
	private String userId;

	public SIAC() {
		client = new DefaultHttpClient();
		HttpProtocolParams.setUserAgent(client.getParams(), "Mozilla/5.0 (X11; U; Linux x86_64; es-CL; rv:1.9.2.12) Gecko/20101027 Ubuntu/10.10 (maverick) Firefox/3.6.12");
		cleaner = new HtmlCleaner();
	}

	@ConstructorProperties({"idEntidad", "baseUrl"})
	public SIAC(String idEntidad, String baseUrl) {
		this();
		setIdEntidad(idEntidad);
		setBaseUrl(baseUrl);
	}

	@Override
	public void login() throws Exception {
		if (characterEncoding == null) {
			detectCharacterEncoding();
		}

		List<NameValuePair> formParams;
		HttpPost post;
		HttpResponse response;
		TagNode document, hiddenUser;

		try {
			formParams = new ArrayList<NameValuePair>();
			formParams.add(new BasicNameValuePair("usuario", username));
			formParams.add(new BasicNameValuePair("clave", password));
			formParams.add(new BasicNameValuePair("accion", "login"));

			post = new HttpPost(baseUrl + "/formulario.gov");
			post.addHeader("Referer", baseUrl + "/formulario.gov?accion=ingresa");
			post.setEntity(new UrlEncodedFormEntity(formParams, characterEncoding));
			response = client.execute(post);
			document = cleaner.clean(new InputStreamReader(response.getEntity().getContent(), characterEncoding));
			hiddenUser = document.findElementByAttValue("id", "user", true, true);

			if (hiddenUser == null || !hiddenUser.hasAttribute("value") || hiddenUser.getAttributeByName("value").equals("0")) {
				throw new RobotException("Invalid user id field");
			}

			userId = hiddenUser.getAttributeByName("value");
			loggedIn = true;
		} catch (Exception ex) {
			logger.error(ex.getMessage(), ex);
			throw ex;
		}
	}

	@Override
	public Request makeRequest(Request request) throws Exception {
		if (!loggedIn) {
			login();
		}

		List<NameValuePair> formParams;
		HttpPost post;
		HttpResponse response;
		TagNode document;
		Pattern pattern = Pattern.compile("^Solicitud ingresada exitosamente, el número de su solicitud es: ([A-Z]{2}[0-9]{3}[A-Z])([0-9]{7})$");
		Matcher matcher;

		try {
			formParams = new ArrayList<NameValuePair>();
			formParams.add(new BasicNameValuePair("user", userId));
			formParams.add(new BasicNameValuePair("accion", "registrar"));
			formParams.add(new BasicNameValuePair("tipo", "7"));
			formParams.add(new BasicNameValuePair("dirigido", idEntidad));
			formParams.add(new BasicNameValuePair("ley_email", "S"));
			formParams.add(new BasicNameValuePair("detalle", request.getTitle() + "\n\n" + request.getBody() + "\n\n" + ApplicationProperties.getProperty("request.signature")));
			formParams.add(new BasicNameValuePair("remLen1", "" + (1996 - request.getTitle().length() - request.getBody().length() - ApplicationProperties.getProperty("request.signature").length())));
			formParams.add(new BasicNameValuePair("nombre", "Felipe"));
			formParams.add(new BasicNameValuePair("ap_paterno", "Heusser"));
			formParams.add(new BasicNameValuePair("ap_materno", "Ferres"));
			formParams.add(new BasicNameValuePair("rut", ""));
			formParams.add(new BasicNameValuePair("rut_dv", ""));
			formParams.add(new BasicNameValuePair("pasaporte", ""));
			formParams.add(new BasicNameValuePair("nacionalidad", "320"));
			formParams.add(new BasicNameValuePair("tipo_zona", "U"));
			formParams.add(new BasicNameValuePair("educacion", "0"));
			formParams.add(new BasicNameValuePair("edad", "0"));
			formParams.add(new BasicNameValuePair("ocupacion", "0"));
			formParams.add(new BasicNameValuePair("sexo", "M"));
			formParams.add(new BasicNameValuePair("rb_organizacion", "S"));
			formParams.add(new BasicNameValuePair("razon_social", "Fundación Ciudadano Inteligente"));
			formParams.add(new BasicNameValuePair("rb_apoderado", "N"));
			formParams.add(new BasicNameValuePair("nombre_apoderado", ""));
			formParams.add(new BasicNameValuePair("residencia", "CHI"));
			formParams.add(new BasicNameValuePair("direccion", "Holanda"));
			formParams.add(new BasicNameValuePair("direccion_numero", "895"));
			formParams.add(new BasicNameValuePair("direccion_villa", ""));
			formParams.add(new BasicNameValuePair("fono_area", ""));
			formParams.add(new BasicNameValuePair("fono", ""));
			formParams.add(new BasicNameValuePair("comuna", "13123"));
			formParams.add(new BasicNameValuePair("email", "info@accesointeligente.org"));
			formParams.add(new BasicNameValuePair("email_verif", "info@accesointeligente.org"));
			formParams.add(new BasicNameValuePair("bt_modificar", "Enviar Datos"));

			post = new HttpPost(baseUrl + "/formulario.gov");
			post.addHeader("Referer", baseUrl + "/formulario.gov?accion=ingresa");
			post.setEntity(new UrlEncodedFormEntity(formParams, characterEncoding));
			response = client.execute(post);
			document = cleaner.clean(new InputStreamReader(response.getEntity().getContent(), characterEncoding));

			for (TagNode node : document.getElementsByName("li", true)) {
				matcher = pattern.matcher(node.getText().toString().trim());

				if (matcher.matches()) {
					RemoteIdentifier remote = new RemoteIdentifier();
					remote.setName(matcher.group(1) + "-" + matcher.group(2));
					Set<RemoteIdentifier> remotesId = request.getRemoteid();
					remotesId.add(remote);
					request.setRemoteid(remotesId);
					break;
				}
			}

			if (!request.getRemoteid().isEmpty()) {
				request.setStatus(RequestStatus.PROCESSED);
				return request;
			} else {
				request.setStatus(RequestStatus.ERROR);
				throw new RobotException("Remote identifier not found after request");
			}
		} catch (Exception ex) {
			logger.error(ex.getMessage(), ex);
			throw ex;
		}
	}

	@Override
	public Boolean checkInstitutionId() throws Exception {
		if (!loggedIn) {
			login();
		}

		HttpGet get;
		HttpResponse response;
		TagNode document, selector;

		try {
			get = new HttpGet(baseUrl + "/formulario.gov?accion=ingresa");
			response = client.execute(get);
			document = cleaner.clean(new InputStreamReader(response.getEntity().getContent(), characterEncoding));
			selector = document.findElementByAttValue("name", "dirigido", true, true);

			if (selector == null) {
				throw new RobotException("Institution selector not found");
			}

			for (TagNode option : selector.getElementsByName("option", true)) {
				if (option.hasAttribute("value") && option.getAttributeByName("value").equals(idEntidad)) {
					return true;
				}
			}

			return false;
		} catch (Exception ex) {
			logger.error(ex.getMessage(), ex);
			throw ex;
		}
	}

	public void detectCharacterEncoding() {
		HttpGet get;
		HttpResponse response;
		Header contentType;
		Pattern pattern;
		Matcher matcher;

		try {
			get = new HttpGet(baseUrl + "/formulario.gov?accion=ingresa");
			response = client.execute(get);
			contentType = response.getFirstHeader("Content-Type");
			EntityUtils.consume(response.getEntity());

			if (contentType == null || contentType.getValue() == null) {
				characterEncoding = "ISO-8859-1";
			}

			pattern = Pattern.compile(".*charset=(.+)");
			matcher = pattern.matcher(contentType.getValue());

			if (!matcher.matches()) {
				characterEncoding = "ISO-8859-1";
			}

			characterEncoding = matcher.group(1);
		} catch (Exception e) {
			characterEncoding = "ISO-8859-1";
		}
	}

	public String getCharacterEncoding() {
		return characterEncoding;
	}

	public void setCharacterEncoding(String characterEncoding) {
		this.characterEncoding = characterEncoding;
	}

	public String getBaseUrl() {
		return baseUrl;
	}

	public void setBaseUrl(String baseUrl) {
		this.baseUrl = baseUrl;
	}

	public String getUserId() {
		return userId;
	}

	public void setUserId(String userId) {
		this.userId = userId;
	}
}
