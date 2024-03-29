/**
 * Acceso Inteligente
 *
 * Copyright (C) 2010-2011 Fundación Ciudadano Inteligente
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

import org.accesointeligente.model.Attachment;
import org.accesointeligente.model.Request;
import org.accesointeligente.model.Response;
import org.accesointeligente.server.ApplicationProperties;
import org.accesointeligente.server.HibernateUtil;
import org.accesointeligente.server.RandomPassword;
import org.accesointeligente.shared.*;

import org.apache.commons.io.FileUtils;
import org.apache.log4j.Logger;
import org.apache.poi.hwpf.extractor.WordExtractor;
import org.apache.poi.xwpf.extractor.XWPFWordExtractor;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.hibernate.Criteria;
import org.hibernate.criterion.Restrictions;
import org.htmlcleaner.HtmlCleaner;
import org.htmlcleaner.TagNode;

import com.itextpdf.text.pdf.PdfReader;
import com.itextpdf.text.pdf.parser.PdfTextExtractor;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.mail.*;
import javax.mail.Flags.Flag;
import javax.mail.internet.MimeUtility;
import javax.mail.search.FlagTerm;

public class ResponseChecker {
	private static final Logger logger = Logger.getLogger(ResponseChecker.class);
	private Properties props;
	private Session session;
	private Store store;
	private Pattern pattern = Pattern.compile(".*([A-Z]{2})[- ]{0,1}([0-9]{3}[A-Z])[- ]{0,1}([0-9]{1,8}).*");
	private Pattern htmlPattern = Pattern.compile("<.*?>");
	private Set<String> remoteIdentifiers;
	private String messageBody;

	public ResponseChecker() {
		props = System.getProperties();
		props.setProperty("mail.store.protocol", "imaps");
	}

	public void connectAndCheck() {
		if (ApplicationProperties.getProperty("email.server") == null || ApplicationProperties.getProperty("email.user") == null ||
				ApplicationProperties.getProperty("email.password") == null || ApplicationProperties.getProperty("email.folder") == null ||
				ApplicationProperties.getProperty("email.failfolder") == null || ApplicationProperties.getProperty("attachment.directory") == null ||
				ApplicationProperties.getProperty("attachment.baseurl") == null) {
			logger.error("Properties are not defined!");
			return;
		}

		org.hibernate.Session hibernate = null;

		try {
			session = Session.getInstance(props, null);
			store = session.getStore("imaps");
			store.connect(ApplicationProperties.getProperty("email.server"), ApplicationProperties.getProperty("email.user"), ApplicationProperties.getProperty("email.password"));

			Folder failbox = store.getFolder(ApplicationProperties.getProperty("email.failfolder"));
			Folder inbox = store.getFolder(ApplicationProperties.getProperty("email.folder"));
			inbox.open(Folder.READ_WRITE);

			for (Message message : inbox.search(new FlagTerm(new Flags(Flags.Flag.SEEN), false))) {
				try {
					logger.info("Sender: " + message.getFrom()[0] + "\tSubject: " + message.getSubject());
					remoteIdentifiers = null;
					messageBody = null;
					remoteIdentifiers = new HashSet<String>();

					if (message.getSubject() != null) {
						Matcher matcher = pattern.matcher(message.getSubject());

						if (matcher.matches()) {
							remoteIdentifiers.add(formatIdentifier(matcher.group(1),matcher.group(2), Integer.parseInt(matcher.group(3))));
							logger.info("remote identifier: " + formatIdentifier(matcher.group(1),matcher.group(2), Integer.parseInt(matcher.group(3))));
						}
					}

					Object content = message.getContent();

					if (content instanceof Multipart) {
						Multipart mp = (Multipart) message.getContent();
						logger.info("Email content type is Multipart, each part of " + mp.getCount() + " will be processed");

						for (int i = 0, n = mp.getCount(); i < n; i++) {
							Part part = mp.getBodyPart(i);
							logger.info("Part: " + (i + 1) + " of " + mp.getCount());
							processPart(part);
						}
					} else if (content instanceof String) {
						logger.info("Email content type is String");
						messageBody = (String) content;
						Matcher matcher;
						StringTokenizer tokenizer = new StringTokenizer(messageBody);

						while (tokenizer.hasMoreTokens()) {
							String token = tokenizer.nextToken();
							matcher = pattern.matcher(token);

							if (matcher.matches()) {
								remoteIdentifiers.add(formatIdentifier(matcher.group(1),matcher.group(2), Integer.parseInt(matcher.group(3))));
								logger.info("remote identifier: " + formatIdentifier(matcher.group(1),matcher.group(2), Integer.parseInt(matcher.group(3))));
							}
						}
					} else {
						logger.info("Email content type isn't String or Multipart");
						message.setFlag(Flag.SEEN, false);
						inbox.copyMessages(new Message[] {message}, failbox);
						message.setFlag(Flag.DELETED, true);
						inbox.expunge();
						continue;
					}

					Boolean requestFound = false;
					Matcher matcher = htmlPattern.matcher(messageBody);

					if (matcher.find()) {
						messageBody = htmlToString(messageBody);
					}

					logger.info("Searching for Request Remote Identifier");
					for (String remoteIdentifier : remoteIdentifiers) {
						hibernate = HibernateUtil.getSession();
						hibernate.beginTransaction();

						Criteria criteria = hibernate.createCriteria(Request.class);
						criteria.add(Restrictions.eq("remoteIdentifier", remoteIdentifier));
						Request request = (Request) criteria.uniqueResult();
						hibernate.getTransaction().commit();

						if (request != null) {
							logger.info("Request found for Remote Identifier: " + remoteIdentifier);
							Response response;

							// If the attachments haven't been used, use them. Otherwise, copy them.
							if (!requestFound) {
								response = createResponse(message.getFrom()[0].toString(), message.getSentDate(), message.getSubject(), messageBody);
							} else {
								response = createResponse(message.getFrom()[0].toString(), message.getSentDate(), message.getSubject(), messageBody);
							}

							hibernate = HibernateUtil.getSession();
							hibernate.beginTransaction();

							response.setRequest(request);
							request.setStatus(RequestStatus.RESPONDED);
							request.setExpired(RequestExpireType.WITHRESPONSE);
							request.setResponseDate(new Date());
							hibernate.update(request);
							hibernate.update(response);
							hibernate.getTransaction().commit();
							requestFound = true;
						}
					}

					if (!requestFound) {
						logger.info("Request not found");
						createResponse(message.getFrom()[0].toString(), message.getSentDate(), message.getSubject(), messageBody);
						message.setFlag(Flag.SEEN, false);
						inbox.copyMessages(new Message[] {message}, failbox);
						message.setFlag(Flag.DELETED, true);
						inbox.expunge();
					}
				} catch (Exception e) {
					if (hibernate != null && hibernate.isOpen() && hibernate.getTransaction().isActive()) {
						hibernate.getTransaction().rollback();
					}

					logger.error(e.getMessage(), e);
				}
			}
		} catch (Exception e) {
			if (hibernate != null && hibernate.isOpen() && hibernate.getTransaction().isActive()) {
				hibernate.getTransaction().rollback();
			}

			logger.error(e.getMessage(), e);
		}
	}

	private String formatIdentifier(String prefix, String suffix, Integer number) {
		return String.format("%s%s-%07d", prefix, suffix, number);
	}

	private Response createResponse(String sender, Date date, String subject, String information, List<Attachment> attachments) {
		org.hibernate.Session hibernate = HibernateUtil.getSession();
		hibernate.beginTransaction();
		Response response = new Response();
		response.setSender(sender);
		response.setDate(date);
		response.setSubject(subject);
		response.setInformation(information);
		response.setUserSatisfaction(UserSatisfaction.NOANSWER);
		response.setResponseKey(RandomPassword.getRandomString(10));
		hibernate.save(response);
		hibernate.getTransaction().commit();

		hibernate = HibernateUtil.getSession();
		hibernate.beginTransaction();

		for (Attachment attachment : attachments) {
			attachment.setResponse(response);
			hibernate.update(attachment);
		}

		hibernate.getTransaction().commit();
		return response;
	}

	private List<Attachment> cloneAttachments(List<Attachment> attachments) {
		List<Attachment> newAttachments = new ArrayList<Attachment>(attachments.size());

		for (Attachment oldAttachment : attachments) {
			Attachment newAttachment = cloneAttachment(oldAttachment);

			if (newAttachment != null) {
				newAttachments.add(newAttachment);
			}
		}

		return newAttachments;
	}

	private Attachment cloneAttachment(Attachment attachment) {
		org.hibernate.Session hibernate = HibernateUtil.getSession();
		hibernate.beginTransaction();
		Attachment newAttachment = new Attachment();
		hibernate.save(newAttachment);
		hibernate.getTransaction().commit();

		File oldFile = new File(ApplicationProperties.getProperty("attachment.directory") + attachment.getId().toString() + "/" + attachment.getName());
		File newDirectory = new File(ApplicationProperties.getProperty("attachment.directory") + newAttachment.getId().toString());

		try {
			newDirectory.mkdir();
			FileUtils.copyFileToDirectory(oldFile, newDirectory);
		} catch (Exception e) {
			hibernate = HibernateUtil.getSession();
			hibernate.beginTransaction();
			hibernate.delete(attachment);
			hibernate.getTransaction().commit();
			logger.error("Error saving " + newDirectory.getAbsolutePath() + "/" + attachment.getName(), e);
			return null;
		}

		String baseUrl = ApplicationProperties.getProperty("attachment.baseurl") + newAttachment.getId().toString();

		newAttachment.setName(attachment.getName());
		newAttachment.setType(attachment.getType());
		newAttachment.setUrl(baseUrl + "/" + newAttachment.getName());

		hibernate = HibernateUtil.getSession();
		hibernate.beginTransaction();
		hibernate.update(newAttachment);
		hibernate.getTransaction().commit();
		return newAttachment;
	}

	private String htmlToString(String string) throws IOException {
		TagNode body;
		TagNode htmlDocument;
		string = string.replaceAll("<br/>", "\n");
		HtmlCleaner cleaner = new HtmlCleaner();
		htmlDocument = cleaner.clean(string);
		body = (TagNode) htmlDocument.getElementsByName("body", true)[0];
		String messageBody = body.getText().toString();

		messageBody = messageBody.replaceAll("&nbsp;", " ");
		messageBody = messageBody.replaceAll("[\t ]+", " ");
		messageBody = messageBody.trim();
		messageBody = messageBody.replaceAll("(\n )", "\n");
		messageBody = messageBody.replaceAll("&aacute;", "á");
		messageBody = messageBody.replaceAll("&Aacute;", "Á");
		messageBody = messageBody.replaceAll("&eacute;", "é");
		messageBody = messageBody.replaceAll("&Eacute;", "É");
		messageBody = messageBody.replaceAll("&iacute;", "í");
		messageBody = messageBody.replaceAll("&Iacute;", "Í");
		messageBody = messageBody.replaceAll("&oacute;", "ó");
		messageBody = messageBody.replaceAll("&Oacute;", "Ó");
		messageBody = messageBody.replaceAll("&uacute;", "ú");
		messageBody = messageBody.replaceAll("&Uacute;", "Ú");
		messageBody = messageBody.replaceAll("&ntilde;", "ñ");
		messageBody = messageBody.replaceAll("&Ntilde;", "Ñ");
		return messageBody;
	}
}
