package cloudbackup;

import java.text.MessageFormat;
import java.text.SimpleDateFormat;
import java.util.logging.Formatter;
import java.util.logging.LogRecord;

public class LogFileFormatter extends Formatter
{
	final static SimpleDateFormat	sdf	= new SimpleDateFormat("yyyy.MM.dd HH:mm:ss");

	@Override
	public String format(LogRecord record)
	{
		return formatRecord(record, sdf) + "\n";
	}

	public static String formatRecord(LogRecord record, SimpleDateFormat sdf)
	{
		MessageFormat mf = new MessageFormat(record.getMessage());
		StringBuffer message = new StringBuffer();
		mf.format(record.getParameters(), message, null);
		return "[" + 
		sdf.format(record.getMillis()) + "] - " + 
		record.getLevel().getName() + " - " + 
		record.getSourceClassName() + "." + record.getSourceMethodName() + " - " + 
		message.toString();
	}
}
