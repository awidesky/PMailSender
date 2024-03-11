package com.awidesky.pMailsender;

import java.awt.Desktop;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.nio.charset.Charset;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;
import java.util.Properties;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.UnsupportedLookAndFeelException;

import io.github.awidesky.guiUtil.LoggerThread;
import io.github.awidesky.guiUtil.SwingDialogs;
import io.github.awidesky.guiUtil.TaskLogger;
import jakarta.activation.DataHandler;
import jakarta.activation.FileDataSource;
import jakarta.mail.AuthenticationFailedException;
import jakarta.mail.Authenticator;
import jakarta.mail.Message;
import jakarta.mail.MessagingException;
import jakarta.mail.Multipart;
import jakarta.mail.PasswordAuthentication;
import jakarta.mail.Session;
import jakarta.mail.Transport;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeBodyPart;
import jakarta.mail.internet.MimeMessage;
import jakarta.mail.internet.MimeMultipart;
import jakarta.mail.internet.MimeUtility;


public class MailSender {

	public static String projectPath = "";
	static {
		String home = System.getProperty("user.home");
		String os = System.getProperty("os.name").toLowerCase();
		if (os.startsWith("mac")) {
			projectPath = home + "/Library/Application Support/awidesky/PMailSender";
		} else if (os.startsWith("windows")) {
			projectPath = System.getenv("LOCALAPPDATA") + "\\awidesky\\PMailSender";
		} else {
			// Assume linux.
			projectPath = home + "/.local/share/awidesky/PMailSender";
		}
		File f = new File(projectPath);
		f.mkdirs();
		if (!f.exists()) {
			SwingDialogs.error("Cannot detect appdata directory!", projectPath + "\nis not a valid data directory!", null, true);
			System.exit(1);
		}
		projectPath += File.separator;
	}

	private static LoggerThread loggerThread = new LoggerThread();
	private static TaskLogger logger = loggerThread.getLogger();
	
	private static String host;
	private static String user;
	private static String password;
	private static String port;
	private static String jmaildebug;
	private static String chooserLocation = ".";
	
	private static String title = "P";
	private static String content = " ";
	
	private static final Properties props = new Properties();
	private static Session session;
	
	private static MainFrame mainFrame;
	private static List<File> files = new LinkedList<>();
	
	public static void main(String[] args) {
		
		setupLogging();
		
		try {

			SwingUtilities.invokeAndWait(() -> {
				try {
					UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
				} catch (ClassNotFoundException | InstantiationException | IllegalAccessException
						| UnsupportedLookAndFeelException e) {
					SwingDialogs.error("Cannot set Look&Feel", "%e%", e, true);
				}
				mainFrame = new MainFrame(loggerThread.getLogger(), title, content);
				mainFrame.setUp();
			});
			config(args);
			setSession();
			if(checkLastAttempt()) {
				exit();
				return;
			}
			
			mainFrame.log("Running...");

			File startPath = new File(chooserLocation);
			files = mainFrame.chooseLoop(startPath);

			title = mainFrame.getTitle();
			content = mainFrame.getContent();
			
			files = files.stream().sorted((f1, f2) -> Long.valueOf(f1.length()).compareTo(Long.valueOf(f2.length()))).collect(Collectors.toCollection(LinkedList::new));

			long attatchLimit = mainFrame.getAttatchLimit();
			if (files.stream().map(File::length).reduce(0L, (a, b) -> a + b) >= attatchLimit) { //if sum of attachment is bigger than 10MB(probably Naver mail limit)

				title += " + 링크(들)도 클릭";
				List<File> dropboxed;
				mainFrame.log("Mail attachment too big! (>" + attatchLimit / (1024 * 1024) + "MB)");
				mainFrame.log("Trying dropbox link instead..");

				dropboxed = files.stream().filter(f -> f.length() >= attatchLimit).collect(Collectors.toList());
				if(dropboxed.size() != 0) {
					files.removeAll(dropboxed);
				}

				long totalSize = 0L;
				for(int i = 0; i < files.size(); i++) {

					totalSize += files.get(i).length();

					if(totalSize >= attatchLimit) {
						//No super big file(s), but still exceed limit.

						List<File> temp = files.subList(i, files.size());
						dropboxed.addAll(temp);
						temp.clear();

					}

				}

				content += System.lineSeparator() + new DropboxFileUploader(mainFrame).uploadFileAndGetLink(dropboxed, "/document/");

			}
		
			send(title, content, files);
		} catch (Exception e) {
			saveMail(title, content, files);
			SwingDialogs.error("Error!", "%e%", e, true);
		}
		exit();
	}
	

	private static void setupLogging() {
		/** Set Default Uncaught Exception Handlers */
		Thread.setDefaultUncaughtExceptionHandler((t, e) -> {
			try {
				SwingDialogs.error("Unhandled exception in thread " + t.getName() + " : " + e.getClass().getName(), "%e%", e , true);
				loggerThread.shutdown(1000);
				System.exit(2);
			} catch(Exception err) {
				err.printStackTrace();
			}
		});
		SwingUtilities.invokeLater(() -> {
			Thread.currentThread().setUncaughtExceptionHandler((t, e) -> {
				try {
					SwingDialogs.error("Unhandled exception in EDT : " + e.getClass().getName(), "%e%", e, true);
					loggerThread.shutdown(1000);
					System.exit(2);
				} catch (Exception err) {
					err.printStackTrace();
				}
			});
		});
		loggerThread.setDatePrefix(new SimpleDateFormat("[kk:mm:ss.SSS]"));
		File logFolder = new File(projectPath + "logs");
		File logFile = new File(logFolder.getAbsolutePath() + File.separator + "log-" + new SimpleDateFormat("yyyy-MM-dd-kk-mm-ss").format(new Date()) + ".txt");
		logFolder.mkdirs();
		try {
			logFile.createNewFile();
			loggerThread.setLogDestination(new FileOutputStream(logFile), true);
		} catch (IOException e1) {
			SwingDialogs.error("Unable to create log file!", "Loging to console instead...\n%e%", e1, true);
			loggerThread.setLogDestination(System.out);
		}
		loggerThread.start();
		SwingDialogs.setLogger(loggerThread.getLogger());
	}
	/**
	 * 
	 * @return <code>true</code> when last saved mail is sent.
	 * @throws Exception When failed during sending mail.
	 * */
	private static boolean checkLastAttempt() throws Exception {
		
		logger.log("Check last attempt...");
		if (new File(projectPath + "lastTriedMailContent.txt").exists()) {
			
			if (SwingDialogs.confirm("Retry sending last saved mail?", "Last attempt wasn't successful!")) {
				sendSavedMail();
				return true;
			} else {
				new File(projectPath + "lastTriedMailContent.txt").delete();
				new File(projectPath + "lastTriedMailAttachment.txt").delete();
			}
		}
		return false;
		
	}
	
	private static void resetLogin() throws MessagingException {
		user = SwingDialogs.input("Username for " + host, "Username :", user);
		password = String.valueOf(SwingDialogs.inputPassword("Password for " + user, "Password :"));
		setSession();
	}

	private static void setSession() throws MessagingException {
		
		mainFrame.log("Preparing session...");
		
		System.setProperty("mail.mime.splitlongparameters", "false");
		
		
		props.put("mail.smtp.host", host);
		props.put("mail.smtp.port", port);
		props.put("mail.smtp.auth", "true");
		props.put("mail.smtp.ssl.trust", host);
		props.put("mail.smtp.ssl.enable", "true");
		props.put("mail.debug", jmaildebug);
		props.put("mail.smtp.connectiontimeout", "5000");
		//mail.smtp.ssl.protocols TLSv1.2
		props.put("mail.smtp.starttls.enable", "true");

		session = Session.getInstance(props, new Authenticator() {
			protected PasswordAuthentication getPasswordAuthentication() {
				return new PasswordAuthentication(user, password);
			}
		});
		
		session.setDebugOut(new PrintStream(new OutputStream() {
			private final TaskLogger tl = loggerThread.getLogger("[jakarta.mail] ");
			@Override
			public void write(int b) throws IOException {
				write(new byte[] {(byte)b});
			}

			@Override
			public void write(byte[] b) throws IOException {
				super.write(b);
			}

			@Override
			public void write(byte[] b, int off, int len) throws IOException {
				tl.log(new String(b, off, len, Charset.defaultCharset()).stripTrailing());
			}
			
		}));
		
		try {
			Transport transport;
			transport = session.getTransport();
			transport.connect();
			transport.close();
		} catch (AuthenticationFailedException e) {
			SwingDialogs.error("Authentication Failed!", "%e%", e, true);
			resetLogin();
		}
	}
	
	
	private static void config(String[] args) {
		mainFrame.log("Reading config file from " + projectPath);
		File configFile = new File(projectPath + "config.txt");
		try { 
			if(!configFile.exists()) {
				configFile.createNewFile();
				try(PrintWriter pw = new PrintWriter(configFile)) {
					pw.println("host = ");
					pw.println("user = ");
					pw.println("password = ");
					pw.println("port = ");
					pw.println("jmail.debug = ");
					pw.println("chooserLocation = ");
					pw.println();
					pw.println();
					pw.println("#for example :");
					pw.println("#host = smtp.gmail.com");
					pw.println("#user = JohnDoe@gmail.com");
					pw.println("#password = doeAdearFema1eDeer1234");
					pw.println("#port = 465");
					pw.println("#jmail.debug = true");
					pw.println("#chooserLocation = C:\\Users\\John Doe\\Downloads");
				}
				throw new FileNotFoundException(configFile.getAbsolutePath() + " was not found!");
			}

			Properties config = new Properties();
			config.load(new FileInputStream(configFile));
			if(Objects.isNull(password = config.getProperty("password"))) password = String.valueOf(SwingDialogs.inputPassword("Password", "Enter password : "));
			if(Stream.of(host = config.getProperty("host"),
					user = config.getProperty("user"),
					port = config.getProperty("port"),
					jmaildebug = config.getProperty("jmail.debug", "false"),
					chooserLocation = config.getProperty("chooserLocation", System.getProperty("user.home")))
					.anyMatch(Objects::isNull)) {
				throw new RuntimeException("One(s) of the properties are invalid!");
			}

		} catch (Exception e1) {
			SwingDialogs.error(e1.toString(), "Please write valid smtp configuration(password is optional) and restart the application!\n%e%", e1, true);
			try {
				Desktop.getDesktop().open(configFile);
			} catch (IOException e) {
				SwingDialogs.error("Unable to open : " + configFile.getAbsolutePath(), "%e%", e, true);
			}
			exit();
			System.exit(1);
		}
		
		
		mainFrame.log("Checking Command line argument...");
		for (int i = 0; i < args.length; i++) {

			if (args[i].startsWith("-title=")) {
				title = args[i].replace("-title=", "");
			}

			if (args[i].startsWith("-content=")) {
				content = args[i].replace("-content=", "");
			}

			if (args[i].equals("-files")) {
				files.addAll(Arrays.asList(args).subList(i + 1, args.length).stream().map(File::new)
						.collect(Collectors.toList()));
				break;
			}
		}
		
	}
		
	private static void sendSavedMail() throws Exception {
		
		BufferedReader br = new BufferedReader(new FileReader(new File(projectPath + "lastTriedMailContent.txt")));
		String line = null, title;
		List<String> content = new LinkedList<>();
		
		title = br.readLine();
		
		while((line = br.readLine()) != null) {
			content.add(line); 
		}
		
		br.close();
		
		setSavedAttatchment();
		send(title, content.stream().collect(Collectors.joining(System.lineSeparator())), files);
		
		new File(projectPath + "lastTriedMailContent.txt").delete();
		new File(projectPath + "lastTriedMailAttachment.txt").delete();
		
	}

	private static void setSavedAttatchment() throws Exception {
		
		BufferedReader br = new BufferedReader(new FileReader(new File(projectPath + "lastTriedMailAttachment.txt")));
		String line = null;
		files = new LinkedList<>();
		
		while((line = br.readLine()) != null) {
			files.add(new File(line)); 
		}
		
		br.close();
		
	}
	
	private static void saveMail(String title, String content, List<File> files2) {

		try (PrintWriter pw1 = new PrintWriter(new File(projectPath + "lastTriedMailContent.txt"));
				PrintWriter pw2 = new PrintWriter(new File(projectPath + "lastTriedMailAttachment.txt"))) {

			pw1.println(title);
			pw1.println(content);

			files2.stream().map(File::getAbsolutePath).forEach(pw2::println);

		} catch (IOException e) {
			
			mainFrame.log();
			mainFrame.log("Error when saving draft!!");
			mainFrame.log(e);
			mainFrame.log();
			
		}

	}
	
	
	public static void send(String title, String content, List<File> attatch) throws Exception {

		mainFrame.log("\tSetting Message Config...");
		MimeMessage message = new MimeMessage(session);
		message.setFrom(new InternetAddress(user));
		message.addRecipient(Message.RecipientType.TO, new InternetAddress(user));
		message.setHeader("content-type", "text/html;charset=UTF-8");
		message.setSubject(title);

		mainFrame.log("\tAdding Text Content Into Message...");
		MimeBodyPart m1 = new MimeBodyPart();
		m1.setText(content.replace("\\n", "\n"), "utf-8");

		Multipart mp = new MimeMultipart();
		mp.addBodyPart(m1);

		mainFrame.log("\tAdding File Attachment Into Message...");

		for (File f : attatch) {
				
				MimeBodyPart m2 = new MimeBodyPart();
				m2.attachFile(f);
				FileDataSource fds = new FileDataSource(f);
				m2.setDataHandler(new DataHandler(fds));
				m2.setDescription("attach");
				m2.setFileName(MimeUtility.encodeText(f.getName()));
				
				mp.addBodyPart(m2);
				
		}
		
		message.setContent(mp, "text/html;charset=UTF-8");
			
		mainFrame.log("\tSending Message...");
		try {
			Transport.send(message);
		} catch (AuthenticationFailedException e) {
			SwingDialogs.error("Authentication Failed", "%e%", e, true);
			resetLogin();
			send(title, content, attatch);
			return;
		}
		mainFrame.log("\nMessage Sent Successfully!");
		SwingDialogs.information("Message Sent Successfully!", "Done!", true);
	}
	

	private static void exit() {
		SwingUtilities.invokeLater(mainFrame::dispose);
		loggerThread.shutdown(1000);
	}
}

