package com.rsicms.community.quickTask.webservice;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import org.apache.commons.io.IOUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.reallysi.rsuite.api.Alias;
import com.reallysi.rsuite.api.ContentAssemblyNodeContainer;
import com.reallysi.rsuite.api.ManagedObject;
import com.reallysi.rsuite.api.RSuiteException;
import com.reallysi.rsuite.api.Session;
import com.reallysi.rsuite.api.User;
import com.reallysi.rsuite.api.VariantDescriptor;
import com.reallysi.rsuite.api.extensions.ExecutionContext;
import com.reallysi.rsuite.api.remoteapi.CallArgumentList;
import com.reallysi.rsuite.api.remoteapi.RemoteApiDefinition;
import com.reallysi.rsuite.api.remoteapi.RemoteApiExecutionContext;
import com.reallysi.rsuite.api.remoteapi.RemoteApiHandler;
import com.reallysi.rsuite.api.remoteapi.RemoteApiResult;
import com.reallysi.rsuite.api.remoteapi.result.MessageDialogResult;
import com.reallysi.rsuite.api.remoteapi.result.MessageType;
import com.reallysi.rsuite.api.remoteapi.result.NotificationResult;
import com.reallysi.rsuite.api.system.MailMessageBean;
import com.reallysi.rsuite.service.ContentAssemblyService;
import com.reallysi.rsuite.service.MailService;
import com.reallysi.rsuite.service.ManagedObjectService;
import com.rsicms.rsuite.helpers.download.ZipHelper;
import com.rsicms.rsuite.helpers.download.ZipHelperConfiguration;
import com.rsicms.rsuite.helpers.utils.RSuiteUtils;

/**
 * Create zip and attach content and email to selected user.
 * <p>
 */
public class EmailAttachmentWebService implements RemoteApiHandler {
	private static Log log = LogFactory.getLog(EmailAttachmentWebService.class);

	private ExecutionContext context;

	/**
	 * Old misspelled initialization method for older versions of RSuite.
	 */
	public void initalize(RemoteApiDefinition def) {
		initialize(def);
	}

	/**
	 * Initialize handler.
	 */
	public void initialize(RemoteApiDefinition def) {
	}

	/**
	 * Process request.
	 * 
	 * @param context
	 *            Execution context.
	 * @param args
	 *            Service parameters provided by client.
	 */
	public RemoteApiResult execute( RemoteApiExecutionContext context,
                                    CallArgumentList args) throws RSuiteException {
		logArgs(args);

		this.context = context;
		
		try {
			String rsuiteId = args.getFirstValue( "rsuiteId");

			String mailFrom = args.getFirstValue( "mailFrom", "noreply@rsicms.com");
			String mailSubject = args.getFirstValue( "mailSubject");
			String mailBody = args.getFirstValue( "mailBody");
			String targetUserId = args.getFirstValue( "targetUserId");

			log.info( "looking for user " + targetUserId);
			User targetUser = context.getAuthorizationService().findUser(targetUserId);

			log.info( "looking for user's email " + targetUser.getEmail());
			String mailTo = targetUser.getEmail();

	        sendEmailWithAttachment(context, mailFrom, mailTo, mailSubject,
					mailBody, rsuiteId);
			
			NotificationResult result = new NotificationResult( "Email Attachment", "email notification sent.");
			return result;

		} catch (Throwable e) {
			log.error(e.getLocalizedMessage(), e);
			return new MessageDialogResult(
					MessageType.ERROR, "Error emailing attachment.",
					e.getLocalizedMessage() + ".");
		}
	}

	public static void sendEmailWithAttachment(RemoteApiExecutionContext context,
			String mailFrom, String mailTo, String mailSubject,
			String mailBody, String rsuiteId) throws RSuiteException {
		MailMessageBean msg = new MailMessageBean();
		msg.setFrom( mailFrom);
		msg.setSubject( mailSubject);
		msg.setContent( mailBody);
		msg.setTo( mailTo);
		
		emailContents( context, context.getSession(), rsuiteId, msg);
	}

	private static void emailContents( ExecutionContext context,
			                    Session session,
			                    String rsuiteId,
			                    MailMessageBean msg) throws RSuiteException {
		User user = session.getUser();
		ManagedObjectService moSvc = context.getManagedObjectService();

		ManagedObject mo = null;
		ContentAssemblyNodeContainer contentItem = null;
		try {
			contentItem = RSuiteUtils.getContentAssemblyNodeContainer(context, user, rsuiteId);
		}
		catch ( Exception ex) {
			// no container object found.
		}

		if( contentItem == null) {
			mo = moSvc.getManagedObject(user, rsuiteId);
		}

		String tempDirPath = context.getConfigurationProperties().getProperty( "rsuite.temp.dir", "/tmp");
		File tempDir = new File( tempDirPath);
		tempDir.mkdir();
		File targetDir = new File( tempDir, rsuiteId);
		targetDir.mkdir();
		File contentDir = new File( targetDir, "content");
		contentDir.mkdir();
		File zipFile = new File( targetDir, "content.zip");
		try {
			FileOutputStream outStream = new FileOutputStream( zipFile);
			if ( mo != null) {
				log.info("Zipping up mo " + mo.getId() + " to " + zipFile.getAbsolutePath());
				ZipHelper.zipManagedObject(context, mo, outStream, new ZipHelperConfiguration());
			}
			else if ( contentItem != null) {
				log.info("Zipping up contentItem " + contentItem.getId() + " to " + zipFile.getAbsolutePath());
				ZipHelper.zipContentAssembly(context, contentItem, outStream, new ZipHelperConfiguration());
			}
			outStream.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
			
		String[] attachments = new String[] { zipFile.getAbsolutePath()};
		
		for ( String attachment : attachments) {
			msg.attachfile(attachment);
		}

        MailService mailSvc = context.getMailService();
        log.info("Sending email to: "+ msg.getTo());

        try {	
        	mailSvc.send(msg);
        }
        catch ( Exception e){
        	log.error("ERROR_MAIL_BEAN: ", e);
        }
	}

	/**
	 * Log service arguments.
	 * 
	 * @param args
	 *            Argument list.
	 */
	private static void logArgs(CallArgumentList args) {
		log.info("Remote API Arguments:");
		for (String name : args.getNames()) {
			log.info("[" + name + "] " + args.getValues(name));
		}
	}
	
	private void writeMoToZip(ZipOutputStream zipOS, String parentPath, ManagedObject mo) throws RSuiteException {
		
		String moName = getFilenameForMo( mo);
		
		String entryPath = parentPath + moName;
		log.debug("Writing MO " + mo.getDisplayName() + " [" + mo.getId() + "] to ZipEntry \"" + entryPath + "\"");
		ZipEntry entry = new ZipEntry(entryPath);
		try {
			zipOS.putNextEntry(entry);
		} catch (IOException e) {
			throw new RSuiteException( "IOException adding entry to Zip output stream.");
		}
		InputStream inStream = mo.getInputStream();
				
		
		try {
			IOUtils.copy(inStream, zipOS);
			zipOS.closeEntry();
		} catch (IOException e) {
			throw new RSuiteException( "IOException writing mo " + RSuiteUtils.formatMoId(mo) + " to Zip output stream.");
		}
		log.debug("Entry written");
		
		if (mo.isNonXml()) {
			log.debug("Checking variants for " + mo.getDisplayName());
			VariantDescriptor[] variants = mo.getVariants();
			for (int v=0; v<variants.length; v++) {
				String varName = variants[v].getFileName();
				byte[] data = variants[v].getContent();
				String varPath = parentPath + "variants/" + varName;
				try {
					ZipEntry varEntry = new ZipEntry(varPath);
					log.debug("Adding variant " + varName + " to " + varEntry);
					zipOS.putNextEntry(varEntry);
					IOUtils.write(data, zipOS);
					zipOS.closeEntry();
				} catch (IOException e) {
					throw new RSuiteException( "IOException adding entry to Zip output stream.");
				}
			}
		}
	}

	private String getFilenameForMo( ManagedObject mo) throws RSuiteException {
		Alias fnAlias = context.getManagedObjectService().getAliasHelper().getFilenameAlias(mo);
		if (fnAlias != null)
			return fnAlias.getText();
		if (mo.isNonXml())
			return mo.getDisplayName();
		return mo.getId() + ".xml";
	} 

}
