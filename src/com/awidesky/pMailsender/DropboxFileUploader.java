package com.awidesky.pMailsender;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Scanner;

import com.dropbox.core.DbxAppInfo;
import com.dropbox.core.DbxAuthFinish;
import com.dropbox.core.DbxRequestConfig;
import com.dropbox.core.DbxWebAuth;
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

	/**
	 * Initiate DropboxFileUploader with clientIdentifier, app key, secret, and
	 * access token(optional) if <code>accessToken</code> is <code>null</code>, this
	 * constructor calls <code>DbxWebAuthNoRedirect.start()</code>
	 * 
	 * @throws Exception
	 */
	public DropboxFileUploader() throws Exception {

		readConfig();

		DbxRequestConfig config = DbxRequestConfig.newBuilder(clientIdentifier).withAutoRetryEnabled()
				.withUserLocaleFrom(Locale.getDefault()).build();
		DbxAppInfo appInfo = new DbxAppInfo(appKey, appSecret);

		if (accessToken == null) { // Web authorization code (from dropbox tutorial)

			DbxWebAuth webAuth = new DbxWebAuth(config, appInfo);
			DbxWebAuth.Request webAuthRequest = DbxWebAuth.newRequestBuilder().withNoRedirect().build();
			String authorizeUrl = webAuth.authorize(webAuthRequest);
			System.out.println("1. Go to " + authorizeUrl);
			System.out.println("2. Click \"Allow\" (you might have to log in first).");
			System.out.println("3. Copy the authorization code.");
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

		client = new DbxClientV2(config, accessToken);

		// Get current account info
		FullAccount account = client.users().getCurrentAccount();
		System.out.println("Account name : " + account.getName().getDisplayName());

	}

	private void readConfig() throws Exception {

		try (BufferedReader br = new BufferedReader(new FileReader(new File("dropboxAuth.txt")))) {

			clientIdentifier = br.readLine().split("=")[1].trim();
			appKey = br.readLine().split("=")[1].trim();
			appSecret = br.readLine().split("=")[1].trim();
			String[] arr = br.readLine().split("=");
			accessToken = (arr.length == 2 && arr[1].trim().equals("")) ? arr[1].trim() : null;

		} catch (Exception e1) {
			System.err.println("Check if dropboxAuth.txt is well-written!");
			throw e1;
		}

	}

	public String uploadFile(List<File> list, String dropboxPath) throws Exception {
		
		StringBuilder sb = new StringBuilder("");
		
		for (File f : list) {
			try (FileInputStream in = new FileInputStream(f)) {

				FileMetadata metadata = client.files().uploadBuilder(dropboxPath + f.getName()).withMode(WriteMode.ADD)
						.withClientModified(new Date(f.lastModified()))
						.uploadAndFinish(in, l -> System.out.println("Uploading " + f.getName() + " : " + String.format("%12d / %12d bytes (%5.2f%%)\n", l, f.length(), 100.0 * l / f.length())));

				System.out.println(f.getName() + "matadata : " + metadata.toStringMultiline());

				sb.append(client.sharing().createSharedLinkWithSettings(dropboxPath));
				sb.append(System.lineSeparator());
			
			} catch (Exception ex) {
				System.err.println("Error reading from file \"" + list + "\": " + ex.getMessage());
				throw ex;
			}
		}
		
		return sb.toString();
	}

}
