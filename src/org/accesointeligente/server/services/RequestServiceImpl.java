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
package org.accesointeligente.server.services;

import net.sf.gilead.core.PersistentBeanManager;
import net.sf.gilead.gwt.PersistentRemoteService;

import org.accesointeligente.model.Request;
import org.accesointeligente.server.HibernateUtil;
import org.accesointeligente.shared.ServiceException;

import org.hibernate.Criteria;
import org.hibernate.Session;
import org.hibernate.criterion.Restrictions;

@SuppressWarnings("serial")
public class RequestServiceImpl extends PersistentRemoteService{
	private PersistentBeanManager persistentBeanManager;

	public RequestServiceImpl() {
		persistentBeanManager = HibernateUtil.getPersistentBeanManager();
		setBeanManager(persistentBeanManager);
	}

	public Request getRequest(String remoteIdentifier) throws ServiceException {
		Session hibernate = HibernateUtil.getSession();
		hibernate.beginTransaction();

		try {
			Criteria criteria = hibernate.createCriteria(Request.class);
			criteria.add(Restrictions.eq("remoteIdentifier", remoteIdentifier));
			Request request = (Request) criteria.uniqueResult();
			hibernate.getTransaction().commit();
			return (Request) persistentBeanManager.clone(request);
		} catch (Throwable ex) {
			hibernate.getTransaction().rollback();
			throw new ServiceException();
		}
	}
}
