package app.ihm.automatisation;

import app.Controleur;
import app.ihm.FenetrePrincipale;
import app.ihm.IhmUtils;
import app.metier.lot.Lot;
import app.metier.personelle.Societe;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.List;

public class PanelAuto extends JPanel
{
	private Controleur ctrl;
	private FenetrePrincipale fP;

	private PanelGantt panelGantt;

	public PanelAuto(Controleur ctrl, FenetrePrincipale fP)
	{
		this.ctrl = ctrl;
		this.fP = fP;

		setLayout(new BorderLayout());
		setBackground(IhmUtils.FOND);

		// ================= HEADER SIMPLE =================

		JPanel top = new JPanel(new BorderLayout());
		top.setBackground(new Color(25,25,30));
		top.setBorder(new EmptyBorder(10,15,10,15));

		JLabel title = new JLabel("Gantt Production (Toutes sociétés)");
		title.setForeground(Color.WHITE);
		title.setFont(new Font("Segoe UI", Font.BOLD, 22));

		JButton refresh = new JButton("Actualiser");
		refresh.setFocusable(false);
		refresh.addActionListener(e -> actualiser());

		JPanel right = new JPanel(new FlowLayout(FlowLayout.RIGHT));
		right.setOpaque(false);
		right.add(refresh);

		top.add(title, BorderLayout.WEST);
		top.add(right, BorderLayout.EAST);

		add(top, BorderLayout.NORTH);

		// ================= GANTT =================

		panelGantt = new PanelGantt(ctrl);

		JScrollPane scroll = new JScrollPane(panelGantt);
		scroll.setBorder(null);

		add(scroll, BorderLayout.CENTER);

		actualiser();
	}

	// =====================================================
	// DATA
	// =====================================================

	public void actualiser()
	{
		List<Lot> lots = ctrl.getLots();
		panelGantt.setLots(lots);
	}

	// =====================================================
	// GANTT PANEL
	// =====================================================

	private class PanelGantt extends JPanel
	{
		private final DateTimeFormatter fmt =
			DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");

		private List<Lot> lots = new ArrayList<>();

		private static final int LEFT = 320;
		private static final int TOP = 70;
		private static final int ROW = 60;
		private static final int BAR = 28;
		private static final int HOUR = 45;

		private Controleur ctrl;

		public PanelGantt(Controleur ctrl)
		{
			setBackground(Color.WHITE);
			this.ctrl = ctrl;
		}

		public void setLots(List<Lot> l)
		{
			lots.clear();

			if (l != null)
			{
				for (Lot x : l)
				{
					if (x == null) continue;
					if (x.getDateDebut() == null || x.getDateDebut().isEmpty()) continue;
					lots.add(x);
				}
			}

			lots.sort(Comparator.comparing(this::start));

			revalidate();
			repaint();
		}

		@Override
		public Dimension getPreferredSize()
		{
			if (lots.isEmpty())
				return new Dimension(1400, 500);

			long h = ChronoUnit.HOURS.between(startMin(), endMax()) + 10;

			return new Dimension(
				LEFT + (int)(h * HOUR),
				TOP + lots.size() * ROW + 80
			);
		}

		@Override
		protected void paintComponent(Graphics g)
		{
			super.paintComponent(g);

			Graphics2D g2 = (Graphics2D) g;

			g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
				RenderingHints.VALUE_ANTIALIAS_ON);

			if (lots.isEmpty())
			{
				g2.setColor(Color.GRAY);
				g2.drawString("Aucun lot", 40, 80);
				return;
			}

			drawGrid(g2);
			drawTimeline(g2);
			drawLots(g2);
		}

		private void drawGrid(Graphics2D g2)
		{
			g2.setColor(new Color(245,245,245));
			g2.fillRect(0,0,getWidth(),TOP);

			g2.setColor(new Color(250,250,250));
			g2.fillRect(0,0,LEFT,getHeight());

			g2.setColor(new Color(220,220,220));
			g2.drawLine(LEFT,0,LEFT,getHeight());
			g2.drawLine(0,TOP,getWidth(),TOP);
		}

		private void drawTimeline(Graphics2D g2)
		{
			LocalDateTime min = startMin();

			for (int i = 0; i < 200; i++)
			{
				int x = LEFT + i * HOUR;
				LocalDateTime d = min.plusHours(i);

				g2.setColor(new Color(235,235,235));
				g2.drawLine(x, TOP, x, getHeight());

				g2.setColor(Color.DARK_GRAY);
				g2.drawString(d.getHour() + ":00", x - 10, 20);
			}
		}

		private void drawLots(Graphics2D g2)
		{
			LocalDateTime min = startMin();
			int i = 0;

			for (Lot l : lots)
			{
				int y = TOP + i * ROW;

				LocalDateTime s = start(l);
				LocalDateTime e = end(l);

				long off = ChronoUnit.MINUTES.between(min, s);
				long dur = ChronoUnit.MINUTES.between(s, e);

				int x = LEFT + (int)(off / 60.0 * HOUR);
				int w = Math.max(40, (int)(dur / 60.0 * HOUR));

				boolean done = l.getdateFin() != null && !l.getdateFin().isEmpty();

				g2.setColor(done ? new Color(60,170,90) : new Color(70,130,220));
				g2.fillRoundRect(x, y + 15, w, BAR, 12, 12);

				// affichage société + lot
				Societe soc = ctrl.getSocieteDuLot(l);

				g2.setColor(Color.BLACK);
				g2.drawString("N° CDE " + l.getNumCDE() + " (" + soc.getNom() + ")", 10, y + 30);

				i++;
			}
		}

		private LocalDateTime start(Lot l)
		{
			return LocalDateTime.parse(l.getDateDebut(), fmt);
		}

		private LocalDateTime end(Lot l)
		{
			try
			{
				if (l.getdateFin() != null && !l.getdateFin().isEmpty())
					return LocalDateTime.parse(l.getdateFin(), fmt);

				return LocalDateTime.parse(l.getdateFinT(), fmt);
			}
			catch (Exception e)
			{
				return start(l).plusHours(1);
			}
		}

		private LocalDateTime startMin()
		{
			return lots.stream().map(this::start)
				.min(LocalDateTime::compareTo).orElse(LocalDateTime.now());
		}

		private LocalDateTime endMax()
		{
			return lots.stream().map(this::end)
				.max(LocalDateTime::compareTo).orElse(LocalDateTime.now());
		}
	}
}