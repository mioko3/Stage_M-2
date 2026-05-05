package app.ihm.map;

import app.Controleur;
import app.ihm.FenetrePrincipale;
import app.ihm.IhmUtils;
import app.metier.lot.Lot;
import app.metier.personelle.Societe;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.*;
import java.util.List;
import javax.swing.*;

public class PanelMap extends JPanel
{
	private final Controleur ctrl;
	private final FenetrePrincipale fenetre;

	private JPanel panelDetail;
	private JLabel lblZoneSelectionnee;
	private DefaultListModel<String> listModel;
	private JList<String> listeLots;

	private String zoneSelectionnee = null;

	// ─────────────────────────────────────────────
	// CONSTRUCTEUR
	// ─────────────────────────────────────────────

	public PanelMap(Controleur ctrl, FenetrePrincipale fenetre)
	{
		this.ctrl = ctrl;
		this.fenetre = fenetre;

		setLayout(new BorderLayout(12, 0));
		setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));
		setBackground(IhmUtils.FOND);

		add(creerPlanEntrepot(), BorderLayout.CENTER);
		add(creerPanelDetail(), BorderLayout.EAST);
		add(creerLegende(), BorderLayout.SOUTH);
	}

	// ─────────────────────────────────────────────
	// PLAN (ZONE DESSIN)
	// ─────────────────────────────────────────────

	private JPanel creerPlanEntrepot()
	{
		PlanPanel panel = new PlanPanel();
		panel.setBackground(Color.WHITE);

		// 👉 Gestion du clic sur zones
		panel.addMouseListener(new MouseAdapter()
		{
			public void mouseClicked(MouseEvent e)
			{
				Point p = e.getPoint();

				// Détection simple des zones (coordonnées)
				if (new Rectangle(50, 50, 200, 200).contains(p))
					selectionnerZone("A99");

				else if (new Rectangle(400, 50, 100, 200).contains(p))
					selectionnerZone("B42");

				else if (new Rectangle(20, 430, 500, 70).contains(p))
					selectionnerZone("LTS");

				else if (new Rectangle(650, 100, 200, 300).contains(p))
					selectionnerZone("REPOS");
			}
		});

		return panel;
	}

	// ─────────────────────────────────────────────
	// PANEL DE DESSIN
	// ─────────────────────────────────────────────

	private class PlanPanel extends JPanel
	{
		@Override
		protected void paintComponent(Graphics g)
		{
			super.paintComponent(g);

			Graphics2D g2 = (Graphics2D) g;

			// 🔲 Bâtiment principal
			g2.setColor(Color.BLACK);
			g2.drawRect(20, 20, 600, 350);

			// 🔳 Zone A
			g2.setColor(Color.BLUE);
			g2.drawRect(50, 50, 200, 200);
			g2.drawString("A", 120, 160);

			// 🔳 Zone B
			g2.drawRect(400, 50, 100, 200);
			g2.drawString("B", 420, 160);

			// 🔳 Couloir
			g2.drawRect(20, 380, 600, 40);

			// 🔳 Zone LTS
			g2.drawRect(20, 430, 500, 70);
			g2.drawString("LTS", 240, 470);

			// 🔳 Petite zone
			g2.drawRect(450, 410, 120, 40);

			// 🏢 Bureaux
			g2.setColor(Color.GRAY);
			g2.drawRect(520, 430, 100, 100);

			// 🟫 Repos
			g2.setColor(Color.BLACK);
			g2.drawRect(650, 100, 200, 300);
			g2.drawString("REPOS", 720, 250);
		}
	}

	// ─────────────────────────────────────────────
	// PANEL DROITE (DETAIL)
	// ─────────────────────────────────────────────

	private JPanel creerPanelDetail()
	{
		panelDetail = new JPanel(new BorderLayout(0, 8));
		panelDetail.setBackground(Color.WHITE);
		panelDetail.setPreferredSize(new Dimension(300, 0));

		lblZoneSelectionnee = new JLabel("← Cliquez sur une zone");
		lblZoneSelectionnee.setFont(new Font("SansSerif", Font.BOLD, 13));

		listModel = new DefaultListModel<>();
		listeLots = new JList<>(listModel);

		panelDetail.add(lblZoneSelectionnee, BorderLayout.NORTH);
		panelDetail.add(new JScrollPane(listeLots), BorderLayout.CENTER);

		return panelDetail;
	}

	// ─────────────────────────────────────────────
	// LOGIQUE SELECTION
	// ─────────────────────────────────────────────

	private void selectionnerZone(String code)
	{
		zoneSelectionnee = code;

		List<Lot> lots = getLotsZone(code);

		lblZoneSelectionnee.setText("Zone " + code + " (" + lots.size() + " lots)");

		listModel.clear();

		for (Lot l : lots)
		{
			Societe s = ctrl.getSocieteDuLot(l);
			String nom = (s != null) ? s.getNom() : "?";

			listModel.addElement(l.getNumCDE() + " - " + nom);
		}
	}

	private List<Lot> getLotsZone(String code)
	{
		List<Lot> res = new ArrayList<>();

		for (Lot l : ctrl.getLots())
		{
			if (code.equalsIgnoreCase(l.getEmplacement()))
				res.add(l);
		}
		return res;
	}

	public void rafraichir()
	{
		// Si une zone est sélectionnée → on met à jour la liste
		if (zoneSelectionnee != null)
		{
			selectionnerZone(zoneSelectionnee);
		}

		// Redessine la map
		repaint();
	}

	// ─────────────────────────────────────────────
	// LEGENDE
	// ─────────────────────────────────────────────

	private JPanel creerLegende()
	{
		JPanel p = new JPanel();
		p.setBackground(IhmUtils.FOND);
		p.add(new JLabel("Clique sur une zone"));
		return p;
	}
}