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


import java.io.File;
import java.io.IOException;
import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import com.eclipsesource.json.Json;
import com.eclipsesource.json.JsonArray;
import com.eclipsesource.json.JsonObject;
import com.eclipsesource.json.JsonValue;
import com.eclipsesource.json.ParseException;
import com.eclipsesource.json.WriterConfig;
import com.vaadin.server.VaadinSession;

import net.ddns.lsmobile.zencashvaadinwalletui4cpp.business.OSUtil.OS_TYPE;
import net.ddns.lsmobile.zencashvaadinwalletui4cpp.entities.Address;
import net.ddns.lsmobile.zencashvaadinwalletui4cpp.entities.User;


/**
 * Calls zcash-cli
 *
 * @author Ivan Vaklinov <ivan@vaklinov.com>
 */
public class ZCashClientCaller implements IConfig
{
	public static class WalletBalance
	{
		public BigDecimal transparentBalance = BigDecimal.ZERO;
		public BigDecimal privateBalance = BigDecimal.ZERO;

		public BigDecimal transparentUnconfirmedBalance = BigDecimal.ZERO;
		public BigDecimal privateUnconfirmedBalance = BigDecimal.ZERO;
		
		public BigDecimal getTotalBalance() {
			return BigDecimal.ZERO.add(this.transparentBalance).add(this.privateBalance);
		}
		public BigDecimal getTotalUnconfirmedBalance() {
			return BigDecimal.ZERO.add(this.transparentUnconfirmedBalance).add(this.privateUnconfirmedBalance);
		}
	}
	
	public class AddressWithBalance
	{
		public String address;
		public BigDecimal confirmedBalance = BigDecimal.ZERO;
		public BigDecimal unconfirmedBalance = BigDecimal.ZERO;
		public boolean isPrivate = false;
		public OutputFormat<AddressWithBalance> outputFormat;
		
		public AddressWithBalance(final String address, final boolean isPrivate) throws WalletCallException, IOException, InterruptedException {
			super();
			this.address = address;
			this.confirmedBalance = getBalanceForAddress(address);
			this.unconfirmedBalance = getUnconfirmedBalanceForAddress(address);
		}

		public BigDecimal getBalance() {
			return this.unconfirmedBalance;
		}
		
		public boolean isConfirmed () {
			return this.confirmedBalance.equals(this.unconfirmedBalance);
		}
		
	    @Override
	    public boolean equals(final Object obj) {
	        return (obj instanceof AddressWithBalance && ((AddressWithBalance)obj).address.equals(this.address)
	        		&& ((AddressWithBalance)obj).confirmedBalance.equals(this.confirmedBalance)
	        		&& ((AddressWithBalance)obj).unconfirmedBalance.equals(this.unconfirmedBalance));
	    }
	    
	    @Override
	    public int hashCode() {
	        return (this.address + this.confirmedBalance.doubleValue() + this.unconfirmedBalance.doubleValue()).hashCode();
	    }
	    
	    public void setOutputFormat (final OutputFormat<AddressWithBalance> outputFormat) {
			this.outputFormat = outputFormat;
	    }

	    @Override
		public String toString() {
	    	if (this.outputFormat != null) {
	    		return this.outputFormat.format(this);
	    	}
	    	return super.toString();
	    }
	}


	public static class NetworkAndBlockchainInfo
	{
		public int numConnections;
		public Date lastBlockDate;
	}


	public static class WalletCallException
		extends Exception
	{
		public WalletCallException(final String message)
		{
			super(message);
		}

		public WalletCallException(final String message, final Throwable cause)
		{
			super(message, cause);
		}
	}
	
	
	private static ZCashClientCaller instance;

	
	// ZCash client program and daemon
	private static File zcashcli, zcashd;
	
	
    public static synchronized ZCashClientCaller getInstance() throws IOException {
        if( instance == null ) {
			instance = new ZCashClientCaller();
		}
        return instance;
    }


	private ZCashClientCaller()
		throws IOException
	{
		// Detect daemon and client tools installation
		zcashcli = OSUtil.findZCashCommand(OSUtil.getZCashCli());

		if ((zcashcli == null) || (!zcashcli.exists()))
		{
			throw new IOException(
				"The ZENCash installation directory needs to contain " +
				"the command line utilities zend and zen-cli. zen-cli is missing!");
		}
		
	    zcashd = OSUtil.findZCashCommand(OSUtil.getZCashd());
		
		if (zcashd == null || (!zcashd.exists()))
		{
		    throw new IOException(
		    	"The ZENCash command line utility " + zcashcli.getCanonicalPath() +
		    	" was found, but zend was not found!");
		}
	}
	

	public static synchronized JsonObject getDaemonRawRuntimeInfo()
		throws IOException, InterruptedException, WalletCallException
	{
	    final CommandExecutor infoGetter = new CommandExecutor(
	            new String[] { zcashcli.getCanonicalPath(), "getinfo"} );
	    String info = infoGetter.execute();
	    
	    log.info("zcashcli.getinfo " + info);
	    
	    if (info.trim().toLowerCase(Locale.ROOT).startsWith("error: couldn't connect to server"))
	    {
	    	throw new IOException(info.trim());
	    }
	    
	    if (info.trim().toLowerCase(Locale.ROOT).startsWith("error: "))
	    {
	        info = info.substring(7);
	        
		    try
		    {
		        return Json.parse(info).asObject();
		    } catch (final ParseException pe)
		    {
		    	log.error("unexpected daemon info: " + info);
		        throw new IOException(pe);
		    }
	    } else if (info.trim().toLowerCase(Locale.ROOT).startsWith("error code:"))
	    {
	    	return Util.getJsonErrorMessage(info);
	    } else
	    {
		    try
		    {
		        return Json.parse(info).asObject();
		    } catch (final ParseException pe)
		    {
		    	log.info("unexpected daemon info: " + info);
		        throw new IOException(pe);
		    }
	    }
	}

	
	public WalletBalance getWalletInfo(final List<AddressWithBalance> addressesWithBalance)
		throws WalletCallException, IOException, InterruptedException
	{
		final WalletBalance balance = new WalletBalance();
		
		for (final AddressWithBalance address : addressesWithBalance)
		{
			if (!address.isPrivate) {
				balance.transparentBalance = balance.transparentBalance.add(address.confirmedBalance);
				balance.transparentUnconfirmedBalance = balance.transparentUnconfirmedBalance.add(address.unconfirmedBalance);
			}
			else {
				balance.privateBalance = balance.privateBalance.add(address.confirmedBalance);
				balance.privateUnconfirmedBalance = balance.privateUnconfirmedBalance.add(address.unconfirmedBalance);
			}
		}
//    	JsonObject objResponse = this.executeCommandAndGetJsonObject("z_gettotalbalance", null);
//
//    	balance.transparentBalance = Double.valueOf(objResponse.getString("transparent", "-1"));
//    	balance.privateBalance     = Double.valueOf(objResponse.getString("private", "-1"));
//    	balance.totalBalance       = Double.valueOf(objResponse.getString("total", "-1"));
//
//        objResponse = this.executeCommandAndGetJsonObject("z_gettotalbalance", "0");
//
//    	balance.transparentUnconfirmedBalance = Double.valueOf(objResponse.getString("transparent", "-1"));
//    	balance.privateUnconfirmedBalance     = Double.valueOf(objResponse.getString("private", "-1"));
//    	balance.totalUnconfirmedBalance       = Double.valueOf(objResponse.getString("total", "-1"));

		return balance;
	}


	public Set<Transaction> getWalletPublicTransactions(final VaadinSession session)
		throws WalletCallException, IOException, InterruptedException
	{
		final User user = (User) session.getAttribute(AUTHENTICATION_RESULT);

		final JsonArray jsonTransactions = executeCommandAndGetJsonArray(
	    	"listtransactions", wrapStringParameter(""), "300");
	    final Set<Transaction> transactions = new HashSet<> ();
	    for (final JsonValue jsonValue : jsonTransactions)
	    {
	    	final JsonObject jsonObject = jsonValue.asObject();
	    	if (!addressDAO.isAddressFromUser(user, jsonObject.getString("address", null)).isEmpty()) {
		    	final Transaction transaction = new Transaction ();
		    	// Needs to be the same as in getWalletZReceivedTransactions()
		    	// TODO: some day refactor to use object containers
		    	transaction.setType(Transaction.Type.PUBLIC);
		    	transaction.setDirection(jsonObject.getString("category", null));
		    	transaction.setIsConfirmed(jsonObject.get("confirmations").toString());
		    	transaction.setAmount(jsonObject.get("amount").toString());
		    	transaction.setDate(jsonObject.get("time").toString());
		    	transaction.setDestinationAddress(jsonObject.getString("address", null));
		    	transaction.setTransaction(jsonObject.get("txid").toString().replaceAll("\"", ""));
		    	transactions.add(transaction);
	    	}
	    }

	    return transactions;
	}
	
	/**
	 * Get available public+private transactions and unify them
	 * @param user
	 * */
	public Set<Transaction> getTransactionsDataFromWallet(final VaadinSession session)
			throws WalletCallException, IOException, InterruptedException
	{
		final Set<Transaction> publicTransactions = getWalletPublicTransactions(session);
		final Set<Transaction> zReceivedTransactions = getWalletZReceivedTransactions(session);

		final Set<Transaction> allTransactions = new HashSet<>();
		
		allTransactions.addAll(publicTransactions);
		allTransactions.addAll(zReceivedTransactions);
		
		return allTransactions;
	}
	
	
	public List<AddressWithBalance> getAddressBalanceDataFromWallet(final VaadinSession session)
			throws WalletCallException, IOException, InterruptedException
	{
		// Z Addresses - they are OK
		final Set<String> zAddresses = getWalletZAddresses(session);
		
		// Combine all known T addresses
		final Set<String> tAddressesCombined = new HashSet<>();
		// T Addresses created inside wallet that may be empty
		tAddressesCombined.addAll(getWalletAllPublicAddresses(session));
		// T addresses with unspent outputs (even if not GUI created)...
		tAddressesCombined.addAll(getWalletPublicAddressesWithUnspentOutputs(session));
		
		final List<AddressWithBalance> addressesWithBalances = new ArrayList<> ();
		
		
		for (final String address : tAddressesCombined)
		{
			addressesWithBalances.add(new AddressWithBalance(address, false));
		}
		
		for (final String address : zAddresses)
		{
			addressesWithBalances.add(new AddressWithBalance(address, true));
		}

		return addressesWithBalances;
	}

	
	public Set<String> getWalletZAddresses(final VaadinSession session)
		throws WalletCallException, IOException, InterruptedException
	{
		final User user = (User) session.getAttribute(AUTHENTICATION_RESULT);

		final JsonArray jsonAddresses = executeCommandAndGetJsonArray("z_listaddresses", null);
		final Set<String> listAddresses = new HashSet<> ();
		for (int i = 0; i < jsonAddresses.size(); i++)
		{
	    	if (!addressDAO.isAddressFromUser(user, jsonAddresses.get(i).asString()).isEmpty()) {
	    		listAddresses.add(jsonAddresses.get(i).asString());
	    	}
		}
	    return listAddresses;
	}


	public Set<Transaction> getWalletZReceivedTransactions(final VaadinSession session)
		throws WalletCallException, IOException, InterruptedException
	{
		final Set<String> zAddresses = this.getWalletZAddresses(session);

		final Set<Transaction> zReceivedTransactions = new HashSet<>();

		for (final String zAddress : zAddresses)
		{
		    final JsonArray jsonTransactions = executeCommandAndGetJsonArray(
		    	"z_listreceivedbyaddress", wrapStringParameter(zAddress), "0");
		    for (int i = 0; i < jsonTransactions.size(); i++)
		    {
		    	final JsonObject trans = jsonTransactions.get(i).asObject();
		    	final Transaction currentTransaction = new Transaction();

		    	final String txID = trans.getString("txid", "ERROR!");
		    	// Needs to be the same as in getWalletPublicTransactions()
		    	// TODO: some day refactor to use object containers
		    	currentTransaction.setType(Transaction.Type.PUBLIC);
		    	currentTransaction.setDirection(Transaction.Direction.RECEIVE);
		    	currentTransaction.setIsConfirmed(this.getWalletTransactionConfirmations(txID));
		    	currentTransaction.setAmount(trans.get("amount").toString());
		    	currentTransaction.setDate(this.getWalletTransactionTime(txID));// TODO: minimize sub-calls
		    	currentTransaction.setDestinationAddress(zAddress);
		    	currentTransaction.setTransaction(trans.get("txid").toString());

		    	zReceivedTransactions.add(currentTransaction);
		    }
		}

		//return zReceivedTransactions.toArray(new String[0][]); TODO LS
		return zReceivedTransactions;
	}

	
	public JsonObject[] getTransactionMessagingDataForZaddress(final String ZAddress)
		throws WalletCallException, IOException, InterruptedException
	{
	    final JsonArray jsonTransactions = executeCommandAndGetJsonArray(
		    	"z_listreceivedbyaddress", wrapStringParameter(ZAddress), "0");
	    final List<JsonObject> transactions = new ArrayList<>();
		for (int i = 0; i < jsonTransactions.size(); i++)
		{
		   	final JsonObject trans = jsonTransactions.get(i).asObject();
	    	transactions.add(trans);
	    }
		
		return transactions.toArray(new JsonObject[0]);
	}
	

	// ./src/zcash-cli listunspent only returns T addresses it seems
	public Set<String> getWalletPublicAddressesWithUnspentOutputs(final VaadinSession session)
		throws WalletCallException, IOException, InterruptedException
	{
		final User user = (User) session.getAttribute(AUTHENTICATION_RESULT);

		final JsonArray jsonUnspentOutputs = executeCommandAndGetJsonArray("listunspent", "0");

		final Set<String> addresses = new HashSet<>();
	    for (int i = 0; i < jsonUnspentOutputs.size(); i++)
	    {
	    	final JsonObject outp = jsonUnspentOutputs.get(i).asObject();
		   	final String address = outp.getString("address", "ERROR!");
	    	if (!addressDAO.isAddressFromUser(user, address).isEmpty()) {
			   	addresses.add(address);
	    	}
	    }

	    return addresses;
     }


	// ./zcash-cli listreceivedbyaddress 0 true
	public Set<String> getWalletAllPublicAddresses(final VaadinSession session)
		throws WalletCallException, IOException, InterruptedException
	{
		final User user = (User) session.getAttribute(AUTHENTICATION_RESULT);

		final JsonArray jsonReceivedOutputs = executeCommandAndGetJsonArray("listreceivedbyaddress", "0", "true");

		final Set<String> addresses = new HashSet<>();
		for (int i = 0; i < jsonReceivedOutputs.size(); i++)
		{
		   	final JsonObject outp = jsonReceivedOutputs.get(i).asObject();
		   	final String address = outp.getString("address", "ERROR!");
	    	if (!addressDAO.isAddressFromUser(user, address).isEmpty()) {
			   	addresses.add(address);
	    	}
		}

		return addresses;
    }

	
	public Map<String, String> getRawTransactionDetails(final String txID)
		throws WalletCallException, IOException, InterruptedException
	{
		final JsonObject jsonTransaction = this.executeCommandAndGetJsonObject(
			"gettransaction", wrapStringParameter(txID));

		final Map<String, String> map = new HashMap<>();

		for (final String name : jsonTransaction.names())
		{
			this.decomposeJSONValue(name, jsonTransaction.get(name), map);
		}
				
		return map;
	}
	
    public String getMemoField(final String acc, final String txID)
		throws WalletCallException, IOException, InterruptedException
	{
		final JsonArray jsonTransactions = this.executeCommandAndGetJsonArray(
			"z_listreceivedbyaddress", wrapStringParameter(acc));
			
        for (int i = 0; i < jsonTransactions.size(); i++)
        {
            if (jsonTransactions.get(i).asObject().getString("txid",  "ERROR!").equals(txID))
            {
            	if (jsonTransactions.get(i).asObject().get("memo") == null)
            	{
            		return null;
            	}
            	
                final String memoHex = jsonTransactions.get(i).asObject().getString("memo", "ERROR!");
                final String decodedMemo = Util.decodeHexMemo(memoHex);
                
                // Return only if not null - sometimes multiple incoming transactions have the same ID
                // if we have loopback send etc.
                if (decodedMemo != null)
                {
                	return decodedMemo;
                }
            }
        }

        return null;
	}
	
	
	public String getRawTransaction(final String txID)
		throws WalletCallException, IOException, InterruptedException
	{
		final JsonObject jsonTransaction = this.executeCommandAndGetJsonObject(
			"gettransaction", wrapStringParameter(txID));

		return jsonTransaction.toString(WriterConfig.PRETTY_PRINT);
	}


	// return UNIX time as tring
	public String getWalletTransactionTime(final String txID)
		throws WalletCallException, IOException, InterruptedException
	{
		final JsonObject jsonTransaction = this.executeCommandAndGetJsonObject(
			"gettransaction", wrapStringParameter(txID));

		return String.valueOf(jsonTransaction.getLong("time", -1));
	}
	
	
	public String getWalletTransactionConfirmations(final String txID)
		throws WalletCallException, IOException, InterruptedException
	{
		final JsonObject jsonTransaction = this.executeCommandAndGetJsonObject(
			"gettransaction", wrapStringParameter(txID));

		return jsonTransaction.get("confirmations").toString();
	}
	

	// Returns confirmed balance only!
	public BigDecimal getBalanceForAddress(final String address)
		throws WalletCallException, IOException, InterruptedException
	{
	    final JsonValue response = this.executeCommandAndGetJsonValue("z_getbalance", wrapStringParameter(address));

		return BigDecimal.valueOf(response.asDouble());
	}


	public BigDecimal getUnconfirmedBalanceForAddress(final String address)
		throws WalletCallException, IOException, InterruptedException
	{
	    final JsonValue response = this.executeCommandAndGetJsonValue("z_getbalance", wrapStringParameter(address), "0");

		return BigDecimal.valueOf(response.asDouble());
	}


	public String createNewAddress(final boolean isZAddress, final VaadinSession session)
			throws WalletCallException, IOException, InterruptedException
	{
		return createNewAddress(isZAddress, (User) session.getAttribute(AUTHENTICATION_RESULT));
	}
	

	public String createNewAddress(final boolean isZAddress, final User user)
		throws WalletCallException, IOException, InterruptedException
	{
	    final String newAddress = this.executeCommandAndGetSingleStringResponse((isZAddress ? "z_" : "") + "getnewaddress").trim();
	    
	    final Address addressDB = new Address ();
	    addressDB.setUser1(user);
	    addressDB.setAddress(newAddress);
	    addressDAO.save(addressDB);
	    addressDAO.commit();

		return newAddress;
	}


	// Returns OPID
	public synchronized String sendCash(final String from, final String to, final Double amount, final String memo, final Double transactionFee)
		throws WalletCallException, IOException, InterruptedException
	{
		final StringBuilder hexMemo = new StringBuilder();
		for (final byte c : memo.getBytes("UTF-8"))
		{
			String hexChar = Integer.toHexString(c);
			if (hexChar.length() < 2)
			{
				hexChar = "0" + hexChar;
			}
			hexMemo.append(hexChar);
		}

		final JsonObject toArgument = new JsonObject();
		toArgument.set("address", to);
		if (hexMemo.length() >= 2)
		{
			toArgument.set("memo", hexMemo.toString());
		}

		// The JSON Builder has a problem with double values that have no fractional part
		// it serializes them as integers that ZCash does not accept. So we do a replacement
		// TODO: find a better/cleaner way to format the amount
		toArgument.set("amount", "\uFFFF\uFFFF\uFFFF\uFFFF\uFFFF");

		final JsonArray toMany = new JsonArray();
		toMany.add(toArgument);
		
		final String amountPattern = "\"amount\":\"\uFFFF\uFFFF\uFFFF\uFFFF\uFFFF\"";
		// Make sure our replacement hack never leads to a mess up
		final String toManyBeforeReplace = toMany.toString();
		final int firstIndex = toManyBeforeReplace.indexOf(amountPattern);
		final int lastIndex = toManyBeforeReplace.lastIndexOf(amountPattern);
		if ((firstIndex == -1) || (firstIndex != lastIndex))
		{
			throw new WalletCallException("Error in forming z_sendmany command: " + toManyBeforeReplace);
		}

		final DecimalFormatSymbols decSymbols = new DecimalFormatSymbols(Locale.ROOT);

	    // This replacement is a hack to make sure the JSON object amount has double format 0.00 etc.
	    // TODO: find a better way to format the amount
		final String toManyArrayStr =	toMany.toString().replace(
		    amountPattern,
			"\"amount\":" + new DecimalFormat("########0.00######", decSymbols).format(amount));
		
		String transactionFeeAsString;
		// Properly format teh transaction fee as a number
		if ((transactionFee == null) || transactionFee <= 0)
		{
			transactionFeeAsString = "0.0001"; // Default value
		} else
		{
			transactionFeeAsString = new DecimalFormat(
				"########0.00######", decSymbols).format(transactionFee);
		}
		final String[] sendCashParameters = new String[]
	    {
		    zcashcli.getCanonicalPath(), "z_sendmany", wrapStringParameter(from),
		    wrapStringParameter(toManyArrayStr),
		    // Default min confirmations for the input transactions is 1
		    "1",
		    // transaction fee
		    transactionFeeAsString
		};
		
		// Safeguard to make sure the monetary amount does not differ after formatting
		final BigDecimal bdAmout = new BigDecimal(amount);
		final JsonArray toManyVerificationArr = Json.parse(toManyArrayStr).asArray();
		final BigDecimal bdFinalAmount =
			new BigDecimal(toManyVerificationArr.get(0).asObject().getDouble("amount", -1));
		final BigDecimal difference = bdAmout.subtract(bdFinalAmount).abs();
		if (difference.compareTo(new BigDecimal("0.000000015")) >= 0)
		{
			throw new WalletCallException("Error in forming z_sendmany command: Amount differs after formatting: " +
		                                  amount + " | " + toManyArrayStr);
		}

		log.info("The following send command will be issued: " +
                sendCashParameters[0] + " " + sendCashParameters[1] + " " +
                sendCashParameters[2] + " " + sendCashParameters[3] + " " +
                sendCashParameters[4] + " " + sendCashParameters[5] + ".");
		
		// Create caller to send cash
	    final CommandExecutor caller = new CommandExecutor(sendCashParameters);
	    final String strResponse = caller.execute();

		if (strResponse.trim().toLowerCase(Locale.ROOT).startsWith("error:") ||
			strResponse.trim().toLowerCase(Locale.ROOT).startsWith("error code:"))
		{
		  	throw new WalletCallException("Error response from wallet: " + strResponse);
		}

		log.info("Sending cash with the following command: " +
                sendCashParameters[0] + " " + sendCashParameters[1] + " " +
                sendCashParameters[2] + " " + sendCashParameters[3] + " " +
                sendCashParameters[4] + " " + sendCashParameters[5] + "." +
                " Got result: [" + strResponse + "]");

		return strResponse.trim();
	}
	
	
	public void findNewAddressAfterSendCash (final VaadinSession session, final String sourceAddress, final Double startBalance, final Double sendedAmount, final Double fee) throws WalletCallException, IOException, InterruptedException {
		final Double endBalance = getBalanceForAddress(sourceAddress).doubleValue();
		final Double needBalance = startBalance - sendedAmount - fee;
		if (endBalance < needBalance) {
			final List<Address> addresses = addressDAO.findOrphanedAddresses();
			for (final Address address : addresses) {
				if (needBalance == getBalanceForAddress(address.getAddress()).doubleValue()) {
					address.setUser1((User) session.getAttribute(AUTHENTICATION_RESULT));
					addressDAO.save(address);
					addressDAO.commit();
					return;
				}
			}
		}
	}
	
	
	// Returns OPID
	public synchronized String sendMessage(final String from, final String to, final double amount, final double fee, final String memo)
		throws WalletCallException, IOException, InterruptedException
	{
		final String hexMemo = Util.encodeHexString(memo);
		final JsonObject toArgument = new JsonObject();
		toArgument.set("address", to);
		if (hexMemo.length() >= 2)
		{
			toArgument.set("memo", hexMemo.toString());
		}
		
		final DecimalFormatSymbols decSymbols = new DecimalFormatSymbols(Locale.ROOT);

		// TODO: The JSON Builder has a problem with double values that have no fractional part
		// it serializes them as integers that ZCash does not accept. This will work with the
		// fractional amounts always used for messaging
		toArgument.set("amount", new DecimalFormat("########0.00######", decSymbols).format(amount));

		final JsonArray toMany = new JsonArray();
		toMany.add(toArgument);
		
		final String toManyArrayStr =	toMany.toString();
		final String[] sendCashParameters = new String[]
	    {
		    zcashcli.getCanonicalPath(), "z_sendmany", wrapStringParameter(from),
		    wrapStringParameter(toManyArrayStr),
		    // Default min confirmations for the input transactions is 1
		    "1",
		    // transaction fee
		    new DecimalFormat("########0.00######", decSymbols).format(fee)
		};
				
		// Create caller to send cash
	    final CommandExecutor caller = new CommandExecutor(sendCashParameters);
	    final String strResponse = caller.execute();

		if (strResponse.trim().toLowerCase(Locale.ROOT).startsWith("error:") ||
			strResponse.trim().toLowerCase(Locale.ROOT).startsWith("error code:"))
		{
		  	throw new WalletCallException("Error response from wallet: " + strResponse);
		}

		log.info("Sending cash message with the following command: " +
                sendCashParameters[0] + " " + sendCashParameters[1] + " " +
                sendCashParameters[2] + " " + sendCashParameters[3] + " " +
                sendCashParameters[4] + " " + sendCashParameters[5] + "." +
                " Got result: [" + strResponse + "]");

		return strResponse.trim();
	}


	// Returns the message signature
	public synchronized String signMessage(final String address, final String message)
		throws WalletCallException, IOException, InterruptedException
	{
	    final String response = this.executeCommandAndGetSingleStringResponse(
	    	"signmessage", wrapStringParameter(address), wrapStringParameter(message));

		return response.trim();
	}
	
	
	// Verifies a message - true if OK
	public synchronized boolean verifyMessage(final String address, final String signature, final String message)
		throws WalletCallException, IOException, InterruptedException
	{
	    final String response = this.executeCommandAndGetSingleStringResponse(
	    	"verifymessage",
	    	wrapStringParameter(address),
	    	wrapStringParameter(signature),
	    	wrapStringParameter(message));

		return response.trim().equalsIgnoreCase("true");
	}


	public synchronized boolean isSendingOperationComplete(final String opID)
	    throws WalletCallException, IOException, InterruptedException
	{
		final JsonArray response = this.executeCommandAndGetJsonArray(
			"z_getoperationstatus", wrapStringParameter("[\"" + opID + "\"]"));
		final JsonObject jsonStatus = response.get(0).asObject();

		final String status = jsonStatus.getString("status", "ERROR");

		log.info("Operation " + opID + " status is " + response + ".");

		if (status.equalsIgnoreCase("success") ||
			status.equalsIgnoreCase("error") ||
			status.equalsIgnoreCase("failed"))
		{
			return true;
		} else if (status.equalsIgnoreCase("executing") || status.equalsIgnoreCase("queued"))
		{
			return false;
		} else
		{
			throw new WalletCallException("Unexpected status response from wallet: " + response.toString());
		}
	}


	public synchronized boolean isCompletedOperationSuccessful(final String opID)
	    throws WalletCallException, IOException, InterruptedException
	{
		final JsonArray response = this.executeCommandAndGetJsonArray(
			"z_getoperationstatus", wrapStringParameter("[\"" + opID + "\"]"));
		final JsonObject jsonStatus = response.get(0).asObject();

		final String status = jsonStatus.getString("status", "ERROR");

		log.info("Operation " + opID + " status is " + response + ".");

		if (status.equalsIgnoreCase("success"))
		{
			return true;
		} else if (status.equalsIgnoreCase("error") || status.equalsIgnoreCase("failed"))
		{
			return false;
		} else
		{
			throw new WalletCallException("Unexpected final operation status response from wallet: " + response.toString());
		}
	}


	// May only be called for already failed operations
	public synchronized String getOperationFinalErrorMessage(final String opID)
	    throws WalletCallException, IOException, InterruptedException
	{
		final JsonArray response = this.executeCommandAndGetJsonArray(
			"z_getoperationstatus", wrapStringParameter("[\"" + opID + "\"]"));
		final JsonObject jsonStatus = response.get(0).asObject();

		final JsonObject jsonError = jsonStatus.get("error").asObject();
		return jsonError.getString("message", "ERROR!");
	}


	public synchronized NetworkAndBlockchainInfo getNetworkAndBlockchainInfo()
		throws WalletCallException, IOException, InterruptedException
	{
		final NetworkAndBlockchainInfo info = new NetworkAndBlockchainInfo();

		final String strNumCons = this.executeCommandAndGetSingleStringResponse("getconnectioncount");
		log.info("getconnectioncount: " + strNumCons); //TODO LS for Linux-DEBUG
		info.numConnections = Integer.valueOf(strNumCons.trim());

		final String strBlockCount = this.executeCommandAndGetSingleStringResponse("getblockcount");
		final String lastBlockHash = this.executeCommandAndGetSingleStringResponse("getblockhash", strBlockCount.trim());
		final JsonObject lastBlock = this.executeCommandAndGetJsonObject("getblock", wrapStringParameter(lastBlockHash.trim()));
		info.lastBlockDate = new Date(Long.valueOf(lastBlock.getLong("time", -1) * 1000L));

		return info;
	}


	public synchronized void lockWallet()
		throws WalletCallException, IOException, InterruptedException
	{
		final String response = this.executeCommandAndGetSingleStringResponse("walletlock");

		// Response is expected to be empty
		if (response.trim().length() > 0)
		{
			throw new WalletCallException("Unexpected response from wallet: " + response);
		}
	}


	// Unlocks the wallet for 5 minutes - meant to be followed shortly by lock!
	// TODO: tests with a password containing spaces
	public synchronized void unlockWallet(final String password)
		throws WalletCallException, IOException, InterruptedException
	{
		final String response = this.executeCommandAndGetSingleStringResponse(
			"walletpassphrase", wrapStringParameter(password), "300");

		// Response is expected to be empty
		if (response.trim().length() > 0)
		{
			throw new WalletCallException("Unexpected response from wallet: " + response);
		}
	}


    // Wallet locks check - an unencrypted wallet will give an error
	// zcash-cli walletlock
	// error: {"code":-15,"message":"Error: running with an unencrypted wallet, but walletlock was called."}
	public synchronized boolean isWalletEncrypted()
   		throws WalletCallException, IOException, InterruptedException
    {
		final String[] params = new String[] { zcashcli.getCanonicalPath(), "walletlock" };
		final CommandExecutor caller = new CommandExecutor(params);
    	final String strResult = caller.execute();

    	 if (strResult.trim().length() <= 0)
    	 {
    		 // If it could be locked with no result - obviously encrypted
    		 return true;
    	 } else if (strResult.trim().toLowerCase(Locale.ROOT).startsWith("error:"))
    	 {
    		 // Expecting an error of an unencrypted wallet
    		 final String jsonPart = strResult.substring(strResult.indexOf("{"));
   			 JsonValue response = null;
   			 try
   			 {
   			   	response = Json.parse(jsonPart);
   		 	 } catch (final ParseException pe)
   			 {
   			   	 throw new WalletCallException(jsonPart + "\n" + pe.getMessage() + "\n", pe);
   			 }

   			 final JsonObject respObject = response.asObject();
   			 if ((respObject.getDouble("code", -1) == -15) &&
   				 (respObject.getString("message", "ERR").indexOf("unencrypted wallet") != -1))
   			 {
   				 // Obviously unencrupted
   				 return false;
   			 } else
   			 {
   	    		 throw new WalletCallException("Unexpected response from wallet: " + strResult);
   			 }
    	 } else if (strResult.trim().toLowerCase(Locale.ROOT).startsWith("error code:"))
    	 {
   			 final JsonObject respObject = Util.getJsonErrorMessage(strResult);
   			 if ((respObject.getDouble("code", -1) == -15) &&
   				 (respObject.getString("message", "ERR").indexOf("unencrypted wallet") != -1))
   			 {
   				 // Obviously unencrupted
   				 return false;
   			 } else
   			 {
   	    		 throw new WalletCallException("Unexpected response from wallet: " + strResult);
   			 }
    	 } else
    	 {
    		 throw new WalletCallException("Unexpected response from wallet: " + strResult);
    	 }
    }


	/**
	 * Encrypts the wallet. Typical success/error use cases are:
	 *
	 * ./zcash-cli encryptwallet "1234"
	 * wallet encrypted; Bitcoin server stopping, restart to run with encrypted wallet.
	 * The keypool has been flushed, you need to make a new backup.
	 *
	 * ./zcash-cli encryptwallet "1234"
	 * error: {"code":-15,"message":"Error: running with an encrypted wallet, but encryptwallet was called."}
	 *
	 * @param password
	 */
	public synchronized void encryptWallet(final String password)
		throws WalletCallException, IOException, InterruptedException
	{
		final String response = this.executeCommandAndGetSingleStringResponse(
			"encryptwallet", wrapStringParameter(password));
		log.info("Result of wallet encryption is: \n" + response);
		// If no exception - obviously successful
	}
	
	public String getTPrivateKey(final String address)
		throws WalletCallException, IOException, InterruptedException
	{
		final String response = this.executeCommandAndGetSingleStringResponse(
			"dumpprivkey", wrapStringParameter(address));
		
		return response.trim();
	}
	
	
	public String getZPrivateKey(final String address)
	    throws WalletCallException, IOException, InterruptedException
	{
		final String response = this.executeCommandAndGetSingleStringResponse(
			"z_exportkey", wrapStringParameter(address));
		
		return response.trim();
	}
	
	
	// Imports a private key - tries both possibilities T/Z
	public synchronized void importPrivateKey(final String key)
		throws WalletCallException, IOException, InterruptedException
	{
		// First try a Z key
		final String[] params = new String[]
		{
			zcashcli.getCanonicalPath(),
			"-rpcclienttimeout=5000",
			"z_importkey",
			wrapStringParameter(key)
		};
		final CommandExecutor caller = new CommandExecutor(params);
    	String strResult = caller.execute();
		
		if ((strResult == null) || (strResult.trim().length() <= 0))
		{
			return;
		}
		
		// Obviously we have an error trying to import a Z key
		if (strResult.trim().toLowerCase(Locale.ROOT).startsWith("error:") &&
			(strResult.indexOf("{") != -1))
		{
   		 	 // Expecting an error of a T address key
   		 	 final String jsonPart = strResult.substring(strResult.indexOf("{"));
  		     JsonValue response = null;
  			 try
  			 {
  			   	response = Json.parse(jsonPart);
  		 	 } catch (final ParseException pe)
  			 {
  			   	 throw new WalletCallException(jsonPart + "\n" + pe.getMessage() + "\n", pe);
  			 }

  			 final JsonObject respObject = response.asObject();
  			 if ((respObject.getDouble("code", +123) == -1) &&
  				 (respObject.getString("message", "ERR").indexOf("wrong network type") != -1))
  			 {
  				 // Obviously T address - do nothing here
  			 } else
  			 {
  	    		 throw new WalletCallException("Unexpected response from wallet: " + strResult);
  			 }
		} else if (strResult.trim().toLowerCase(Locale.ROOT).startsWith("error code:"))
		{
 			 final JsonObject respObject = Util.getJsonErrorMessage(strResult);
 			 if ((respObject.getDouble("code", +123) == -1) &&
 				 (respObject.getString("message", "ERR").indexOf("wrong network type") != -1))
 			 {
 				 // Obviously T address - do nothing here
 			 } else
 			 {
 	    		 throw new WalletCallException("Unexpected response from wallet: " + strResult);
 			 }
		} else
		{
			throw new WalletCallException("Unexpected response from wallet: " + strResult);
		}
		
		// Second try a T key
		strResult = this.executeCommandAndGetSingleStringResponse("importprivkey", wrapStringParameter(key));
		
		if ((strResult == null) || (strResult.trim().length() <= 0))
		{
			return;
		}
		
		// Obviously an error
		throw new WalletCallException("Unexpected response from wallet: " + strResult);
	}
	

	private JsonObject executeCommandAndGetJsonObject(final String command1, final String command2)
		throws WalletCallException, IOException, InterruptedException
	{
		final JsonValue response = this.executeCommandAndGetJsonValue(command1, command2);

		if (response.isObject())
		{
			return response.asObject();
		} else
		{
			throw new WalletCallException("Unexpected non-object response from wallet: " + response.toString());
		}

	}


	private JsonArray executeCommandAndGetJsonArray(final String command1, final String command2)
		throws WalletCallException, IOException, InterruptedException
	{
		return this.executeCommandAndGetJsonArray(command1, command2, null);
	}


	private JsonArray executeCommandAndGetJsonArray(final String command1, final String command2, final String command3)
		throws WalletCallException, IOException, InterruptedException
	{
		final JsonValue response = this.executeCommandAndGetJsonValue(command1, command2, command3);

		if (response.isArray())
		{
			return response.asArray();
		} else
		{
			throw new WalletCallException("Unexpected non-array response from wallet: " + response.toString());
		}
	}


	private JsonValue executeCommandAndGetJsonValue(final String command1, final String command2)
			throws WalletCallException, IOException, InterruptedException
	{
		return this.executeCommandAndGetJsonValue(command1, command2, null);
	}


	private JsonValue executeCommandAndGetJsonValue(final String command1, final String command2, final String command3)
		throws WalletCallException, IOException, InterruptedException
	{
		final String strResponse = this.executeCommandAndGetSingleStringResponse(command1, command2, command3);

		JsonValue response = null;
		try
		{
		  	response = Json.parse(strResponse);
		} catch (final ParseException pe)
		{
		  	throw new WalletCallException(strResponse + "\n" + pe.getMessage() + "\n", pe);
		}

		return response;
	}


	private String executeCommandAndGetSingleStringResponse(final String command1)
		throws WalletCallException, IOException, InterruptedException
	{
		return this.executeCommandAndGetSingleStringResponse(command1, null);
	}


	private String executeCommandAndGetSingleStringResponse(final String command1, final String command2)
		throws WalletCallException, IOException, InterruptedException
	{
		return this.executeCommandAndGetSingleStringResponse(command1, command2, null);
	}
	
	
	private String executeCommandAndGetSingleStringResponse(final String command1, final String command2, final String command3)
		throws WalletCallException, IOException, InterruptedException
	{
		return executeCommandAndGetSingleStringResponse(command1, command2, command3, null);
	}


	private synchronized String executeCommandAndGetSingleStringResponse(
			                        final String command1, final String command2, final String command3, final String command4)
		throws WalletCallException, IOException, InterruptedException
	{
		String[] params;
		if (command4 != null)
		{
			params = new String[] { zcashcli.getCanonicalPath(), command1, command2, command3, command4 };
		} else if (command3 != null)
		{
			params = new String[] { zcashcli.getCanonicalPath(), command1, command2, command3 };
		} else if (command2 != null)
		{
			params = new String[] { zcashcli.getCanonicalPath(), command1, command2 };
		} else
		{
			params = new String[] { zcashcli.getCanonicalPath(), command1 };
		}

	    final CommandExecutor caller = new CommandExecutor(params);
	    
//	    log.info(command1 + "#" + command2 + "#" + command3 + "#" + command4);

		final String strResponse = caller.execute();
		if (strResponse.trim().toLowerCase(Locale.ROOT).startsWith("error:")       ||
			strResponse.trim().toLowerCase(Locale.ROOT).startsWith("error code:"))
		{
		  	throw new WalletCallException("Error response from wallet: " + strResponse);
		}

		return strResponse;
	}
	
	
	// Used to wrap string parameters on the command line - not doing so causes problems on Windows.
	private String wrapStringParameter(String param)
	{
		final OS_TYPE os = OSUtil.getOSType();
		
		// Fix is made for Windows only
		if (os == OS_TYPE.WINDOWS)
		{
			param = "\"" + param.replace("\"", "\\\"") + "\"";
		}
		
		return param;
	}
	
	
	private void decomposeJSONValue(final String name, final JsonValue val, final Map<String, String> map)
	{
		if (val.isObject())
		{
			final JsonObject obj = val.asObject();
			for (final String memberName : obj.names())
			{
				this.decomposeJSONValue(name + "." + memberName, obj.get(memberName), map);
			}
		} else if (val.isArray())
		{
			final JsonArray arr = val.asArray();
			for (int i = 0; i < arr.size(); i++)
			{
				this.decomposeJSONValue(name + "[" + i + "]", arr.get(i), map);
			}
		} else
		{
			map.put(name, val.toString());
		}
	}
}
