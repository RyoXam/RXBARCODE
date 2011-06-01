// barcode.java : implementation file
// Contains the Nirva external service class barcode

// Please insert code where there is a TODO string in comments

import java.awt.color.ColorSpace;
import java.awt.image.AffineTransformOp;
import java.awt.image.BufferedImage;
import java.awt.image.BufferedImageOp;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.URI;
import java.util.Hashtable;
import java.util.Vector;

import javax.imageio.ImageIO;

import org.krysalis.barcode4j.impl.AbstractBarcodeBean;
import org.krysalis.barcode4j.impl.codabar.CodabarBean;
import org.krysalis.barcode4j.impl.code128.Code128Bean;
import org.krysalis.barcode4j.impl.code128.EAN128Bean;
import org.krysalis.barcode4j.impl.code39.Code39Bean;
import org.krysalis.barcode4j.impl.datamatrix.DataMatrixBean;
import org.krysalis.barcode4j.impl.pdf417.PDF417Bean;
import org.krysalis.barcode4j.impl.upcean.EAN13Bean;
import org.krysalis.barcode4j.impl.upcean.EAN8Bean;
import org.krysalis.barcode4j.impl.upcean.UPCABean;
import org.krysalis.barcode4j.impl.upcean.UPCEBean;
import org.krysalis.barcode4j.output.bitmap.BitmapCanvasProvider;
import org.krysalis.barcode4j.tools.UnitConv;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.BinaryBitmap;
import com.google.zxing.DecodeHintType;
import com.google.zxing.LuminanceSource;
import com.google.zxing.MultiFormatReader;
import com.google.zxing.NotFoundException;
import com.google.zxing.ReaderException;
import com.google.zxing.Result;
import com.google.zxing.client.j2se.BufferedImageLuminanceSource;
import com.google.zxing.client.result.ParsedResult;
import com.google.zxing.client.result.ResultParser;
import com.google.zxing.common.BitArray;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.common.HybridBinarizer;
import com.google.zxing.qrcode.QRCodeWriter;
import com.nirvasoft.nirva.nvcmd;


// class instancied when the service is started
class rbarcode
{
	//static int ERROR_BARCODE_MAX_SESSIONS_REACHED = 1;
	java.util.Hashtable<String,rxbarcodesession> Sessions;
	
	//int maxSessions  = 3;  // Initialize with 3 sessions for demo mode
	// Called one time when the service is started
	// Should return true if successful and false otherwise
	public boolean Init()
	{
		// Create a hash table for maintaining session objects
		//Sessions = new java.util.Hashtable<String,barcodesession>();
		
		// Create a hash table for maintaining session objects
		synchronized (this) {
			// Create a hash table for maintaining session objects
			Sessions = new java.util.Hashtable<String,rxbarcodesession>();
		}
		
		nvcmd command = new nvcmd();
		
		// TODO
		// Insert init service code here
		// Check the license
//		if(!command.Command("NV_CMD=|LICENSE:GET| SERVICE=|BARCODE| SERVICE_ID=|C68592DF9EDC2FBEEC399AF9664ABD0F| KEY=|RUNNING|")) {
//			command.Command("NV_CMD=|DEBUG:DISPLAY_MESSAGE| MESSAGE=|*** WARNING *** : No license found for BARCODE, running in restricted mode|");
//		}
//		
//		if(command.Command("NV_CMD=|LICENSE:GET| SERVICE=|BARCODE| SERVICE_ID=|C68592DF9EDC2FBEEC399AF9664ABD0F| KEY=|MAX_SESSIONS|")) {
//			String maxSessionString = command.NvResult;
//			try {
//				this.maxSessions = Integer.parseInt(maxSessionString);
//			} catch(Exception e) {
//				command.Command("NV_CMD=|DEBUG:DISPLAY_MESSAGE| MESSAGE=|WARNING : max sessions found but not well defined (working with a limited number of sessions)|");
//			}
//		}
//		
//		//create DEBUG log
//		if(!command.Command("NV_CMD=|LOG:CREATE| LOG=|DEBUG| SERVICE=|BARCODE|")) {
//			command.Command("NV_CMD=|DEBUG:DISPLAY_MESSAGE| MESSAGE=|*** WARNING *** : ERROR IN DEBUG LOG CREATION|");
//			return false;
//		}
		// Everything is OK		
		return true;
	}
	
	
	// Called one time when the service is stopped
	// Should return true if successful and false otherwise
	public boolean Exit()
	{
		// TODO
		// Insert service cleanup code here
		
		// Free the session hash table
		Sessions.clear();
		Sessions = null;
		return true;
	}
	
	
	public boolean Command()
	{
		nvcmd NvCommand = new nvcmd();

		// Used for testing the service
		if(NvCommand.IsCommand("BARCODE", "NOP", ""))
			return true;


		// Check the session init command
		String SessionId = NvCommand.GetSessionId();
		if(NvCommand.IsCommand("SYSTEM", "NV_INIT_SESSION", "SERVER"))
		{
			// Check that we have not reached the maximum number of sessions
			//int sessionCount;
			//synchronized (this) {
			//	sessionCount = Sessions.size();
			//}
			//if(maxSessions > 0 && sessionCount >= maxSessions) {
			//	NvCommand.Command("*** WARNING *** : Reached maximum number of sessions for BARCODE Service ("+maxSessions+")");
			//	NvCommand.SetError("BARCODE", barcode.ERROR_BARCODE_MAX_SESSIONS_REACHED, Integer.toString(maxSessions));
			//	return false;
			//}
			// Create a new session object
			rxbarcodesession Session = new rxbarcodesession(SessionId);
			
			// Init the object
			boolean Result = true;
			try {
				Result = Session.OnInit(NvCommand);
			} catch (Throwable e) {
				Result = false;
			}
			if (!Result) {
				return false;
			}
			// Put the session object into the hash table
			synchronized (this) {
				Sessions.put(SessionId, Session);
			}
			return true; // end processing init session
		}
		
		// Get the session object from the hash table
		rxbarcodesession Session = Sessions.get(SessionId);
		if(Session == null)
			return false;
		
		// Check the session exit command
		if(NvCommand.IsCommand("SYSTEM", "NV_EXIT_SESSION", "SERVER"))
		{
			// Cleanup the session object
			boolean Result = true;
			try
			{
				Result = Session.OnCleanup(NvCommand);
			}
			catch(Throwable e)
			{
				Result = false;
			}
			if(!Result)
				return false;
			// And remove it from the hash table
			Sessions.remove(SessionId);
			return true;	// end processing exit session
		}
		
		// Other command processing
		boolean Result = true;
		try
		{
			Result = Session.OnCommand(NvCommand);
		}
		catch (NVBarcodeException e) {
			e.printStackTrace();
			NvCommand.SetError(e.getSyst(), e.getErrorCode(), e.getMessage());
			Result = false;
		}
		catch(Throwable e)
		{
			e.printStackTrace();
			Result = false;
		}
		if(!Result)
			return false;
		
		return true;
	}
}

class rxbarcodesession
{
	// Session Id
	String sessionId;
	public static final int MISSING_PARAMETER = 101;
	public static final int INPUT_FILE = 101;
	
	// Constructor with session if, do not modify
	public rxbarcodesession(String sessionId) {
		this.sessionId = sessionId;
	}

	// Used for sessions hash table, do not modify
	public int hashCode() {
		return sessionId.hashCode();
	}

	// Called one time when initializing the session
	// return true if successful and false otherwise
	public boolean OnInit(nvcmd Command)
	{
		// TODO
		// Insert eventual initialization code here

		// All is OK
		return true;
	}
	
	
	// Called one time when closing the session
	// return true if successful and false otherwise
	public boolean OnCleanup(nvcmd Command)
	{
		// TODO
		// Insert eventual cleanup code here

		// All is OK
		return true;
	}


	// Called for each Nirva command to the service
	// This is the command entry point for session
	// return true if successful and false otherwise
	public boolean OnCommand(nvcmd Command) throws NirvaException
	{
		// TODO
		// Insert command processing here
		
		if(Command.IsCommand("BARCODE", "WRITER", "")){
			String data = getParameter(Command,"DATA");
			String type = getParameter(Command,"TYPE");
			String result = getParameter(Command,"RESULT","RESULT");				
			if (type.compareToIgnoreCase("QRCODE")==0){
				do_barcodezxing(Command,type,data,result);
			}
			else{
				do_barcode4j(Command,type,data,result);
			}
		}
		
		if(Command.IsCommand("BARCODE", "WRITERXZING", "")){
			String text = getParameter(Command,"DATA");
			
			String new_name = getParameter(Command,"RESULT","RESULT");
			Command.Command("NV_CMD=|OBJECT:CREATE| TYPE=|FILE| NAME=|"+new_name+"| EXTENSION=|png| NV_REVERSE_CONTAINER=|YES|");
			Command.Command("NV_CMD=|OBJECT:FILE_GET_PATHNAME|  NAME=|"+new_name+"| NV_REVERSE_CONTAINER=|YES|");
			String res_out = Command.GetResult();
			
			
			int width = 150;
			int height = 150;
			try{
				String type = getParameter(Command,"TYPE","QRCODE");
				QRCodeWriter qr = new QRCodeWriter();
				BitMatrix bm = qr.encode(text, BarcodeFormat.QR_CODE, width, height);
				
				BufferedImage image = new BufferedImage(width,height,ColorSpace.TYPE_RGB); 
				
				File f = new File(res_out);
				for(int x=0;x<width;x++){
					for(int y=0;y<height;y++){
						boolean b = bm.get(y, x);
						if (b){
							image.setRGB(x, y, 1);	
						}
						else{
							image.setRGB(x, y, 0);
						}
					}
				}
				ImageIO.write(image, "png", f); 
			}
			catch(Exception e){
				e.printStackTrace();
			}
			
		}
		
		if(Command.IsCommand("BARCODE", "WRITER4J", "")){
			String text = getParameter(Command,"DATA");
			
			String new_name = getParameter(Command,"RESULT","RESULT");
			Command.Command("NV_CMD=|OBJECT:CREATE| TYPE=|FILE| NAME=|"+new_name+"| EXTENSION=|png| NV_REVERSE_CONTAINER=|YES|");
			Command.Command("NV_CMD=|OBJECT:FILE_GET_PATHNAME|  NAME=|"+new_name+"| NV_REVERSE_CONTAINER=|YES|");
			String res_out = Command.GetResult();
			
			String type = getParameter(Command,"TYPE","DATAMATRIX");
						
			//int width = 150;
			//int height = 150;
			
			try {
				AbstractBarcodeBean bean = null;
				if (type.compareToIgnoreCase("DATAMATRIX")==0){
					bean = new DataMatrixBean();
				}
				if (type.compareToIgnoreCase("CODE_128")==0){
					bean = new Code128Bean();
				}
				if (type.compareToIgnoreCase("CODE_39")==0){
					bean = new Code39Bean();
				}
				if (type.compareToIgnoreCase("EAN-128")==0){
					bean = new EAN128Bean();
				}
				if (type.compareToIgnoreCase("EAN-123")==0){
					bean = new EAN13Bean();
				}
				if (type.compareToIgnoreCase("EAN-8")==0){
					bean = new EAN8Bean();
				}
				if (type.compareToIgnoreCase("PDF417")==0){
					bean = new PDF417Bean();
				}
				if (type.compareToIgnoreCase("UPCA")==0){
					bean = new UPCABean();
				}
				if (type.compareToIgnoreCase("UPCE")==0){
					bean = new UPCEBean();
				}
				if (type.compareToIgnoreCase("CODABAR")==0){
					bean = new CodabarBean();
				}
				//Dimension d = new Dimension();
				//d.setSize(width, height);
				//bean.setMinSize(d);
	            final int dpi = 150;
	            
	            //Configure the barcode generator
	            //bean.setModuleWidth(UnitConv.in2mm(1.0f / dpi)); //makes the narrow bar 
	                                                             //width exactly one pixel
	            bean.setModuleWidth(UnitConv.in2mm(5.0f / dpi));
	            //bean.setWideFactor(3);
	            bean.doQuietZone(false);
	            
	            //Open output file
	            File outputFile = new File(res_out);
	            OutputStream out = new FileOutputStream(outputFile);
	            try {
	                //Set up the canvas provider for monochrome JPEG output 
	                BitmapCanvasProvider canvas = new BitmapCanvasProvider(out, "image/png", dpi, BufferedImage.TYPE_BYTE_BINARY, false, 0);
	            
	                //Generate the barcode
	                bean.generateBarcode(canvas, text);
	            
	                //Signal end of generation
	                canvas.finish();
	            } finally {
	                out.close();
	            }
	        } catch (Exception e) {
	            e.printStackTrace();
	        }
		}
		
		if(Command.IsCommand("BARCODE", "READER", "")){
			String name = getParameter(Command,"NAME");
			Command.Command("NV_CMD=|OBJECT:FILE_GET_PATHNAME|  NAME=|"+name+"|");
			String in = Command.GetResult();
			
			String result_name = getParameter(Command,"RESULT","RESULT");
			BinaryBitmap bitmap = null;
			LuminanceSource source;
			
			try{
				File testImage = new File(in);
				BufferedImage image = ImageIO.read(testImage);			
				source = new BufferedImageLuminanceSource(image);
				bitmap = new BinaryBitmap(new HybridBinarizer(source));
			}
			catch(IOException ioe){
				ioe.printStackTrace();
				System.out.println("IO FILE: No file found");
				throw new NVBarcodeException("BARCODE",INPUT_FILE, "Unable to read file");
			}
			
			Hashtable<DecodeHintType, Object> hints_real = buildHints(true, false, false);
			Hashtable<DecodeHintType, Object> hints_bw = buildHints(true, true, false);
			
			try {			
				//step one, real
				Result result = new MultiFormatReader().decode(bitmap, hints_real);		
			    ParsedResult parsedResult = ResultParser.parseResult(result);  
			    //Command.Command("NV_CMD=|OBJECT:CREATE| TYPE=|STRING| NAME=|RAW| VALUE=|"+result.getText().replace("|","||")+"| NV_REVERSE_CONTAINER=|YES|");
			    Command.Command("NV_CMD=|OBJECT:CREATE| TYPE=|INDSTRINGLIST| NAME=|"+result_name+"| REPLACE=|YES| NV_REVERSE_CONTAINER=|YES|");
			    Command.Command("NV_CMD=|OBJECT:INDSTRINGLIST_SET_VALUE| NAME=|"+result_name+"| KEY=|TYPE| VALUE=|"+result.getBarcodeFormat().toString().replace("|","||")+"| NV_REVERSE_CONTAINER=|YES|");
			    Command.Command("NV_CMD=|OBJECT:INDSTRINGLIST_SET_VALUE| NAME=|"+result_name+"| KEY=|FORMAT| VALUE=|"+parsedResult.getType().toString().replace("|","||")+"| NV_REVERSE_CONTAINER=|YES|");
			    Command.Command("NV_CMD=|OBJECT:INDSTRINGLIST_SET_VALUE| NAME=|"+result_name+"| KEY=|VALUE| VALUE=|"+parsedResult.getDisplayResult().replace("|","||")+"| NV_REVERSE_CONTAINER=|YES|");
			    //System.out.println(" (format: " + result.getBarcodeFormat() +
			    //      ", type: " + parsedResult.getType() + "):\nRaw result:\n" + result.getText() +
			    //      "\nParsed result:\n" + parsedResult.getDisplayResult());
			    } 
			catch (ReaderException re) {
				try{
					Result result = new MultiFormatReader().decode(bitmap, hints_bw);		
					ParsedResult parsedResult = ResultParser.parseResult(result);  
					//Command.Command("NV_CMD=|OBJECT:CREATE| TYPE=|STRING| NAME=|RAW| VALUE=|"+result.getText().replace("|","||")+"| NV_REVERSE_CONTAINER=|YES|");
				    Command.Command("NV_CMD=|OBJECT:CREATE| TYPE=|INDSTRINGLIST| NAME=|"+result_name+"| REPLACE=|YES| NV_REVERSE_CONTAINER=|YES|");
				    Command.Command("NV_CMD=|OBJECT:INDSTRINGLIST_SET_VALUE| NAME=|"+result_name+"| KEY=|TYPE| VALUE=|"+result.getBarcodeFormat().toString().replace("|","||")+"| NV_REVERSE_CONTAINER=|YES|");
				    Command.Command("NV_CMD=|OBJECT:INDSTRINGLIST_SET_VALUE| NAME=|"+result_name+"| KEY=|FORMAT| VALUE=|"+parsedResult.getType().toString().replace("|","||")+"| NV_REVERSE_CONTAINER=|YES|");
				    Command.Command("NV_CMD=|OBJECT:INDSTRINGLIST_SET_VALUE| NAME=|"+result_name+"| KEY=|VALUE| VALUE=|"+parsedResult.getDisplayResult().replace("|","||")+"| NV_REVERSE_CONTAINER=|YES|");
					//System.out.println(" (format: " + result.getBarcodeFormat() +
					//		", type: " + parsedResult.getType() + "):\nRaw result:\n" + result.getText() +
					//		"\nParsed result:\n" + parsedResult.getDisplayResult());
			    }
				catch(ReaderException re2){
					re2.printStackTrace();
					Command.Command("NV_CMD=|OBJECT:CREATE| TYPE=|INDSTRINGLIST| NAME=|"+result_name+"| REPLACE=|YES| NV_REVERSE_CONTAINER=|YES|");
					Command.Command("NV_CMD=|OBJECT:INDSTRINGLIST_SET_VALUE| NAME=|"+result_name+"| KEY=|NO_RESULT| VALUE=|UNABLE_TO_READ| NV_REVERSE_CONTAINER=|YES|");
					//System.out.println("No barcode found");
				}
			}
		}
				
		// Everything is OK		
		return true;
	}
	
	
	public String getParameter(nvcmd cmd, String name, String def) throws NirvaException {
		if (!cmd.ParameterExist(name)) {
			return def;
		}
		return cmd.GetParameter(name);
	}
	
	public String getParameter(nvcmd cmd,String name) throws NirvaException {
		if (!cmd.ParameterExist(name)) {
			throw new NVBarcodeException("BARCODE",MISSING_PARAMETER, "Missing parameter");
		}
		return cmd.GetParameter(name);
	}
	
	protected static BufferedImage rotateImage(BufferedImage original, float degrees) {
	    if (degrees == 0.0f) {
	      return original;
	    } else {
	      java.awt.geom.AffineTransform at = new java.awt.geom.AffineTransform();
	      at.rotate(Math.toRadians(degrees), original.getWidth() / 2.0, original.getHeight() / 2.0);
	      BufferedImageOp op = new AffineTransformOp(at, AffineTransformOp.TYPE_BICUBIC);
	      return op.filter(original, null);
	    }
	  }
	
	
	
	// Writes out a single PNG which is three times the width of the input image, containing from left
	  // to right: the original image, the row sampling monochrome version, and the 2D sampling
	  // monochrome version.
	  // TODO: Update to compare different Binarizer implementations.
	private static void dumpBlackPoint(URI uri, BufferedImage image, BinaryBitmap bitmap) {
	    String inputName = uri.getPath();
	    if (inputName.contains(".mono.png")) {
	      return;
	    }

	    int width = bitmap.getWidth();
	    int height = bitmap.getHeight();
	    int stride = width * 3;
	    int[] pixels = new int[stride * height];

	    // The original image
	    int[] argb = new int[width];
	    for (int y = 0; y < height; y++) {
	      image.getRGB(0, y, width, 1, argb, 0, width);
	      System.arraycopy(argb, 0, pixels, y * stride, width);
	    }

	    // Row sampling
	    BitArray row = new BitArray(width);
	    for (int y = 0; y < height; y++) {
	      try {
	        row = bitmap.getBlackRow(y, row);
	      } catch (NotFoundException nfe) {
	        // If fetching the row failed, draw a red line and keep going.
	        int offset = y * stride + width;
	        for (int x = 0; x < width; x++) {
	          pixels[offset + x] = 0xffff0000;
	        }
	        continue;
	      }

	      int offset = y * stride + width;
	      for (int x = 0; x < width; x++) {
	        if (row.get(x)) {
	          pixels[offset + x] = 0xff000000;
	        } else {
	          pixels[offset + x] = 0xffffffff;
	        }
	      }
	    }

	    // 2D sampling
	    try {
	      for (int y = 0; y < height; y++) {
	        BitMatrix matrix = bitmap.getBlackMatrix();
	        int offset = y * stride + width * 2;
	        for (int x = 0; x < width; x++) {
	          if (matrix.get(x, y)) {
	            pixels[offset + x] = 0xff000000;
	          } else {
	            pixels[offset + x] = 0xffffffff;
	          }
	        }
	      }
	    } catch (NotFoundException nfe) {
	    }

	    // Write the result
	    BufferedImage result = new BufferedImage(stride, height, BufferedImage.TYPE_INT_ARGB);
	    result.setRGB(0, 0, stride, height, pixels, 0, stride);

	    // Use the current working directory for URLs
	    String resultName = inputName;
	    if ("http".equals(uri.getScheme())) {
	      int pos = resultName.lastIndexOf('/');
	      if (pos > 0) {
	        resultName = '.' + resultName.substring(pos);
	      }
	    }
	    int pos = resultName.lastIndexOf('.');
	    if (pos > 0) {
	      resultName = resultName.substring(0, pos);
	    }
	    resultName += ".mono.png";
	    OutputStream outStream = null;
	    try {
	      outStream = new FileOutputStream(resultName);
	      ImageIO.write(result, "png", outStream);
	    } catch (FileNotFoundException e) {
	      System.err.println("Could not create " + resultName);
	    } catch (IOException e) {
	      System.err.println("Could not write to " + resultName);
	    } finally {
	      try {
	        if (outStream != null) {
	          outStream.close();
	        }
	      } catch (IOException ioe) {
	        // continue
	      }
	    }
	  }
	
	private static Hashtable<DecodeHintType, Object> buildHints(boolean tryHarder, boolean pureBarcode, boolean productsOnly) {
		Hashtable<DecodeHintType, Object> hints = new Hashtable<DecodeHintType, Object>(3);
		Vector<BarcodeFormat> vector = new Vector<BarcodeFormat>(8);
		vector.addElement(BarcodeFormat.UPC_A);
		vector.addElement(BarcodeFormat.UPC_E);
		vector.addElement(BarcodeFormat.EAN_13);
		vector.addElement(BarcodeFormat.EAN_8);
		vector.addElement(BarcodeFormat.RSS14);    
		if (!productsOnly) {
		vector.addElement(BarcodeFormat.CODE_39);
		//vector.addElement(BarcodeFormat.CODE_93); ?? 
		vector.addElement(BarcodeFormat.CODE_128);
		vector.addElement(BarcodeFormat.ITF);
		vector.addElement(BarcodeFormat.QR_CODE);
		vector.addElement(BarcodeFormat.DATAMATRIX);
		//vector.addElement(BarcodeFormat.PDF417); ??
		//vector.addElement(BarcodeFormat.CODABAR); ??
		}
		hints.put(DecodeHintType.POSSIBLE_FORMATS, vector);
		if (tryHarder) {
		hints.put(DecodeHintType.TRY_HARDER, Boolean.TRUE);
		}
		if (pureBarcode) {
		hints.put(DecodeHintType.PURE_BARCODE, Boolean.TRUE);
		}
		return hints;
		}
	
	public void do_barcode4j(nvcmd Command, String type, String data, String result){
		Command.Command("NV_CMD=|OBJECT:CREATE| TYPE=|FILE| NAME=|"+result+"| EXTENSION=|png| NV_REVERSE_CONTAINER=|YES|");
		Command.Command("NV_CMD=|OBJECT:FILE_GET_PATHNAME|  NAME=|"+result+"| NV_REVERSE_CONTAINER=|YES|");
		String res_out = Command.GetResult();
							
		//int width = 150;
		//int height = 150;
		
		try {
			AbstractBarcodeBean bean = null;
			if (type.compareToIgnoreCase("DATAMATRIX")==0){
				bean = new DataMatrixBean();
			}
			if (type.compareToIgnoreCase("CODE_128")==0){
				bean = new Code128Bean();
			}
			if (type.compareToIgnoreCase("CODE_39")==0){
				bean = new Code39Bean();
			}
			if (type.compareToIgnoreCase("EAN-128")==0){
				bean = new EAN128Bean();
			}
			if (type.compareToIgnoreCase("EAN-123")==0){
				bean = new EAN13Bean();
			}
			if (type.compareToIgnoreCase("EAN-8")==0){
				bean = new EAN8Bean();
			}
			if (type.compareToIgnoreCase("PDF417")==0){
				bean = new PDF417Bean();
			}
			if (type.compareToIgnoreCase("UPCA")==0){
				bean = new UPCABean();
			}
			if (type.compareToIgnoreCase("UPCE")==0){
				bean = new UPCEBean();
			}
			if (type.compareToIgnoreCase("CODABAR")==0){
				bean = new CodabarBean();
			}
			//Dimension d = new Dimension();
			//d.setSize(width, height);
			//bean.setMinSize(d);
            final int dpi = 150;
            
            //Configure the barcode generator
            //bean.setModuleWidth(UnitConv.in2mm(1.0f / dpi)); //makes the narrow bar 
                                                             //width exactly one pixel
            bean.setModuleWidth(UnitConv.in2mm(5.0f / dpi));
            //bean.setWideFactor(3);
            bean.doQuietZone(false);
            
            //Open output file
            File outputFile = new File(res_out);
            OutputStream out = new FileOutputStream(outputFile);
            try {
                //Set up the canvas provider for monochrome JPEG output 
                BitmapCanvasProvider canvas = new BitmapCanvasProvider(out, "image/png", dpi, BufferedImage.TYPE_BYTE_BINARY, false, 0);
            
                //Generate the barcode
                bean.generateBarcode(canvas, data);
            
                //Signal end of generation
                canvas.finish();
            } finally {
                out.close();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
	}
	public void do_barcodezxing(nvcmd Command, String type, String data, String result){
		Command.Command("NV_CMD=|OBJECT:CREATE| TYPE=|FILE| NAME=|"+result+"| EXTENSION=|png| NV_REVERSE_CONTAINER=|YES|");
		Command.Command("NV_CMD=|OBJECT:FILE_GET_PATHNAME|  NAME=|"+result+"| NV_REVERSE_CONTAINER=|YES|");
		String res_out = Command.GetResult();
				
		int width = 200;
		int height = 200;
		try{
			System.out.println("DATA is "+data);
			QRCodeWriter qr = new QRCodeWriter();
			BitMatrix bm = qr.encode(data, BarcodeFormat.QR_CODE, width, height);
			
			BufferedImage image = new BufferedImage(width,height,ColorSpace.TYPE_RGB); 
			
			File f = new File(res_out);
			for(int x=0;x<width;x++){
				for(int y=0;y<height;y++){
					boolean b = bm.get(x,y);
					//System.out.println("BOOLEAN IS "+b);
					if (b){
						//System.out.println("("+x+","+y+") set to 1");
						image.setRGB(x, y, 0);
					}
					else{
//						System.out.println("("+x+","+y+") set to 0");
						image.setRGB(x, y, 16777215);
					}
				}
			}
			ImageIO.write(image, "png", f); 
		}
		catch(Exception e){
			e.printStackTrace();
		}
	}	
}


