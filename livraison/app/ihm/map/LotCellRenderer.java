package app.ihm.map;

import java.awt.Color;
import java.awt.Component;
import javax.swing.BorderFactory;
import javax.swing.DefaultListCellRenderer;
import javax.swing.JLabel;
import javax.swing.JList;

/**
 * Renderer personnalisé pour la liste des lots.
 */
public class LotCellRenderer extends DefaultListCellRenderer
{
	@Override
	public Component getListCellRendererComponent(JList<?> list, Object value,
			int index, boolean isSelected, boolean cellHasFocus)
	{
		JLabel l = (JLabel) super.getListCellRendererComponent(
			list, value, index, isSelected, cellHasFocus);
		l.setBorder(BorderFactory.createEmptyBorder(4, 6, 4, 6));
		if (!isSelected && index % 2 == 0)
			l.setBackground(new Color(248, 249, 252));
		return l;
	}
}