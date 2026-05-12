package app.ihm.ficheroute;

import app.Controleur;
import app.ihm.IhmUtils;
import app.metier.lot.Lot;
import java.awt.*;
import java.awt.event.*;
import java.util.Arrays;

import javax.swing.*;

/**
 * Carte visuelle représentant un lot dans la fiche de route.
 * Affiche toutes les infos du lot, phases cochables, champs éditables.
 */
public class CarteLot extends JPanel implements ActionListener
{
	// ── Couleurs d'état ────────────────────────────────────────────────
	static final Color BG_FINI    = new Color(220, 250, 220);
	static final Color BG_DOUANE  = new Color(238, 224, 255);
	static final Color BG_URGENCE = new Color(255, 232, 232);
	static final Color BG_NORMAL  = Color.WHITE;

	private final Lot        lot;
	private final Controleur ctrl;
	private final PanelFicheRoute m;

	private JTextField textPcsEtiq;
	private JTextField textPcsPart;

	
	private JTextField textDistri;
	private JTextField textLotCharge;
	private JTextField textFormCart;
	private JTextField textCollisage;
	private JTextField textColisRecup;
	private JTextField textMethode;

	public CarteLot(Lot lot, Color couleurAce, Controleur ctrl,PanelFicheRoute m)
	{
		this.lot  = lot;
		this.ctrl = ctrl;
		this.m    = m;

		Color bg     = bgPourLot(lot);
		Color accent = couleurAce != null ? couleurAce : IhmUtils.BLEU;

		setLayout(new BorderLayout());
		setBackground(bg);
		setBorder(BorderFactory.createCompoundBorder(
			BorderFactory.createEmptyBorder(3, 8, 3, 8),
			BorderFactory.createCompoundBorder(
				BorderFactory.createMatteBorder(0, 5, 0, 0, accent),
				BorderFactory.createCompoundBorder(
					BorderFactory.createLineBorder(new Color(215, 220, 228)),
					BorderFactory.createEmptyBorder(7, 10, 7, 10)
				)
			)
		));

		JPanel corps = new JPanel();
		corps.setLayout(new BoxLayout(corps, BoxLayout.Y_AXIS));
		corps.setBackground(bg);

		corps.add(construireLigne1(bg, accent));
		corps.add(Box.createVerticalStrut(5));
		corps.add(separateur());
		corps.add(construireLigne2(bg));
		corps.add(separateur());
		corps.add(construireLigne3Phases(bg, accent));
		corps.add(separateur());
		corps.add(construireLigne4Avancement(bg));
		corps.add(separateur());
		corps.add(construireLigne5Logistique(bg));
		corps.add(separateur());
		corps.add(construireCommentaire(bg));

		add(corps, BorderLayout.CENTER);
		setMaximumSize(new Dimension(Integer.MAX_VALUE, getPreferredSize().height + 16));
	}

	// ══════════════════════════════════════════════════════════════════
	// Lignes de contenu
	// ══════════════════════════════════════════════════════════════════

	private JPanel construireLigne1(Color bg, Color accent)
	{
		JPanel l1 = new JPanel(new BorderLayout(6, 0));
		l1.setBackground(bg);

		JPanel gauche = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
		gauche.setBackground(bg);

		int   prio   = lot.getPriorite();
		Color prioBg = prio >= 8 ? IhmUtils.ROUGE : prio >= 5 ? IhmUtils.AMBER : new Color(80, 130, 80);
		gauche.add(badge(" P" + prio + " ", prioBg));

		JLabel num = new JLabel("N° " + lot.getNumCDE());
		num.setFont(new Font("SansSerif", Font.BOLD, 14));
		num.setForeground(new Color(20, 55, 120));
		gauche.add(num);

		String desig    = s(lot.getAffaire());
		String typo     = s(lot.getTypologie());
		String affDesig = desig.isEmpty() ? typo : (typo.isEmpty() ? desig : desig + "  —  " + typo);
		JLabel lblDes   = new JLabel(affDesig.isEmpty() ? "(sans désignation)" : affDesig);
		lblDes.setFont(new Font("SansSerif", Font.PLAIN, 13));
		gauche.add(lblDes);

		if (!s(lot.getSemaine()).isEmpty()) {
			JLabel sem = new JLabel("  S" + lot.getSemaine());
			sem.setFont(new Font("SansSerif", Font.PLAIN, 11));
			sem.setForeground(new Color(120, 120, 120));
			gauche.add(sem);
		}

		JPanel droite = new JPanel(new FlowLayout(FlowLayout.RIGHT, 3, 0));
		droite.setBackground(bg);
		if (lot.isEstSousDouane())              droite.add(badge("DOUANE",    new Color(120, 40, 180)));
		if (lot.getPhase().isFinit())           droite.add(badge("✓ TERMINÉ", new Color(30, 130, 50)));
		if (lot.estMachine())                   droite.add(badge("MACHINE",   new Color(0, 100, 160)));
		if (!s(lot.getStatut()).isEmpty())       droite.add(badge(lot.getStatut(), new Color(70, 70, 70)));
		if (!s(lot.getStatutEchant()).isEmpty()) droite.add(badge("Éch: " + lot.getStatutEchant(), new Color(50, 90, 160)));

		l1.add(gauche, BorderLayout.CENTER);
		l1.add(droite, BorderLayout.EAST);
		return l1;
	}

	private JPanel construireLigne2(Color bg)
	{
		JPanel l2 = new JPanel(new FlowLayout(FlowLayout.LEFT, 16, 2));
		l2.setBackground(bg);
		info(l2, "VVS",      lot.getValeurVente() > 0 ? fmt(lot.getValeurVente()) + " €" : "—", bg);
		info(l2, "Pièces",   fmt(lot.getNbPieces()), bg);
		info(l2, "PU",       lot.getPrixUnitaire() > 0 ? String.format("%.2f €", lot.getPrixUnitaire()) : "—", bg);
		info(l2, "Cadence",  lot.getCadence() > 0 ? String.format("%.0f p/h", lot.getCadence()) : "—", bg);
		info(l2, "H. Total", lot.getHeures() > 0 ? String.format("%.1f h", lot.getHeures()) : "—", bg);
		info(l2, "H. sur piste",   lot.getHeuresAce() > 0 ? String.format("%.1f h", lot.getHeuresAce()) : "—", bg);
		if (!s(lot.getDateReception()).isEmpty()) info(l2, "Réception", lot.getDateReception(), bg);
		if (!s(lot.getDatePaiement()).isEmpty())  info(l2, "Paiement",  lot.getDatePaiement(),  bg);
		return l2;
	}

	private JPanel construireLigne3Phases(Color bg, Color accent)
	{
		JPanel l3 = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 2));
		l3.setBackground(bg);

		JLabel titPhases = new JLabel("Phases :");
		titPhases.setFont(new Font("SansSerif", Font.BOLD, 11));
		titPhases.setForeground(Color.GRAY);
		l3.add(titPhases);

		l3.add(checkPhase("PRÉ TRI",     lot.getPhase().isPreTri(),     "PRETRI",   accent, bg));
		l3.add(checkPhase("SUR PISTE",   lot.getPhase().isSurPiste(),   "SURPISTE", accent, bg));
		l3.add(checkPhase("SORTIE ÉTIQ", lot.getPhase().isSortieEtiq(), "SORETIQ",  accent, bg));
		l3.add(checkPhase("TRI",         lot.getPhase().isTri(),        "TRI",      accent, bg));
		l3.add(checkPhase("FINI",        lot.getPhase().isFinit(),      "FINI",     accent, bg));

		l3.add(Box.createHorizontalStrut(6));
		int nb = (lot.getPhase().isPreTri()     ? 1 : 0)
		       + (lot.getPhase().isSurPiste()   ? 1 : 0)
		       + (lot.getPhase().isSortieEtiq() ? 1 : 0)
		       + (lot.getPhase().isTri()         ? 1 : 0)
		       + (lot.getPhase().isFinit()        ? 1 : 0);
		l3.add(barreProg(nb, 5, 80));
		return l3;
	}

	private JPanel construireLigne4Avancement(Color bg)
	{
		JPanel l4 = new JPanel(new FlowLayout(FlowLayout.LEFT, 16, 2));
		l4.setBackground(bg);

		this.textPcsEtiq = new JTextField(String.valueOf(lot.getSuivieProd().getNbPieceEtiq()), 6);
		l4.add(champEditable("Pces Étiq.",this.textPcsEtiq,bg,"PCS_ETIQ",this));
		info(l4, "Av. Étiq %",   lot.getSuivieProd().getAvancementEtiqPct(), bg);
		info(l4, "H Étiq rest.", lot.getSuivieProd().getNbHeureEtiqRestant() + " h", bg);

		JLabel sep = new JLabel("|");
		sep.setForeground(new Color(190, 190, 190));
		l4.add(sep);

		this.textPcsPart = new JTextField(String.valueOf(lot.getSuivieProd().getNbPieceRepart()), 6);
		l4.add(champEditable("Pces Parts",this.textPcsPart,bg,"PCS_PART",this));
		info(l4, "Av. Parts %",   lot.getSuivieProd().getAvancementPartsPct(),   bg);
		info(l4, "H Parts rest.", lot.getSuivieProd().getNbHeureRepartRestant() + " h", bg);
		return l4;
	}

	private JPanel construireLigne5Logistique(Color bg)
	{
		JPanel l5 = new JPanel(new GridLayout(2, 4, 4, 2));
		l5.setBackground(bg);

		this.textDistri     = new JTextField(s(lot.getLotACharge()), 10);
		this.textLotCharge  = new JTextField(s(lot.getDistribution()), 10);
		this.textFormCart   = new JTextField(s(lot.getFormatCarton()), 10);
		this.textCollisage  = new JTextField(String.valueOf(lot.getCollisage()),10);
		this.textColisRecup = new JTextField(String.valueOf(lot.getNbColisRecup()), 6);
		this.textMethode    = new JTextField(lot.getMethode() == null ? "" : lot.getMethode().getNom(), 10);

		info(l5, "Emplacement", s(lot.getEmplacement()) , bg);
		l5.add(champEditable("Distribution" , this.textDistri    , bg, "DISTRI"      , this));
		l5.add(champEditable("Lot à charge" , this.textLotCharge , bg, "LOT_CHARGE"  , this));
		l5.add(champEditable("Format carton", this.textFormCart  , bg, "FORM_CART"   , this));
		l5.add(champEditable("Collisage",this.textCollisage,bg,"COLLISAGES",this));
		info(l5, "Palettes", String.valueOf(lot.getNbPalettes()), bg);
		info(l5, "Colis prévus", String.valueOf(lot.getNbColisPrevue()), bg);
		l5.add(champEditable("Colis récup." , this.textColisRecup, bg, "COLIS_RECUP" , this));
		l5.add(champEditable("Méthode"      , this.textMethode   , bg, "METHODE"     , this));
		return l5;
	}

	private JPanel construireCommentaire(Color bg)
	{
		JPanel lCom = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 1));
		lCom.setBackground(bg);

		JLabel ico = new JLabel("\uD83D\uDCAC");
		ico.setFont(new Font("SansSerif", Font.PLAIN, 12));

		boolean vide = s(lot.getCommentaire()).isEmpty();
		JTextField tf = new JTextField(vide ? "Commentaire..." : lot.getCommentaire(), 42);
		tf.setFont(new Font("SansSerif", Font.ITALIC, 12));
		tf.setForeground(vide ? Color.LIGHT_GRAY : Color.DARK_GRAY);
		tf.addFocusListener(new FocusAdapter() {
			@Override public void focusGained(FocusEvent e) {
				if (tf.getText().equals("Commentaire...")) { tf.setText(""); tf.setForeground(Color.DARK_GRAY); }
			}
			@Override public void focusLost(FocusEvent e) {
				String v = tf.getText().trim();
				if (v.isEmpty()) { tf.setText("Commentaire..."); tf.setForeground(Color.LIGHT_GRAY); }
				lot.setCommentaire(v.equals("Commentaire...") ? "" : v);
			}
		});
		tf.setBorder(BorderFactory.createCompoundBorder(
			BorderFactory.createLineBorder(new Color(200, 210, 220)),
			BorderFactory.createEmptyBorder(2, 5, 2, 5)));

		lCom.add(ico);
		lCom.add(tf);
		return lCom;
	}

	// ══════════════════════════════════════════════════════════════════
	// Widgets internes
	// ══════════════════════════════════════════════════════════════════

	private JCheckBox checkPhase(String label, boolean etat, String code, Color accent, Color bg)
	{
		JCheckBox cb = new JCheckBox(label, etat);
		cb.setFont(new Font("SansSerif", Font.PLAIN, 11));
		cb.setOpaque(false);
		cb.setForeground(etat ? new Color(20, 120, 20) : new Color(100, 100, 100));
		cb.addActionListener(e -> {
			boolean v  = cb.isSelected();
			boolean pt = lot.getPhase().isPreTri(),    sp = lot.getPhase().isSurPiste(),
			        se = lot.getPhase().isSortieEtiq(), tr = lot.getPhase().isTri(),
			        fi = lot.getPhase().isFinit();
			switch (code) {
				case "PRETRI":   pt = v; break;
				case "SURPISTE": sp = v; break;
				case "SORETIQ":  se = v; break;
				case "TRI":      tr = v; break;
				case "FINI":     fi = v; break;
			}
			ctrl.modifierPhase(lot, pt, sp, se, tr, fi);
			recolorierCarte();
		});
		return cb;
	}

	/**
	 * Met à jour la couleur de fond de toute la carte quand l'état change
	 * (ex : cochage de FINI → fond devient vert).
	 */
	private void recolorierCarte()
	{
		Color nouvBg = bgPourLot(lot);
		appliquerFond(this, nouvBg);
		revalidate();
		repaint();
	}

	private void appliquerFond(Container c, Color bg)
	{
		c.setBackground(bg);
		for (Component child : c.getComponents())
			if (child instanceof Container) appliquerFond((Container) child, bg);
	}

	// ── Helpers visuels ─────────────────────────────────────────────────

	static JLabel badge(String txt, Color col)
	{
		JLabel l = new JLabel(txt);
		l.setFont(new Font("SansSerif", Font.BOLD, 10));
		l.setOpaque(true);
		l.setBackground(col);
		l.setForeground(Color.WHITE);
		l.setBorder(BorderFactory.createEmptyBorder(2, 5, 2, 5));
		return l;
	}

	static JPanel barreProg(int val, int max, int w)
	{
		JPanel p = new JPanel(null)
		{
			@Override protected void paintComponent(Graphics g)
			{
				super.paintComponent(g);
				Graphics2D g2 = (Graphics2D) g;
				g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
				int W = getWidth(), H = getHeight();
				g2.setColor(new Color(215, 220, 228));
				g2.fillRoundRect(0, 0, W, H, H, H);
				int fill = max > 0 ? (int)(W * val / (double) max) : 0;
				Color c = val == max ? IhmUtils.VERT : val > 0 ? IhmUtils.AMBER : new Color(200, 200, 200);
				if (fill > 0) { g2.setColor(c); g2.fillRoundRect(0, 0, fill, H, H, H); }
			}
		};
		p.setPreferredSize(new Dimension(w, 10));
		p.setOpaque(false);
		return p;
	}

	static void info(JPanel p, String label, String val, Color bg)
	{
		JPanel bloc = new JPanel(new BorderLayout(0, 0));
		bloc.setBackground(bg);
		JLabel l = new JLabel(label);
		l.setFont(new Font("SansSerif", Font.PLAIN, 10));
		l.setForeground(new Color(130, 130, 130));
		JLabel v = new JLabel(val);
		v.setFont(new Font("SansSerif", Font.BOLD, 12));
		v.setForeground(Color.DARK_GRAY);
		bloc.add(l, BorderLayout.NORTH);
		bloc.add(v, BorderLayout.CENTER);
		p.add(bloc);
	}

	static JSeparator separateur()
	{
		JSeparator sep = new JSeparator(SwingConstants.HORIZONTAL);
		sep.setForeground(new Color(220, 225, 232));
		sep.setMaximumSize(new Dimension(Integer.MAX_VALUE, 1));
		return sep;
	}

	static JPanel champEditable(String label, JTextField t, Color bg, String action, ActionListener listener)
	{
		JPanel p = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 0));
		p.setBackground(bg);

		JLabel l = new JLabel(label + " :");
		l.setFont(new Font("SansSerif", Font.BOLD, 11));
		l.setForeground(new Color(90, 90, 90));

		t.setFont(new Font("SansSerif", Font.PLAIN, 12));

		t.setActionCommand(action);
		t.addActionListener(listener);

		p.add(l);
		p.add(t);

		return p;
	}

	// ----- Action Listner ----------

	public void actionPerformed(ActionEvent e)
	{
		String cmd = e.getActionCommand();

		try
		{
			switch (cmd)
			{
				case "PCS_ETIQ":
				{
					int v = Integer.parseInt(textPcsEtiq.getText().trim());

					if (v < 0 || v > lot.getNbPieces())
						throw new NumberFormatException();

					lot.getSuivieProd().setNbPieceEtiq(v);
					textPcsEtiq.setBackground(Color.WHITE);
					break;
				}

				case "PCS_PART":
				{
					int v = Integer.parseInt(textPcsPart.getText().trim());

					if (v < 0 || v > lot.getNbPieces())
						throw new NumberFormatException();

					lot.getSuivieProd().setNbPieceRepart(v);
					textPcsPart.setBackground(Color.WHITE);
					break;
				}

				case "DISTRI":
					lot.setLotACharge(textDistri.getText().trim());
					break;

				case "LOT_CHARGE":
					lot.setDistribution(textLotCharge.getText().trim());
					break;

				case "FORM_CART":
					if (Arrays.asList(Lot.F_CARTON).contains(textFormCart.getText().trim()))
						lot.setFormatCarton(textFormCart.getText().trim());
					break;
				
				case "COLLISAGES":
				{
					int v = Integer.parseInt(textCollisage.getText().trim());

					if (v < 0)
						throw new NumberFormatException();

					lot.setCollisage(v);
				}

				case "COLIS_RECUP":
				{
					int v = Integer.parseInt(textColisRecup.getText().trim());

					if (v < 0)
						throw new NumberFormatException();

					lot.setNbColisRecup(v);
					textColisRecup.setBackground(Color.WHITE);
					break;
				}

				case "METHODE":
					lot.setMethode(textMethode.getText().trim());
					break;
			}
			this.m.rafraichir();
		}
		catch (NumberFormatException ex)
		{
			JTextField tf = (JTextField)e.getSource();
			tf.setBackground(new Color(255, 220, 220));
		}
	}

	// ── Utilitaires statiques ────────────────────────────────────────────

	static Color bgPourLot(Lot lot)
	{
		if (lot.getPhase().isFinit()) return BG_FINI;
		if (lot.isEstSousDouane())    return BG_DOUANE;
		if (lot.getPriorite() >= 8)   return BG_URGENCE;
		return BG_NORMAL;
	}

	private static String s(String v) { return v != null ? v : ""; }
	private static String fmt(int n)  { return String.format("%,d", n); }
}