package com.awidesky.pMailsender;

import java.awt.Desktop;
import java.awt.Dimension;
import java.awt.Toolkit;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JProgressBar;

import com.dropbox.core.DbxAppInfo;
import com.dropbox.core.DbxAuthFinish;
import com.dropbox.core.DbxException;
import com.dropbox.core.DbxRequestConfig;
import com.dropbox.core.DbxWebAuth;
import com.dropbox.core.InvalidAccessTokenException;
import com.dropbox.core.util.IOUtil;
import com.dropbox.core.v2.DbxClientV2;
import com.dropbox.core.v2.files.FileMetadata;
import com.dropbox.core.v2.files.WriteMode;
import com.dropbox.core.v2.users.FullAccount;

import io.github.awidesky.guiUtil.SwingDialogs;

public class DropboxFileUploader {
	
	/** for logging & error dialog */
	private MainFrame mainFrame; 

	private String clientIdentifier;
	private String appKey;
	private String appSecret;
	private String accessToken;

	private DbxClientV2 client;
	private DbxRequestConfig config;
	private DbxAppInfo appInfo;
	
	/**
	 * Initiate DropboxFileUploader with clientIdentifier, app key, secret, and
	 * access token(optional) if <code>accessToken</code> is <code>null</code>, this
	 * constructor calls <code>DbxWebAuthNoRedirect.start()</code>
	 * 
	 * @throws Exception
	 */
	public DropboxFileUploader(MainFrame mainFrame) throws Exception {
		
		this.mainFrame = mainFrame;
		mainFrame.log("Authorizing Dropbox account..");
		readConfig();

		config = DbxRequestConfig.newBuilder(clientIdentifier).withAutoRetryEnabled()
				.withUserLocaleFrom(Locale.getDefault()).build();
		appInfo = new DbxAppInfo(appKey, appSecret);

		if (accessToken == null) { // Web authorization code (from dropbox tutorial)
			webAuth();
		}

		client = new DbxClientV2(config, accessToken);
		FullAccount account;

		try {
			// Get current account info
			account = client.users().getCurrentAccount();
		} catch (InvalidAccessTokenException e) {
			mainFrame.log("Invalide access token! : " + accessToken);
			webAuth();
			account = client.users().getCurrentAccount();
		}
		
		mainFrame.log("Account name : " + account.getName().getDisplayName());

	}

	private void webAuth() throws DbxException {
		
		DbxWebAuth webAuth = new DbxWebAuth(config, appInfo);
		DbxWebAuth.Request webAuthRequest = DbxWebAuth.newRequestBuilder().withNoRedirect().build();
		String authorizeUrl = webAuth.authorize(webAuthRequest);
		mainFrame.log();
		mainFrame.log("1. Go to " + authorizeUrl);
		mainFrame.log("2. Click \"Allow\" (you might have to log in first).");
		mainFrame.log("3. Copy the authorization code.");
		mainFrame.log("(copy it to dropboxAuth.txt too when you don't want to do this twice)");

		DbxAuthFinish authFinish = webAuth.finishFromCode(String.valueOf(SwingDialogs.inputPassword("Dropbox auth info", "Enter the authorization code here: ")));
		mainFrame.log("Authorization complete.");
		mainFrame.log("- User ID: " + authFinish.getUserId());

		accessToken = authFinish.getAccessToken();

	}
	
	private void readConfig() throws Exception {

		try (BufferedReader br = new BufferedReader(new FileReader(new File(MailSender.projectPath + "dropboxAuth.txt")))) {

			clientIdentifier = br.readLine().split("=")[1].trim();
			appKey = br.readLine().split("=")[1].trim();
			appSecret = br.readLine().split("=")[1].trim();
			String[] arr = br.readLine().split("=");
			accessToken = (arr.length == 2 && !arr[1].trim().equals("")) ? arr[1].trim() : null;

		} catch (FileNotFoundException nf) {
			SwingDialogs.error(nf.toString(), "Please write dropbox auth information and restart the application!\n%e%", nf, true);
			try {
				File f = new File(MailSender.projectPath + "dropboxAuth.txt");
				f.createNewFile();
				try(PrintWriter pw = new PrintWriter(f)) {
					pw.println("App Identifier = ");
					pw.println("App key = ");
					pw.println("App Secret = ");
					pw.println("Access Token (optional) = ");
					pw.println();
					pw.println();
					pw.println("#if you have dropbox app, you can use it to upload files and send links when attached files are bigger than limit.");
					pw.println("#for example :");
					pw.println("App Identifier = PMailSender/1.0");
					pw.println("App key = abcdefg12345678");
					pw.println("App Secret = abcdefg12345678");
					pw.println("Access Token (optional) = fasdijiojefinaihweaio3hr30493=eawjfpefa");
				}
				Desktop.getDesktop().open(f);
			} catch (IOException e) {
				mainFrame.log(e);
				throw e;
			}
			throw nf;
		} catch (Exception e1) {
			mainFrame.log("Check if dropboxAuth.txt is well-written!");
			throw e1;
		}

	}

	public String uploadFileAndGetLink(List<File> list, String dropboxPath) throws Exception {
		
		mainFrame.log();
		mainFrame.log("Getting download links...");
		StringBuilder sb = new StringBuilder("");
		
		for (File f : list) {
				
			String link;
			try {
				
				if(!isLinkExists(dropboxPath + f.getName())) {
					mainFrame.log("Uploading \"" + f.getName() + "\"...");
					uploadFile(f, dropboxPath);
					link = client.sharing().createSharedLinkWithSettings(dropboxPath + f.getName()).getUrl();
				} else {
					mainFrame.log("\"" + f.getName() + "\"S is already uploaded, retrieving link...");
					link = client.sharing().listSharedLinksBuilder().withPath(dropboxPath + f.getName()).start().getLinks().get(0).getUrl();
				}
				
				sb.append(System.lineSeparator());
				sb.append(System.lineSeparator());
				sb.append(link.contains("dl=0") ? link.replace("dl=0", "dl=1") : (link.contains("?") ? link + "&dl=1" : link + "?dl=1"));
			
			} catch (Exception ex) {
				
				mainFrame.log();
				mainFrame.log("Failed to upload file \"" + f.getName() + "\": " + ex.getMessage());
				mainFrame.log("Try deleting uploaded files...");
				mainFrame.log();
				
				for (int i = 0; i < list.indexOf(f); i++) {
					
					deleteLink(dropboxPath + list.get(i).getName());

				}
				
				throw ex;
				
			}
		}
		
		mainFrame.log();
		return sb.toString();
	}
	
	private void uploadFile(File f, String path) throws Exception {

		FileInputStream in = new FileInputStream(f);

		UploadProgress prog = new UploadProgress(f);
		FileMetadata metadata = client.files().uploadBuilder(path + f.getName()).withMode(WriteMode.ADD)
				.withClientModified(new Date(f.lastModified())).uploadAndFinish(in, prog);
		mainFrame.log();
		mainFrame.log(f.getName() + "matadata : " + metadata.toStringMultiline());
		prog.done();

		in.close();

	}
	
	private boolean isLinkExists(String path) {
		
		boolean result = true;
		
        try {
            client.files().getMetadata(path);
        } catch (Exception e){
        	result = false;
        }

        return result;
		
	}
	
	private void deleteLink(String path) {
		
		try {
			
			if(isLinkExists(path)) client.files().deleteV2(path);
			
		} catch (Exception e) {
			mainFrame.log();
			mainFrame.log("Failed to delete uploaded file \"" + path + "\"");
			mainFrame.log(e);
			mainFrame.log();
		}
		
	}
	
	private class UploadProgress extends JFrame implements IOUtil.ProgressListener {

		/**
		 * 
		 */
		private static final long serialVersionUID = 7545937069224806068L;
		
		private JProgressBar progress;
		private JLabel status;
		
		private File file;
		
		public UploadProgress(File f) {
			
			setTitle("uploading...");
			setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
			setSize(420, 120);
			Dimension dim = Toolkit.getDefaultToolkit().getScreenSize();
			setLocation(dim.width/2-getSize().width/2, dim.height/2-getSize().height/2);
			setLayout(null);
			setResizable(false);
			addWindowListener(new WindowAdapter() {

				@Override
				public void windowClosing(WindowEvent e) {
					
					e.getWindow().dispose();
					throw new RuntimeException("Process canceled while uploading : " + f.getName() + "(user canceled)");

				}

			});
			
			file = f;
			
			status = new JLabel("Uploading " + f.getName());
			status.setBounds(14, 8, 370, 40);
			
			progress = new JProgressBar();
			progress.setStringPainted(true);
			progress.setBounds(15, 50, 370, 18);
			
			add(status);
			add(progress);
			setVisible(true);
			
		}
		
		@Override
		public void onProgress(long bytesWritten) {

			double percent =  100.0 * bytesWritten / file.length();
			status.setText("<html>Uploading " + file.getName() + "<br>" + String.format("%5.2fMB / %5.2fMB (%5.2f%%)\n", 1.0 * bytesWritten / 1024 / 1024, 1.0 * file.length()  / 1024 / 1024, percent) + "</html>");
			progress.setValue((int)percent);
			
		}
		
		public void done() {
			
			setVisible(false);
			dispose();
			
		}
		
	}

}
