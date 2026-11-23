package org.yourcompany.yourproject.telas;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.util.ArrayList;
import java.util.List;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.SwingConstants;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import org.yourcompany.yourproject.GerenciadorRemedio;
import org.yourcompany.yourproject.Remedio;

/**
 * Gestão de estoque dos remédios (RF21): mostra quanto resta de cada um,
 * destaca em vermelho os que estão acabando e permite registrar uma compra.
 */
public class TelaEstoque extends TelaBase {

    private final GerenciadorRemedio gerenciadorRemedio;
    private List<Remedio> remediosNaTabela = new ArrayList<>();

    private DefaultTableModel modelo;
    private JTable tabela;
    private JLabel lblResumo;

    public TelaEstoque(GerenciadorRemedio gerenciadorRemedio) {
        super("Estoque", 940, 560);
        this.gerenciadorRemedio = gerenciadorRemedio;
    }

    @Override
    public void inicializarComponentes() {
        lblResumo = new JLabel();
        lblResumo.setFont(FONTE_DESTAQUE);
        JPanel topo = new JPanel(new GridLayout(2, 1, 0, 4));
        topo.setOpaque(false);
        topo.add(criarTitulo("Estoque de Remédios"));
        topo.add(lblResumo);
        conteudo.add(topo, BorderLayout.NORTH);

        modelo = criarModeloTabela("Remédio", "Estoque atual", "Mínimo", "Situação");
        tabela = criarTabela(modelo);
        DefaultTableCellRenderer centralizado = new DefaultTableCellRenderer();
        centralizado.setHorizontalAlignment(SwingConstants.CENTER);
        tabela.getColumnModel().getColumn(1).setCellRenderer(centralizado);
        tabela.getColumnModel().getColumn(2).setCellRenderer(centralizado);
        tabela.getColumnModel().getColumn(3).setCellRenderer(new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable t, Object valor, boolean selecionado,
                                                           boolean foco, int linha, int coluna) {
                super.getTableCellRendererComponent(t, valor, selecionado, foco, linha, coluna);
                boolean baixo = String.valueOf(valor).startsWith("BAIXO");
                setFont(FONTE_DESTAQUE);
                setForeground(selecionado ? Color.WHITE : (baixo ? VERMELHO : VERDE));
                return this;
            }
        });
        conteudo.add(new JScrollPane(tabela), BorderLayout.CENTER);

        JButton btnVoltar = criarBotao("Voltar", CINZA);
        JButton btnMinimo = criarBotao("Alterar mínimo", AZUL);
        JButton btnRepor = criarBotao("Registrar compra", VERDE);
        btnVoltar.addActionListener(e -> fechar());
        btnMinimo.addActionListener(e -> alterarMinimo());
        btnRepor.addActionListener(e -> reporEstoque());

        JPanel acoes = new JPanel(new FlowLayout(FlowLayout.RIGHT, 12, 0));
        acoes.setOpaque(false);
        acoes.add(btnMinimo);
        acoes.add(btnRepor);

        JPanel botoes = new JPanel(new BorderLayout());
        botoes.setOpaque(false);
        botoes.add(btnVoltar, BorderLayout.WEST);
        botoes.add(acoes, BorderLayout.EAST);
        conteudo.add(botoes, BorderLayout.SOUTH);
    }

    @Override
    public void atualizarTela() {
        modelo.setRowCount(0);
        remediosNaTabela = gerenciadorRemedio.listarRemedios();
        int baixos = 0;
        for (Remedio r : remediosNaTabela) {
            boolean baixo = r.getEstoqueAtual() <= r.getEstoqueMinimo();
            if (baixo) {
                baixos++;
            }
            modelo.addRow(new Object[]{
                r.getNomeTecnico(), r.getEstoqueAtual() + " " + unidadeEstoque(r),
                r.getEstoqueMinimo() + " " + unidadeEstoque(r), baixo ? "BAIXO - comprar" : "OK"
            });
        }
        if (remediosNaTabela.isEmpty()) {
            lblResumo.setText("Nenhum remédio cadastrado ainda.");
            lblResumo.setForeground(TEXTO);
        } else if (baixos == 0) {
            lblResumo.setText("Tudo certo: nenhum remédio está acabando.");
            lblResumo.setForeground(VERDE);
        } else {
            lblResumo.setText("Atenção: " + baixos + " remédio(s) acabando. Providencie a compra.");
            lblResumo.setForeground(VERMELHO);
        }
    }

    // ---------- Ações ----------

    // Soma ao estoque a quantidade comprada
    private void reporEstoque() {
        Remedio r = selecionado();
        if (r == null) {
            return;
        }
        String pergunta = (r.getTipo() == Remedio.TipoRemedio.LIQUIDO)
                ? "Quantas doses de " + r.getNomeTecnico() + " você comprou?\n"
                  + "(exemplo: frasco de 100 ml com dose de 5 ml = 20 doses)"
                : "Quantos comprimidos de " + r.getNomeTecnico() + " você comprou?";
        Integer quantidade = pedirNumero(pergunta);
        if (quantidade == null) {
            return;
        }
        if (quantidade <= 0 || quantidade > 10000) {
            mostrarAviso("Digite uma quantidade entre 1 e 10000.");
            return;
        }
        gerenciadorRemedio.reporEstoque(r.getId(), quantidade);
        atualizarTela();
        mostrarMensagem("Estoque atualizado: agora há " + r.getEstoqueAtual()
                + " " + unidadeEstoque(r) + " de " + r.getNomeTecnico() + ".");
    }

    // Muda a partir de quanto o remédio é considerado "acabando"
    private void alterarMinimo() {
        Remedio r = selecionado();
        if (r == null) {
            return;
        }
        Integer minimo = pedirNumero("Avisar que " + r.getNomeTecnico()
                + " está acabando quando restarem quantos(as) " + unidadeEstoque(r) + "?");
        if (minimo == null) {
            return;
        }
        if (minimo < 0 || minimo > 10000) {
            mostrarAviso("Digite um número entre 0 e 10000.");
            return;
        }
        r.setEstoqueMinimo(minimo);
        gerenciadorRemedio.editarRemedio(r);
        atualizarTela();
    }

    private Remedio selecionado() {
        int linha = tabela.getSelectedRow();
        if (linha < 0) {
            mostrarAviso("Clique em um remédio da lista primeiro.");
            return null;
        }
        return remediosNaTabela.get(linha);
    }

    // Pede um número inteiro num JOptionPane. Devolve null se a pessoa cancelar.
    private Integer pedirNumero(String pergunta) {
        while (true) {
            String resposta = JOptionPane.showInputDialog(frame, pergunta, "Estoque", JOptionPane.QUESTION_MESSAGE);
            if (resposta == null) {
                return null;
            }
            try {
                return Integer.parseInt(resposta.trim());
            } catch (NumberFormatException e) {
                mostrarAviso("Digite apenas números, por exemplo: 30");
            }
        }
    }
}
