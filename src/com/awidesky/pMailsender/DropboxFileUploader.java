package com.awidesky.pMailsender;

import java.awt.Dimension;
import java.awt.Toolkit;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Scanner;

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

public class DropboxFileUploader {

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
	public DropboxFileUploader() throws Exception {

		System.out.println("Authorizing Dropbox account..");
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
			System.out.println("Invalide access token! : " + accessToken);
			webAuth();
			account = client.users().getCurrentAccount();
		}
		
		System.out.println("Account name : " + account.getName().getDisplayName());

	}

	private void webAuth() throws DbxException {
		
		DbxWebAuth webAuth = new DbxWebAuth(config, appInfo);
		DbxWebAuth.Request webAuthRequest = DbxWebAuth.newRequestBuilder().withNoRedirect().build();
		String authorizeUrl = webAuth.authorize(webAuthRequest);
		System.out.println();
		System.out.println("1. Go to " + authorizeUrl);
		System.out.println("2. Click \"Allow\" (you might have to log in first).");
		System.out.println("3. Copy the authorization code.");
		System.out.println("(copy it to dropboxAuth.txt too when you don't want to do this twice)");
		System.out.print("Enter the authorization code here: ");
		Scanner sc = new Scanner(System.in);
		String code = "";

		while ((code = sc.nextLine()) == null) {
		} // ATTENTION! ONE-LINE LOOP!

		sc.close();

		code = code.trim();
		DbxAuthFinish authFinish = webAuth.finishFromCode(code);
		System.out.println("Authorization complete.");
		System.out.println("- User ID: " + authFinish.getUserId());

		accessToken = authFinish.getAccessToken();

	}
	
	private void readConfig() throws Exception {

		try (BufferedReader br = new BufferedReader(new FileReader(new File("dropboxAuth.txt")))) {

			clientIdentifier = br.readLine().split("=")[1].trim();
			appKey = br.readLine().split("=")[1].trim();
			appSecret = br.readLine().split("=")[1].trim();
			String[] arr = br.readLine().split("=");
			accessToken = (arr.length == 2 && !arr[1].trim().equals("")) ? arr[1].trim() : null;

		} catch (Exception e1) {
			System.err.println("Check if dropboxAuth.txt is well-written!");
			throw e1;
		}

	}

	public String uploadFileAndGetLink(List<File> list, String dropboxPath) throws Exception {
		
		System.out.println();
		System.out.println("Uploading...");
		StringBuilder sb = new StringBuilder("");
		
		for (File f : list) {
				
			String link;
			try {
				
				if(!isLinkExists(dropboxPath + f.getName())) {
					uploadFile(f, dropboxPath);
					link = client.sharing().createSharedLinkWithSettings(dropboxPath + f.getName()).getUrl();
				}

				link = client.sharing().listSharedLinksBuilder().withPath(dropboxPath + f.getName()).start().getLinks().get(0).getUrl();
				
				sb.append(link.contains("dl=0") ? link.replace("dl=0", "dl=1") : (link.contains("?") ? link + "&dl=1" : link + "?dl=1"));
				sb.append(System.lineSeparator());
			
			} catch (Exception ex) {
				
				System.out.println();
				System.out.println("Failed to upload file \"" + f.getName() + "\": " + ex.getMessage());
				System.out.println("Try deleting uploaded files...");
				System.out.println();
				
				for (int i = 0; i < list.indexOf(f); i++) {
					
					deleteLink(dropboxPath + list.get(i).getName());

				}
				
				throw ex;
				
			}
		}
		
		System.out.println();
		return sb.toString();
	}
	
	private void uploadFile(File f, String path) throws Exception {

		FileInputStream in = new FileInputStream(f);

		UploadProgress prog = new UploadProgress(f);
		FileMetadata metadata = client.files().uploadBuilder(path + f.getName()).withMode(WriteMode.ADD)
				.withClientModified(new Date(f.lastModified())).uploadAndFinish(in, prog);
		System.out.println();
		System.out.println(f.getName() + "matadata : " + metadata.toStringMultiline());
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
			System.out.println();
			System.out.println("Failed to delete uploaded file \"" + path + "\"");
			e.printStackTrace();
			System.out.println();
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
