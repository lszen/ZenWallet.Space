
package net.ddns.lsmobile.zencashvaadinwalletui4cpp.ui.desktop;

import java.io.IOException;
import java.text.ParseException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import com.vaadin.data.sort.Sort;
import com.vaadin.navigator.ViewChangeListener;
import com.vaadin.server.ClientConnector;
import com.vaadin.server.VaadinSession;
import com.vaadin.shared.data.sort.SortDirection;
import com.vaadin.shared.ui.MarginInfo;
import com.vaadin.shared.ui.label.ContentMode;
import com.vaadin.ui.Alignment;
import com.vaadin.ui.Button;
import com.vaadin.ui.CustomComponent;
import com.vaadin.ui.Grid;
import com.vaadin.ui.MenuBar;
import com.vaadin.ui.Notification;
import com.vaadin.ui.Notification.Type;
import com.vaadin.ui.UI;
import com.vaadin.ui.renderers.HtmlRenderer;
import com.xdev.communication.RunnableAccessWrapper;
import com.xdev.security.authentication.ui.Authentication;
import com.xdev.ui.XdevButton;
import com.xdev.ui.XdevGridLayout;
import com.xdev.ui.XdevLabel;
import com.xdev.ui.XdevMenuBar;
import com.xdev.ui.XdevMenuBar.XdevMenuItem;
import com.xdev.ui.XdevPanel;
import com.xdev.ui.XdevProgressBar;
import com.xdev.ui.XdevTabSheet;
import com.xdev.ui.XdevTextField;
import com.xdev.ui.XdevView;
import com.xdev.ui.entitycomponent.combobox.XdevComboBox;
import com.xdev.util.ConverterBuilder;

import net.ddns.lsmobile.zencashvaadinwalletui4cpp.business.DataGatheringThread;
import net.ddns.lsmobile.zencashvaadinwalletui4cpp.business.IWallet;
import net.ddns.lsmobile.zencashvaadinwalletui4cpp.business.OSUtil;
import net.ddns.lsmobile.zencashvaadinwalletui4cpp.business.OSUtil.OS_TYPE;
import net.ddns.lsmobile.zencashvaadinwalletui4cpp.business.Transaction;
import net.ddns.lsmobile.zencashvaadinwalletui4cpp.business.Util;
import net.ddns.lsmobile.zencashvaadinwalletui4cpp.business.ZCashClientCaller.WalletBalance;
import net.ddns.lsmobile.zencashvaadinwalletui4cpp.business.ZCashClientCaller.WalletCallException;
import net.ddns.lsmobile.zencashvaadinwalletui4cpp.business.ZenNode;
import net.ddns.lsmobile.zencashvaadinwalletui4cpp.dal.AddressDAO;
import net.ddns.lsmobile.zencashvaadinwalletui4cpp.ui.Servlet;

public class MainView extends XdevView implements IWallet {
	
	protected VaadinSession session;
	
	protected ZenNode zenNode;
	
	private final Boolean walletIsEncrypted   = null;

	private final String OSInfo              = null;

	private DataGatheringThread<Set<Transaction>> transactionGatheringThread = null;
	
	public MainView() {
		super();
		this.initUI();
	}
	
	@Override
	public void enter(final ViewChangeListener.ViewChangeEvent event) {
		super.enter(event);
	
		this.session = getSession();
		this.session.setAttribute(AddressDAO.class, new AddressDAO ());
		this.zenNode = ((Servlet) Servlet.getCurrent()).zenNode;
		defaultNumberFormat.setMaximumFractionDigits(MAXIMUM_FRACTION_DIGITS);
		usNumberFormat.setMaximumFractionDigits(MAXIMUM_FRACTION_DIGITS);

		try {

//			final WalletBalance balance = this.zenNode.clientCaller.getWalletInfo();
//			if (MainView.this.walletIsEncrypted == null)
//			{
//				MainView.this.walletIsEncrypted = MainView.this.zenNode.clientCaller.isWalletEncrypted();
//			}
//			updateWalletStatusLabel(balance);
			
			// Thread and timer to update the transactions table
			this.transactionGatheringThread = new DataGatheringThread<>(
				new DataGatheringThread.DataGatherer<Set<Transaction>>()
				{
					@Override
					public Set<Transaction> gatherData()
						throws Exception
					{
						final UI ui = getUI();
						if (ui != null) {
							final long start = System.currentTimeMillis();
							final Set<Transaction> lastData = MainView.this.transactionGatheringThread.getLastData();
							final Set<Transaction> data = MainView.this.zenNode.clientCaller.getTransactionsDataFromWallet(MainView.this.session);
							final long end = System.currentTimeMillis();
							log.info("Gathering of dashboard wallet transactions table data done in " + (end - start) + "ms." );

							if (lastData == null || data.size() > lastData.size()) {
								ui.access(()->{
									try {
										updateWalletStatusLabel(MainView.this.zenNode.clientCaller.getWalletInfo(MainView.this.session));
										updateWalletTransactionsTable(data);
										updateWalletAddressPositiveBalanceComboBox(MainView.this.zenNode.clientCaller.getAddressPositiveBalanceDataFromWallet(MainView.this.session));
										ui.push();
									} catch (final Exception e) {
										log.error(e, e);
									}
								});
							}
							
							return data;
						}
						return null;
					}
				},
				20000, true);
			threads.add(this.transactionGatheringThread);

						
			//AddressesPanel
			this.lastInteractiveRefresh = System.currentTimeMillis();
			
			//this.lastAddressBalanceData = getAddressBalanceDataFromWallet();
			
			updateWalletAddressBalanceTableInteractive();
						
			//SendCashPanel
						
			//TODO LS
			this.textFieldTransactionFee.setValue(defaultNumberFormat.format(Double.valueOf(0.0001)));
//
//						// Add a popup menu to the destination address field - for convenience
//						final JMenuItem paste = new JMenuItem("Paste address");
//						final JPopupMenu popupMenu = new JPopupMenu();
//				        popupMenu.add(paste);
//				        paste.addActionListener(new ActionListener()
//				        {
//							@Override
//							public void actionPerformed(final ActionEvent e)
//							{
//								try
//								{
//									final String address = (String)Toolkit.getDefaultToolkit().getSystemClipboard().
//											         getData(DataFlavor.stringFlavor);
//									if ((address != null) && (address.trim().length() > 0))
//									{
//										SendCashPanel.this.destinationAddressField.setText(address);
//									}
//								} catch (final Exception ex)
//								{
//									log.error("Unexpected error", ex);
//									// TODO: clipboard exception handling - do it better
//									// java.awt.datatransfer.UnsupportedFlavorException: Unicode String
//									//SendCashPanel.this.errorReporter.reportError(ex);
//								}
//							}
//						});
//
//				        this.destinationAddressField.addMouseListener(new MouseAdapter()
//				        {
//				        	@Override
//							public void mousePressed(final MouseEvent e)
//				        	{
//				                if ((!e.isConsumed()) && e.isPopupTrigger())
//				                {
//				                    popupMenu.show(e.getComponent(), e.getPoint().x, e.getPoint().y);
//				                    e.consume();
//				                };
//				        	}
//
//				            @Override
//							public void mouseReleased(final MouseEvent e)
//				            {
//				            	if ((!e.isConsumed()) && e.isPopupTrigger())
//				            	{
//				            		mousePressed(e);
//				            	}
//				            }
//				        });



		} catch (final Exception e) {
			log.error(e, e);
		}
	}

	
	private void updateWalletStatusLabel(final WalletBalance balance)
			throws WalletCallException, IOException, InterruptedException
		{
			this.labelTransparentBalance.setValue(defaultNumberFormat.format(balance.transparentBalance) + " ZEN");
			this.labelPrivateBalance.setValue(defaultNumberFormat.format(balance.privateBalance) + " ZEN");
			this.labelTotalBalance.setValue(defaultNumberFormat.format(balance.totalBalance) + " ZEN");
			
			/* TODO LS
			
			final WalletBalance balance = this.walletBalanceGatheringThread.getLastData();
			
			// It is possible there has been no gathering initially
			if (balance == null)
			{
				return;
			}
			
			// Format double numbers - else sometimes we get exponential notation 1E-4 ZEN
			final DecimalFormat df = new DecimalFormat("########0.00######");
			
			final String transparentBalance = df.format(balance.transparentBalance);
			final String privateBalance = df.format(balance.privateBalance);
			final String totalBalance = df.format(balance.totalBalance);
			
			final String transparentUCBalance = df.format(balance.transparentUnconfirmedBalance);
			final String privateUCBalance = df.format(balance.privateUnconfirmedBalance);
			final String totalUCBalance = df.format(balance.totalUnconfirmedBalance);

			final String color1 = transparentBalance.equals(transparentUCBalance) ? "" : "color:#cc3300;";
			final String color2 = privateBalance.equals(privateUCBalance)         ? "" : "color:#cc3300;";
			final String color3 = totalBalance.equals(totalUCBalance)             ? "" : "color:#cc3300;";
			
			final String text =
				"<html>" +
			    "<span style=\"font-family:monospace;font-size:1em;" + color1 + "\">Transparent balance: <span style=\"font-size:1.1em;\">" +
					transparentUCBalance + " ZEN </span></span><br/> " +
				"<span style=\"font-family:monospace;font-size:1em;" + color2 + "\">Private (Z) balance: <span style=\"font-weight:bold;font-size:1.1em;\">" +
			    	privateUCBalance + " ZEN </span></span><br/> " +
				"<span style=\"font-family:monospace;;font-size:1em;" + color3 + "\">Total (Z+T) balance: <span style=\"font-weight:bold;font-size:1.35em;\">" +
			    	totalUCBalance + " ZEN </span></span>" +
				"<br/>  </html>";
			
			this.labelTotalBalance.setValue(text);
			
			String toolTip = null;
			if ((!transparentBalance.equals(transparentUCBalance)) ||
			    (!privateBalance.equals(privateUCBalance))         ||
			    (!totalBalance.equals(totalUCBalance)))
			{
				toolTip = "<html>" +
						  "Unconfirmed (unspendable) balance is being shown due to an<br/>" +
			              "ongoing transaction! Actual confirmed (spendable) balance is:<br/>" +
			              "<span style=\"font-size:5px\"><br/></span>" +
						  "Transparent: " + transparentBalance + " ZEN<br/>" +
			              "Private ( Z ): <span style=\"font-weight:bold\">" + privateBalance + " ZEN</span><br/>" +
						  "Total ( Z+T ): <span style=\"font-weight:bold\">" + totalBalance + " ZEN</span>" +
						  "</html>";
			}
			
			this.labelTotalBalance.setDescription(toolTip);
			*/
		}
	
	private void updateWalletTransactionsTable(final Set<Transaction> newTransactionsData) throws WalletCallException, IOException, InterruptedException {
		log.info("Updating table of transactions...");
		// TODO LS this.transactionGatheringThread.getLastData();

		// May be null - not even gathered once
		if (newTransactionsData == null) {
			return;
		}

		final Grid gridTransactions = new Grid("Transactions:");
		// final HeaderRow headerWallets =
		// gridTransactions.prependHeaderRow();

		// Formats
		// final DecimalFormat formatUsd = new
		// DecimalFormat(UsdToHtmlConverter.FORMAT_USD);

		// Columns
		gridTransactions.addColumn(Transaction.COLUMN_TYPE, String.class)
				.setRenderer(new HtmlRenderer())/* .setHeaderCaption("") */;
		gridTransactions.addColumn(Transaction.COLUMN_DIRECTION, String.class).setRenderer(new HtmlRenderer());
		gridTransactions.addColumn(Transaction.COLUMN_CONFIRMED, String.class).setRenderer(new HtmlRenderer());
		gridTransactions.addColumn(Transaction.COLUMN_AMOUNT, String.class/* Double.class */)
				.setRenderer(new HtmlRenderer()/* NumberRenderer(formatUsd), new UsdToHtmlConverter() */);
		gridTransactions.addColumn(Transaction.COLUMN_DATE,
				String.class/* Date.class */).setRenderer(new HtmlRenderer/* DateRenderer */());
		gridTransactions.addColumn(Transaction.COLUMN_DESTINATION_ADDRESS, String.class)
				.setRenderer(new HtmlRenderer()).setSortable(false);
		gridTransactions.addColumn(Transaction.COLUMN_DESTINATION_TRANSACTION, String.class)
				.setRenderer(new HtmlRenderer()).setSortable(false)/* .setWidth(0) */;

		// gridTransactions.setFrozenColumnCount(2);

		// Rows (Values)
		for (final Transaction transactionsRow : newTransactionsData) {
			gridTransactions.addRow(transactionsRow.getTypeAsString(), transactionsRow.getDirectionAsString(),
					transactionsRow.getIsConfirmedAsString(),
					defaultNumberFormat.format(transactionsRow.getAmount()), transactionsRow.getDateAsString(),
					transactionsRow.getDestinationAddressAsString(), transactionsRow.getTransactionAsString());
		}

		gridTransactions.sort(Sort.by(Transaction.COLUMN_DATE, SortDirection.DESCENDING));

		gridTransactions.setSizeFull();
		this.panelGridTransactions.setContent(gridTransactions);
	}
	
	
	//AddressesPanel
	
	protected Grid addressBalanceTable = null;
	
	String[][] lastAddressBalanceFullData = null;
	
	private final DataGatheringThread<String[][]> balanceGatheringThread = null;
	
	private long lastInteractiveRefresh;
	
	protected String selectedAddress = null;
	
//	// Null if not selected
//	public String getSelectedAddress()
//	{
//		String address = null;
//
//		final String[] selectedRow = (String[]) this.addressBalanceTable.get;
//
//		if (selectedRow != null)
//		{
//			address = selectedRow[2];
////			address = this.addressBalanceTable.getModel().getValueAt(selectedRow, 2).toString();
//		}
//
//		return address;
//	}

	
	private void createNewAddress(final boolean isZAddress)
	{
		try
		{
			/*TODO LS bEncryptedWallet
			// Check for encrypted wallet
			final boolean bEncryptedWallet = this.servlet.clientCaller.isWalletEncrypted();
			if (bEncryptedWallet && isZAddress)
			{
				final PasswordDialog pd = new PasswordDialog((JFrame)(this.getRootPane().getParent()));
				pd.setVisible(true);
				
				if (!pd.isOKPressed())
				{
					return;
				}
				
				this.servlet.clientCaller.unlockWallet(pd.getPassword());
			}
			*/
			final String address = this.zenNode.clientCaller.createNewAddress(isZAddress);
			
			/*TODO LS bEncryptedWallet
			// Lock the wallet again
			if (bEncryptedWallet && isZAddress)
			{
				this.servlet.clientCaller.lockWallet();
			}
			*/
			Notification.show("Address created", "A new " + (isZAddress ? "Z (Private)" : "T (Transparent)")
					+ " address has been created cuccessfully:\n" + address, Type.HUMANIZED_MESSAGE);

			this.updateWalletAddressBalanceTableInteractive();
		} catch (final Exception e)
		{
			log.error("Unexpected error: ", e);
		}
	}

	// Interactive and non-interactive are mutually exclusive
	private synchronized void updateWalletAddressBalanceTableInteractive()
		throws WalletCallException, IOException, InterruptedException
	{
		this.lastInteractiveRefresh = System.currentTimeMillis();
		
		final String[][] newAddressBalanceData = this.getAddressBalanceDataFromWallet();

		if (Util.arraysAreDifferent(this.lastAddressBalanceFullData, newAddressBalanceData))
		{
			log.info("Updating table of addresses/balances I...");
			this.panelGridOwnAddresses.setContent(this.createAddressBalanceTable(newAddressBalanceData));
			this.lastAddressBalanceFullData = newAddressBalanceData;

		}
	}
	

	// Interactive and non-interactive are mutually exclusive
	private synchronized void updateWalletAddressBalanceTableAutomated()
		throws WalletCallException, IOException, InterruptedException
	{
		// Make sure it is > 1 min since the last interactive refresh
		if ((System.currentTimeMillis() - this.lastInteractiveRefresh) < (60 * 1000))
		{
			return;
		}
		
		final String[][] newAddressBalanceData = this.balanceGatheringThread.getLastData();
		
		if ((newAddressBalanceData != null) &&
			Util.arraysAreDifferent(this.lastAddressBalanceFullData, newAddressBalanceData))
		{
			log.info("Updating table of addresses/balances A...");
			this.panelGridOwnAddresses.setContent(this.createAddressBalanceTable(newAddressBalanceData));
			this.lastAddressBalanceFullData = newAddressBalanceData;
		}
	}


	private Grid createAddressBalanceTable(final String rowData[][])
		throws WalletCallException, IOException, InterruptedException
	{
		this.addressBalanceTable = new Grid(/*"Addresses:"*/);
//		final HeaderRow headerWallets = gridTransactions.prependHeaderRow();

		// Formats
//		final DecimalFormat formatUsd = new DecimalFormat(UsdToHtmlConverter.FORMAT_USD);

		// Columns
		this.addressBalanceTable.addColumn(ADDRESSES_COLUMN_BALANCE, String.class/*Double.class*/).setRenderer(new HtmlRenderer()/*NumberRenderer(formatUsd), new UsdToHtmlConverter()*/)/*.setHeaderCaption("")*/;
		this.addressBalanceTable.addColumn(ADDRESSES_COLUMN_CONFIRMED, String.class).setRenderer(new HtmlRenderer());
		this.addressBalanceTable.addColumn(ADDRESSES_COLUMN_ADDRESS, String.class).setRenderer(new HtmlRenderer()).setSortable(false)/*.setWidth(0)*/;
		
//		gridTransactions.setFrozenColumnCount(2);

		// Rows (Values)
		for (final String[] row : rowData) {
			this.addressBalanceTable.addRow(row);
		}

//		gridBalance.sort(Sort.by(COLUMN_SECURITY_DEGREE, SortDirection.ASCENDING)
//		          .then(COLUMN_SUM, SortDirection.DESCENDING));

		this.addressBalanceTable.addSelectionListener((selectionEvent) -> {
			this.selectedAddress = (String) ((Grid)selectionEvent.getSource()).getContainerDataSource()
					.getItem(selectionEvent.getSelected().iterator().next()).getItemProperty(ADDRESSES_COLUMN_ADDRESS).getValue();
		});
		
		this.addressBalanceTable.setSizeFull();
		this.panelGridOwnAddresses.setContent(this.addressBalanceTable);

		
//		final String columnNames[] = { "Balance", "Confirmed?", "Address" };
//        final JTable table = new AddressTable(rowData, columnNames, this.clientCaller);
//        table.setAutoResizeMode(JTable.AUTO_RESIZE_SUBSEQUENT_COLUMNS);
//        table.getColumnModel().getColumn(0).setPreferredWidth(160);
//        table.getColumnModel().getColumn(1).setPreferredWidth(140);
//        table.getColumnModel().getColumn(2).setPreferredWidth(1000);

        return this.addressBalanceTable;
	}


	private String[][] getAddressBalanceDataFromWallet()
		throws WalletCallException, IOException, InterruptedException
	{
		// Z Addresses - they are OK
		final String[] zAddresses = this.zenNode.clientCaller.getWalletZAddresses(this.session);
		
		// T Addresses listed with the list received by addr comamnd
		final String[] tAddresses = this.zenNode.clientCaller.getWalletAllPublicAddresses(this.session);
		final Set<String> tStoredAddressSet = new HashSet<>();
		for (final String address : tAddresses)
		{
			tStoredAddressSet.add(address);
		}
		
		// T addresses with unspent outputs - just in case they are different
		final String[] tAddressesWithUnspentOuts = this.zenNode.clientCaller.getWalletPublicAddressesWithUnspentOutputs(this.session);
		final Set<String> tAddressSetWithUnspentOuts = new HashSet<>();
		for (final String address : tAddressesWithUnspentOuts)
		{
			tAddressSetWithUnspentOuts.add(address);
		}
		
		// Combine all known T addresses
		final Set<String> tAddressesCombined = new HashSet<>();
		tAddressesCombined.addAll(tStoredAddressSet);
		tAddressesCombined.addAll(tAddressSetWithUnspentOuts);
		
		final String[][] addressBalances = new String[zAddresses.length + tAddressesCombined.size()][];
		
		String confirmed    = "\u2690";
		String notConfirmed = "\u2691";
		
		// Windows does not support the flag symbol (Windows 7 by default)
		// TODO: isolate OS-specific symbol codes in a separate class
		final OS_TYPE os = OSUtil.getOSType();
		if (os == OS_TYPE.WINDOWS)
		{
			confirmed = " \u25B7";
			notConfirmed = " \u25B6";
		}
		
		int i = 0;

		for (final String address : tAddressesCombined)
		{
			final String confirmedBalance = this.zenNode.clientCaller.getBalanceForAddress(address);
			final String unconfirmedBalance = this.zenNode.clientCaller.getUnconfirmedBalanceForAddress(address);
			final boolean isConfirmed =  (confirmedBalance.equals(unconfirmedBalance));
			final String balanceToShow = defaultNumberFormat.format(Double.valueOf(
				isConfirmed ? confirmedBalance : unconfirmedBalance));
			
			addressBalances[i++] = new String[]
			{
				balanceToShow,
				isConfirmed ? ("Yes " + confirmed) : ("No  " + notConfirmed),
				address
			};
		}
		
		for (final String address : zAddresses)
		{
			final String confirmedBalance = this.zenNode.clientCaller.getBalanceForAddress(address);
			final String unconfirmedBalance = this.zenNode.clientCaller.getUnconfirmedBalanceForAddress(address);
			final boolean isConfirmed =  (confirmedBalance.equals(unconfirmedBalance));
			final String balanceToShow = defaultNumberFormat.format(Double.valueOf(
				isConfirmed ? confirmedBalance : unconfirmedBalance));
			
			addressBalances[i++] = new String[]
			{
				balanceToShow,
				isConfirmed ? ("Yes " + confirmed) : ("No  " + notConfirmed),
				address
			};
		}

		return addressBalances;
	}


	// SendCashPanel
	
	private String[][] lastAddressBalanceData  = null;
	private AddressWithBalance[]   comboBoxItems           = null;
	private final DataGatheringThread<String[][]> addressBalanceGatheringThread = null;
	
//	private Timer        operationStatusTimer        = null;
	private String       operationStatusID           = null;
	private float        operationStatusCounter      = 0;

	/**
	 * Event handler delegate method for the {@link XdevButton} {@link #buttonSend}.
	 *
	 * @see Button.ClickListener#buttonClick(Button.ClickEvent)
	 * @eventHandlerDelegate Do NOT delete, used by UI designer!
	 */
	private void buttonSend_buttonClick(final Button.ClickEvent event) {
		try
	    {
			sendCash();
		} catch (final Exception ex)
		{
			log.error("Unexpected error: ", ex);
			
			String errMessage = "";
			if (ex instanceof WalletCallException)
			{
				errMessage = ((WalletCallException)ex).getMessage().replace(",", ",\n");
			}
			
			Notification.show("Error in sending cash", "An unexpected error occurred when sending cash!\n" +
					"Please ensure that the ZENCash daemon is running and\n" +
					"parameters are correct. You may try again later...\n" +
					errMessage, Type.ERROR_MESSAGE);
		}
	}
	

	
	private void sendCash()
			throws WalletCallException, IOException, InterruptedException
	{
		if (this.comboBoxBalanceAddress.size() <= 0) {
			Notification.show("No funds available",
					"There are no addresses with a positive balance to send\n" + "cash from!", Type.ERROR_MESSAGE);
			return;
		}

		if (this.comboBoxBalanceAddress.getValue() == null) {
			Notification.show("Please select source address",
					"Please select a source address with a current positive\n" + "balance to send cash from!",
					Type.ERROR_MESSAGE);
			return;
		}

		final String sourceAddress = ((AddressWithBalance) this.comboBoxBalanceAddress.getValue()).address;
		final String destinationAddress = this.textFieldDestinationAddress.getValue();
		final String memo = this.textFieldDestinationMemo.getValue();
		String amount = this.textFieldDestinationAmount.getValue();
		String fee = this.textFieldTransactionFee.getValue();

		// Verify general correctness.
		String errorMessage = null;

		if ((sourceAddress == null) || (sourceAddress.trim().length() <= 20)) {
			errorMessage = "Source address is invalid; it is too short or missing.";
		} else if (sourceAddress.length() > 512) {
			errorMessage = "Source address is invalid; it is too long.";
		}

		// TODO: full address validation
		if ((destinationAddress == null) || (destinationAddress.trim().length() <= 0)) {
			errorMessage = "Destination address is invalid; it is missing.";
		} else if (destinationAddress.trim().length() <= 20) {
			errorMessage = "Destination address is invalid; it is too short.";
		} else if (destinationAddress.length() > 512) {
			errorMessage = "Destination address is invalid; it is too long.";
		}

		if ((amount == null) || (amount.trim().length() <= 0)) {
			errorMessage = "Amount to send is invalid; it is missing.";
		} else {
			try {
				final double d = Double.valueOf(amount);
			} catch (final Exception nfe) {
				try {
					amount = usNumberFormat.format(defaultNumberFormat.parse(amount));
					final double d = Double.valueOf(amount);
				} catch (final ParseException e) {
					errorMessage = "Amount to send is invalid; it is not a number.";
				}
			}
		}

		if ((fee == null) || (fee.trim().length() <= 0)) {
			errorMessage = "Transaction fee is invalid; it is missing.";
		} else {
			try {
				final double d = Double.valueOf(fee);
			} catch (final Exception nfe) {
				try {
					fee = usNumberFormat.format(defaultNumberFormat.parse(fee));
					final double d = Double.valueOf(fee);
				} catch (final ParseException e) {
					errorMessage = "Transaction fee is invalid; it is not a number.";
				}
			}
		}

		if (errorMessage != null) {
			Notification.show("Sending parameters are incorrect", errorMessage, Type.ERROR_MESSAGE);
			return;
		}

		// Check for encrypted wallet
		final boolean bEncryptedWallet = this.zenNode.clientCaller.isWalletEncrypted();
		/*
		 * TODO LS if (bEncryptedWallet) { final PasswordDialog pd = new
		 * PasswordDialog((JFrame)(SendCashPanel.this.getRootPane().getParent())
		 * ); pd.setVisible(true);
		 * 
		 * if (!pd.isOKPressed()) { return; }
		 * 
		 * this.servlet.clientCaller.unlockWallet(pd.getPassword()); }
		 */

		// Call the wallet send method
		this.operationStatusID = this.zenNode.clientCaller.sendCash(sourceAddress, destinationAddress, amount, memo,
				fee);

		// Disable controls after send
		this.buttonSend.setEnabled(false);
		this.comboBoxBalanceAddress.setEnabled(false);
		this.textFieldDestinationAddress.setEnabled(false);
		this.textFieldDestinationAmount.setEnabled(false);
		this.textFieldDestinationMemo.setEnabled(false);
		this.textFieldTransactionFee.setEnabled(false);

		// Start a data gathering thread specific to the operation being
		// executed - this is done is a separate
		// thread since the server responds more slowly during JoinSPlits and
		// this blocks he GUI somewhat.
		final DataGatheringThread<Boolean> opFollowingThread = new DataGatheringThread<>(
				new DataGatheringThread.DataGatherer<Boolean>() {
					@Override
					public Boolean gatherData() throws Exception {
						final long start = System.currentTimeMillis();
						final Boolean result = MainView.this.zenNode.clientCaller
								.isSendingOperationComplete(MainView.this.operationStatusID);
						final long end = System.currentTimeMillis();
						log.info("Checking for operation " + MainView.this.operationStatusID + " status done in "
								+ (end - start) + "ms.");

						return result;
					}
				}, 2000, true);

		// Start a new thread on the serverside to update the progress of the
		// operation
		this.operationStatusCounter = 0;
		final String amountFinal = amount;
		final UI ui = getUI();
		new Thread(new RunnableAccessWrapper(() -> {
			try {
				Boolean opComplete = null;
				while (!((opComplete != null) && opComplete.booleanValue())) {
					// TODO: Handle errors in case of restarted server while
					// wallet is sending ...
					opComplete = opFollowingThread.getLastData();

					if ((opComplete != null) && opComplete.booleanValue()) {
						// End the special thread used to follow the
						// operation
						opFollowingThread.setSuspended(true);

						ui.access(() -> {
							try {
								if (MainView.this.zenNode.clientCaller
										.isCompletedOperationSuccessful(MainView.this.operationStatusID)) {
									MainView.this.labelOperationStatus
											.setValue("<span style=\"color:green;font-weight:bold\">SUCCESSFUL</span>");
									Notification.show("Cash sent successfully",
											"Succesfully sent " + amountFinal + " ZEN from address: \n" + sourceAddress
													+ "\n" + "to address: \n" + destinationAddress,
											Type.HUMANIZED_MESSAGE);
								} else {
									final String errorMessage2 = MainView.this.zenNode.clientCaller
											.getOperationFinalErrorMessage(MainView.this.operationStatusID);
									MainView.this.labelOperationStatus
											.setValue("<html><span style=\"color:red;font-weight:bold\">ERROR: "
													+ errorMessage2 + "</span></html>");

									Notification.show("Error in sending cash",
											"An error occurred when sending cash. Error message is:\n" + errorMessage2
													+ "\n\n"
													+ "Please ensure that sending parameters are correct. You may try again later...\n",
											Type.ERROR_MESSAGE);
								}

								// Lock the wallet again
								if (bEncryptedWallet) {
									MainView.this.zenNode.clientCaller.lockWallet();
								}

								// Restore controls etc.
								MainView.this.operationStatusCounter = 0;
								MainView.this.operationStatusID = null;
								MainView.this.prohgressBarOperationStatus.setValue(0F);

								MainView.this.buttonSend.setEnabled(true);
								MainView.this.comboBoxBalanceAddress.setEnabled(true);
								MainView.this.textFieldDestinationAddress.setEnabled(true);
								MainView.this.textFieldDestinationAmount.setEnabled(true);
								MainView.this.textFieldTransactionFee.setEnabled(true);
								MainView.this.textFieldDestinationMemo.setEnabled(true);
							} catch (final Exception e) {
								log.error("Unexpected error: ", e);
							}
						});
						ui.push();
					} else {
						ui.access(() -> {
							try {
								// Update the progress
								MainView.this.labelOperationStatus
										.setValue("<span style=\"color:orange;font-weight:bold\">IN PROGRESS</span>");
								MainView.this.operationStatusCounter += 0.02;
								float progress = 0;
								if (MainView.this.operationStatusCounter <= 1) {
									progress = MainView.this.operationStatusCounter;
								} else {
									progress = 1 + (((MainView.this.operationStatusCounter - 1) * 6) / 10);
								}
								// Server Push to update in the ProgressBar
								// in the
								// Browser
								MainView.this.prohgressBarOperationStatus.setValue(progress);
							} catch (final Exception e) {
								log.error("Unexpected error: ", e);
							}
						});
						ui.push();
					}
					// SendCashPanel.this.repaint();

					Thread.sleep(2000);
				}
			} catch (final Exception ex) {
				log.error("Unexpected error: ", ex);
			}
		})).start();
	}

	public void prepareForSending(final String address)
	{
	    this.textFieldDestinationAddress.setValue(address);
	}
	
	public static class AddressWithBalance {
		String address;
		String balance;
		public AddressWithBalance(final String address, final String balance) {
			super();
			this.address = address;
			this.balance = balance;
		}
	    @Override
		public String toString() {
	    	return defaultNumberFormat.format(Double.valueOf(this.balance))  +
					" - " + this.address;
	    }
	}
	
	
	private void updateWalletAddressPositiveBalanceComboBox(final String[][] newAddressBalanceData)
		throws WalletCallException, IOException, InterruptedException
	{
		boolean notChanged = true;
		// The data may be null if nothing is yet obtained
		if (newAddressBalanceData == null) {
		}
		else if (this.lastAddressBalanceData == null) {
			notChanged = false;
		}
		else if (this.lastAddressBalanceData.length != newAddressBalanceData.length) {
			notChanged = false;
		}
		else {
			for (int i = 0; i < newAddressBalanceData.length; i++) {
				if (!Arrays.equals(newAddressBalanceData[i], this.lastAddressBalanceData[i])) {
					notChanged = false;
					break;
				}
			}
		}
		
		if (notChanged) {
			return;
		}
		
		this.lastAddressBalanceData = newAddressBalanceData;
		
		this.comboBoxItems = new AddressWithBalance[this.lastAddressBalanceData.length];
		for (int i = 0; i < this.lastAddressBalanceData.length; i++)
		{
			// Do numeric formatting or else we may get 1.1111E-5
			this.comboBoxItems[i] = new AddressWithBalance (this.lastAddressBalanceData[i][1], this.lastAddressBalanceData[i][0]);
		}
		
		this.comboBoxBalanceAddress.removeAllItems();
		this.comboBoxBalanceAddress.addItems(Arrays.asList(this.comboBoxItems));

		/* TODO LS
		final int selectedIndex = this.comboBoxBalanceAddress.getSelectedIndex();
		final boolean isEnabled = this.comboBoxBalanceAddress.isEnabled();
		this.comboBoxParentPanel.remove(balanceAddressCombo);
		balanceAddressCombo = new JComboBox<>(this.comboBoxItems);
		comboBoxParentPanel.add(balanceAddressCombo);
		if ((balanceAddressCombo.getItemCount() > 0) &&
			(selectedIndex >= 0) &&
			(balanceAddressCombo.getItemCount() > selectedIndex))
		{
			this.comboBoxBalanceAddress.setSelectedIndex(selectedIndex);
		}
		this.comboBoxBalanceAddress.setEnabled(isEnabled);

		this.validate();
		this.repaint();
		*/
	}


	/**
	 * Event handler delegate method for the {@link XdevButton}
	 * {@link #buttonNewTAddress}.
	 *
	 * @see Button.ClickListener#buttonClick(Button.ClickEvent)
	 * @eventHandlerDelegate Do NOT delete, used by UI designer!
	 */
	private void buttonNewTAddress_buttonClick(final Button.ClickEvent event) {
		createNewAddress (false);
	}

	/**
	 * Event handler delegate method for the {@link XdevButton}
	 * {@link #buttonNewZAddress}.
	 *
	 * @see Button.ClickListener#buttonClick(Button.ClickEvent)
	 * @eventHandlerDelegate Do NOT delete, used by UI designer!
	 */
	private void buttonNewZAddress_buttonClick(final Button.ClickEvent event) {
		createNewAddress (true);
	}

	/**
	 * Event handler delegate method for the {@link XdevMenuBar.XdevMenuItem}
	 * {@link #menuItemShowPrivateKey}.
	 *
	 * @see MenuBar.Command#menuSelected(MenuBar.MenuItem)
	 * @eventHandlerDelegate Do NOT delete, used by UI designer!
	 */
	private void menuItemShowPrivateKey_menuSelected(final MenuBar.MenuItem selectedItem) {
		showPrivateKey();
	}
	
	public void showPrivateKey()
	{
		/*TODO LS select tab
		if (this.tabs.getSelectedIndex() != 1)
		{
			Notification.show("Please select an address...", "Please select an address in the \"Own addresses\" tab " +
					"to view its private key", Type.HUMANIZED_MESSAGE);
			this.tabs.setSelectedIndex(1);
			return;
		}
		*/
		
		if (this.selectedAddress == null)
		{
			Notification.show("Please select an address...", "Please select an address in the table of addresses " +
					"to view its private key", Type.HUMANIZED_MESSAGE);
			return;
		}
		
		try
		{
			/*TODO LS bEncryptedWallet
			// Check for encrypted wallet
			final boolean bEncryptedWallet = servlet.clientCaller.isWalletEncrypted();
			if (bEncryptedWallet)
			{
				PasswordDialog pd = new PasswordDialog((JFrame)(this.parent));
				pd.setVisible(true);
				
				if (!pd.isOKPressed())
				{
					return;
				}
				
				servlet.clientCaller.unlockWallet(pd.getPassword());
			}
			*/
			final boolean isZAddress = Util.isZAddress(this.selectedAddress);
			
			final String privateKey = isZAddress ?
					this.zenNode.clientCaller.getZPrivateKey(this.selectedAddress) : this.zenNode.clientCaller.getTPrivateKey(this.selectedAddress);
			/*TODO LS bEncryptedWallet
			// Lock the wallet again
			if (bEncryptedWallet)
			{
				this.servlet.clientCaller.lockWallet();
			}
			*/
					
/*
			final ClipboardButton clipboardButton = new ClipboardButton(getId());
			clipboardButton.addSuccessListener(new ClipboardButton.SuccessListener() {

			    @Override
			    public void onSuccess() {
			        Notification.show("Copy to clipboard successful");
			    }
			});
			clipboardButton.addErrorListener(new ClipboardButton.ErrorListener() {

			    @Override
			    public void onError() {
			        Notification.show("Copy to clipboard unsuccessful", Notification.Type.ERROR_MESSAGE);
			    }
			});
			clipboardButton.setClipboardText(privateKey);
					
			TODO LS Clipboard
			final JsClipboard JsClipboard = new JsClipboard ();
			
			final Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
			clipboard.setContents(new StringSelection(privateKey), null);
*/
			Notification.show("Private key information", isZAddress ? "Z (Private)" : "T (Transparent)" + " address:\n" +
					this.selectedAddress + "\n" +
					"has private key:\n" +
					privateKey + "\n\n" +
					"The private key has also been copied to the clipboard.", Type.HUMANIZED_MESSAGE);
		} catch (final Exception ex)
		{
			log.error("", ex);
		}
	}

	/**
	 * Event handler delegate method for the {@link XdevMenuBar.XdevMenuItem}
	 * {@link #menuItemLogOut}.
	 *
	 * @see MenuBar.Command#menuSelected(MenuBar.MenuItem)
	 * @eventHandlerDelegate Do NOT delete, used by UI designer!
	 */
	private void menuItemLogOut_menuSelected(final MenuBar.MenuItem selectedItem) {
		Authentication.logout();
		stopThreadsAndTimers();
	}

	/**
	 * Event handler delegate method for the {@link XdevView}.
	 *
	 * @see ClientConnector.DetachListener#detach(ClientConnector.DetachEvent)
	 * @eventHandlerDelegate Do NOT delete, used by UI designer!
	 */
	private void this_detach(final ClientConnector.DetachEvent event) {
		stopThreadsAndTimers();
	}

	/*
	 * WARNING: Do NOT edit!<br>The content of this method is always regenerated by
	 * the UI designer.
	 */
	// <generated-code name="initUI">
	private void initUI() {
		this.gridLayout = new XdevGridLayout();
		this.menuBar = new XdevMenuBar();
		this.menuItemMain = this.menuBar.addItem("Main", null);
		this.menuItemAbout = this.menuItemMain.addItem("About...", null);
		this.menuItemLogOut = this.menuItemMain.addItem("Log out...", null);
		this.menuItemWallet = this.menuBar.addItem("Wallet", null);
		this.menuItemBackup = this.menuItemWallet.addItem("Backup...", null);
		this.menuItemEncrypt = this.menuItemWallet.addItem("Encrypt...", null);
		this.menuItemExportPrivateKeys = this.menuItemWallet.addItem("Export private keys...", null);
		this.menuItemImportPrivateKeys = this.menuItemWallet.addItem("Import private keys...", null);
		this.menuItemShowPrivateKey = this.menuItemWallet.addItem("Show private key...", null);
		this.menuItemImportOnePrivateKey = this.menuItemWallet.addItem("Import one private key...", null);
		this.menuItemMessaging = this.menuBar.addItem("Messaging", null);
		this.menuItemOwnIdentity = this.menuItemMessaging.addItem("Own identity...", null);
		this.menuItemExportOwnIdentity = this.menuItemMessaging.addItem("Export own identity...", null);
		this.menuItemAddMessagingGroup = this.menuItemMessaging.addItem("Add messaging group...", null);
		this.menuItemImportContactIdentity = this.menuItemMessaging.addItem("Import contact identity...", null);
		this.menuItemRemoveContact = this.menuItemMessaging.addItem("Remove contact...", null);
		this.menuItemOptions = this.menuItemMessaging.addItem("Options...", null);
		this.tabSheet = new XdevTabSheet();
		this.tabOverview = new XdevGridLayout();
		this.labelTransparentBalanceCaption = new XdevLabel();
		this.labelTransparentBalance = new XdevLabel();
		this.labelPrivateBalanceCaption = new XdevLabel();
		this.labelPrivateBalance = new XdevLabel();
		this.labelTotalBalanceCaption = new XdevLabel();
		this.labelTotalBalance = new XdevLabel();
		this.panelGridTransactions = new XdevPanel();
		this.tabOwnAddresses = new XdevGridLayout();
		this.panelGridOwnAddresses = new XdevPanel();
		this.buttonNewTAddress = new XdevButton();
		this.buttonNewZAddress = new XdevButton();
		this.tabSendCash = new XdevGridLayout();
		this.comboBoxBalanceAddress = new XdevComboBox<>();
		this.label = new XdevLabel();
		this.textFieldDestinationAddress = new XdevTextField();
		this.textFieldDestinationMemo = new XdevTextField();
		this.label2 = new XdevLabel();
		this.textFieldDestinationAmount = new XdevTextField();
		this.label3 = new XdevLabel();
		this.textFieldTransactionFee = new XdevTextField();
		this.label4 = new XdevLabel();
		this.buttonSend = new XdevButton();
		this.prohgressBarOperationStatus = new XdevProgressBar();
		this.labelOperationStatus = new XdevLabel();
		this.label5 = new XdevLabel();
		this.tabAddressBook = new XdevGridLayout();
		this.tabMessaging = new XdevGridLayout();
	
		this.gridLayout.setMargin(new MarginInfo(false));
		this.menuItemAbout.setEnabled(false);
		this.menuItemBackup.setEnabled(false);
		this.menuItemEncrypt.setEnabled(false);
		this.menuItemExportPrivateKeys.setEnabled(false);
		this.menuItemImportPrivateKeys.setEnabled(false);
		this.menuItemImportOnePrivateKey.setEnabled(false);
		this.menuItemOwnIdentity.setEnabled(false);
		this.menuItemExportOwnIdentity.setEnabled(false);
		this.menuItemAddMessagingGroup.setEnabled(false);
		this.menuItemImportContactIdentity.setEnabled(false);
		this.menuItemRemoveContact.setEnabled(false);
		this.menuItemOptions.setEnabled(false);
		this.tabSheet.setStyleName("framed");
		this.labelTransparentBalanceCaption.setValue("Transparent (T) balance:");
		this.labelTransparentBalance.setValue("0");
		this.labelPrivateBalanceCaption.setValue("Private (Z) balance:");
		this.labelPrivateBalance.setValue("0");
		this.labelTotalBalanceCaption.setValue("Total (T+Z) balance:");
		this.labelTotalBalance.setStyleName("bold");
		this.labelTotalBalance.setValue("0");
		this.buttonNewTAddress.setCaption("New T (Transparent) address");
		this.buttonNewZAddress.setCaption("New Z (Private) address");
		this.comboBoxBalanceAddress.setTextInputAllowed(false);
		this.comboBoxBalanceAddress.setCaption("Send cash from:");
		this.comboBoxBalanceAddress.setRequired(true);
		this.label.setStyleName("tiny");
		this.label.setValue("* Only addresses with a confirmed balance are shown as sources for sending!");
		this.textFieldDestinationAddress.setCaption("Destination address:");
		this.textFieldDestinationAddress.setRequired(true);
		this.textFieldDestinationMemo.setCaption("Memo (optional):");
		this.label2.setStyleName("tiny");
		this.label2.setValue("* Memo may be specified only if the destination is a Z (Private) address!");
		this.textFieldDestinationAmount
				.setConverter(ConverterBuilder.stringToDouble().minimumFractionDigits(0).maximumFractionDigits(10).build());
		this.textFieldDestinationAmount.setCaption("Amount to send:");
		this.textFieldDestinationAmount.setStyleName("align-right");
		this.textFieldDestinationAmount.setRequired(true);
		this.label3.setValue("ZEN");
		this.textFieldTransactionFee
				.setConverter(ConverterBuilder.stringToDouble().minimumFractionDigits(0).maximumFractionDigits(10).build());
		this.textFieldTransactionFee.setCaption("Transaction fee:");
		this.textFieldTransactionFee.setStyleName("align-right");
		this.textFieldTransactionFee.setRequired(true);
		this.label4.setValue("ZEN");
		this.buttonSend.setCaption("Send");
		this.prohgressBarOperationStatus.setCaption("Progress:");
		this.labelOperationStatus.setCaption("Last operation status: ");
		this.labelOperationStatus.setValue("N/A");
		this.labelOperationStatus.setContentMode(ContentMode.HTML);
		this.label5.setStyleName("tiny");
		this.label5.setValue(
				" * When sending cash from a T (Transparent) address, the remining unspent balance is sent to another auto-generated T address.<br>When sending from a Z (Private) address, the remining unspent balance remains with the Z address. In both cases the original sending <br>address cannot be used for sending again until the transaction is confirmed. The address is temporarily removed from the list! <br>Freshly mined coins may only be sent to a Z (Private) address.");
		this.label5.setContentMode(ContentMode.HTML);
		this.tabAddressBook.setEnabled(false);
		this.tabAddressBook.setVisible(false);
		this.tabMessaging.setEnabled(false);
		this.tabMessaging.setVisible(false);
	
		this.tabOverview.setColumns(2);
		this.tabOverview.setRows(4);
		this.labelTransparentBalanceCaption.setSizeUndefined();
		this.tabOverview.addComponent(this.labelTransparentBalanceCaption, 0, 0);
		this.tabOverview.setComponentAlignment(this.labelTransparentBalanceCaption, Alignment.TOP_RIGHT);
		this.labelTransparentBalance.setSizeUndefined();
		this.tabOverview.addComponent(this.labelTransparentBalance, 1, 0);
		this.labelPrivateBalanceCaption.setSizeUndefined();
		this.tabOverview.addComponent(this.labelPrivateBalanceCaption, 0, 1);
		this.tabOverview.setComponentAlignment(this.labelPrivateBalanceCaption, Alignment.TOP_RIGHT);
		this.labelPrivateBalance.setSizeUndefined();
		this.tabOverview.addComponent(this.labelPrivateBalance, 1, 1);
		this.labelTotalBalanceCaption.setSizeUndefined();
		this.tabOverview.addComponent(this.labelTotalBalanceCaption, 0, 2);
		this.tabOverview.setComponentAlignment(this.labelTotalBalanceCaption, Alignment.TOP_RIGHT);
		this.labelTotalBalance.setSizeUndefined();
		this.tabOverview.addComponent(this.labelTotalBalance, 1, 2);
		this.panelGridTransactions.setSizeFull();
		this.tabOverview.addComponent(this.panelGridTransactions, 0, 3, 1, 3);
		this.tabOverview.setColumnExpandRatio(0, 10.0F);
		this.tabOverview.setColumnExpandRatio(1, 10.0F);
		this.tabOverview.setRowExpandRatio(3, 10.0F);
		this.tabOwnAddresses.setColumns(2);
		this.tabOwnAddresses.setRows(2);
		this.panelGridOwnAddresses.setSizeFull();
		this.tabOwnAddresses.addComponent(this.panelGridOwnAddresses, 0, 0, 1, 0);
		this.buttonNewTAddress.setSizeUndefined();
		this.tabOwnAddresses.addComponent(this.buttonNewTAddress, 0, 1);
		this.tabOwnAddresses.setComponentAlignment(this.buttonNewTAddress, Alignment.TOP_RIGHT);
		this.buttonNewZAddress.setSizeUndefined();
		this.tabOwnAddresses.addComponent(this.buttonNewZAddress, 1, 1);
		this.tabOwnAddresses.setColumnExpandRatio(0, 10.0F);
		this.tabOwnAddresses.setColumnExpandRatio(1, 10.0F);
		this.tabOwnAddresses.setRowExpandRatio(0, 10.0F);
		this.tabSendCash.setColumns(4);
		this.tabSendCash.setRows(9);
		this.comboBoxBalanceAddress.setWidth(100, Unit.PERCENTAGE);
		this.comboBoxBalanceAddress.setHeight(-1, Unit.PIXELS);
		this.tabSendCash.addComponent(this.comboBoxBalanceAddress, 0, 0, 3, 0);
		this.label.setWidth(100, Unit.PERCENTAGE);
		this.label.setHeight(-1, Unit.PIXELS);
		this.tabSendCash.addComponent(this.label, 0, 1, 3, 1);
		this.textFieldDestinationAddress.setWidth(100, Unit.PERCENTAGE);
		this.textFieldDestinationAddress.setHeight(-1, Unit.PIXELS);
		this.tabSendCash.addComponent(this.textFieldDestinationAddress, 0, 2, 3, 2);
		this.textFieldDestinationMemo.setWidth(100, Unit.PERCENTAGE);
		this.textFieldDestinationMemo.setHeight(-1, Unit.PIXELS);
		this.tabSendCash.addComponent(this.textFieldDestinationMemo, 0, 3, 3, 3);
		this.label2.setWidth(100, Unit.PERCENTAGE);
		this.label2.setHeight(-1, Unit.PIXELS);
		this.tabSendCash.addComponent(this.label2, 0, 4, 3, 4);
		this.textFieldDestinationAmount.setWidth(100, Unit.PERCENTAGE);
		this.textFieldDestinationAmount.setHeight(-1, Unit.PIXELS);
		this.tabSendCash.addComponent(this.textFieldDestinationAmount, 0, 5);
		this.label3.setSizeUndefined();
		this.tabSendCash.addComponent(this.label3, 1, 5);
		this.tabSendCash.setComponentAlignment(this.label3, Alignment.BOTTOM_LEFT);
		this.textFieldTransactionFee.setWidth(100, Unit.PERCENTAGE);
		this.textFieldTransactionFee.setHeight(-1, Unit.PIXELS);
		this.tabSendCash.addComponent(this.textFieldTransactionFee, 2, 5);
		this.label4.setSizeUndefined();
		this.tabSendCash.addComponent(this.label4, 3, 5);
		this.tabSendCash.setComponentAlignment(this.label4, Alignment.BOTTOM_LEFT);
		this.buttonSend.setWidth(100, Unit.PERCENTAGE);
		this.buttonSend.setHeight(-1, Unit.PIXELS);
		this.tabSendCash.addComponent(this.buttonSend, 0, 6, 1, 6);
		this.prohgressBarOperationStatus.setWidth(100, Unit.PERCENTAGE);
		this.prohgressBarOperationStatus.setHeight(-1, Unit.PIXELS);
		this.tabSendCash.addComponent(this.prohgressBarOperationStatus, 2, 6);
		this.labelOperationStatus.setSizeUndefined();
		this.tabSendCash.addComponent(this.labelOperationStatus, 3, 6);
		this.label5.setWidth(100, Unit.PERCENTAGE);
		this.label5.setHeight(-1, Unit.PIXELS);
		this.tabSendCash.addComponent(this.label5, 0, 7, 3, 7);
		this.tabSendCash.setColumnExpandRatio(0, 20.0F);
		this.tabSendCash.setColumnExpandRatio(2, 10.0F);
		final CustomComponent tabSendCash_vSpacer = new CustomComponent();
		tabSendCash_vSpacer.setSizeFull();
		this.tabSendCash.addComponent(tabSendCash_vSpacer, 0, 8, 3, 8);
		this.tabSendCash.setRowExpandRatio(8, 1.0F);
		this.tabOverview.setSizeFull();
		this.tabSheet.addTab(this.tabOverview, "Overview", null);
		this.tabOwnAddresses.setSizeFull();
		this.tabSheet.addTab(this.tabOwnAddresses, "Own addresses", null);
		this.tabSendCash.setSizeFull();
		this.tabSheet.addTab(this.tabSendCash, "Send cash", null);
		this.tabAddressBook.setSizeFull();
		this.tabSheet.addTab(this.tabAddressBook, "Address book", null);
		this.tabMessaging.setSizeFull();
		this.tabSheet.addTab(this.tabMessaging, "Messaging", null);
		this.tabSheet.setSelectedTab(this.tabOverview);
		this.gridLayout.setColumns(1);
		this.gridLayout.setRows(2);
		this.menuBar.setWidth(100, Unit.PERCENTAGE);
		this.menuBar.setHeight(-1, Unit.PIXELS);
		this.gridLayout.addComponent(this.menuBar, 0, 0);
		this.tabSheet.setSizeFull();
		this.gridLayout.addComponent(this.tabSheet, 0, 1);
		this.gridLayout.setColumnExpandRatio(0, 100.0F);
		this.gridLayout.setRowExpandRatio(1, 100.0F);
		this.gridLayout.setSizeFull();
		this.setContent(this.gridLayout);
		this.setSizeFull();
	
		this.addDetachListener(event -> this.this_detach(event));
		this.menuItemLogOut.setCommand(selectedItem -> this.menuItemLogOut_menuSelected(selectedItem));
		this.menuItemShowPrivateKey.setCommand(selectedItem -> this.menuItemShowPrivateKey_menuSelected(selectedItem));
		this.buttonNewTAddress.addClickListener(event -> this.buttonNewTAddress_buttonClick(event));
		this.buttonNewZAddress.addClickListener(event -> this.buttonNewZAddress_buttonClick(event));
		this.buttonSend.addClickListener(event -> this.buttonSend_buttonClick(event));
	} // </generated-code>



	// <generated-code name="variables">
	private XdevLabel labelTransparentBalanceCaption, labelTransparentBalance, labelPrivateBalanceCaption,
			labelPrivateBalance, labelTotalBalanceCaption, labelTotalBalance, label, label2, label3, label4,
			labelOperationStatus, label5;
	private XdevButton buttonNewTAddress, buttonNewZAddress, buttonSend;
	private XdevMenuBar menuBar;
	private XdevMenuItem menuItemMain, menuItemAbout, menuItemLogOut, menuItemWallet, menuItemBackup, menuItemEncrypt,
			menuItemExportPrivateKeys, menuItemImportPrivateKeys, menuItemShowPrivateKey, menuItemImportOnePrivateKey,
			menuItemMessaging, menuItemOwnIdentity, menuItemExportOwnIdentity, menuItemAddMessagingGroup,
			menuItemImportContactIdentity, menuItemRemoveContact, menuItemOptions;
	private XdevProgressBar prohgressBarOperationStatus;
	private XdevTabSheet tabSheet;
	private XdevPanel panelGridTransactions, panelGridOwnAddresses;
	private XdevGridLayout gridLayout, tabOverview, tabOwnAddresses, tabSendCash, tabAddressBook, tabMessaging;
	private XdevTextField textFieldDestinationAddress, textFieldDestinationMemo, textFieldDestinationAmount,
			textFieldTransactionFee;
	private XdevComboBox<AddressWithBalance> comboBoxBalanceAddress;
	// </generated-code>

}
