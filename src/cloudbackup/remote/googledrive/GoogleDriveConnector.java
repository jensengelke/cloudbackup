package cloudbackup.remote.googledrive;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Properties;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.codehaus.jackson.map.ObjectMapper;

import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.extensions.java6.auth.oauth2.AuthorizationCodeInstalledApp;
import com.google.api.client.extensions.jetty.auth.oauth2.LocalServerReceiver;
import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeFlow;
import com.google.api.client.googleapis.auth.oauth2.GoogleClientSecrets;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.googleapis.json.GoogleJsonResponseException;
import com.google.api.client.googleapis.media.MediaHttpUploader;
import com.google.api.client.googleapis.media.MediaHttpUploaderProgressListener;
import com.google.api.client.http.GenericUrl;
import com.google.api.client.http.HttpResponse;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.http.InputStreamContent;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.client.util.DateTime;
import com.google.api.client.util.store.DataStoreFactory;
import com.google.api.client.util.store.FileDataStoreFactory;
import com.google.api.services.drive.Drive;
import com.google.api.services.drive.Drive.Children;
import com.google.api.services.drive.Drive.Files.Insert;
import com.google.api.services.drive.Drive.Files.Update;
import com.google.api.services.drive.Drive.Revisions;
import com.google.api.services.drive.DriveScopes;
import com.google.api.services.drive.model.ChildList;
import com.google.api.services.drive.model.ChildReference;
import com.google.api.services.drive.model.Comment;
import com.google.api.services.drive.model.File;
import com.google.api.services.drive.model.ParentReference;
import com.google.api.services.drive.model.Property;
import com.google.api.services.drive.model.Revision;
import com.google.api.services.drive.model.RevisionList;

import cloudbackup.config.CloudBackupBackupOptions;
import cloudbackup.exception.CloudBackupException;
import cloudbackup.model.LocalFile;
import cloudbackup.model.RemoteFile;
import cloudbackup.model.RemoteFileVersion;

public class GoogleDriveConnector {

	private static final String sourceClass = GoogleDriveConnector.class.getName();
	private static final Logger log = Logger.getLogger(sourceClass);

//	public final static String FILE_PROP_NAME_RELATIVEPATH = "relativePath";
	public final static String FILE_PROP_NAME_DELETED = "deleted";
	public final static String FILE_PROP_NAME_DELETION_TIMESTAMP = "deletedTS";
	public final static String FILE_PROP_NAME_LASTREVISIONMODIFIEDTIMESTAMP = "lastRevisionModifiedTimestamp";

	private final static String FOLDER = "application/vnd.google-apps.folder";

	/**
	 * Be sure to specify the name of your application. If the application name
	 * is {@code null} or blank, the application will log a warning. Suggested
	 * format is "MyCompany-ProductName/1.0".
	 */
	private static final String APPLICATION_NAME = "cloudBackup";

	/** Directory to store user credentials. */
	private static final java.io.File DATA_STORE_DIR = new java.io.File("config/.credentials");

	/**
	 * Global instance of the {@link DataStoreFactory}. The best practice is to
	 * make it a single globally shared instance across your application.
	 */
	private static FileDataStoreFactory dataStoreFactory;

	/** Global instance of the HTTP transport. */
	private static HttpTransport httpTransport;

	/** Global instance of the JSON factory. */
	private static final JsonFactory JSON_FACTORY = JacksonFactory.getDefaultInstance();

	/** Global Drive API client. */
	private static Drive drive;

	File driveRootFolder;

	private static final GoogleDriveConnector INSTANCE = new GoogleDriveConnector();

	private GoogleDriveConnector() {
		try {
			httpTransport = GoogleNetHttpTransport.newTrustedTransport();
			dataStoreFactory = new FileDataStoreFactory(DATA_STORE_DIR);
			// authorization
			Credential credential = authorize();
			// set up the global Drive instance
			drive = new Drive.Builder(httpTransport, JSON_FACTORY, credential).setApplicationName(APPLICATION_NAME).build();
			if (null == getDriveBackupRootFolder(CloudBackupBackupOptions.getInstance().getGoogleDriveBackupDirectory())) {
				this.driveRootFolder = createDriveFolder(CloudBackupBackupOptions.getInstance().getGoogleDriveBackupDirectory(), null);
			}
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	/** Authorizes the installed application to access user's protected data. */
	private static Credential authorize() throws Exception {
		String sourceMethod = "authorize";
		if (log.isLoggable(Level.FINE)) {
			log.entering(sourceClass, sourceMethod);
		}
		// load client secrets
		java.io.File oauthClientCreds = new java.io.File("config/client_secrets.json");
		GoogleClientSecrets clientSecrets = GoogleClientSecrets.load(JSON_FACTORY, new InputStreamReader(new FileInputStream(oauthClientCreds)));
		// set up authorization code flow
		GoogleAuthorizationCodeFlow flow = new GoogleAuthorizationCodeFlow.Builder(httpTransport, JSON_FACTORY, clientSecrets, Collections.singleton(DriveScopes.DRIVE))
				.setDataStoreFactory(dataStoreFactory).setAccessType("offline").build();
		// authorize
		return new AuthorizationCodeInstalledApp(flow, new LocalServerReceiver()).authorize("user");
	}

	public static GoogleDriveConnector getInstance() {
		return INSTANCE;
	}

	public Set<RemoteFile> getAllDriveFilesForBackup(String driveFolderName) {
		String sourceMethod = "getAllDriveFilesForBackup";
		if (log.isLoggable(Level.FINE)) {
			log.entering(sourceClass, sourceMethod);
		}
		
		Set<RemoteFile> driveFiles = null;

		try {
			final String backupRootFolderId = getDriveBackupRootFolder(driveFolderName).getId();
			Children.List request = drive.children().list(backupRootFolderId);
			//TODO: get multiple properties using list operation
			//TODO: get long values (relative path) from comment instead of properties
			do {
				try {
					ChildList children = request.setQ("trashed=false").execute();
					driveFiles = new HashSet<RemoteFile>(children.size());
					for (ChildReference child : children.getItems()) {
						
						File driveFile = drive.files().get(child.getId()).execute();
						RemoteFile remoteFile = new RemoteFile();
						remoteFile.setFileId(driveFile.getId());
						remoteFile.setDeleted(Boolean.valueOf(getPropertyFromFile(driveFile, FILE_PROP_NAME_DELETED)));
						//TODO: use comment for fileName prop
						Properties props = getPropertiesFromFile(remoteFile.getFileId());
						remoteFile.setRelativePath(props.getProperty("filename"));
						remoteFile.setLastRevisionTimestamp(driveFile.getModifiedDate().getValue());
						remoteFile.setBackupId(driveFile.getTitle());
						driveFiles.add(remoteFile);
						Revisions.List revisionsRequest = drive.revisions().list(driveFile.getId());
						RevisionList revisions = revisionsRequest.execute();
						if (0 > revisions.size()) {
							remoteFile.setRevisions(new ArrayList<RemoteFileVersion>(revisions.size()));
							for (Revision revision : revisions.getItems()) {
								RemoteFileVersion rfv = new RemoteFileVersion();
								rfv.setRevisionId(revision.getId());
								rfv.setTimestamp(revision.getModifiedDate().getValue());
								remoteFile.getRevisions().add(rfv);
							}
						}
					}
					request.setPageToken(children.getNextPageToken());
				} catch (IOException e) {
					System.out.println("An error occurred: " + e);
					request.setPageToken(null);
				}
			} while (request.getPageToken() != null && request.getPageToken().length() > 0);

		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		if (log.isLoggable(Level.FINE)) {
			log.exiting(sourceMethod, sourceMethod, driveFiles);
		}

		return driveFiles;
	}

	private File getDriveFolder(final String path) {
		String sourceMethod = "getDriveFolder";
		if (log.isLoggable(Level.FINE)) {
			log.entering(sourceClass, sourceMethod, new Object[] { path });
		}
		StringTokenizer pathSegments = new StringTokenizer(path, "/");
		String parentId = null;
		File segment = null;
		try {
			while (pathSegments.hasMoreTokens()) {
				final String pathSegment = pathSegments.nextToken();
				if (log.isLoggable(Level.FINER)) {
					log.finer("searching for path segment: " + pathSegment);
				}
				segment = getDriveFolder(pathSegment, parentId); 
				if (null != segment) {
					parentId = segment.getId();
				} else {
					if (log.isLoggable(Level.FINER)) {
						log.finer("not found ;-(");
					}
					break;
				}
			}
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		
		if (log.isLoggable(Level.FINE)) {
			log.exiting(sourceClass, sourceMethod, segment);
		}
		return segment;
	}
	
	private File getDriveFolder(String folderName, String parentId) throws IOException {
		String sourceMethod = "getDriveFolder";
		if (log.isLoggable(Level.FINE)) {
			log.entering(sourceClass, sourceMethod, new Object[] { folderName,  parentId});
		}
		
		File returnFile = null;
		if (null == parentId) {
			parentId = drive.about().get().execute().getRootFolderId();
		}
		
		StringBuilder querybuilder = new StringBuilder("trashed=false and mimeType='" + FOLDER + "' and title='" + folderName);
		querybuilder.append("' and '" + parentId + "' in parents");
		String query = querybuilder.toString();
		
		List<File> folders = drive.files().list().setQ(query).execute().getItems();

		if (log.isLoggable(Level.FINER)) {
			log.finer("files found: " + folders.size());
		}
		if (null != folders && 0 < folders.size()) {
			//TODO: is it ok to select the first folder by name?
			returnFile = folders.get(0);
		} else {
			if (log.isLoggable(Level.FINER)) {
				log.finer("not found ;-(");
			}
		}
		
		if (log.isLoggable(Level.FINE)) {
			log.exiting(sourceClass, sourceMethod, returnFile);
		}
		return returnFile;
	}

	private File getDriveBackupRootFolder(final String rootFolderName) {
		final String sourceMethod = "getDriveBackupRootFolder";
		if (log.isLoggable(Level.FINE)) {
			log.entering(sourceClass, sourceMethod, new Object[] { rootFolderName });
		}

		if (null == this.driveRootFolder) {
			this.driveRootFolder = getDriveFolder(rootFolderName);
		}

		if (log.isLoggable(Level.FINE)) {
			log.exiting(sourceClass, sourceMethod, this.driveRootFolder);
		}

		return this.driveRootFolder;
	}

	public String getPropertyFromFile(File file, String propertyName) {
		final String sourceMethod = "getPropertyFromFile";
		if (log.isLoggable(Level.FINE)) {
			log.entering(sourceClass, sourceMethod, new Object[] { propertyName, file });
		}
		assert(propertyName != null);

		String propValue = null;

		List<Property> fileProps = file.getProperties();
		if (null != fileProps) {
			for (Property fileProp : fileProps) {
				if (propertyName.equals(fileProp.getKey())) {
					propValue = fileProp.getValue();
					break;
				}
			}
		}

		if (log.isLoggable(Level.FINE)) {
			log.exiting(sourceClass, sourceMethod);
		}
		return propValue;
	}

	public Properties getPropertiesFromFile(String fileId) {
		final String sourceMethod = "getPropertiesFromFile";
		if (log.isLoggable(Level.FINE)) {
			log.entering(sourceClass, sourceMethod, new Object[] { fileId });
		}
		Properties props = new Properties();
		int retryCount = 10;
		
		do {
			try {
	
				Comment comment = drive.comments().list(fileId).execute().getItems().get(0);
				ObjectMapper mapper = new ObjectMapper();
				
				props = mapper.readValue(comment.getContent(), Properties.class);
				
			} catch (GoogleJsonResponseException e2) {
				if (e2.getStatusCode() == 403 && "User Rate Limit Exceeded".equals(e2.getDetails().getMessage())) {
					System.out.println("rate limited... waiting a bit");
					try {
						Thread.sleep(10000*(15-retryCount));
					} catch (InterruptedException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				}
				retryCount--;
				
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (NullPointerException npe) {
				log.severe("no such file!");
			}
		} while (props.isEmpty() && retryCount > 0);
		
		if (log.isLoggable(Level.FINE)) {
			log.exiting(sourceClass, sourceMethod, props);
		}
		return props;
	}

	private File createDriveFolder(final String folderName, String parentId) throws CloudBackupException {
		String sourceMethod = "createDriveFolder";
		if (log.isLoggable(Level.FINE)) {
			log.entering(sourceClass, sourceMethod, new Object[] { folderName, parentId });
		}

		File currentSegment = null;
		StringTokenizer pathSegments = new StringTokenizer(folderName, "/");
		while (pathSegments.hasMoreTokens()) {
			String pathSegment = pathSegments.nextToken();
			
			try {
				currentSegment = getDriveFolder(pathSegment, parentId);
			} catch (IOException e1) {
				throw new CloudBackupException("Cannot create folder", e1);
			}

			if (null != currentSegment) {
				parentId = currentSegment.getId();
			} else {
				
				if (log.isLoggable(Level.FINER)) {
					log.finer("current segment: " + pathSegment + " under " + parentId);
				}
				
				File newFolder = new File();
				newFolder.setTitle(pathSegment);
				newFolder.setMimeType(FOLDER);
				if (null != parentId) {
					List<ParentReference> parents = new ArrayList<ParentReference>();
					ParentReference parentRef = new ParentReference();
					parentRef.setId(parentId);
					parents.add(parentRef);
					newFolder.setParents(parents);
				}
				try {
					currentSegment = drive.files().insert(newFolder).execute();
					parentId = currentSegment.getId();
				} catch (IOException e) {
					throw new CloudBackupException("Cannot create folder", e);
				}	
			}
		}
		if (log.isLoggable(Level.FINE)) {
			log.exiting(sourceClass, sourceMethod, currentSegment);
		}
		return currentSegment;
	}

	public RemoteFile createFile(LocalFile localFile, InputStream in) throws CloudBackupException {
		final String sourceMethod = "createFile";
		if (log.isLoggable(Level.FINE)) {
			log.entering(sourceClass, sourceMethod, new Object[] { localFile, in });
		}

		RemoteFile remoteFile = new RemoteFile();
		remoteFile.setBackupId(localFile.getFileId());
		remoteFile.setRelativePath(localFile.getRelativePath());
		remoteFile.setLastRevisionTimestamp(localFile.getVersions().get(0).getModifiedTimestamp());

		File driveFile = new File();
		driveFile.setTitle(localFile.getFileId());
		driveFile.setModifiedDate(new DateTime(localFile.getVersions().get(0).getModifiedTimestamp()));

		try {
			InputStreamContent content = new InputStreamContent("application/octet-stream", in);
			content.setLength(-1);

			Insert insert = drive.files().insert(driveFile, content);
			insert.getMediaHttpUploader().setDirectUploadEnabled(true);

			insert.getMediaHttpUploader().setProgressListener(new MediaHttpUploaderProgressListener() {

				@Override
				public void progressChanged(MediaHttpUploader uploader) throws IOException {
					switch (uploader.getUploadState()) {
					case INITIATION_STARTED:
						if (log.isLoggable(Level.FINE)) {
							log.fine(remoteFile.getRelativePath() + " upload started");
						}
						break;
					case MEDIA_IN_PROGRESS:
						if (log.isLoggable(Level.FINE)) {
							log.fine(remoteFile.getRelativePath() + " bytes transferred: " + uploader.getNumBytesUploaded()); // TODO:
																																// convert
																																// to
																																// other
																																// units
						}
						break;
					case MEDIA_COMPLETE:
						log.info(remoteFile.getRelativePath() + " upload completed " + uploader.getNumBytesUploaded());
						break;
					default:
						break;
					}

				}
			});

			List<ParentReference> parents = new ArrayList<ParentReference>();
			ParentReference parentRef = new ParentReference();
			parentRef.setId(this.driveRootFolder.getId());
			parents.add(parentRef);
			driveFile.setParents(parents);

			File createdFile = insert.execute();
			remoteFile.setFileId(createdFile.getId());
			addPropertyToFile(createdFile.getId(), FILE_PROP_NAME_DELETED, "FALSE");
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		if (log.isLoggable(Level.FINE)) {
			log.exiting(sourceClass, sourceMethod, remoteFile);
		}
		return remoteFile;
	}

	public RemoteFileVersion updateFile(LocalFile localFile, RemoteFile remoteFile, InputStream in) throws CloudBackupException {
		final String sourceMethod = "createFile";
		if (log.isLoggable(Level.FINE)) {
			log.entering(sourceClass, sourceMethod, new Object[] { remoteFile, in });
		}

		RemoteFileVersion rfv = null;
		try {
			File driveFile = drive.files().get(remoteFile.getFileId()).execute();

//			driveFile.setModifiedDate(new DateTime(localFile.getVersions().get(0).getModifiedTimestamp()));

			InputStreamContent content = new InputStreamContent("application/octet-stream", in);
			content.setLength(-1);

			Update updateRequest = drive.files().update(remoteFile.getFileId(), driveFile, content);
			updateRequest.getMediaHttpUploader().setDirectUploadEnabled(true);

			updateRequest.getMediaHttpUploader().setProgressListener(new MediaHttpUploaderProgressListener() {

				@Override
				public void progressChanged(MediaHttpUploader uploader) throws IOException {
					switch (uploader.getUploadState()) {
					case INITIATION_STARTED:
						if (log.isLoggable(Level.FINE)) {
							log.fine(remoteFile.getRelativePath() + " upload started");
						}
						break;
					case MEDIA_IN_PROGRESS:
						if (log.isLoggable(Level.FINE)) {
							log.fine(remoteFile.getRelativePath() + " bytes transferred: " + uploader.getNumBytesUploaded()); // TODO:
																																// convert
																																// to
																																// other
																																// units
						}
						break;
					case MEDIA_COMPLETE:
						log.info(remoteFile.getRelativePath() + " upload completed");
						break;
					default:
						break;
					}

				}
			});

			updateRequest.setNewRevision(true);
			updateRequest.setSetModifiedDate(false);

			File createdFile = updateRequest.execute();
			rfv = new RemoteFileVersion();
			rfv.setRevisionId(createdFile.getHeadRevisionId());
			rfv.setTimestamp(localFile.getVersions().get(0).getModifiedTimestamp());

		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		if (log.isLoggable(Level.FINE)) {
			log.exiting(sourceClass, sourceMethod, rfv);
		}
		return rfv;
	}

	public RemoteFile createFile(LocalFile localFile) throws CloudBackupException {
		final String sourceMethod = "createFile";
		if (log.isLoggable(Level.FINE)) {
			log.entering(sourceClass, sourceMethod, new Object[] { localFile });
		}

		RemoteFile remoteFile = new RemoteFile();
		remoteFile.setBackupId(localFile.getFileId());
		remoteFile.setRelativePath(localFile.getRelativePath());

		File driveFile = new File();
		driveFile.setTitle(localFile.getFileId());

		try {
			Insert insert = drive.files().insert(driveFile);
			List<ParentReference> parents = new ArrayList<ParentReference>();
			ParentReference parentRef = new ParentReference();
			parentRef.setId(this.driveRootFolder.getId());
			parents.add(parentRef);
			driveFile.setParents(parents);

			File createdFile = insert.execute();
			remoteFile.setFileId(createdFile.getId());
			addPropertyToFile(createdFile.getId(), FILE_PROP_NAME_DELETED, "FALSE");
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		if (log.isLoggable(Level.FINE)) {
			log.exiting(sourceClass, sourceMethod, remoteFile);
		}
		return remoteFile;
	}

	public long upload(RemoteFile remoteFile, InputStream in) {
		final String sourceMethod = "upload";
		if (log.isLoggable(Level.FINE)) {
			log.entering(sourceClass, sourceMethod, new Object[] { remoteFile });
		}

		long fileSize = -1l;
		try {
			InputStreamContent content = new InputStreamContent("application/octet-stream", in);
			content.setLength(-1);
			File driveFile = drive.files().get(remoteFile.getFileId()).execute();
			Update update = drive.files().update(remoteFile.getFileId(), driveFile, content);
			MediaHttpUploader uploader = update.getMediaHttpUploader();
			uploader.setDirectUploadEnabled(false);
			fileSize = update.execute().getFileSize();

		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		if (log.isLoggable(Level.FINE)) {
			log.exiting(sourceClass, sourceMethod, fileSize);
		}
		return fileSize;
	}

	public String getHeadRevision(String fileId) {
		String sourceMethod = "getHeadRevision";
		if (log.isLoggable(Level.FINE)) {
			log.entering(sourceClass, sourceMethod, new Object[] { fileId });
		}

		String headRevision = null;
		File file = null;
		int retryCount = 10;
		do {
			try {
				file = drive.files().get(fileId).execute();
				if (null != file) {
					headRevision = file.getHeadRevisionId();
				}
			} catch (GoogleJsonResponseException e2) {
				if (e2.getStatusCode() == 403 && "User Rate Limit Exceeded".equals(e2.getDetails().getMessage())) {
					System.out.println("rate limited... waiting a bit");
					try {
						Thread.sleep(10000*(15-retryCount));
					} catch (InterruptedException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				}
				retryCount--;
				
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (Exception e1) {
				System.out.println("EXCEPTION: " + e1.getClass());
				e1.printStackTrace();
			}
		} while (file == null && retryCount > 0);

		if (log.isLoggable(Level.FINE)) {
			log.exiting(sourceClass, sourceMethod, headRevision);
		}
		return headRevision;
	}

	public String getRevisionIdForTimestamp(String fileId, long timestamp) {
		String sourceMethod = "getRevisionIdForTimestamp";
		if (log.isLoggable(Level.FINE)) {
			log.entering(sourceClass, sourceMethod, new Object[] { fileId });
		}
		
		String revisionId = null;
		
		if (timestamp > 0) {
			
			try {
				RevisionList revisionList = drive.revisions().list(fileId).execute();
				long latestTimestampBefore = -1L;
				for (Revision revision : revisionList.getItems()) {
					if (revision.getModifiedDate().getValue() < timestamp && 
							revision.getModifiedDate().getValue() > latestTimestampBefore) {
						if (log.isLoggable(Level.FINE)) {
							log.fine("found revision: " + revision.getId() + " with modified date: " + revision.getModifiedDate().getValue() + ", which is newer than current newest before with timestamp " + latestTimestampBefore);
						}
						latestTimestampBefore = revision.getModifiedDate().getValue();
						revisionId = revision.getId();					
					} else {
						if (log.isLoggable(Level.FINE)) {
							log.fine("found revision: " + revision.getId() + " with modified date: " + revision.getModifiedDate().getValue() + ", which is newer than configured timestamp: " + timestamp);
						}
					}
				}
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			
		} else {
			revisionId = getHeadRevision(fileId);
		}
		
		if (log.isLoggable(Level.FINE)) {
			log.exiting(sourceClass, sourceMethod, revisionId);
		}
		return revisionId;
		
	}
	
	public InputStream getFileContent(String fileId, String revisionId) {
		String sourceMethod = "getFileContent";
		if (log.isLoggable(Level.FINE)) {
			log.entering(sourceClass, sourceMethod, new Object[] { fileId, revisionId });
		}
		InputStream in = null;
		
		int retryCount = 10;
		
		do {
			try {
				Revision revision = drive.revisions().get(fileId, revisionId).execute();
				if (revision.getDownloadUrl() != null && revision.getDownloadUrl().length() > 0) {
					
					HttpResponse resp = drive.getRequestFactory().buildGetRequest(new GenericUrl(revision.getDownloadUrl())).execute();
					in = resp.getContent();
				} else {
					// The file doesn't have any content stored on Drive.
				}
			} catch (GoogleJsonResponseException e2) {
				if (e2.getStatusCode() == 403 && "User Rate Limit Exceeded".equals(e2.getDetails().getMessage())) {
					System.out.println("rate limited... waiting a bit");
					try {
						Thread.sleep(10000*(15-retryCount));
					} catch (InterruptedException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				}
				retryCount--;
				
			} catch (IOException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			}
		} while (null == in && retryCount>=0);

		if (log.isLoggable(Level.FINE)) {
			log.exiting(sourceClass, sourceMethod, in);
		}
		return in;
	}

	public void addPropertyToFile(String fileId, String key, String value) throws IOException {
		String sourceMethod = "addPropertyToFile";
		if (log.isLoggable(Level.FINE)) {
			log.entering(sourceClass, sourceMethod, new Object[] { fileId, key, value });
		}
		Property prop = new Property();
		prop.setKey(key);
		prop.setValue(value);

		drive.properties().insert(fileId, prop).execute();

		if (log.isLoggable(Level.FINE)) {
			log.exiting(sourceClass, sourceMethod);
		}
	}
	
	public void addCommentToFile(String fileId, String value) throws IOException {
		String sourceMethod = "addCommentToFile";
		if (log.isLoggable(Level.FINE)) {
			log.entering(sourceClass, sourceMethod, new Object[] { fileId, value });
		}
		Comment comment = new Comment();
		comment.setContent(value);
		drive.comments().insert(fileId, comment).execute();
		
		if (log.isLoggable(Level.FINE)) {
			log.exiting(sourceClass, sourceMethod);
		}
	}
	
	public void markDeleted(RemoteFile remoteFile) throws IOException {
		String sourceMethod = "markDeleted";
		if (log.isLoggable(Level.FINE)) {
			log.entering(sourceClass, sourceMethod, new Object[] { remoteFile});
		}
		
		if (null == remoteFile) {
			log.warning("Trying to mark a file as deleted, but remoteFile was null");
		} else {
			addPropertyToFile(remoteFile.getFileId(), FILE_PROP_NAME_DELETED, "true");
			addPropertyToFile(remoteFile.getFileId(), FILE_PROP_NAME_DELETION_TIMESTAMP, Long.toString(remoteFile.getDeletionTimestamp()));
		}
		
		
		if (log.isLoggable(Level.FINE)) {
			log.exiting(sourceClass, sourceMethod);
		}
	}
	
	public void markUndeleted(RemoteFile remoteFile) throws IOException {
		String sourceMethod = "markDeleted";
		if (log.isLoggable(Level.FINE)) {
			log.entering(sourceClass, sourceMethod, new Object[] { remoteFile});
		}
		
		if (null == remoteFile) {
			log.warning("Trying to mark a file as undeleted, but remoteFile was null");
		} else {
			addPropertyToFile(remoteFile.getFileId(), FILE_PROP_NAME_DELETED, "false");
			addPropertyToFile(remoteFile.getFileId(), FILE_PROP_NAME_DELETION_TIMESTAMP, Long.toString(1l));
		}
		
		
		if (log.isLoggable(Level.FINE)) {
			log.exiting(sourceClass, sourceMethod);
		}
	}
}
