package com.awidesky.pMailsender;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;

import javax.activation.DataHandler;
import javax.activation.FileDataSource;
import javax.mail.Authenticator;
import javax.mail.Message;
import javax.mail.Multipart;
import javax.mail.PasswordAuthentication;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeMultipart;
import javax.mail.internet.MimeUtility;
import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.UnsupportedLookAndFeelException;
import javax.swing.filechooser.FileFilter;


public class MailSender {

	private static String host;
	private static String user;
	private static String password;
	private static String port;
	private static String chooserLocation;
	
	private static final Properties props = new Properties();
	private static final Session session;
	
	private static final ArrayList<File> files = new ArrayList<>();
	
	private static final JDialog dialog = new JDialog();
	
	
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
		
		session.getProperties().put("mail.smtp.ssl.trust", host);
		session.getProperties().put("mail.smtp.starttls.enable", "true");
		
	}
	
	public static void main(String[] args) {
		
		try {
			UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
		} catch (ClassNotFoundException | InstantiationException | IllegalAccessException
				| UnsupportedLookAndFeelException e) {
			e.printStackTrace();
		}
		
		
		dialog.setAlwaysOnTop(true);
		
		JFileChooser chooser = new JFileChooser((new File(chooserLocation).exists()) ? chooserLocation : null);
		
		chooser.setMultiSelectionEnabled(true);
		chooser.setAccessory(new ImageViewer(chooser));
		chooser.addChoosableFileFilter(new FileFilter() {
			
			public boolean accept(File f) {
				if (f.isDirectory()
						|| f.getName().endsWith(".jpeg")
						|| f.getName().endsWith(".jpg")
						|| f.getName().endsWith(".bmp")
						|| f.getName().endsWith(".png"))
					return true;
				else
					return false;
			}

			public String getDescription() {
				return "Picture files (*.jpeg, *.jpg, *.png, *.bmp)";
			}
		});
		
		chooser.addChoosableFileFilter(new FileFilter() {
			
			public boolean accept(File f) {
				if (f.isDirectory()
						|| f.getName().endsWith(".pdf")
						|| f.getName().endsWith(".docx")
						|| f.getName().endsWith(".hwp")
						|| f.getName().endsWith(".xlsx")
						|| f.getName().endsWith(".pptx"))
					return true;
				else
					return false;
			}

			public String getDescription() {
				return "Document files (*.pdf, *.docx, *.hwp, *.xlsx, *.pptx)";
			}
		});
		
		System.out.println("Running...");
		
		File startPath = new File(chooserLocation);
		while (true) {
			
			StringBuilder sb = new StringBuilder("");
			chooser.setCurrentDirectory(startPath);
			if (chooser.showOpenDialog(dialog) != JFileChooser.APPROVE_OPTION) break;
			List<File> temp = Arrays.asList(chooser.getSelectedFiles());
			startPath = temp.get(temp.size() - 1);
			files.addAll(temp);
			files.stream().forEach((f) -> sb.append(f.getAbsolutePath()).append("\n"));
			System.out.println("Selected files : \n" + sb.toString() + "\n");
		
		}

		dialog.dispose();
		
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
			m1.setText(content.replace("\\n", "\n"), "utf-8");
			
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
			SwingUtilities.invokeLater(() -> {
				
				JOptionPane.showMessageDialog(dialog, "Message Sent Successfully!", "Done!", JOptionPane.INFORMATION_MESSAGE);
				dialog.dispose();
				
			});
			
		} catch (Exception e) {
		
			SwingUtilities.invokeLater(() -> {
				
				JOptionPane.showMessageDialog(dialog, e.getMessage(), "Error!", JOptionPane.ERROR_MESSAGE);
				e.printStackTrace();
				dialog.dispose();
				System.exit(1);
				
			});
			
		}

		
	}

}

