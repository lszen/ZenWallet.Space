/************************************************************************************************
 *  _________          _     ____          _           __        __    _ _      _   _   _ ___
 * |__  / ___|__ _ ___| |__ / ___|_      _(_)_ __   __ \ \      / /_ _| | | ___| |_| | | |_ _|
 *   / / |   / _` / __| '_ \\___ \ \ /\ / / | '_ \ / _` \ \ /\ / / _` | | |/ _ \ __| | | || |
 *  / /| |__| (_| \__ \ | | |___) \ V  V /| | | | | (_| |\ V  V / (_| | | |  __/ |_| |_| || |
 * /____\____\__,_|___/_| |_|____/ \_/\_/ |_|_| |_|\__, | \_/\_/ \__,_|_|_|\___|\__|\___/|___|
 *                                                 |___/
 *
 * Copyright (c) 2016 Ivan Vaklinov <ivan@vaklinov.com>
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 **********************************************************************************/
package net.ddns.lsmobile.zencashvaadinwalletui4cpp.business;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.LineNumberReader;
import java.io.Reader;
import java.io.StringReader;
import java.io.UnsupportedEncodingException;
import java.lang.reflect.Method;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Collection;
import java.util.List;

import com.eclipsesource.json.Json;
import com.eclipsesource.json.JsonObject;

/**
 * Utilities - generally reusable across classes.
 *
 * @author Ivan Vaklinov <ivan@vaklinov.com>
 */
public class Util
{
	// Compares two string arrays (two dimensional).
	public static boolean arraysAreDifferent(final String ar1[][], final String ar2[][])
	{
		if (ar1 == null)
		{
			if (ar2 != null)
			{
				return true;
			}
		} else if (ar2 == null)
		{
			return true;
		}
		
		if (ar1.length != ar2.length)
		{
			return true;
		}
		
		for (int i = 0; i < ar1.length; i++)
		{
			if (ar1[i].length != ar2[i].length)
			{
				return true;
			}
			
			for (int j = 0; j < ar1[i].length; j++)
			{
				final String s1 = ar1[i][j];
				final String s2 = ar2[i][j];
				
				if (s1 == null)
				{
					if (s2 != null)
					{
						return true;
					}
				} else if (s2 == null)
				{
					return true;
				} else
				{
					if (!s1.equals(s2))
					{
						return true;
					}
				}
			}
		}
		
		return false;
	}
	
	// Compares two string arrays (two dimensional).
	public static boolean listsAreDifferent(final List<String[]> ar1, final List<String[]> ar2)
	{
		if (ar1 == null)
		{
			if (ar2 != null)
			{
				return true;
			}
		} else if (ar2 == null)
		{
			return true;
		}
		
		if (ar1.size() != ar2.size())
		{
			return true;
		}
		
		for (int i = 0; i < ar1.size(); i++)
		{
			if (ar1.get(i).length != ar2.get(i).length)
			{
				return true;
			}
			
			for (int j = 0; j < ar1.get(i).length; j++)
			{
				final String s1 = ar1.get(i)[j];
				final String s2 = ar2.get(i)[j];
				
				if (s1 == null)
				{
					if (s2 != null)
					{
						return true;
					}
				} else if (s2 == null)
				{
					return true;
				} else
				{
					if (!s1.equals(s2))
					{
						return true;
					}
				}
			}
		}
		
		return false;
	}
	
	
	public static <C extends Collection<T>, T> boolean contentsAreDifferent(final C collection1, final C collection2, final C difference) throws InstantiationException, IllegalAccessException
	{
		if (collection1 == null && collection2 != null || collection1 != null && collection2 == null) {
			return true;
		}
		if (collection1 == null && collection2 == null) {
			return false;
		}
		if (collection1.size() != collection2.size()) {
			return true;
		}
//		final C difference = clazz.newInstance();
		difference.clear();
		difference.addAll(collection1);
		difference.removeAll(collection2);
		return !difference.isEmpty();
	}
	
	
	// Turns a 1.0.7+ error message to a an old JSOn style message
	// info - new style error message
	public static JsonObject getJsonErrorMessage(final String info)
	    throws IOException
	{
    	final JsonObject jInfo = new JsonObject();
    	
    	// Error message here comes from ZCash 1.0.7+ and is like:
    	//zcash-cli getinfo
    	//error code: -28
    	//error message:
    	//Loading block index...
    	final LineNumberReader lnr = new LineNumberReader(new StringReader(info));
    	final int errCode =  Integer.parseInt(lnr.readLine().substring(11).trim());
    	jInfo.set("code", errCode);
    	lnr.readLine();
    	jInfo.set("message", lnr.readLine().trim());
    	
    	return jInfo;
	}
	
	
	/**
	 * Escapes a text value to a form suitable to be displayed in HTML content. Important control
	 * characters are replaced with entities.
	 * 
	 * @param inputValue th value to escape
	 * 
	 * @return the "safe" value to display.
	 */
	public static String escapeHTMLValue(final String inputValue)
	{
	    final StringBuilder outputValue = new StringBuilder();
	    for (final char c : inputValue.toCharArray())
	    {
	        if ((c > 127) || (c == '"') || (c == '<') || (c == '>') || (c == '&'))
	        {
	            outputValue.append("&#");
	            outputValue.append((int)c);
	            outputValue.append(';');
	        } else
	        {
	            outputValue.append(c);
	        }
	    }
	    return outputValue.toString();
	}
	
	
	public static boolean stringIsEmpty(final String s)
	{
		return (s == null) || (s.length() <= 0);
	}
	
	
	public static String decodeHexMemo(final String memoHex)
		throws UnsupportedEncodingException
	{
        // Skip empty memos
        if (memoHex.startsWith("f600000000"))
        {
        	return null;
        }
        
        // should be read with UTF-8
        final byte[] bytes = new byte[(memoHex.length() / 2) + 1];
        int count = 0;
        
        for (int j = 0; j < memoHex.length(); j += 2)
        {
            final String str = memoHex.substring(j, j + 2);
            bytes[count++] = (byte)Integer.parseInt(str, 16);
        }
        
        // Remove zero padding
        // TODO: may cause problems if UNICODE chars have trailing ZEROS
        while (count > 0)
        {
        	if (bytes[count - 1] == 0)
        	{
        		count--;
        	} else
        	{
        		break;
        	}
        }
        
        return new String(bytes, 0, count, "UTF-8");
	}
	
	
	public static String encodeHexString(final String str)
		throws UnsupportedEncodingException
	{
		return encodeHexArray(str.getBytes("UTF-8"));
	}
	
	
	public static String encodeHexArray(final byte array[])
	{
		final StringBuilder encoded = new StringBuilder();
		for (final byte c : array)
		{
			String hexByte = Integer.toHexString(c);
			if (hexByte.length() < 2)
			{
				hexByte = "0" + hexByte;
			} else if (hexByte.length() > 2)
			{
				hexByte = hexByte.substring(hexByte.length() - 2, hexByte.length());
			}
			encoded.append(hexByte);
		}
		
		return encoded.toString();
	}
	
	
	/**
	 * Maintains a set of old copies for a file.
	 * For a file dir/file, the old versions are dir/file.1, dir/file.2 etc. up to 9.
	 * 
	 * @param dir base directory
	 * 
	 * @param file name of the original file
	 */
	public static void renameFileForMultiVersionBackup(final File dir, final String file)
	{
		final int VERSION_COUNT = 9;
		
		// Delete last one if it exists
		final File last = new File(dir, file + "." + VERSION_COUNT);
		if (last.exists())
		{
			last.delete();
		}
		
		// Iterate and rename
		for (int i = VERSION_COUNT - 1; i >= 1; i--)
		{
			final File f = new File(dir, file + "." + i);
			final int newIndex = i + 1;
			if (f.exists())
			{
				f.renameTo(new File(dir, file + "." + newIndex));
			}
		}
		
		// Rename last one
		final File orig = new File(dir, file);
		if (orig.exists())
		{
			orig.renameTo(new File(dir, file + ".1"));
		}
	}
	
	
	public static JsonObject parseJsonObject(final String json)
		throws IOException
	{
		try
		{
			return Json.parse(json).asObject();
		} catch (final RuntimeException rte)
		{
			throw new IOException(rte);
		}
	}
	
	
	public static JsonObject parseJsonObject(final Reader r)
		throws IOException
	{
		try
		{
			return Json.parse(r).asObject();
		} catch (final RuntimeException rte)
		{
			throw new IOException(rte);
		}
	}

	
	public static byte[] calculateSHA256Digest(final byte[] input)
		throws IOException
	{
        try
        {
        	final MessageDigest sha256 = MessageDigest.getInstance("SHA-256");
        	final DigestInputStream dis = new DigestInputStream(new ByteArrayInputStream(input), sha256);
            final byte [] temp = new byte[0x1 << 13];
            byte[] digest;
            while(dis.read(temp) >= 0) {
				;
			}
            {
            	digest = sha256.digest();
            }
            
            return digest;
        } catch (final NoSuchAlgorithmException impossible)
        {
            throw new IOException(impossible);
        }
	}
	
	
	public static String convertGroupPhraseToZPrivateKey(final String phrase)
		throws IOException
	{
		final byte phraseBytes[] = phrase.getBytes("UTF-8");
		final byte phraseDigest[] = calculateSHA256Digest(phraseBytes);
		
		phraseDigest[0] &= (byte)0x0f;
		
		//System.out.println(encodeHexArray(phraseDigest));
		
		final byte base58Input[] = new byte[phraseDigest.length + 2];
		base58Input[0] = (byte)0xab;
		base58Input[1] = (byte)0x36;
		System.arraycopy(phraseDigest, 0, base58Input, 2, phraseDigest.length);
		
		// Do a double SHA356 to get a checksum for the data to encode
		final byte shaStage1[] = calculateSHA256Digest(base58Input);
		final byte checksum[] = calculateSHA256Digest(shaStage1);
		
		final byte base58CheckInput[] = new byte[base58Input.length + 4];
		System.arraycopy(base58Input, 0, base58CheckInput, 0, base58Input.length);
		System.arraycopy(checksum, 0, base58CheckInput, base58Input.length, 4);
		
		// Call BitcoinJ via reflection - and report error if missing
		try
		{
			final Class base58Class = Class.forName("org.bitcoinj.core.Base58");
			final Method encode = base58Class.getMethod("encode", byte[].class);
			return (String)encode.invoke(null, base58CheckInput);
		} catch (final Exception e)
		{
			throw new IOException(
				"There was a problem invoking the BitcoinJ library to do Base58 encoding. " +
			    "Make sure the bitcoinj-core-0.14.5.jar is available!", e);
		}
	}
	
	
	// zc/zt - mainnet and testnet
	// TODO: We need a much more precise criterion to distinguish T/Z adresses;
	public static boolean isZAddress(final String address)
	{
		return (address != null) &&
			   (address.startsWith("zc") || address.startsWith("zt")) &&
			   (address.length() > 40);
	}
	
	
	/**
	 * Delets a directory and all of its subdirectories.
	 * 
	 * @param dir directory to delete.
	 * 
	 * @throws IOException if not successful
	 */
	public static void deleteDirectory(final File dir)
		throws IOException
	{
		for (final File f : dir.listFiles())
		{
			if (f.isDirectory())
			{
				deleteDirectory(f);
			} else
			{
				if (!f.delete())
				{
					throw new IOException("Could not delete file: " + f.getAbsolutePath());
				}
			}
		}
		
		if (!dir.delete())
		{
			throw new IOException("Could not delete directory: " + dir.getAbsolutePath());
		}
	}
	
	
	/**
	 * Wraps an input string in a block form with the specified width. LF is used to end each line.
	 * 
	 * @param inStr
	 * @param width
	 * 
	 * @return input wrapped
	 */
	public static String blockWrapString(final String inStr, final int width)
	{
		final StringBuilder block = new StringBuilder();
		
		int position = 0;
		while (position < inStr.length())
		{
			final int endPosition = Math.min(position + width, inStr.length());
			block.append(inStr.substring(position, endPosition));
			block.append("\n");
			position += width;
		}
		
		return block.toString();
	}
	
}
