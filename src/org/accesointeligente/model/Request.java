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
package org.accesointeligente.model;

import org.accesointeligente.shared.RequestStatus;

import net.sf.gilead.pojo.gwt.LightEntity;

import java.util.Set;

@SuppressWarnings("serial")
public class Request extends LightEntity {

	private Integer id;
	private Institution institution; // obtained from the email's TO: field
	private String title;
	private String body;
	private RequestStatus status;
	private Set<RemoteIdentifier> remoteid;
	
	public Integer getId() {
		return id;
	}
	
	public void setId(Integer id) {
		this.id = id;
	}
	
	public Institution getInstitution() {
		return institution;
	}
	
	public void setInstitution(Institution institution) {
		this.institution = institution;
	}
	
	public String getTitle() {
		return title;
	}
	
	public void setTitle(String title) {
		this.title = title;
	}
	
	public String getBody() {
		return body;
	}
	
	public void setBody(String body) {
		this.body = body;
	}

	public RequestStatus getStatus() {
		return status;
	}

	public void setStatus(RequestStatus status) {
		this.status = status;
	}

	public Set<RemoteIdentifier> getRemoteid() {
		return remoteid;
	}

	public void setRemoteid(Set<RemoteIdentifier> remoteid) {
		this.remoteid = remoteid;
	}
}