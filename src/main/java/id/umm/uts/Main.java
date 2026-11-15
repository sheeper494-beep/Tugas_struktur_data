package id.umm.uts;

import com.formdev.flatlaf.FlatDarkLaf;
import id.umm.uts.ds.DynamicArray;
import id.umm.uts.io.CsvExporter;
import id.umm.uts.io.CsvImporter;
import id.umm.uts.model.Putusan;
import id.umm.uts.service.PutusanService;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.io.File;

public class Main extends JFrame {

    private static final String ALL_TYPES = "Semua jenis";

    private static final Color BG_DARK = new Color(15, 23, 42);
    private static final Color BG_SIDEBAR = new Color(22, 30, 49);
    private static final Color BG_CARD = new Color(30, 41, 59);
    private static final Color BG_CARD_ALT = new Color(38, 50, 72);
    private static final Color BORDER_COLOR = new Color(51, 65, 85);
    private static final Color ACCENT_BLUE = new Color(37, 99, 235);
    private static final Color TEXT_PRIMARY = new Color(248, 250, 252);
    private static final Color TEXT_MUTED = new Color(148, 163, 184);

    private final PutusanService service = new PutusanService();
    private DefaultTableModel tableModel;
    private JTable dataTable;
    private final JTextField searchField = new JTextField(15);
    private final JTextField minSentenceField = new JTextField(6);
    private final JComboBox<String> typeFilter = new JComboBox<>();

    private JLabel statTotalKasus;
    private JLabel statKasusBulan;
    private JLabel statJenisTeratas;
    private JLabel statBanding;

    public Main() {
        super("Sistem Manajemen Putusan Narkotika");
        initUI();

        DynamicArray<Putusan> dataAwal = DataDummy.getData();
        for (int i = 0; i < dataAwal.size(); i++) {
            service.tambahPutusan(dataAwal.get(i));
        }
        refreshTypeChoices();
        tampilkanDataKeTabel();
    }

    private void initUI() {
        setSize(1400, 920);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);

        JPanel root = new JPanel(new BorderLayout());
        root.setBackground(BG_DARK);

        JPanel header = new JPanel(new BorderLayout());
        header.setBackground(BG_SIDEBAR);
        header.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 0, 1, 0, BORDER_COLOR),
                new EmptyBorder(14, 24, 14, 24)
        ));
        JLabel title = new JLabel("Sistem Manajemen Putusan Narkotika");
        title.setFont(new Font("Segoe UI", Font.BOLD, 18));
        title.setForeground(TEXT_PRIMARY);
        header.add(title, BorderLayout.WEST);
        root.add(header, BorderLayout.NORTH);

        JPanel sidebar = new JPanel();
        sidebar.setLayout(new BoxLayout(sidebar, BoxLayout.Y_AXIS));
        sidebar.setBackground(BG_SIDEBAR);
        sidebar.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 0, 0, 1, BORDER_COLOR),
                new EmptyBorder(16, 12, 16, 12)
        ));
        sidebar.setPreferredSize(new Dimension(230, 0));

        addButton(sidebar, "Impor Data (CSV)", this::importCsv);
        addButton(sidebar, "Ekspor Data (CSV)", this::exportCsv);
        addButton(sidebar, "Dasbor & Analitik", this::tampilkanDataKeTabel);
        addButton(sidebar, "Tambah Data Baru", this::tambahData);
        addButton(sidebar, "Hapus Arsip Data", this::hapusData);
        addButton(sidebar, "Agregasi & Statistik", this::tampilkanAgregasi);
        addButton(sidebar, "Riwayat Audit", this::tampilkanRiwayatAudit);
        root.add(sidebar, BorderLayout.WEST);

        JPanel contentPanel = new JPanel(new BorderLayout(0, 12));
        contentPanel.setBackground(BG_DARK);
        contentPanel.setBorder(new EmptyBorder(16, 16, 16, 16));

        JPanel statsPanel = new JPanel(new GridLayout(1, 4, 12, 0));
        statsPanel.setOpaque(false);
        statsPanel.setPreferredSize(new Dimension(0, 90));

        statTotalKasus = new JLabel("0");
        statKasusBulan = new JLabel("0");
        statJenisTeratas = new JLabel("-");
        statBanding = new JLabel("0");

        statsPanel.add(createStatCard("Total Kasus", statTotalKasus, new Color(16, 185, 129)));
        statsPanel.add(createStatCard("Kasus Aktif", statKasusBulan, new Color(59, 130, 246)));
        statsPanel.add(createStatCard("Jenis Teratas", statJenisTeratas, new Color(245, 158, 11)));
        statsPanel.add(createStatCard("Jatuh Tempo Banding", statBanding, new Color(239, 68, 68)));
        contentPanel.add(statsPanel, BorderLayout.NORTH);

        JPanel centerContainer = new JPanel(new BorderLayout(0, 10));
        centerContainer.setOpaque(false);

        JPanel controls = new JPanel(new BorderLayout());
        controls.setBackground(BG_CARD);
        controls.setBorder(BorderFactory.createCompoundBorder(
                new LineBorder(BORDER_COLOR, 1, true),
                new EmptyBorder(10, 14, 10, 14)
        ));

        JPanel searchGroup = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        searchGroup.setOpaque(false);
        searchField.putClientProperty("JTextField.placeholderText", "Cari Case ID / Terdakwa...");
        JButton searchButton = new JButton("Cari");
        searchButton.setBackground(ACCENT_BLUE);
        searchButton.setForeground(Color.WHITE);
        searchButton.setFocusPainted(false);
        searchButton.addActionListener(event -> runSafely(this::cariData));

        JLabel lblCari = new JLabel("Pencarian:");
        lblCari.setForeground(TEXT_PRIMARY);
        searchGroup.add(lblCari);
        searchGroup.add(searchField);
        searchGroup.add(searchButton);

        JPanel filterGroup = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        filterGroup.setOpaque(false);
        minSentenceField.putClientProperty("JTextField.placeholderText", "Min vonis");
        JButton filterButton = new JButton("Filter");
        filterButton.setBackground(ACCENT_BLUE);
        filterButton.setForeground(Color.WHITE);
        filterButton.setFocusPainted(false);
        filterButton.addActionListener(event -> runSafely(this::filterData));

        JButton resetButton = new JButton("Reset");
        resetButton.setBackground(BG_SIDEBAR);
        resetButton.setForeground(TEXT_PRIMARY);
        resetButton.setFocusPainted(false);
        resetButton.addActionListener(event -> runSafely(this::tampilkanDataKeTabel));

        JLabel lblJenis = new JLabel("Jenis:");
        lblJenis.setForeground(TEXT_PRIMARY);
        JLabel lblVonis = new JLabel("Min Vonis:");
        lblVonis.setForeground(TEXT_PRIMARY);

        filterGroup.add(lblJenis);
        filterGroup.add(typeFilter);
        filterGroup.add(Box.createHorizontalStrut(5));
        filterGroup.add(lblVonis);
        filterGroup.add(minSentenceField);
        filterGroup.add(filterButton);
        filterGroup.add(resetButton);

        controls.add(searchGroup, BorderLayout.WEST);
        controls.add(filterGroup, BorderLayout.EAST);
        centerContainer.add(controls, BorderLayout.NORTH);

        String[] columns = {"Case ID", "Nomor Perkara", "Terdakwa", "Jenis Narkotika", "Vonis (Bulan)", "Kategori", "Status", "Berat (Gram)"};
        tableModel = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };

        dataTable = new JTable(tableModel);
        dataTable.setBackground(BG_CARD);
        dataTable.setForeground(TEXT_PRIMARY);
        dataTable.setRowHeight(42);
        dataTable.setShowVerticalLines(false);
        dataTable.setGridColor(BORDER_COLOR);
        dataTable.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        dataTable.getTableHeader().setBackground(BG_SIDEBAR);
        dataTable.getTableHeader().setForeground(TEXT_MUTED);
        dataTable.getTableHeader().setFont(new Font("Segoe UI", Font.BOLD, 13));
        dataTable.getTableHeader().setPreferredSize(new Dimension(0, 42));

        dataTable.getColumnModel().getColumn(0).setPreferredWidth(75);
        dataTable.getColumnModel().getColumn(1).setPreferredWidth(250);
        dataTable.getColumnModel().getColumn(2).setPreferredWidth(280);
        dataTable.getColumnModel().getColumn(3).setPreferredWidth(110);
        dataTable.getColumnModel().getColumn(4).setPreferredWidth(100);
        dataTable.getColumnModel().getColumn(5).setPreferredWidth(100);
        dataTable.getColumnModel().getColumn(6).setPreferredWidth(90);
        dataTable.getColumnModel().getColumn(7).setPreferredWidth(100);

        DefaultTableCellRenderer centerRenderer = new DefaultTableCellRenderer();
        centerRenderer.setHorizontalAlignment(JLabel.CENTER);
        dataTable.getColumnModel().getColumn(0).setCellRenderer(centerRenderer);
        dataTable.getColumnModel().getColumn(4).setCellRenderer(centerRenderer);

        DefaultTableCellRenderer rightRenderer = new DefaultTableCellRenderer();
        rightRenderer.setHorizontalAlignment(JLabel.RIGHT);
        dataTable.getColumnModel().getColumn(7).setCellRenderer(rightRenderer);

        dataTable.getColumnModel().getColumn(5).setCellRenderer(new PillBadgeRenderer());
        dataTable.getColumnModel().getColumn(6).setCellRenderer(new PillBadgeRenderer());

        JScrollPane scrollPane = new JScrollPane(dataTable);
        scrollPane.getViewport().setBackground(BG_CARD);
        scrollPane.setBorder(new LineBorder(BORDER_COLOR, 1, true));
        centerContainer.add(scrollPane, BorderLayout.CENTER);

        contentPanel.add(centerContainer, BorderLayout.CENTER);
        root.add(contentPanel, BorderLayout.CENTER);
        setContentPane(root);
    }

    private JPanel createStatCard(String title, JLabel valueLabel, Color indicatorColor) {
        JPanel card = new JPanel(new BorderLayout(8, 4));
        card.setBackground(BG_CARD_ALT);
        card.setBorder(BorderFactory.createCompoundBorder(
                new LineBorder(BORDER_COLOR, 1, true),
                new EmptyBorder(12, 16, 12, 16)
        ));

        JPanel topBar = new JPanel(new BorderLayout());
        topBar.setOpaque(false);
        JLabel titleLbl = new JLabel(title);
        titleLbl.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        titleLbl.setForeground(TEXT_MUTED);
        topBar.add(titleLbl, BorderLayout.WEST);

        JPanel indicator = new JPanel();
        indicator.setBackground(indicatorColor);
        indicator.setPreferredSize(new Dimension(8, 8));
        topBar.add(indicator, BorderLayout.EAST);

        card.add(topBar, BorderLayout.NORTH);

        valueLabel.setFont(new Font("Segoe UI", Font.BOLD, 24));
        valueLabel.setForeground(TEXT_PRIMARY);
        card.add(valueLabel, BorderLayout.CENTER);

        return card;
    }

    private void addButton(JPanel panel, String label, Runnable action) {
        JButton button = new JButton(label);
        button.setHorizontalAlignment(SwingConstants.LEFT);
        button.addActionListener(event -> runSafely(action));
        button.setAlignmentX(LEFT_ALIGNMENT);
        button.setMaximumSize(new Dimension(Integer.MAX_VALUE, 40));
        button.setPreferredSize(new Dimension(200, 40));
        button.setBackground(BG_CARD);
        button.setForeground(TEXT_PRIMARY);
        button.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        button.setFocusPainted(false);
        button.setBorder(new EmptyBorder(0, 12, 0, 12));
        panel.add(button);
        panel.add(Box.createVerticalStrut(6));
    }

    private static class PillBadgeRenderer extends DefaultTableCellRenderer {
        private String badgeType = "";

        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
            super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
            setHorizontalAlignment(JLabel.CENTER);
            setFont(new Font("Segoe UI", Font.BOLD, 12));
            badgeType = value != null ? value.toString() : "";
            setOpaque(false);
            return this;
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            Color pillBg;
            Color textColor;

            if (badgeType.equals("Tinggi")) {
                pillBg = new Color(248, 113, 113, 40);
                textColor = new Color(248, 113, 113);
            } else if (badgeType.equals("Sedang")) {
                pillBg = new Color(251, 191, 36, 40);
                textColor = new Color(251, 191, 36);
            } else if (badgeType.equals("Ringan")) {
                pillBg = new Color(52, 211, 153, 40);
                textColor = new Color(52, 211, 153);
            } else if (badgeType.equals("Final")) {
                pillBg = new Color(96, 165, 250, 40);
                textColor = new Color(96, 165, 250);
            } else {
                pillBg = null;
                textColor = Color.WHITE;
            }

            if (pillBg != null) {
                g2.setColor(pillBg);
                int paddingX = 10;
                int paddingY = 6;
                g2.fillRoundRect(paddingX, paddingY, getWidth() - (paddingX * 2), getHeight() - (paddingY * 2), 16, 16);
            }

            setForeground(textColor);
            super.paintComponent(g2);
            g2.dispose();
        }
    }

    private void importCsv() {
        JFileChooser chooser = new JFileChooser();
        chooser.setDialogTitle("Pilih file CSV untuk diimpor");
        chooser.setFileFilter(new FileNameExtensionFilter("CSV (*.csv)", "csv"));
        if (chooser.showOpenDialog(this) != JFileChooser.APPROVE_OPTION) {
            return;
        }

        DynamicArray<Putusan> dataBaru = CsvImporter.importCsv(chooser.getSelectedFile().getAbsolutePath());
        for (int i = 0; i < dataBaru.size(); i++) {
            service.tambahPutusan(dataBaru.get(i));
        }
        refreshTypeChoices();
        tampilkanDataKeTabel();
        JOptionPane.showMessageDialog(this, dataBaru.size() + " data berhasil diimpor.", "Impor CSV", JOptionPane.INFORMATION_MESSAGE);
    }

    private void exportCsv() {
        JFileChooser chooser = new JFileChooser();
        chooser.setDialogTitle("Simpan data sebagai CSV");
        chooser.setFileFilter(new FileNameExtensionFilter("CSV (*.csv)", "csv"));
        if (chooser.showSaveDialog(this) != JFileChooser.APPROVE_OPTION) {
            return;
        }
        File selected = chooser.getSelectedFile();
        String path = selected.getAbsolutePath();
        if (!path.toLowerCase().endsWith(".csv")) {
            path += ".csv";
        }
        CsvExporter.exportCsv(service.getArsipUtama(), path);
        JOptionPane.showMessageDialog(this, "Data berhasil diekspor ke:\n" + path);
    }

    private void tambahData() {
        JTextField caseId = new JTextField();
        JTextField nomorPerkara = new JTextField();
        JTextField jenisNarkotika = new JTextField();
        JTextField vonisBulan = new JTextField();

        JPanel form = new JPanel(new GridLayout(0, 2, 8, 8));
        form.add(new JLabel("CaseId:"));
        form.add(caseId);
        form.add(new JLabel("Nomor perkara:"));
        form.add(nomorPerkara);
        form.add(new JLabel("Jenis narkotika:"));
        form.add(jenisNarkotika);
        form.add(new JLabel("Vonis (bulan):"));
        form.add(vonisBulan);

        int result = JOptionPane.showConfirmDialog(this, form, "Tambah Putusan", JOptionPane.OK_CANCEL_OPTION);
        if (result != JOptionPane.OK_OPTION) {
            return;
        }

        String id = caseId.getText().trim();
        if (id.isEmpty()) {
            throw new IllegalArgumentException("CaseId wajib diisi.");
        }
        if (!id.startsWith("P")) {
            id = "P" + id;
        }
        int sentence = parseRequiredInteger(vonisBulan.getText(), "Vonis");
        Putusan putusan = new Putusan(
                id,
                nomorPerkara.getText().trim(),
                "PN Mgt",
                null,
                "",
                jenisNarkotika.getText().trim(),
                0.0,
                "-",
                "-",
                0,
                sentence,
                0,
                "",
                "-",
                "-",
                "-");
        service.tambahPutusan(putusan);
        refreshTypeChoices();
        tampilkanDataKeTabel();
    }

    private void hapusData() {
        String caseId = JOptionPane.showInputDialog(this, "Masukkan CaseId yang akan dihapus:");
        if (caseId == null || caseId.trim().isEmpty()) {
            return;
        }
        String normalizedId = caseId.trim();
        if (!normalizedId.startsWith("P")) {
            normalizedId = "P" + normalizedId;
        }
        Putusan found = service.cariBerdasarkanCaseId(normalizedId);
        if (found == null) {
            JOptionPane.showMessageDialog(this, "Putusan tidak ditemukan.");
            return;
        }

        DynamicArray<Putusan> archive = service.getArsipUtama();
        for (int i = 0; i < archive.size(); i++) {
            if (archive.get(i) == found) {
                service.hapusPutusan(i);
                refreshTypeChoices();
                tampilkanDataKeTabel();
                return;
            }
        }
        JOptionPane.showMessageDialog(this, "Putusan tidak ditemukan di arsip.");
    }

    private void cariData() {
        String keyword = searchField.getText().trim();
        if (keyword.isEmpty()) {
            tampilkanDataKeTabel();
            return;
        }
        DynamicArray<Putusan> archive = service.getArsipUtama();
        DynamicArray<Putusan> results = new DynamicArray<>();
        for (int i = 0; i < archive.size(); i++) {
            Putusan p = archive.get(i);
            if (p.getCaseId().toLowerCase().contains(keyword.toLowerCase()) ||
                    p.getTerdakwaKode().toLowerCase().contains(keyword.toLowerCase()) ||
                    p.getNomorPerkara().toLowerCase().contains(keyword.toLowerCase())) {
                results.add(p);
            }
        }
        isiTabel(results);
    }

    private void filterData() {
        String selectedType = (String) typeFilter.getSelectedItem();
        String jenis = ALL_TYPES.equals(selectedType) ? "" : selectedType;
        String minText = minSentenceField.getText().trim();
        int minVonis = minText.isEmpty() ? 0 : parseRequiredInteger(minText, "Vonis minimum");
        DynamicArray<Putusan> results = service.filterGabungan(jenis, 0, 0, minVonis, 0);
        isiTabel(results);
    }

    private void tampilkanAgregasi() {
        JOptionPane.showMessageDialog(this, service.ringkasanAgregasi(), "Agregasi Data", JOptionPane.INFORMATION_MESSAGE);
    }

    private void tampilkanRiwayatAudit() {
        JOptionPane.showMessageDialog(this, service.ringkasanRiwayatAudit(), "Riwayat Audit", JOptionPane.INFORMATION_MESSAGE);
    }

    private void refreshTypeChoices() {
        String selected = (String) typeFilter.getSelectedItem();
        typeFilter.removeAllItems();
        typeFilter.addItem(ALL_TYPES);
        DynamicArray<Putusan> archive = service.getArsipUtama();
        for (int i = 0; i < archive.size(); i++) {
            String type = archive.get(i).getJenisNarkotika();
            if (type != null && !type.isBlank() && !containsType(type)) {
                typeFilter.addItem(type);
            }
        }
        if (selected != null) {
            typeFilter.setSelectedItem(selected);
        }
    }

    private boolean containsType(String candidate) {
        for (int i = 1; i < typeFilter.getItemCount(); i++) {
            if (candidate.equalsIgnoreCase(typeFilter.getItemAt(i))) {
                return true;
            }
        }
        return false;
    }

    private void tampilkanDataKeTabel() {
        searchField.setText("");
        minSentenceField.setText("");
        typeFilter.setSelectedItem(ALL_TYPES);
        isiTabel(service.getArsipUtama());
    }

    private void isiTabel(DynamicArray<Putusan> data) {
        tableModel.setRowCount(0);
        if (data != null) {
            for (int i = 0; i < data.size(); i++) {
                Putusan p = data.get(i);
                int vonis = p.getVonisBulan();
                String kategori = vonis <= 12 ? "Ringan" : (vonis <= 60 ? "Sedang" : "Tinggi");

                tableModel.addRow(new Object[]{
                        p.getCaseId(),
                        p.getNomorPerkara(),
                        p.getTerdakwaKode(),
                        p.getJenisNarkotika(),
                        vonis + " bulan",
                        kategori,
                        "Final",
                        String.format("%.2f", p.getBeratBarangBuktiGram())
                });
            }
            statTotalKasus.setText(String.valueOf(data.size()));
            statKasusBulan.setText(String.valueOf(data.size()));
            statJenisTeratas.setText(data.size() > 0 ? "Sabu" : "-");
            statBanding.setText("0");
        }
    }

    private int parseRequiredInteger(String value, String fieldName) {
        try {
            return Integer.parseInt(value.trim());
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException(fieldName + " harus berupa bilangan bulat.");
        }
    }

    private void runSafely(Runnable action) {
        try {
            action.run();
        } catch (RuntimeException e) {
            JOptionPane.showMessageDialog(this, e.getMessage(), "Operasi gagal", JOptionPane.ERROR_MESSAGE);
        }
    }

    public static void main(String[] args) {
        FlatDarkLaf.setup();
        UIManager.put("Button.arc", 8);
        UIManager.put("Component.arc", 8);
        UIManager.put("TextComponent.arc", 8);

        SwingUtilities.invokeLater(() -> new Main().setVisible(true));
    }
}
