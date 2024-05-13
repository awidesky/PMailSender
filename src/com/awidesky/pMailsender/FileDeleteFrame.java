package com.awidesky.pMailsender;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.io.File;
import java.util.List;
import java.util.function.Consumer;

import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JFrame;
import javax.swing.JPanel;

public class FileDeleteFrame  extends JFrame {
	
	private static final long serialVersionUID = 7916525836590817176L;

	public FileDeleteFrame(List<File> files, Consumer<String> logger, Runnable callback, int x, int y)  {
		super("Delete attatchment(s)");
		setLayout(new BorderLayout(10, 10));

		JPanel p = new JPanel();
		p.setLayout(new BoxLayout(p, BoxLayout.Y_AXIS));
		List<JCheckBox> list = files.stream().map(File::getAbsolutePath).map(JCheckBox::new).toList();
		list.forEach(p::add);
		
		JPanel btns = new JPanel();
		JButton cancel = new JButton("cancel");
		JButton ok = new JButton("delete selected");
		cancel.addActionListener((e) -> {
			closeFrame();
		});
		ok.addActionListener((e) -> {
			List<String> selected = list.stream().filter(JCheckBox::isSelected).map(JCheckBox::getText).toList();
			if (!selected.isEmpty()) {
				logger.accept("Deleted Files :");
				selected.stream().map("    "::concat).forEach(logger::accept);
				files.removeIf(selected.stream().map(File::new).toList()::contains);
			}
			closeFrame();
			callback.run();
		});
		FlowLayout fl = new FlowLayout();
		fl.setHgap(30);
		btns.setLayout(fl);
		btns.add(cancel);
		btns.add(ok);
		
		add(p, BorderLayout.CENTER);
		add(btns, BorderLayout.SOUTH);
		
		pack();
		setLocation(x - getPreferredSize().width / 2, y);
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        setVisible(true);
        
	}

	private void closeFrame() {
		setVisible(false);
		dispose();		
	}
	
}
