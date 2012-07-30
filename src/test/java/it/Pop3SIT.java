package it;

import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.security.Security;
import java.util.Properties;

import javax.mail.Folder;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.Session;
import javax.mail.Store;
import javax.mail.URLName;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.icegreen.greenmail.user.GreenMailUser;
import com.icegreen.greenmail.user.UserException;
import com.icegreen.greenmail.util.DummySSLSocketFactory;
import com.icegreen.greenmail.util.GreenMail;
import com.icegreen.greenmail.util.ServerSetupTest;

public class Pop3SIT {
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
		mailServer = new GreenMail(ServerSetupTest.POP3S);
		mailServer.start();
	}

	@After
	public void tearDown() {
		mailServer.stop();
	}

	@Test
	public void getMails() throws IOException, MessagingException,
			UserException, InterruptedException {
		// create user on mail server
		GreenMailUser user = mailServer.setUser(EMAIL_USER_ADDRESS, USER_NAME,
				USER_PASSWORD);

		// create an e-mail message using javax.mail ..
		MimeMessage message = new MimeMessage((Session) null);
		message.setFrom(new InternetAddress(EMAIL_TO));
		message.addRecipient(Message.RecipientType.TO, new InternetAddress(
				EMAIL_USER_ADDRESS));
		message.setSubject(EMAIL_SUBJECT);
		message.setText(EMAIL_TEXT);

		// use greenmail to store the message
		user.deliver(message);

		// fetch the e-mail from pop3 using javax.mail ..
		Properties props = new Properties();
		props.setProperty("mail.pop3.connectiontimeout", "5000");
		Session session = Session.getInstance(props);
		URLName urlName = new URLName("pop3s", LOCALHOST,
				ServerSetupTest.POP3S.getPort(), null, user.getLogin(),
				user.getPassword());
		Store store = session.getStore(urlName);
		store.connect();

		Folder folder = store.getFolder("INBOX");
		folder.open(Folder.READ_ONLY);
		Message[] messages = folder.getMessages();
		assertNotNull(messages);
		assertThat(1, equalTo(messages.length));
		assertEquals(EMAIL_SUBJECT, messages[0].getSubject());
		assertTrue(String.valueOf(messages[0].getContent())
				.contains(EMAIL_TEXT));
		assertEquals(EMAIL_TO, messages[0].getFrom()[0].toString());
	}
}
