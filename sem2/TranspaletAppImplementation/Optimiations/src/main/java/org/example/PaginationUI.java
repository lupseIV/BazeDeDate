package org.example;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.List;

public class PaginationUI extends JFrame {

    private final PaginationService service = new PaginationService(HibernateUtil.getSessionFactory());

    // ── Offset state ──────────────────────────────────────────────────────────
    private int  currentPage   = 0;
    private long totalElements = 0;
    private long totalPages    = 0;

    // ── Keyset state ──────────────────────────────────────────────────────────
    private final Deque<Long> cursorHistory    = new ArrayDeque<>();
    private long              currentCursor   = 0L;
    private Page<Employee>    currentKeysetPage;

    // ── Shared state ──────────────────────────────────────────────────────────
    private boolean offsetMode = true;
    private int     pageSize   = 25;

    // ── Components ────────────────────────────────────────────────────────────
    private JRadioButton       rbOffset, rbKeyset;
    private JComboBox<Integer> cmbSize;
    private JLabel             lblPage, lblTotal;
    private JButton            btnPrev, btnNext, btnBenchmark;
    private DefaultTableModel  tableModel;
    private JTextArea          txtBench;

    public PaginationUI() {
        super("Paginare Employees - Strategii Comparate");
        setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
        addWindowListener(new WindowAdapter() {
            @Override public void windowClosing(WindowEvent e) {
                HibernateUtil.close();
                dispose();
            }
        });

        setLayout(new BorderLayout());
        add(buildTopPanel(),    BorderLayout.NORTH);
        add(buildTablePanel(),  BorderLayout.CENTER);
        add(buildBottomPanel(), BorderLayout.SOUTH);

        setPreferredSize(new Dimension(1050, 720));
        pack();
        setLocationRelativeTo(null);
        loadPage();
    }

    // ── TOP: strategy selector + page-size combo ──────────────────────────────

    private JPanel buildTopPanel() {
        JPanel p = new JPanel(new FlowLayout(FlowLayout.LEFT, 12, 6));
        p.setBorder(BorderFactory.createTitledBorder("Configurare"));

        rbOffset = new JRadioButton("Strategie A - Offset  (LIMIT/OFFSET)", true);
        rbKeyset = new JRadioButton("Strategie B - Keyset  (Cursor/Seek)");
        ButtonGroup grp = new ButtonGroup();
        grp.add(rbOffset);
        grp.add(rbKeyset);

        rbOffset.addActionListener(e -> { if (!offsetMode) { offsetMode = true;  reset(); } });
        rbKeyset.addActionListener(e -> { if (offsetMode)  { offsetMode = false; reset(); } });

        cmbSize = new JComboBox<>(new Integer[]{10, 25, 50, 100});
        cmbSize.setSelectedItem(pageSize);
        cmbSize.addActionListener(e -> {
            pageSize = (Integer) cmbSize.getSelectedItem();
            reset();
        });

        lblTotal = new JLabel("Total: -");

        p.add(rbOffset);
        p.add(rbKeyset);
        p.add(new JSeparator(SwingConstants.VERTICAL));
        p.add(new JLabel("Dimensiune pagina:"));
        p.add(cmbSize);
        p.add(Box.createHorizontalStrut(20));
        p.add(lblTotal);
        return p;
    }

    // ── CENTER: employee table ─────────────────────────────────────────────────

    private JScrollPane buildTablePanel() {
        tableModel = new DefaultTableModel(
                new String[]{"#", "ID", "Nume", "Email", "Dept ID", "Salariu"}, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };

        JTable table = new JTable(tableModel);
        table.setFillsViewportHeight(true);
        table.setRowHeight(22);
        table.getTableHeader().setReorderingAllowed(false);

        int[] widths = {45, 60, 160, 290, 65, 100};
        for (int i = 0; i < widths.length; i++)
            table.getColumnModel().getColumn(i).setPreferredWidth(widths[i]);

        return new JScrollPane(table);
    }

    // ── BOTTOM: navigation + benchmark ────────────────────────────────────────

    private JPanel buildBottomPanel() {
        JPanel p = new JPanel(new BorderLayout(5, 4));
        p.setBorder(new EmptyBorder(4, 8, 8, 8));

        // Navigation row
        btnPrev = new JButton("< Anterior");
        btnNext = new JButton("Urmator >");
        lblPage = new JLabel("Pagina 1", SwingConstants.CENTER);
        lblPage.setFont(lblPage.getFont().deriveFont(Font.BOLD, 13f));
        lblPage.setPreferredSize(new Dimension(220, 24));

        btnPrev.addActionListener(e -> prevPage());
        btnNext.addActionListener(e -> nextPage());

        JPanel nav = new JPanel(new FlowLayout(FlowLayout.CENTER, 20, 4));
        nav.add(btnPrev);
        nav.add(lblPage);
        nav.add(btnNext);
        p.add(nav, BorderLayout.NORTH);
        return p;
    }

    // ── Data loading ──────────────────────────────────────────────────────────

    private void loadPage() {
        if (offsetMode) loadOffset();
        else            loadKeyset();
    }

    private void loadOffset() {
        Page<Employee> page = service.getPage(currentPage, pageSize);
        totalElements = page.getTotalElements();
        totalPages    = page.getTotalPages();

        populate(page.getContent(), (long) currentPage * pageSize);
        lblPage.setText(String.format("Pagina %d din %d", currentPage + 1, totalPages));
        lblTotal.setText(String.format("Total: %,d inregistrari", totalElements));
        btnPrev.setEnabled(currentPage > 0);
        btnNext.setEnabled(page.hasNext());
    }

    private void loadKeyset() {
        currentKeysetPage = service.getPageAfter(currentCursor, pageSize);
        int pageNum = cursorHistory.size() + 1;

        populate(currentKeysetPage.getContent(), (long)(pageNum - 1) * pageSize);
        String suffix = currentKeysetPage.hasNext() ? "" : " (ultima)";
        lblPage.setText(String.format("Pagina %d%s", pageNum, suffix));
        lblTotal.setText("Keyset - navigare secventiala");
        btnPrev.setEnabled(!cursorHistory.isEmpty());
        btnNext.setEnabled(currentKeysetPage.hasNext());
    }

    private void populate(List<Employee> rows, long firstRowNum) {
        tableModel.setRowCount(0);
        for (int i = 0; i < rows.size(); i++) {
            Employee e = rows.get(i);
            tableModel.addRow(new Object[]{
                firstRowNum + i + 1,
                e.getId(),
                e.getName(),
                e.getEmail(),
                e.getDepartmentId(),
                String.format("%,.0f", e.getSalary())
            });
        }
    }

    // ── Navigation ────────────────────────────────────────────────────────────

    private void nextPage() {
        if (offsetMode) {
            currentPage++;
            loadOffset();
        } else {
            cursorHistory.push(currentCursor);
            currentCursor = currentKeysetPage.getLastId();
            loadKeyset();
        }
    }

    private void prevPage() {
        if (offsetMode) {
            if (currentPage > 0) { currentPage--; loadOffset(); }
        } else {
            if (!cursorHistory.isEmpty()) {
                currentCursor = cursorHistory.pop();
                loadKeyset();
            }
        }
    }

    private void reset() {
        currentPage   = 0;
        currentCursor = 0L;
        cursorHistory.clear();
        loadPage();
    }


    // ── Entry point ───────────────────────────────────────────────────────────

    public static void launch() {
        SwingUtilities.invokeLater(() -> new PaginationUI().setVisible(true));
    }
}
