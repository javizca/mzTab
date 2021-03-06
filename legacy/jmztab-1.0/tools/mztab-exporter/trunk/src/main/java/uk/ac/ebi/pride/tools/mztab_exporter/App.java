package uk.ac.ebi.pride.tools.mztab_exporter;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.GnuParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import uk.ac.ebi.pride.jmztab.MzTabFile;
import uk.ac.ebi.pride.jmztab.MzTabParsingException;
import uk.ac.ebi.pride.tools.mztab_exporter.exporter.ExporterFactory;
import uk.ac.ebi.pride.tools.mztab_exporter.exporter.MzTabExporter;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;

/**
 * Hello world!
 *
 */
public class App 
{
	private static final Logger logger = Logger.getLogger(App.class);
	
    public static void main( String[] args )
    {
        CommandLineParser parser = new GnuParser();
        ExporterFactory.FileType inputFormat = null;
        
        try {
        	CommandLine commandLine = parser.parse(CliOptions.getOptions(), args);
        	
        	 if (commandLine.hasOption(CliOptions.OPTIONS.HELP.toString()) || args.length == 0) {
                 printUsage();
             }
        	// logging level
 			if (commandLine.hasOption(CliOptions.OPTIONS.LOGGING_LEVEL.toString())) {
 				String level = commandLine.getOptionValue(CliOptions.OPTIONS.LOGGING_LEVEL.toString());
 				
 				Logger logger = Logger.getRootLogger();
 				Level logLevel = null;
 				
 				if ("debug".equals(level))
 					logLevel = Level.DEBUG;
 				else if ("info".equals(level))
 					logLevel = Level.INFO;
 				else if ("warn".equals(level))
 					logLevel = Level.WARN;
 				else if ("error".equals(level))
 					logLevel = Level.ERROR;
 				else
 					throw new Exception("Invalid logging level '" + level + "' set.");
 				
 				logger.setLevel(logLevel);
 			}
        	 if (commandLine.hasOption(CliOptions.OPTIONS.CHECK_FILE.toString())) {
        		 checkFile(commandLine.getOptionValue(CliOptions.OPTIONS.CHECK_FILE.toString()));
        	 }
        	 if (commandLine.hasOption(CliOptions.OPTIONS.FORMAT.toString())) {
        		 inputFormat = ExporterFactory.FileType.getFileType(commandLine.getOptionValue(CliOptions.OPTIONS.FORMAT.toString()));
        		 
        		 if (inputFormat == null)
        			 throw new Exception("Unknown file format passed.");
        	 }
        	 if (commandLine.hasOption(CliOptions.OPTIONS.CONVERT.toString())) {
        		 if (inputFormat == null)
        			 throw new Exception("Missing required parameter \"-format\".");
        		 
        		 String inputFilePath = commandLine.getOptionValue(CliOptions.OPTIONS.CONVERT.toString());
        		 
        		 convertFile(inputFilePath, inputFormat);
        	 }
        	 if (commandLine.hasOption(CliOptions.OPTIONS.READ_WRITE.toString())) {
        		 String inputFilePath = commandLine.getOptionValue(CliOptions.OPTIONS.READ_WRITE.toString());
        		 
        		 readWriteFile(inputFilePath);
        	 }
        }
        catch (Exception e) {
        	logger.error(e.getMessage());
        	e.printStackTrace();
        }
    }

    private static void readWriteFile(String inputFilePath) throws Exception {
    	File inputFile = new File(inputFilePath);
		
		if (!inputFile.exists())
			throw new Exception("Input file '" + inputFilePath + " could not be found.");
		if (!inputFile.canRead())
			throw new Exception("Missing privilege to read input file '" + inputFilePath + "'.");
		if (!inputFile.canWrite())
			throw new Exception("Missing privilege to write mzTab file '" + inputFilePath + "'.");
		
		// read the file into memory and replace all "-" with NA
		BufferedReader in = new BufferedReader(new FileReader(inputFile));
		String line;
		StringBuffer fileContent = new StringBuffer();
		// read and parse the file line by line
		while ((line = in.readLine()) != null) {
			line = line.replaceAll("\t-\t", "\tNA\t");
			line = line.replaceAll("\t-\t", "\tNA\t");
			if (line.endsWith("-"))
				line = line.substring(0, line.length() - 1) + "NA";
			fileContent.append(line + MzTabFile.EOL);
		}
		in.close();
		
		MzTabFile file = new MzTabFile(fileContent.toString());		
		String mzTabString = file.toMzTab();
		
		FileWriter writer = new FileWriter(inputFile);
		writer.write(mzTabString);
		writer.close();
		
		System.out.println(inputFilePath + " successfully updated.");
	}

	/**
     * Converts the given file
     * into mzTab format. The output filename
     * will be the name of the input file with
     * "-mztab.txt" added. 
     * @param inputFilePath
     * @param format
     * @throws Exception 
     */
	private static void convertFile(String inputFilePath, ExporterFactory.FileType format) throws Exception {
		File inputFile = new File(inputFilePath);
		
		if (!inputFile.exists())
			throw new Exception("Input file '" + inputFilePath + " could not be found.");
		if (!inputFile.canRead())
			throw new Exception("Missing privilege to read input file '" + inputFilePath + "'.");
		
		// create the output file
		File outputFile = new File(inputFile.getAbsolutePath() + "-mztab.txt");
		
		// create the exporter
		MzTabExporter exporter = ExporterFactory.getExporter(format);
		
		System.out.println("Converting " + inputFilePath + " to mzTab...");
		exporter.exportToMzTab(inputFile, outputFile);
		
		System.out.println("Conversion complete.");
	}

	private static void printUsage() {
		HelpFormatter formatter = new HelpFormatter();
        formatter.printHelp("PRIDE mzTab Exporter", "Converts PRIDE files to mzTab files.\n", CliOptions.getOptions(), "\n\n", true);
		
	}
	
	private static void checkFile(String filename) {
		System.out.println("Checking file <" + filename + ">...");
		
		// create the file obect
		File file = new File(filename);
		
		try {
			MzTabFile mzTabFile = new MzTabFile(file);
			
			System.out.println("Successfully parsed mzTab file.");
			System.out.println("\tUnits: " + mzTabFile.getUnitIds());
			System.out.println("\t" + mzTabFile.getProteins().size() + " proteins found");
			System.out.println("\t" + mzTabFile.getPeptides().size() + " peptides found");
			System.out.println("\t" + mzTabFile.getSmallMolecules().size() + " small molecules found");
		} catch (MzTabParsingException e) {
			System.out.println("Failed to parse mzTab file.\nError: " + e.getMessage());
			
			e.printStackTrace();
		} catch (Exception e) {
			System.out.println("Error: " + e.getMessage());
			
			e.printStackTrace();
		}
	}
}
