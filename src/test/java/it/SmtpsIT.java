package it;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.security.Security;
import java.util.Date;
import java.util.Properties;

import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.Session;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.icegreen.greenmail.user.UserException;
import com.icegreen.greenmail.util.DummySSLSocketFactory;
import com.icegreen.greenmail.util.GreenMail;
import com.icegreen.greenmail.util.ServerSetupTest;
import com.sun.mail.smtp.SMTPTransport;

public class SmtpsIT {
	private static final String USER_PASSWORD = "abcdef123";
	private static final String USER_NAME = "hascode";
	private static final String EMAIL_USER_ADDRESS = "hascode@localhost";
	private static final String EMAIL_TO = "someone@localhost.com";
	private static final String EMAIL_SUBJECT = "Test E-Mail";
	private static final String EMAIL_TEXT = "This is a test e-mail.";
	private static final String LOCALHOST = "127.0.0.1";
	private GreenMail mailServer;

	@Before
	public void setUp() {
		Security.setProperty("ssl.SocketFactory.provider",
				DummySSLSocketFactory.class.getName());
		mailServer = new GreenMail(ServerSetupTest.SMTPS);
		mailServer.start();
	}

	@After
	public void tearDown() {
		mailServer.stop();
	}

	@Test
	public void getMails() throws IOException, MessagingException,
			UserException, InterruptedException {
		// setup user on the mail server
		mailServer.setUser(EMAIL_USER_ADDRESS, USER_NAME, USER_PASSWORD);

		// create the javax.mail stack with session, message and transport ..
		Properties props = System.getProperties();
		props.put("mail.smtps.host", LOCALHOST);
		props.put("mail.smtps.auth", "true");
		props.put("mail.smtps.port", ServerSetupTest.SMTPS.getPort());
		Session session = Session.getInstance(props, null);
		Message msg = new MimeMessage(session);
		msg.setFrom(new InternetAddress(EMAIL_TO));
		msg.setRecipients(Message.RecipientType.TO,
				InternetAddress.parse(EMAIL_USER_ADDRESS, false));
		msg.setSubject(EMAIL_SUBJECT);
		msg.setText(EMAIL_TEXT);
		msg.setSentDate(new Date());
		SMTPTransport t = (SMTPTransport) session.getTransport("smtps");
		t.connect(LOCALHOST, EMAIL_USER_ADDRESS, USER_PASSWORD);
		t.sendMessage(msg, msg.getAllRecipients());

		assertEquals("250 OK\n", t.getLastServerResponse());
		t.close();

		// fetch messages from server
		MimeMessage[] messages = mailServer.getReceivedMessages();
		assertNotNull(messages);
		assertEquals(1, messages.length);
		MimeMessage m = messages[0];
		assertEquals(EMAIL_SUBJECT, m.getSubject());
		assertTrue(String.valueOf(m.getContent()).contains(EMAIL_TEXT));
		assertEquals(EMAIL_TO, m.getFrom()[0].toString());

	}
}
