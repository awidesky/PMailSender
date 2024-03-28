package com.awidesky.pMailsender;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Desktop;
import java.awt.Dialog.ModalityType;
import java.awt.Dimension;
import java.awt.HeadlessException;
import java.awt.Image;
import java.awt.Taskbar;
import java.awt.Toolkit;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.reflect.InvocationTargetException;
import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import javax.imageio.ImageIO;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import javax.swing.filechooser.FileFilter;

import io.github.awidesky.guiUtil.SwingDialogs;
import io.github.awidesky.guiUtil.TaskLogger;
import io.github.awidesky.projectPath.JarPath;

public class MainFrame extends JFrame {

	private static final long serialVersionUID = 7255143921312710354L;
	private static final Dimension dim = Toolkit.getDefaultToolkit().getScreenSize();
	private JDialog dialog = new JDialog();
	private JFileChooser chooser;
	
	private JLabel title = new JLabel("Title : ");
	private JLabel content = new JLabel("Content : ");
	private JTextField tf_title;
	private JTextField tf_content;
	private JTextArea files = new JTextArea();
	private JTextArea console = new JTextArea();
	private JLabel maxAttat = new JLabel("Attatchment size limit(MB) :");
	private JTextField tf_maxAttat = new JTextField("10", 3);
	private JButton openConfig = new JButton("config.txt");
	private JButton openDropbox = new JButton("dropboxAuth.txt");
	private JButton openAppFolder = new JButton("open app folder");
	
	private TaskLogger logger;
	
	public MainFrame(TaskLogger taskLogger, String t, String c) {
		super();
		this.logger = taskLogger;
		tf_title = new JTextField(t, 10);
		tf_content = new JTextField(c, 20);
	}
	
	public void setUp() {
		log("Preparing GUI...");

		setDialog();
		setLayout(new BorderLayout(5, 5));
		setSize(610, 620);
		
		
		JPanel texts = new JPanel();
		texts.add(title);
		texts.add(tf_title);
		texts.add(content);
		texts.add(tf_content);
		add(texts, BorderLayout.NORTH);
		
		JPanel consoles = new JPanel();
		consoles.setLayout(new BoxLayout(consoles, BoxLayout.Y_AXIS));
		files.setEditable(false);
		files.setRows(10);
		console.setEditable(false);
		console.setRows(15);
		JScrollPane jsc_files = new JScrollPane(files, JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED, JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
		JScrollPane jsc_console = new JScrollPane(console, JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED, JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
		consoles.add(Box.createVerticalStrut(5));
		consoles.add(Box.createHorizontalStrut(5));
		consoles.add(jsc_files);
		consoles.add(Box.createVerticalStrut(5));
		consoles.add(Box.createHorizontalStrut(5));
		consoles.add(jsc_console);
		add(consoles, BorderLayout.CENTER);
		
		JPanel buttons = new JPanel(new BorderLayout());
		openConfig.addActionListener(i -> {
			try {
				Desktop.getDesktop().open(new File(MailSender.projectPath + "config.txt"));
			} catch (IOException e) {
				SwingDialogs.error("Cannot open config file!", "%e%", e, false);
			}
		});
		openDropbox.addActionListener(i -> {
			try {
				File f = new File(MailSender.projectPath + "dropboxAuth.txt");
				if(!f.exists()) DropboxFileUploader.createDropboxAuth();
				Desktop.getDesktop().open(f);
			} catch (IOException e) {
				SwingDialogs.error("Cannot open dropboxAuth file!", "%e%", e, false);
			}
		});
		openAppFolder.addActionListener(i -> {
			try {
				File f = new File(MailSender.projectPath);
				Desktop.getDesktop().open(f);
			} catch (IOException e) {
				SwingDialogs.error("Cannot open app folder!", "%e%", e, false);
			}
		});
		JPanel bottum_p1 = new JPanel();
		bottum_p1.add(openConfig);
		bottum_p1.add(openDropbox);
		bottum_p1.add(openAppFolder);
		JPanel bottum_p2 = new JPanel();
		bottum_p2.add(maxAttat);
		bottum_p2.add(tf_maxAttat);
		buttons.add(bottum_p1, BorderLayout.NORTH);
		buttons.add(bottum_p2, BorderLayout.SOUTH);
		add(buttons, BorderLayout.SOUTH);
		
		pack();
		setLocation(dim.width / 2 - getSize().width - chooser.getPreferredSize().width / 2, dim.height / 2 - getSize().height / 2);
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setTitle("PMailSender");
        Image image;
		final File imageFile = new File(JarPath.getProjectPath(MailSender.class) + "/icon.png");
		try {
			image = ImageIO.read(imageFile);
			Taskbar.getTaskbar().setIconImage(image);
		} catch (IOException e) {
			SwingDialogs.warning("Unable to find the icon image file!", "%e%\n" + imageFile.getAbsolutePath() + "\nDoes not exist! Default Java icon will be used...", e, false);
			image = null;
		}
        setIconImage(image);
        setVisible(true);
	}
	

	private void setDialog() {
		
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
		dialog.setAlwaysOnTop(true);
		
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
	
	public void disableInputs() {
		tf_title.setEnabled(false);
		tf_content.setEnabled(false);
	}
	public String getTitle() { return tf_title.getText(); }
	public String getContent() { return tf_content.getText(); }
	
	public void log() {
		log("\n");
	}
	public void log(String str) {
		SwingUtilities.invokeLater(() -> {
			console.append(str);
			console.append("\n");
			adjustSize(console);
		});
		logger.log(str);
	}

	public void log(Exception e) {
		logger.log(e);
		StringWriter sw = new StringWriter();
		PrintWriter pw = new PrintWriter(sw);
		e.printStackTrace(pw);
		pw.close(); pw.close();
		log(sw.toString());
	}
	public void selectedFiles(Collection<File> f) {
		SwingUtilities.invokeLater(() -> {
			files.setText("Selected Files(" + formatSize(f.stream().mapToLong(File::length).sum()) + ") :\n");
			files.append(f.stream().map(File::getAbsolutePath).collect(Collectors.joining("\n")));
			adjustSize(files);
		});
	}

	private void adjustSize(JTextArea jta) {
		int h = jta.getPreferredSize().height - jta.getHeight();
		int w = jta.getPreferredSize().width - jta.getWidth();
		if(h > 0) jta.setRows((int)jta.getText().lines().count() + 1);
		h = h > 0 ? h : 0;
		w = w > 0 ? w : 0;
		w = getX() - w / 2;
		w = w > 0 ? w : 0;
		pack();
		setLocation(w, dim.height / 2 - getSize().height / 2);
	}

	public List<File> chooseLoop(File startPath) {
		LinkedHashSet<File> list = new LinkedHashSet<>();
		
		while (true) {
			chooser.setCurrentDirectory(startPath);
			if (chooser.showOpenDialog(dialog) != JFileChooser.APPROVE_OPTION) break;
			List<File> temp = Arrays.asList(chooser.getSelectedFiles());
			startPath = temp.get(temp.size() - 1);
			if(list.stream().anyMatch(temp::contains)) log("[WARNING] Same files will not be attached multiple times!");
			list.addAll(temp);
			selectedFiles(list);
		}

		dialog.dispose();
		disableInputs();
		return new LinkedList<>(list);
	}
	

	public long getAttatchLimit() {
		AtomicInteger value = new AtomicInteger(10);
		try {
			SwingUtilities.invokeAndWait(() -> {
				try {
					value.set(Integer.parseInt(tf_maxAttat.getText().strip()));
				} catch (NumberFormatException e) {
					SwingDialogs.error("Invalid integer!", "\"" + tf_maxAttat.getText() + "\"" + " is invalid number!\nConsidering max attachment value as 10MB...", null, true);
				}
			});
		} catch (InvocationTargetException | InterruptedException e) {
			SwingDialogs.error("Invalid integer!", "\"" + tf_maxAttat.getText() + "\"" + " is invalid number!\nConsidering max attachment value as 10MB...", null, true);
		}
		return value.get() * 1024 * 1024;
	}


	private String formatSize(long fileSize) {
		if(fileSize == 0L) return "0.00byte";
		
		switch ((int)(Math.log(fileSize) / Math.log(1024))) {
		case 0:
			return String.format("%d", fileSize) + "byte";
		case 1:
			return String.format("%.2f", fileSize / 1024.0) + "KB";
		case 2:
			return String.format("%.2f", fileSize / (1024.0 * 1024)) + "MB";
		case 3:
			return String.format("%.2f", fileSize / (1024.0 * 1024 * 1024)) + "GB";
		}
		return String.format("%.2f", fileSize / (1024.0 * 1024 * 1024 * 1024)) + "TB";
	}

	@Override
	public void dispose() {
		dialog.dispose();
		super.dispose();
	}

}
