package com.awidesky.pMailsender;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.HeadlessException;
import java.awt.Toolkit;
import java.awt.Dialog.ModalityType;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.io.File;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.reflect.InvocationTargetException;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.UnsupportedLookAndFeelException;
import javax.swing.filechooser.FileFilter;

public class MainFrame extends JFrame {

	private static final long serialVersionUID = 7255143921312710354L;
	private JDialog dialog = new JDialog();;
	private JFileChooser chooser;
	
	private JLabel title = new JLabel("Title : ");
	private JLabel content = new JLabel("Content : ");
	private JTextField tf_title;
	private JTextField tf_content;
	private JTextArea files = new JTextArea(10, 50);
	private JTextArea console = new JTextArea(20, 50);
	
	public MainFrame(String t, String c) {
		super("PMailSender");
		tf_title = new JTextField(t, 10);
		tf_content = new JTextField(c, 20);
	}
	
	public void setUp() {
		setDialog();
		setLayout(new BorderLayout(10, 10));
		setSize(200, 300);
		Dimension dim = Toolkit.getDefaultToolkit().getScreenSize();
		JPanel texts = new JPanel();
		texts.add(title);
		texts.add(tf_title);
		texts.add(content);
		texts.add(tf_content);
		add(texts, BorderLayout.NORTH);
		
		JPanel consoles = new JPanel();
		consoles.setLayout(new BoxLayout(consoles, BoxLayout.Y_AXIS));
		files.setEditable(false);
		files.setBackground(Color.BLACK);
		files.setForeground(Color.WHITE);
		console.setEditable(false);
		console.setBackground(Color.BLACK);
		console.setForeground(Color.WHITE);
		JScrollPane jsc_files = new JScrollPane(files);
		JScrollPane jsc_console = new JScrollPane(console);
		consoles.add(Box.createVerticalStrut(5));
		consoles.add(jsc_files);
		consoles.add(Box.createVerticalStrut(5));
		consoles.add(jsc_console);
		add(consoles, BorderLayout.CENTER);
		
		pack();
		setLocation(dim.width / 2 - getSize().width / 2, dim.height / 2 - getSize().height / 2);
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setVisible(true);  
	}
	

	private void setDialog() {
		
		log("Preparing GUI...");
		try {
			UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
		} catch (ClassNotFoundException | InstantiationException | IllegalAccessException
				| UnsupportedLookAndFeelException e) {
			e.printStackTrace();
		}

		dialog.setAlwaysOnTop(true);
		
		chooser = new JFileChooser() {

			private static final long serialVersionUID = 1685021336154920714L;

			@Override
			protected JDialog createDialog(Component parent) throws HeadlessException {
				JDialog d = super.createDialog(parent);
				d.setModalityType(ModalityType.DOCUMENT_MODAL);
				d.setAlwaysOnTop(true);
				return d;
			}
		};
		ImageViewer imageV = new ImageViewer(chooser);
		chooser.setMultiSelectionEnabled(true);
		chooser.setAccessory(imageV);
		chooser.addPropertyChangeListener(imageV);
		chooser.addComponentListener(new ComponentAdapter() {
		    public void componentResized(ComponentEvent e) {
		    	imageV.dialogSizeChange();
		    }
		});
		chooser.addChoosableFileFilter(new FileFilter() {
			
			public boolean accept(File f) {
				if (f.isDirectory()
						|| f.getName().endsWith(".jpeg")
						|| f.getName().endsWith(".jpg")
						|| f.getName().endsWith(".bmp")
						|| f.getName().endsWith(".png"))
					return true;
				else
					return false;
			}

			public String getDescription() {
				return "Picture files (*.jpeg, *.jpg, *.png, *.bmp)";
			}
			
		});
		
		chooser.addChoosableFileFilter(new FileFilter() {
			
			public boolean accept(File f) {
				if (f.isDirectory()
						|| f.getName().endsWith(".pdf")
						|| f.getName().endsWith(".docx")
						|| f.getName().endsWith(".hwp")
						|| f.getName().endsWith(".xlsx")
						|| f.getName().endsWith(".pptx"))
					return true;
				else
					return false;
			}

			public String getDescription() {
				return "Document files (*.pdf, *.docx, *.hwp, *.xlsx, *.pptx)";
			}
		});
		
	}
	
	public void disable() {
		tf_title.setEnabled(false);
		tf_content.setEnabled(false);
	}
	public String getTitle() { return tf_title.getText(); }
	public String getContent() { return tf_content.getText(); }
	
	public void log() {
		SwingUtilities.invokeLater(() -> {
			console.append("\n");
		});
		System.out.println();
	}
	public void log(String str) {
		SwingUtilities.invokeLater(() -> {
			console.append(str);
			console.append("\n");
		});
		System.out.println(str);
	}
	public void log(Exception e) {
		StringWriter sw = new StringWriter();
		PrintWriter pw = new PrintWriter(sw);
		e.printStackTrace(pw);
		pw.close(); pw.close();
		log(sw.toString());
	}
	public void selectedFiles(String str) {
		SwingUtilities.invokeLater(() -> {
			files.setText("Selected Files :\n");
			files.append(str);
		});
	}


	public List<File> chooseLoop(File startPath) {
		List<File> list = new LinkedList<>();
		
		while (true) {
			
			chooser.setCurrentDirectory(startPath);
			if (chooser.showOpenDialog(dialog) != JFileChooser.APPROVE_OPTION) break;
			List<File> temp = Arrays.asList(chooser.getSelectedFiles());
			startPath = temp.get(temp.size() - 1);
			list.addAll(temp);
			selectedFiles(list.stream().map(File::getAbsolutePath).collect(Collectors.joining("\n")));
		
		}

		dialog.dispose();
		disable();
		return list;
	}

	public char[] inputPassword() {
		char[] password = new char[] {'\0'};
		final JPasswordField pf = new JPasswordField();
		if (JOptionPane.showConfirmDialog(dialog, pf, "Enter password : ", JOptionPane.OK_OPTION, JOptionPane.QUESTION_MESSAGE) == JOptionPane.OK_OPTION) {
			password = pf.getPassword();
		} else { error("Error", "You didin't type password!"); System.exit(1); }
		
		return password;
	}
	
	public void inform(String title, String content) {
		SwingUtilities.invokeLater(() -> {
			JOptionPane.showMessageDialog(dialog, title, content, JOptionPane.INFORMATION_MESSAGE);
		});
	}
	public void error(String title, String content) {
		SwingUtilities.invokeLater(() -> {
			JOptionPane.showMessageDialog(dialog, content, title, JOptionPane.ERROR_MESSAGE);
		});
	}

	public boolean confirm(String title, String content) {
		final AtomicReference<Boolean> result = new AtomicReference<>();
		try {
			SwingUtilities.invokeAndWait(() -> {
				result.set(JOptionPane.showConfirmDialog(dialog, title, content, JOptionPane.YES_NO_OPTION) == JOptionPane.YES_OPTION);
			});
		} catch (InvocationTargetException | InterruptedException e) {
			log(e);
			return false;
		}
		return result.get();

	}

	@Override
	public void dispose() {
		dialog.dispose();
		super.dispose();
	}

}
