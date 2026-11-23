package org.yourcompany.yourproject.telas;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.GridLayout;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import org.yourcompany.yourproject.GerenciadorAlarme;
import org.yourcompany.yourproject.HistoricoMedicacao;

/**
 * Histórico de medicação dos últimos 7 dias (RF20):
 * cada vez que o alarme tocou, se a dose foi tomada ou não.
 */
public class TelaHistorico extends TelaBase {

    private static final DateTimeFormatter FORMATO_DIA =
            DateTimeFormatter.ofPattern("EEE, dd/MM", Locale.of("pt", "BR"));
    private static final DateTimeFormatter FORMATO_HORA = DateTimeFormatter.ofPattern("HH:mm");

    private final GerenciadorAlarme gerenciadorAlarme;
    private DefaultTableModel modelo;
    private JLabel lblResumo;

    public TelaHistorico(GerenciadorAlarme gerenciadorAlarme) {
        super("Histórico da Semana", 900, 600);
        this.gerenciadorAlarme = gerenciadorAlarme;
    }

    @Override
    public void inicializarComponentes() {
        lblResumo = new JLabel();
        lblResumo.setFont(FONTE_DESTAQUE);
        JPanel topo = new JPanel(new GridLayout(2, 1, 0, 4));
        topo.setOpaque(false);
        topo.add(criarTitulo("Histórico dos últimos 7 dias"));
        topo.add(lblResumo);
        conteudo.add(topo, BorderLayout.NORTH);

        modelo = criarModeloTabela("Dia", "Horário", "Remédio", "Situação");
        JTable tabela = criarTabela(modelo);
        tabela.getColumnModel().getColumn(0).setPreferredWidth(190);
        tabela.getColumnModel().getColumn(1).setPreferredWidth(110);
        tabela.getColumnModel().getColumn(2).setPreferredWidth(300);
        tabela.getColumnModel().getColumn(3).setPreferredWidth(190);
        tabela.getColumnModel().getColumn(3).setCellRenderer(new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable t, Object valor, boolean selecionado,
                                                           boolean foco, int linha, int coluna) {
                super.getTableCellRendererComponent(t, valor, selecionado, foco, linha, coluna);
                setFont(FONTE_DESTAQUE);
                boolean tomado = String.valueOf(valor).startsWith("Tomado");
                setForeground(selecionado ? Color.WHITE : (tomado ? VERDE : VERMELHO));
                return this;
            }
        });
        conteudo.add(new JScrollPane(tabela), BorderLayout.CENTER);

        JButton btnVoltar = criarBotao("Voltar", CINZA);
        btnVoltar.addActionListener(e -> fechar());
        JPanel botoes = new JPanel(new BorderLayout());
        botoes.setOpaque(false);
        botoes.add(btnVoltar, BorderLayout.WEST);
        conteudo.add(botoes, BorderLayout.SOUTH);
    }

    @Override
    public void atualizarTela() {
        modelo.setRowCount(0);
        LocalDateTime limite = LocalDateTime.now().minusDays(7);

        List<HistoricoMedicacao> semana = new ArrayList<>();
        for (HistoricoMedicacao h : gerenciadorAlarme.getHistorico()) {
            if (h.getDataHoraRegistro() != null && h.getDataHoraRegistro().isAfter(limite)) {
                semana.add(h);
            }
        }
        semana.sort(Comparator.comparing(HistoricoMedicacao::getDataHoraRegistro).reversed());   // mais recente primeiro

        int tomados = 0;
        for (HistoricoMedicacao h : semana) {
            boolean tomado = "TOMADO".equals(h.getStatus());
            if (tomado) {
                tomados++;
            }
            // Dia da dose: a das 22:00 respondida às 07:00 do dia seguinte é do dia anterior
            LocalDate diaDaDose = (h.getHorarioPrevisto() != null)
                    ? GerenciadorAlarme.dataDaDose(h.getHorarioPrevisto(), h.getDataHoraRegistro())
                    : h.getDataHoraRegistro().toLocalDate();
            String dia = diaDaDose.format(FORMATO_DIA);
            // Horário marcado para a dose (registros antigos só têm a hora da resposta)
            String horario = (h.getHorarioPrevisto() != null)
                    ? h.getHorarioPrevisto().format(FORMATO_HORA)
                    : h.getDataHoraRegistro().format(FORMATO_HORA);
            modelo.addRow(new Object[]{
                Character.toUpperCase(dia.charAt(0)) + dia.substring(1),
                horario,
                (h.getNomeRemedio() != null) ? h.getNomeRemedio() : "-",
                tomado ? "Tomado" : "Não tomado"
            });
        }

        if (semana.isEmpty()) {
            lblResumo.setText("Nenhum alarme tocou nos últimos 7 dias.");
            lblResumo.setForeground(TEXTO);
        } else {
            int naoTomados = semana.size() - tomados;
            lblResumo.setText("Doses tomadas: " + tomados + "    |    Não tomadas: " + naoTomados);
            lblResumo.setForeground(naoTomados == 0 ? VERDE : TEXTO);
        }
    }
}
