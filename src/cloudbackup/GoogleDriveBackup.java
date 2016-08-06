package cloudbackup;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Properties;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.logging.ConsoleHandler;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.extensions.java6.auth.oauth2.AuthorizationCodeInstalledApp;
import com.google.api.client.extensions.jetty.auth.oauth2.LocalServerReceiver;
import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeFlow;
import com.google.api.client.googleapis.auth.oauth2.GoogleClientSecrets;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.googleapis.media.MediaHttpDownloader;
import com.google.api.client.googleapis.media.MediaHttpUploader;
import com.google.api.client.http.FileContent;
import com.google.api.client.http.GenericUrl;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.client.util.store.DataStoreFactory;
import com.google.api.client.util.store.FileDataStoreFactory;
import com.google.api.services.drive.Drive;
import com.google.api.services.drive.Drive.Children;
import com.google.api.services.drive.DriveScopes;
import com.google.api.services.drive.model.ChildList;
import com.google.api.services.drive.model.ChildReference;
import com.google.api.services.drive.model.File;
import com.google.api.services.drive.model.ParentReference;
import com.google.api.services.drive.model.Property;
import com.google.api.services.drive.model.Revision;
import com.google.api.services.drive.model.RevisionList;

public class GoogleDriveBackup implements Backup {
	
	
	private static final String sourceClass = GoogleDriveBackup.class.getName();
	private static final Logger log = Logger.getLogger(sourceClass);	

	private Properties backupProps;
	private String driveRootFolderName;
	private File driveRootFolder;
	
	private Set<String> fileIdsTouchedDuringThisRun = new HashSet<String>();
	private Set<String> fileIdsOfActiveItems = null;
	private Set<String> fileIdsOfDeletedItems = null;
	
	private final static String FILE_PROP_NAME_CONTENTHASH = "contentHash";
	private final static String FILE_PROP_NAME_RELATIVEPATH = "relativePath";
	private final static String FILE_PROP_NAME_DELETED = "deleted";
	
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

	public GoogleDriveBackup(Properties backupProps) {
		log.setLevel(Level.INFO);
		log.setUseParentHandlers(false);
		Handler handler = new ConsoleHandler(); 
		handler.setLevel(Level.INFO);
		handler.setFormatter(new LogFileFormatter());
		log.addHandler(handler);
		String sourceMethod = "constructor";
		if (log.isLoggable(Level.FINE)) {
			log.entering(sourceClass, sourceMethod, new Object[] {backupProps});
		}
		
		this.backupProps = backupProps;
		
		try {
			httpTransport = GoogleNetHttpTransport.newTrustedTransport();
			dataStoreFactory = new FileDataStoreFactory(DATA_STORE_DIR);
			// authorization
			Credential credential = authorize();
			// set up the global Drive instance
			drive = new Drive.Builder(httpTransport, JSON_FACTORY, credential).setApplicationName(APPLICATION_NAME).build();

			// check backup root folder
			this.driveRootFolderName = (String) backupProps.getOrDefault("drive.backupFolder", "cloudbackup");
			driveRootFolder = getDriveFolder(this.driveRootFolderName);
			if (null == driveRootFolder) {
				driveRootFolder = createDriveFolder(this.driveRootFolderName, null);
			}
			
			fileIdsOfDeletedItems = initializeExistingDriveFilesCache("TRUE");
			fileIdsOfActiveItems = initializeExistingDriveFilesCache("FALSE");
				
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

	private File createDriveFolder(final String folderName, String parentId) throws Exception {
		String sourceMethod = "createDriveFolder";
		if (log.isLoggable(Level.FINE)) {
			log.entering(sourceClass, sourceMethod, new Object[] {folderName, parentId});
		}
				
		File currentSegment = null;
		StringTokenizer pathSegments = new StringTokenizer(folderName, "/");
		while (pathSegments.hasMoreTokens()) {
			String pathSegment = pathSegments.nextToken();
			
			if (null != currentSegment) {
				parentId = currentSegment.getId();
			}
			
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
			currentSegment = drive.files().insert(newFolder).execute();

		}
		if (log.isLoggable(Level.FINE)) {
			log.exiting(sourceClass, sourceMethod, currentSegment);
		}
		return currentSegment;
	}

	private File getDriveFolder(final String path) {
		String sourceMethod = "getDriveFolder";
		if (log.isLoggable(Level.FINE)) {
			log.entering(sourceClass, sourceMethod, new Object[] {path});
		}
		StringTokenizer pathSegments = new StringTokenizer(path, "/");

		List<File> tempfiles = null;
		String parentId = null;
		try {
			parentId = drive.about().get().execute().getRootFolderId();
		

			while (pathSegments.hasMoreTokens()) {
				final String pathSegment = pathSegments.nextToken();
				if (log.isLoggable(Level.FINER)) {
					log.finer("searching for path segment: " + pathSegment);
				}
				
		
				StringBuilder querybuilder = new StringBuilder("trashed=false and mimeType='" + FOLDER + "' and title='" + pathSegment);
				if (null == parentId) {
					querybuilder.append("'");
				} else {
					querybuilder.append("' and '" + parentId + "' in parents");
				}
				String query = querybuilder.toString();
				if (log.isLoggable(Level.FINER)) {
					log.finer("queryString: " + query);
				}
				
				tempfiles = drive.files().list().setQ(query).execute().getItems();
				
				if (log.isLoggable(Level.FINER)) {
					log.finer("files found: " + tempfiles.size());
				}
				if (null != tempfiles && 1 == tempfiles.size()) {
					parentId = tempfiles.get(0).getId();
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
		
		File returnFile = null;
		if (null != tempfiles && 1 == tempfiles.size()) {
			returnFile = tempfiles.get(0);
		} 
		if (log.isLoggable(Level.FINE)) {
			log.exiting(sourceClass, sourceMethod, returnFile);
		}
		return returnFile;

	}
	
	private String getDriveFileId(String title) {
		String sourceMethod = "getDriveFileId";
		if (log.isLoggable(Level.FINE)) {
			log.entering(sourceClass, sourceMethod, new Object[]{title});
		}
		File f = getDriveFile(title);
		String id = null;
		if (null != f) {
			id = f.getId();
		}
		if (log.isLoggable(Level.FINE)) {
			log.exiting(sourceClass, sourceMethod, id);
		}
		return id;
	}
	
	private File getDriveFile(String title) {
		String sourceMethod = "getDriveFileId";
		if (log.isLoggable(Level.FINE)) {
			log.entering(sourceClass, sourceMethod, new Object[]{title});
		}
		String query = "trashed=false and title='" + title + "' and '" + this.driveRootFolder.getId() + "' in parents";
		if (log.isLoggable(Level.FINE)) {
			log.fine("query: " + query);
		}
		List<File> files = null;
		try {
			files = drive.files().list().setQ(query).execute().getItems();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		if (null == files || 1 != files.size()) {
			System.out.println("unexpected number of files: " + files.size());
			return null;
		} 
		return files.get(0);
	}
	
	@Override
	public boolean contains(BackupItem backupItem) throws IOException {
		String sourceMethod = "contains";
		if (log.isLoggable(Level.FINE)) {
			log.entering(sourceClass, sourceMethod, new Object[]{backupItem});
		}
		boolean contains = false;
		String fileId = getDriveFileId(backupItem.getBackupId());
		if (null != fileId) {
			contains = true;
			fileIdsTouchedDuringThisRun.add(fileId);
			
			//re-activate if the file was marked deleted
			if (fileIdsOfDeletedItems.contains(fileId)) {
				log.info("File was marked deleted. Reactivating:  " + backupItem.getMetaData().getRelativePath());
				addPropertyToFile(fileId, FILE_PROP_NAME_DELETED, "FALSE");
				fileIdsOfDeletedItems.remove(fileId);
				fileIdsOfActiveItems.add(fileId);
			}
		}
		
		if (log.isLoggable(Level.FINE)) {
			log.exiting(sourceClass, sourceMethod, contains);
		}
		return contains; 
	}
	
	@Override
	public void add(BackupItem backupItem) throws IOException {
		String sourceMethod = "add";
		if (log.isLoggable(Level.FINE)) {
			log.entering(sourceClass, sourceMethod, new Object[]{backupItem});
		}
		String localRootDir = backupProps.getProperty(CloudBackup.BACKUP_PROPNAME_ROOTDIR);
		File fileMetadata = new File();
	    fileMetadata.setTitle(backupItem.getBackupId());

	    FileContent mediaContent = new FileContent("application/octet-stream", new java.io.File(localRootDir + java.io.File.separator + backupItem.getMetaData().getRelativePath()));
	    
	    Drive.Files.Insert insert = drive.files().insert(fileMetadata, mediaContent);
	    MediaHttpUploader uploader = insert.getMediaHttpUploader();
	    uploader.setDirectUploadEnabled(false);
		List<ParentReference> parents = new ArrayList<ParentReference>();
		ParentReference parentRef = new ParentReference();
		parentRef.setId(this.driveRootFolder.getId());
		parents.add(parentRef);
		fileMetadata.setParents(parents);
		
		File uploadedFile = insert.execute();
		
	    addPropertyToFile(uploadedFile.getId(), FILE_PROP_NAME_CONTENTHASH, backupItem.getMetaData().getContentHash());
	    addPropertyToFile(uploadedFile.getId(), FILE_PROP_NAME_RELATIVEPATH, backupItem.getMetaData().getRelativePath());
	    addPropertyToFile(uploadedFile.getId(), FILE_PROP_NAME_DELETED, "FALSE");
	    
	    fileIdsTouchedDuringThisRun.add(uploadedFile.getId());
	    
	    if (log.isLoggable(Level.FINE)) {
			log.exiting(sourceClass, sourceMethod);
		}
	}
	
	private void addPropertyToFile(String fileId, String key, String value) throws IOException {
		String sourceMethod = "addPropertyToFile";
		if (log.isLoggable(Level.FINE)) {
			log.entering(sourceClass, sourceMethod, new Object[]{fileId, key, value});
		}
		Property prop = new Property();
	    prop.setKey(key);
	    prop.setValue(value);
	    
	    drive.properties().insert(fileId, prop).execute();
	    
	    if (log.isLoggable(Level.FINE)) {
			log.exiting(sourceClass, sourceMethod);
		}
	}
	
	/**
	 * expects that contains was called before...
	 * @throws IOException 
	 */
	@Override
	public boolean hasChanged(BackupItem backupItem) throws IOException {
		String sourceMethod = "hasChanged";
		if (log.isLoggable(Level.FINE)) {
			log.entering(sourceClass, sourceMethod, new Object[]{backupItem});
		}
		boolean hasChanged = true;
		File file = getDriveFile(backupItem.getBackupId());
		if (null != file) {
			String contentHashOnDrive = getPropertyFromFile(file, FILE_PROP_NAME_CONTENTHASH);
			if (backupItem.getMetaData().getContentHash().equals(contentHashOnDrive)) {
				hasChanged = false;
				if (log.isLoggable(Level.FINER)) {
					log.finer("contentHash in local file system matches contentHash in drive: " + contentHashOnDrive);
				}
			} else {
				if (log.isLoggable(Level.FINER)) {
					log.finer("contentHash in local file system: " + backupItem.getMetaData().getContentHash() + " does not match contentHash in drive: " +contentHashOnDrive);
				}
			}
		}

	    if (log.isLoggable(Level.FINE)) {
			log.exiting(sourceClass, sourceMethod, hasChanged);
		}
		return hasChanged;
	}
	
	@Override
	public void update(BackupItem backupItem) throws IOException {		
		String sourceMethod = "update";
		if (log.isLoggable(Level.FINE)) {
			log.entering(sourceClass, sourceMethod, new Object[]{backupItem});
		}
		String localRootDir = backupProps.getProperty(CloudBackup.BACKUP_PROPNAME_ROOTDIR);
		File fileMetaData = getDriveFile(backupItem.getBackupId());

	    FileContent mediaContent = new FileContent("application/octet-stream", new java.io.File(localRootDir + java.io.File.separator + backupItem.getMetaData().getRelativePath()));

	    Drive.Files.Update update = drive.files().update(fileMetaData.getId(), fileMetaData, mediaContent);
	    MediaHttpUploader uploader = update.getMediaHttpUploader();
	    uploader.setDirectUploadEnabled(false);
		update.setNewRevision(Boolean.TRUE);
		File uploadedFile = update.execute();
		
	    addPropertyToFile(uploadedFile.getId(), FILE_PROP_NAME_CONTENTHASH, backupItem.getMetaData().getContentHash());
	    addPropertyToFile(uploadedFile.getId(), FILE_PROP_NAME_DELETED, "FALSE");
	    
	    if (log.isLoggable(Level.FINE)) {
			log.exiting(sourceClass, sourceMethod);
		}		
		
		
	}
	
	@Override
	public void list() {
		String sourceMethod = "list";
		if (log.isLoggable(Level.FINE)) {
			log.entering(sourceClass, sourceMethod);
		}
		
		try {
			Children.List request = drive.children().list(driveRootFolder.getId());

		    do {
		      try {
		        ChildList children = request.execute();

		        for (ChildReference child : children.getItems()) {
		          dumpFileInfoById(child.getId());
		        }
		        request.setPageToken(children.getNextPageToken());
		      } catch (IOException e) {
		        System.out.println("An error occurred: " + e);
		        request.setPageToken(null);
		      }
		    } while (request.getPageToken() != null &&
		             request.getPageToken().length() > 0);
		  
			
			
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		
		if (log.isLoggable(Level.FINE)) {
			log.exiting(sourceClass, sourceMethod);
		}				
	}

	private void dumpFileInfoById(String fileId) {
		String sourceMethod = "dumpFileInfoById";
		if (log.isLoggable(Level.FINE)) {
			log.entering(sourceClass, sourceMethod);
		}
		try {
			File f = drive.files().get(fileId).execute();
			dumpFileInfo(f);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		if (log.isLoggable(Level.FINE)) {
			log.exiting(sourceClass, sourceMethod);
		}	
	}
	
	private void dumpFileInfo(File f) throws IOException{
		String sourceMethod = "dumpFileInfo";
		if (log.isLoggable(Level.FINE)) {
			log.entering(sourceClass, sourceMethod);
		}
		
		log.info("fileId: " + f.getId());
		log.info("> contentHash:  " + getPropertyFromFile(f, FILE_PROP_NAME_CONTENTHASH));
		log.info("> relativePath: " + getPropertyFromFile(f, FILE_PROP_NAME_RELATIVEPATH));
		log.info("> deleted:      " + getPropertyFromFile(f, FILE_PROP_NAME_DELETED));
		RevisionList revisionsList = drive.revisions().list(f.getId()).execute();
		if (null == revisionsList  || revisionsList.isEmpty()) {
			log.info("> no revisions");
		} else {
			for (Revision r : revisionsList.getItems()) {
				log.info(">> Revision: " + r.getModifiedDate());
			}
		}
		
		
		if (log.isLoggable(Level.FINE)) {
			log.exiting(sourceClass, sourceMethod);
		}	
	}
	
	@Override
	public void restore() throws IOException {
		String sourceMethod = "restore";
		if (log.isLoggable(Level.FINE)) {
			log.entering(sourceClass, sourceMethod);
		}
		
		String restoreDir = backupProps.getProperty(CloudBackup.BACKUP_PROPNAME_RESTOREDIR);
		
		try {
			Children.List request = drive.children().list(driveRootFolder.getId());

		    do {
		      try {
		        ChildList children = request.execute();

		        for (ChildReference child : children.getItems()) {
		        	File driveFile = drive.files().get(child.getId()).execute();
		        	String relativePath = getPropertyFromFile(driveFile, FILE_PROP_NAME_RELATIVEPATH);
		        	boolean deleted = Boolean.valueOf(getPropertyFromFile(driveFile, FILE_PROP_NAME_DELETED));
		        	
		        	if (!deleted) {
		        		String absolutePath = restoreDir + java.io.File.separator + relativePath;
			        	log.info("restoring " + absolutePath);
			        	java.io.File restoreFile = new java.io.File(absolutePath);
			        	restoreFile.getParentFile().mkdirs();
			        	
			        	OutputStream out = new FileOutputStream(new java.io.File(restoreDir, relativePath));

			            MediaHttpDownloader downloader =
			                new MediaHttpDownloader(httpTransport, drive.getRequestFactory().getInitializer());
			            downloader.setDirectDownloadEnabled(false);
			            downloader.download(new GenericUrl(driveFile.getDownloadUrl()), out);		        		
		        	} else {
		        		log.info("Skipping deleted file: " + relativePath);
		        	}
		        }
		        request.setPageToken(children.getNextPageToken());
		      } catch (IOException e) {
		        System.out.println("An error occurred: " + e);
		        request.setPageToken(null);
		      }
		    } while (request.getPageToken() != null &&
		             request.getPageToken().length() > 0);
		  
			
			
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		

		if (log.isLoggable(Level.FINE)) {
			log.exiting(sourceClass, sourceMethod);
		}	
	}
	
	private String getPropertyFromFile(File file, String propertyName) {
		final String sourceMethod = "getPropertyFromFile"; 
		if (log.isLoggable(Level.FINE)) {
			log.entering(sourceClass, sourceMethod, new Object[] {file, propertyName});
		}
		assert(propertyName!=null);
		
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

	@Override
	public void markUntouchedFilesAsDeleted() throws IOException {
		final String sourceMethod = "markUntouchedFilesAsDeleted"; 
		if (log.isLoggable(Level.FINE)) {
			log.entering(sourceClass, sourceMethod);
		}

		try {
			Children.List request = drive.children().list(driveRootFolder.getId()).setQ("properties has { key='" + FILE_PROP_NAME_DELETED +"' and value='FALSE' and visibility='PRIVATE'}");//

		    do {
		      try {
		        ChildList children = request.execute();

		        for (ChildReference child : children.getItems()) {
		        	if (fileIdsTouchedDuringThisRun.contains(child.getId())) {
		        		log.info("File was touched: " + child.getId());
		        	} else {
		        		log.info("marking file as deleted: " + child.getId());
		        		addPropertyToFile(child.getId(), FILE_PROP_NAME_DELETED, "TRUE");
		        	}
		        }
		        request.setPageToken(children.getNextPageToken());
		      } catch (IOException e) {
		        System.out.println("An error occurred: " + e);
		        request.setPageToken(null);
		      }
		    } while (request.getPageToken() != null &&
		             request.getPageToken().length() > 0);
		  
			
			
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		
		if (log.isLoggable(Level.FINE)) {
			log.exiting(sourceClass, sourceMethod);
		}		
	
	}
	
	
	private	Set<String> initializeExistingDriveFilesCache(String deleted) {
		final String sourceMethod = "initializeExistingDriveFilesCache"; 
		if (log.isLoggable(Level.FINE)) {
			log.entering(sourceClass, sourceMethod, new Object[] { deleted });
		}
		
		Set<String> fileIds = new HashSet<String>();
		try {
			Children.List request = drive.children().list(driveRootFolder.getId()).setQ("properties has { key='" + FILE_PROP_NAME_DELETED +"' and value='" + deleted +"' and visibility='PRIVATE'}");//

		    do {
		      try {
		        ChildList children = request.execute();
		        for (ChildReference child : children.getItems()) {
		        	fileIds.add(child.getId());
		        }
		        request.setPageToken(children.getNextPageToken());
		      } catch (IOException e) {
		        log.severe("An error occurred: " + e);
		        request.setPageToken(null);
		      }
		    } while (request.getPageToken() != null &&
		             request.getPageToken().length() > 0);
		  
			
			
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		if (log.isLoggable(Level.FINE)) {
			log.exiting(sourceClass, sourceMethod, fileIds.size());
		}	
		return fileIds;
	}
	
}
