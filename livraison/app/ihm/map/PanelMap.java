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
			// dessine l'entrepot 
			dessineTour(g2);
			// dessiner les zone de stockage
			dessineZones(g2);
			// dessiner les lots
			dessineLots(g2);
		}
	}

	private void dessineTour(Graphics2D g2)
	{
		g2.setColor(new Color(220, 220, 220));
		g2.fillRect(0, 0, getWidth(), getHeight());
	}

	public void dessineZones(Graphics2D g2)
	{
		// dessiner toute les Zone A,B,C,LTS,HD
		// zone A
		g2.setColor(new Color(200, 230, 255));
		g2.fillRect(50, 50, 200, 200);
		// zone B
		g2.setColor(new Color(200, 255, 200));
		g2.fillRect(400, 50, 100, 200);
		// zone LTS
		g2.setColor(new Color(255, 200, 200));
		g2.fillRect(20, 430, 500, 70);
		// zone REPOS
		g2.setColor(new Color(255, 255, 200));
		g2.fillRect(650, 100, 200, 300);
	}

	public void dessineLots(Graphics2D g2)
	{
		// dessiner les lots dans les zones
		for (Lot lot : ctrl.getLots())
		{
			String zone = ""+lot.getEmplacement().charAt(0);
			Point pos = null;

			switch (zone)
			{
				case "A": pos = new Point(60, 60 ); break;
				case "B": pos = new Point(410, 60); break;
				case "L": pos = new Point(30, 440); break;
				case "H": pos = new Point(30, 440); break;
			}

			if (pos != null)
			{
				g2.setColor(new Color(100, 100, 255));
				g2.fillRect(pos.x, pos.y, 20, 20);
				lot.setPosX(pos.x);
				lot.setPosY(pos.y);
				g2.setColor(Color.BLACK);
			}
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