package com.awidesky.pMailsender;

import java.awt.Dimension;
import java.awt.Image;
import java.awt.image.BufferedImage;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.File;
import java.io.IOException;

import javax.imageio.ImageIO;
import javax.swing.ImageIcon;
import javax.swing.JFileChooser;
import javax.swing.JLabel;

public class ImageViewer extends JLabel implements PropertyChangeListener {


	private static final long serialVersionUID = -9022451413854391431L;
	
	private static double PREFERRED_WIDTH_RATIO = 0.0;
	private static double PREFERRED_HEIGHT_RATIO = 0.0;
	
	private JFileChooser ch;
	private File file = null;
	
	public  ImageViewer(JFileChooser chooser) {

		ch = chooser;
		
		setVerticalAlignment(JLabel.CENTER);
	    setHorizontalAlignment(JLabel.CENTER);
	    setPreferredSize(new Dimension(240, 160));
	    
	    PREFERRED_WIDTH_RATIO = 0.35;
	    PREFERRED_HEIGHT_RATIO = 0.4;
	    
	}
	
	@Override
	public void propertyChange(PropertyChangeEvent arg0) {

		if (arg0.getPropertyName().equals(JFileChooser.SELECTED_FILE_CHANGED_PROPERTY)) {
			loadImage( (file = (File)arg0.getNewValue()) );
		}
		
	}
	
	public void dialogSizeChange() {
		
		setPreferredSize(new Dimension(getPreferredWidth(), getPreferredHeight()));
		loadImage(file);
		
	}
	
	private void loadImage(File f) {

		try {
			if (f == null) return;
			BufferedImage bi = ImageIO.read(f);
			if (bi == null) return;

			ImageIcon icon = new ImageIcon(bi);
			icon = new ImageIcon(icon.getImage().getScaledInstance(-1, getHeight(), Image.SCALE_DEFAULT));
			if (icon.getIconWidth() > getPreferredWidth()) {
				icon = new ImageIcon(icon.getImage().getScaledInstance(getWidth(), -1, Image.SCALE_DEFAULT));
			}
			setIcon(icon);
		} catch (IOException e) {
			e.printStackTrace();
		}

	}

	public int getPreferredWidth() {
		return (int)(ch.getWidth() * PREFERRED_WIDTH_RATIO);
	}
	
	public int getPreferredHeight() {
		return (int)(ch.getHeight() * PREFERRED_HEIGHT_RATIO);
	}

}
