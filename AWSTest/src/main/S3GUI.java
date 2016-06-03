package main;
/*
 * File: S3GUI.java
 * Author(s): Ethan Allnutt & Josh Testa
 * Company: Praxis Engineering (praxiseng.com)
 * Date Last Modified: 5/31/16
 * Project: Interns Summer 2016
 *
 * This is the gui class for the S3Reader class
 * Allows the key(s) and bucket info to be entered
 * Has an output text area that word count info is printed to
 */

import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.io.IOException;
import java.util.ArrayList;

import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;

public class S3GUI extends JFrame implements ActionListener {

	private static final long serialVersionUID = 440019824721479174L;

	S3Reader s3Reader = new S3Reader();

	//I used panels to help with the layout
	private JPanel
	textFieldPanel = new JPanel(),
	buttonPanel = new JPanel(),
	textAreaPanel = new JPanel();

	//TextFields that show a 'hint' for what is supposed to be entered
	private JTextField
	accKey = new HintTextField("Access Key"),
	saccKey = new HintTextField("Secret Access Key"),
	bucket = new HintTextField("Bucket Name"),
	bkey = new HintTextField("Bucket Key");

	private JTextArea
	outputBuzz = new JTextArea(),
	outputNew = new JTextArea();

	private JScrollPane
	scrollPane1 = new JScrollPane(outputBuzz),
	scrollPane2 = new JScrollPane(outputNew);

	private JButton
	submitButton = new JButton(),
	showExcludedButton = new JButton(),
	infoButton = new JButton(),
	continueButton = new JButton(),
	quitButton = new JButton();

	private JDialog
	warningDialog = new JDialog();

	private JLabel
	warningLabel = new JLabel();

	//For storing the access keys and bucket info
	private String[] fieldArgs = {"","","",""};

	//used to hold the gui until user enters info
	public boolean
	guiNotReady = true;

	PrinterArea
	excludedWords = new PrinterArea(),
	info = new PrinterArea();


	public S3GUI() {

		//General window setup
		this.setLayout(new FlowLayout());
		this.setTitle("S3Reader - Intern Project Summer 2016 Phase 1 (Josh Testa & Ethan Allnutt)");
		this.setSize(1250, 1000);
		this.getContentPane().setBackground(Color.decode("#313133"));
		this.setDefaultCloseOperation(EXIT_ON_CLOSE);


		//First text area
		accKey.setEditable(true);
		accKey.setPreferredSize(new Dimension(250,45));
		accKey.setBorder(null);
		Font font = accKey.getFont();
		accKey.setFont( font.deriveFont(20.0f) );


		//Second text area
		saccKey.setEditable(true);
		saccKey.setPreferredSize(new Dimension(250,45));
		saccKey.setBorder(null);
		saccKey.setFont( font.deriveFont(20.0f) );


		//Third text area
		bucket.setEditable(true);
		bucket.setPreferredSize(new Dimension(250,45));
		bucket.setBorder(null);
		bucket.setFont( font.deriveFont(20.0f) );


		//Fourth text area
		bkey.setEditable(true);
		bkey.setPreferredSize(new Dimension(250,45));
		bkey.addActionListener(this);
		bkey.setBorder(null);
		bkey.setFont( font.deriveFont(20.0f) );

		textFieldPanel.add(accKey);
		textFieldPanel.add(saccKey);
		textFieldPanel.add(bucket);
		textFieldPanel.add(bkey);
		textFieldPanel.setBorder(null);
		textFieldPanel.setBackground(Color.decode("#313133"));


		//Button that can be pressed to continue
		submitButton.setText("CONTINUE");
		submitButton.setPreferredSize(new Dimension(210,25));
		submitButton.addActionListener(this);
		submitButton.setForeground(Color.decode("#39393A"));

		//Button to show excluded words
		showExcludedButton.setText("SHOW EXCLUDED WORDS");
		showExcludedButton.setPreferredSize(new Dimension(210,25));
		showExcludedButton.addActionListener(this);
		showExcludedButton.setForeground(Color.decode("#39393A"));

		//Button to show info
		infoButton.setText("INFO");
		infoButton.setPreferredSize(new Dimension(210,25));
		infoButton.addActionListener(this);
		infoButton.setForeground(Color.decode("#39393A"));

		buttonPanel.add(submitButton);
		buttonPanel.add(showExcludedButton);
		buttonPanel.add(infoButton);
		buttonPanel.setBorder(null);
		buttonPanel.setBackground(Color.decode("#313133"));


		//Text area that the output is printed to, with scroll pane
		outputBuzz.setEditable(false);
		outputBuzz.setForeground(Color.decode("#AACCFF"));
		outputBuzz.setBackground(Color.decode("#222223"));
		scrollPane1.setPreferredSize(new Dimension(600,850));
		scrollPane1.setBorder(null);


		outputNew.setEditable(false);
		outputNew.setForeground(Color.decode("#AACCFF"));
		outputNew.setBackground(Color.decode("#222223"));
		scrollPane2.setPreferredSize(new Dimension(600,850));
		scrollPane2.setBorder(null);

		textAreaPanel.add(scrollPane1);
		textAreaPanel.add(scrollPane2);
		textAreaPanel.setBorder(null);
		textAreaPanel.setBackground(Color.decode("#313133"));

		this.add(textFieldPanel);
		this.add(buttonPanel);
		this.add(textAreaPanel);

	}

	/*
	 * You can either press the 'CONTINUE' button or press enter when the last text field is active
	 *
	 * (non-Javadoc)
	 * @see java.awt.event.ActionListener#actionPerformed(java.awt.event.ActionEvent)
	 */
	@Override
	public void actionPerformed(ActionEvent e) {
		if(e.getSource() == submitButton || e.getSource() == bkey) {
			fieldArgs[0] = accKey.getText();
			fieldArgs[1] = saccKey.getText();
			fieldArgs[2] = bucket.getText();
			fieldArgs[3] = bkey.getText();
			guiNotReady = false;
		}
		else if(e.getSource() == showExcludedButton) {
			showWarning();
		}
		else if(e.getSource() == infoButton) {
			showInfo();
		}
		else if(e.getSource() == continueButton) {
			warningDialog.dispose();
			printList(s3Reader.excludedWords);
		}
		else if(e.getSource() == quitButton) {
			warningDialog.dispose();
		}
	}

	/*
	 * Returns the arguments gathered from the text fields
	 */
	public String[] getArgs() {
		while(guiNotReady) {
			try {
				Thread.sleep(100);
			} catch(InterruptedException e) {
				e.printStackTrace();
			}
		}
		return fieldArgs;
	}

	/*
	 * Appends text onto the output text area
	 *
	 * @param arg	String to be added
	 */
	public void addTextB(String arg) {
		outputBuzz.append(arg);
		Font font = outputBuzz.getFont();
		outputBuzz.setFont( font.deriveFont(25.0f) );
		//outputb.setCaretPosition(output.getDocument().getLength());
	}

	/*
	 * Appends text onto the output text area
	 *
	 * @param arg	String to be added
	 */
	public void addTextN(String arg) {
		outputNew.append(arg);
		Font font = outputNew.getFont();
		outputNew.setFont( font.deriveFont(25.0f) );
		//outputn.setCaretPosition(output.getDocument().getLength());
	}

	/*
	 * Prints passed in List
	 */
	public void printList(ArrayList<String> list) {
		excludedWords.clear();
		excludedWords.setVisible(true);
		for(String s : list) {
			excludedWords.addText(s);
		}
	}

	public void showInfo() {
		info.clear();
		info.setSize(800, 300);
		info.addText("Summer 2016 Intern Project\n");
		info.addText("How to Use:");
		info.addText("1) Enter credentials into the fields at the top of the window\t");
		info.addText("2) Press the 'continue' button and wait for the specified file to be downloaded and parsed\t");
		info.addText("3) Specified Key Words are listed on the left with their respective counts\t");
		info.addText("4) Any word that is not a Key Words or in the list of excluded words is listed on the right with its repective count\t");
		info.addText("5) To view the list of excluded words, click on the 'show excluded words' button\t");
		info.setVisible(true);
	}

	public void showWarning() {
		warningDialog.setLayout(new FlowLayout());
		warningDialog.setSize(new Dimension(350,100));
		warningDialog.setDefaultCloseOperation(DISPOSE_ON_CLOSE);
		warningLabel.setText("That might take a couple seconds, are you sure?");
		Font font = warningLabel.getFont();
		warningLabel.setFont( font.deriveFont(12.0f) );
		warningDialog.add(warningLabel);
		continueButton.setText("Continue");
		continueButton.setPreferredSize(new Dimension(125,25));
		continueButton.addActionListener(this);
		quitButton.setText("Exit");
		quitButton.setPreferredSize(new Dimension(75,25));
		quitButton.addActionListener(this);
		warningDialog.add(continueButton);
		warningDialog.add(quitButton);
		warningDialog.setVisible(true);
	}

	/*
	 * Helper structure that is a simple text area pop-up window that contains the excluded words
	 */
	class PrinterArea extends JFrame {

		/**
		 *
		 */
		private static final long serialVersionUID = 1L;

		private JTextArea
		mainTextArea = new JTextArea();

		private JScrollPane
		mainScrollPane = new JScrollPane(mainTextArea);

		private JPanel
		mainPanel = new JPanel();

		public PrinterArea() {
			this.setSize(new Dimension(600,750));
			mainTextArea.setEditable(false);
			mainScrollPane.setPreferredSize(new Dimension(600,750));
			mainPanel.add(mainScrollPane);
			this.add(mainScrollPane);
		}

		public void addText(String arg) {
			mainTextArea.append(arg+"\n");
			Font font = mainTextArea.getFont();
			mainTextArea.setFont( font.deriveFont(15.0f) );
			mainTextArea.setCaretPosition(0);
		}

		public void clear() {
			mainTextArea.setText("");
		}

	}


	/*
	 * Helper structure that allows the 'hint' to appear when no other text is entered
	 */
	class HintTextField extends JTextField implements FocusListener {

		/**
		 *
		 */
		private static final long serialVersionUID = 1L;

		private final String hint;
		private boolean showingHint;

		public HintTextField(final String hint) {
			super(hint);
			this.hint = hint;
			this.showingHint = true;
			super.addFocusListener(this);
		}

		@Override
		public void focusGained(FocusEvent e) {
			if(this.getText().isEmpty()) {
				super.setText("");
				showingHint = false;
			}
		}
		@Override
		public void focusLost(FocusEvent e) {
			if(this.getText().isEmpty()) {
				super.setText(hint);
				showingHint = true;
			}
		}

		@Override
		public String getText() {
			return showingHint ? "" : super.getText();
		}
	}


	public static void main(String[] args) throws IOException {
		long startTime = System.nanoTime();
		S3GUI s3g = new S3GUI();
		s3g.setVisible(true);
		/*String[] a = s3g.getArgs();
		s3g.s3Reader.init(a[0], a[1]);
		try {
			s3g.s3Reader.readFromS3(a[2],a[3],s3g);
		} catch (IOException e) {
			e.printStackTrace();
		}*/
		s3g.s3Reader.init("AKIAISOSDPH653DS47HQ","ZPBgANZ9VLY9y5ZY8PIkz8Muzv20fiJ3BIvatE8b");
		try {
			s3g.s3Reader.readFromS3("praxis-interns", "PIMS_data_interns.xlsx",s3g);
		} catch (IOException e) {
			e.printStackTrace();
		}

		long endTime = System.nanoTime();
		System.out.println("\nTook "+((endTime - startTime)/1000000000.0) + " s");

	}


}