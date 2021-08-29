package com.awidesky.pMailsender;

import java.awt.Dimension;
import java.awt.Image;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.File;

import javax.swing.ImageIcon;
import javax.swing.JFileChooser;
import javax.swing.JLabel;

public class ImageViewer extends JLabel implements PropertyChangeListener {


	private static final long serialVersionUID = -9022451413854391431L;
	
	private static final int PREFERRED_WIDTH = 240;
	private static final int PREFERRED_HEIGHT = 160;
	
	public  ImageViewer(JFileChooser chooser) {

		setVerticalAlignment(JLabel.CENTER);
	    setHorizontalAlignment(JLabel.CENTER);
	    chooser.addPropertyChangeListener(this);
	    setPreferredSize(new Dimension(PREFERRED_WIDTH, PREFERRED_HEIGHT));
	    
	}
	
	@Override
	public void propertyChange(PropertyChangeEvent arg0) {

		if (arg0.getPropertyName().equals(JFileChooser.SELECTED_FILE_CHANGED_PROPERTY)) {

			File file = (File)arg0.getNewValue();
			if (file != null) {
				ImageIcon icon = new ImageIcon(file.getPath());
				if (icon.getIconWidth() > PREFERRED_WIDTH) {
					icon = new ImageIcon(icon.getImage().getScaledInstance(PREFERRED_WIDTH, -1, Image.SCALE_SMOOTH));
					if (icon.getIconHeight() > PREFERRED_HEIGHT) {
						icon = new ImageIcon(
								icon.getImage().getScaledInstance(-1, PREFERRED_HEIGHT, Image.SCALE_SMOOTH));
					}
				}
				setIcon(icon);
			}
		}
	}

}
