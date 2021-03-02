package com.awidesky.pMailsender;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Properties;

import javax.activation.DataHandler;
import javax.activation.FileDataSource;
import javax.mail.Authenticator;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.Multipart;
import javax.mail.PasswordAuthentication;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeMultipart;
import javax.mail.internet.MimeUtility;
import javax.swing.JFileChooser;


public class MailSender {

	private static String host;
	private static String user;
	private static String password;
	private static String port;
	private static String chooserLocation;
	
	private static final Properties props = new Properties();
	private static final Session session;
	
	private static final ArrayList<File> files = new ArrayList<>();
	
	
	static { /* get email address and password */
		
		try (BufferedReader br = new BufferedReader(new FileReader(new File("config.txt")))) {

			host = br.readLine().substring(7);
			user = br.readLine().substring(7);
			password = br.readLine().substring(11);
			port = br.readLine().substring(7);
			chooserLocation = br.readLine().substring(18);

		} catch (Exception e1) {
			e1.printStackTrace();
		}
		
	}
	
	
	static { /* set JavaMail configurations */
		
		System.setProperty("mail.mime.splitlongparameters", "false");
		
		
		props.put("mail.smtp.host", host);
		props.put("mail.smtp.port", port);
		props.put("mail.smtp.auth", "true");

		session = Session.getDefaultInstance(props, new Authenticator() {
			
			protected PasswordAuthentication getPasswordAuthentication() {
			
				return new PasswordAuthentication(user, password);
				
			}
			
		});
		
	}
	
	public static void main(String[] args) {
		
		
		JFileChooser chooser = new JFileChooser((new File(chooserLocation).exists()) ? chooserLocation : null);
		chooser.setMultiSelectionEnabled(true);
		
		while (true) {

			if (chooser.showOpenDialog(null) != JFileChooser.APPROVE_OPTION) break;  
			files.addAll(Arrays.asList(chooser.getSelectedFiles()));
			System.out.println("Selected files : " + files + "\n");
		
		}

		if (args.length != 0) send(args[0], args[1], files.toArray(new File[]{}));
		else p(files.toArray(new File[]{}));
		
	}
	
	public static void p(File... attatch) {
	
		send("p", " ", attatch);
		
	}
	
	public static void send(String title, String content, File... attatch) {
		
		try {
		
			System.out.println("\tSetting Message Config...");
			MimeMessage message = new MimeMessage(session);
			message.setFrom(new InternetAddress(user));
			message.addRecipient(Message.RecipientType.TO, new InternetAddress(user));
			message.setHeader("content-type", "text/html;charset=UTF-8");
			message.setSubject(title);
			
			System.out.println("\tAdding Text Content Into Message...");
			MimeBodyPart m1 = new MimeBodyPart();
			m1.setText(content, "utf-8");
			
			Multipart mp = new MimeMultipart();
			mp.addBodyPart(m1);
			
			System.out.println("\tAdding File Attachment Into Message...");
			
			for(File f : attatch) {
				
				MimeBodyPart m2 = new MimeBodyPart();
				m2.attachFile(f);
				FileDataSource fds = new FileDataSource(f);
				m2.setDataHandler(new DataHandler(fds));
				m2.setDescription("attach");
				m2.setFileName(MimeUtility.encodeText(f.getName()));
				
				mp.addBodyPart(m2);
				
			}
			
			message.setContent(mp, "text/html;charset=UTF-8");
			
			System.out.println("\tSending Message...");
			Transport.send(message);
			System.out.println("\nMessage Sent Successfully!");
		
		} catch (MessagingException | IOException e) {
		
			e.printStackTrace();
		
		}

		
	}

}

